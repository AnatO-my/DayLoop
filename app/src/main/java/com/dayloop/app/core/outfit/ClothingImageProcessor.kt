package com.dayloop.app.core.outfit

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import kotlinx.coroutines.tasks.await
import java.util.Locale

class ClothingImageProcessor(private val context: Context) {
    private val segmentOptions = SubjectSegmenterOptions.Builder()
        .enableForegroundBitmap()
        .build()

    private val segmenter = SubjectSegmentation.getClient(segmentOptions)
    private val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

    data class ProcessedClothing(
        val bitmap: Bitmap?,
        val labels: Map<String, String>
    )

    suspend fun processImage(uri: Uri): ProcessedClothing {
        return try {
            val image = InputImage.fromFilePath(context, uri)
            
            // Run segmentation and labeling
            // Note: Subject Segmentation might still be downloading, but labeling is likely available
            val segmentTask = segmenter.process(image)
            val labelTask = labeler.process(image)
            
            val segmentResult = runCatching { segmentTask.await() }.getOrNull()
            val labelsResult = labelTask.await()
            
            val labelsMap = labelsResult.associate { label ->
                label.text.lowercase(Locale.US) to String.format(Locale.US, "%.2f", label.confidence)
            }
            
            ProcessedClothing(
                bitmap = segmentResult?.foregroundBitmap,
                labels = labelsMap
            )
        } catch (e: Exception) {
            e.printStackTrace()
            ProcessedClothing(null, emptyMap())
        }
    }
}
