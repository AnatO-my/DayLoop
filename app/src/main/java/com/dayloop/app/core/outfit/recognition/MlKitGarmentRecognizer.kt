package com.dayloop.app.core.outfit.recognition

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.SystemClock
import androidx.exifinterface.media.ExifInterface
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.math.max

/** Why a photo could not be used. Messages are safe to show; they contain no file details. */
class WardrobeImageException(val userMessage: String, cause: Throwable? = null) : Exception(userMessage, cause)

/** One recognised garment with a transparent-background cut-out, when one could be made. */
class RecognizedGarment(val prediction: GarmentPrediction, val cutout: Bitmap?)

/** [imageWidth] x [imageHeight] is the upright photo the regions refer to (after EXIF rotation). */
class RecognizedPhoto(
    val result: RecognitionResult,
    val garments: List<RecognizedGarment>,
    val imageWidth: Int,
    val imageHeight: Int,
)

/** The recognition boundary the UI depends on; tests and previews can supply a fake. */
interface GarmentRecognizer {
    /**
     * @param region when set, only this part of the photo is analysed and exactly one garment
     *   is returned for it (used when the user draws around an item the recogniser missed)
     * @throws WardrobeImageException when the photo cannot be read at all.
     */
    suspend fun recognize(uri: Uri, region: NormalizedBox? = null): RecognizedPhoto
}

/**
 * On-device recognition. Nothing leaves the phone: ML Kit runs locally and the app has no
 * INTERNET permission. Works offline, except that Google Play services downloads the
 * background-removal model once; until then items are analysed without a cut-out.
 *
 * Photos, labels and results are never logged.
 */
class MlKitGarmentRecognizer(private val context: Context) : GarmentRecognizer {
    private val shapeClassifier: ShapeClassifier? by lazy {
        // A missing or corrupt asset degrades to label-only recognition instead of crashing.
        runCatching { context.assets.open(ShapeModelWeights.ASSET_PATH).use(ShapeModelWeights::read) }
            .getOrNull()?.let(::ShapeClassifier)
    }
    private val labeler by lazy { ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS) }
    private val segmenter by lazy {
        SubjectSegmentation.getClient(
            SubjectSegmenterOptions.Builder()
                .enableMultipleSubjects(
                    SubjectSegmenterOptions.SubjectResultOptions.Builder().enableConfidenceMask().build()
                )
                .build()
        )
    }

    override suspend fun recognize(uri: Uri, region: NormalizedBox?): RecognizedPhoto = withContext(Dispatchers.Default) {
        val started = SystemClock.elapsedRealtime()
        val full = withContext(Dispatchers.IO) { loadOriented(uri) }
        val fullWidth = full.width
        val fullHeight = full.height
        val bitmap = if (region == null) full else crop(full, region)
        try {
            val notes = mutableSetOf<RecognitionNote>()
            if (max(bitmap.width, bitmap.height) < RECOMMENDED_EDGE) notes += RecognitionNote.SMALL_IMAGE
            val image = InputImage.fromBitmap(bitmap, 0)

            val labels = optional { labeler.process(image).await() }
                ?.map { ImageLabel(it.text, it.confidence) }.orEmpty()
            val evidence = LabelEvidenceMapper.map(labels)

            val subjects = optional { segmenter.process(image).await() }?.subjects.orEmpty()
                .filter { it.width > 0 && it.height > 0 && it.confidenceMask != null }
                .filter { it.width.toLong() * it.height >= RecognitionPolicy.MIN_SUBJECT_AREA * bitmap.width * bitmap.height }
                .sortedByDescending { it.width.toLong() * it.height }
                .take(RecognitionPolicy.MAX_GARMENTS)

            val garments = if (subjects.isEmpty()) {
                notes += RecognitionNote.NO_CUTOUT
                listOf(RecognizedGarment(wholeImage(bitmap, evidence), cutout = null))
            } else {
                if (subjects.size > 1) notes += RecognitionNote.SEVERAL_ITEMS
                // Image-level labels cannot be assigned to one of several items; only the
                // "person present" signal is shared between them.
                val shared = if (subjects.size == 1) evidence else LabelEvidence.NONE.copy(personCue = evidence.personCue)
                subjects.mapNotNull { subject ->
                    val left = subject.startX.coerceIn(0, bitmap.width - 1)
                    val top = subject.startY.coerceIn(0, bitmap.height - 1)
                    val width = subject.width.coerceAtMost(bitmap.width - left)
                    val height = subject.height.coerceAtMost(bitmap.height - top)
                    val buffer = subject.confidenceMask ?: return@mapNotNull null
                    if (width != subject.width || height != subject.height || buffer.capacity() < width * height) return@mapNotNull null
                    val mask = FloatArray(width * height)
                    buffer.rewind()
                    buffer.get(mask)
                    val pixels = IntArray(width * height)
                    bitmap.getPixels(pixels, 0, width, left, top, width, height)
                    val luma = FloatArray(pixels.size) { ColorMath.luma(pixels[it]) }
                    val probabilities = shapeClassifier?.let { classifier ->
                        ShapeInputBuilder.build(mask, luma, width, height)?.let(classifier::probabilities)
                    }
                    val prediction = PredictionFusion.fuse(
                        shapeProbabilities = probabilities,
                        labels = shared,
                        colors = ColorAnalyzer.analyze(pixels, mask),
                        region = NormalizedBox(
                            left.toFloat() / bitmap.width, top.toFloat() / bitmap.height,
                            (left + width).toFloat() / bitmap.width, (top + height).toFloat() / bitmap.height,
                        ),
                    )
                    for (i in pixels.indices) if (mask[i] < 0.5f) pixels[i] = 0
                    RecognizedGarment(prediction, Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888))
                }.ifEmpty {
                    notes += RecognitionNote.NO_CUTOUT
                    listOf(RecognizedGarment(wholeImage(bitmap, evidence), cutout = null))
                }
            }

            val models = buildList {
                if (shapeClassifier != null) add(ModelInfo(ShapeModelWeights.MODEL_ID, ShapeModelWeights.MODEL_VERSION, "garment shape"))
                add(ModelInfo(LabelEvidenceMapper.MODEL_ID, MLKIT_LABELING_VERSION, "image labels"))
                add(ModelInfo("mlkit-subject-segmentation", MLKIT_SEGMENTATION_VERSION, "background removal"))
            }
            // A drawn region is one item by definition: keep the largest find and report the drawn box.
            val found = if (region == null) garments
            else listOf(garments.first().let {
                // Without a cut-out, keep the drawn area itself as the item's picture.
                RecognizedGarment(it.prediction.copy(region = region), it.cutout ?: bitmap.copy(Bitmap.Config.ARGB_8888, false))
            })
            RecognizedPhoto(
                RecognitionResult(found.map { it.prediction }, notes, models, SystemClock.elapsedRealtime() - started),
                found, fullWidth, fullHeight,
            )
        } finally {
            if (bitmap !== full) bitmap.recycle()
            full.recycle()
        }
    }

    private fun crop(full: Bitmap, region: NormalizedBox): Bitmap {
        val left = (region.left * full.width).toInt().coerceIn(0, full.width - 1)
        val top = (region.top * full.height).toInt().coerceIn(0, full.height - 1)
        val width = ((region.right - region.left) * full.width).toInt().coerceIn(1, full.width - left)
        val height = ((region.bottom - region.top) * full.height).toInt().coerceIn(1, full.height - top)
        return Bitmap.createBitmap(full, left, top, width, height)
    }

    /** Colour and labels for the photo as a whole, using the centre to avoid most background. */
    private fun wholeImage(bitmap: Bitmap, evidence: LabelEvidence): GarmentPrediction {
        val width = max(1, bitmap.width * 6 / 10)
        val height = max(1, bitmap.height * 6 / 10)
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, (bitmap.width - width) / 2, (bitmap.height - height) / 2, width, height)
        return PredictionFusion.fuse(null, evidence, ColorAnalyzer.analyze(pixels), extraNotes = setOf(RecognitionNote.NO_CUTOUT))
    }

    /** ML Kit failures (model not yet downloaded, no Play services) are an expected state. */
    private suspend fun <T> optional(block: suspend () -> T): T? = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    }

    /** Bounded decode, EXIF orientation applied, longest edge at most [MAX_EDGE]. */
    private fun loadOriented(uri: Uri): Bitmap {
        try {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            // A bounds-only decode deliberately returns no Bitmap. Check whether the stream
            // opened before decoding instead; otherwise every valid photo is reported as
            // unreadable here.
            val boundsStream = resolver.openInputStream(uri)
                ?: throw WardrobeImageException("That photo could not be opened.")
            boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw WardrobeImageException("That file is not a supported image.")
            if (max(bounds.outWidth, bounds.outHeight) < MIN_EDGE) throw WardrobeImageException("That photo is too small to recognise.")
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_EDGE) sample *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = sample; inPreferredConfig = Bitmap.Config.ARGB_8888 }
            val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                ?: throw WardrobeImageException("That photo could not be read.")
            val orientation = resolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.postRotate(90f); matrix.postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.postRotate(270f); matrix.postScale(-1f, 1f) }
            }
            val scale = MAX_EDGE.toFloat() / max(decoded.width, decoded.height)
            if (scale < 1f) matrix.postScale(scale, scale)
            val oriented = if (matrix.isIdentity) decoded else
                Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            if (oriented !== decoded) decoded.recycle()
            return if (oriented.config == Bitmap.Config.ARGB_8888) oriented
            else oriented.copy(Bitmap.Config.ARGB_8888, false).also { oriented.recycle() }
        } catch (known: WardrobeImageException) {
            throw known
        } catch (memory: OutOfMemoryError) {
            throw WardrobeImageException("That photo is too large for this device to process.", memory)
        } catch (other: Exception) {
            throw WardrobeImageException("That photo could not be read.", other)
        }
    }

    private companion object {
        const val MAX_EDGE = 1024
        const val MIN_EDGE = 96
        /** ML Kit recommends at least 512x512 for subject segmentation. */
        const val RECOMMENDED_EDGE = 512
        const val MLKIT_LABELING_VERSION = "17.0.9"
        const val MLKIT_SEGMENTATION_VERSION = "16.0.0-beta1"
    }
}
