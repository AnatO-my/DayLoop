package com.dayloop.app.feature.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.tooling.preview.Preview
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

enum class MascotMood {
    NEUTRAL,
    FOCUSED,
    ACCOMPLISHED,
    OVERWHELMED,
    SUGGESTING,
    EXCITED,
    SLEEPY,
    ENERGETIC,
    STARGAZING
}

/*
 * ============================================================================
 * EXPRESSION MODEL
 * ============================================================================
 *
 * Each mood supplies a static destination pose.
 *
 * The destination pose is animated independently from:
 *
 * 1. idle motion,
 * 2. gaze behaviour,
 * 3. one-shot reactions.
 *
 * This keeps mood changes smooth instead of replacing an entire animation
 * instantly.
 */

private data class MascotExpression(
    val leftEyeWidth: Float = 26.243f,
    val leftEyeHeight: Float = 26.243f,
    val rightEyeWidth: Float = 26.243f,
    val rightEyeHeight: Float = 26.243f,

    val leftEyeRotation: Float = 0f,
    val rightEyeRotation: Float = 0f,

    val restingClosure: Float = 0f,
    val pupilScale: Float = 1f,

    val focusBrows: Float = 0f,
    val worryBrows: Float = 0f,
    val happyEyes: Float = 0f,

    val cheeks: Float = 0f,
    val sparkles: Float = 0f,
    val suggestion: Float = 0f,
    val sweat: Float = 0f,
    val stress: Float = 0f,

    val bodyY: Float = 0f,
    val bodyRotation: Float = 0f,
    val bodyScaleX: Float = 1f,
    val bodyScaleY: Float = 1f,

    /*
     * General animation intensity.
     *
     * 0 = calm / almost still.
     * 1 = maximum kinetic energy.
     */
    val energy: Float = 0.15f
)

private fun expressionFor(
    mood: MascotMood
): MascotExpression {
    return when (mood) {

        MascotMood.NEUTRAL -> MascotExpression(
            energy = 0.12f
        )

        MascotMood.FOCUSED -> MascotExpression(
            leftEyeWidth = 27f,
            leftEyeHeight = 23f,
            rightEyeWidth = 27f,
            rightEyeHeight = 23f,

            leftEyeRotation = 2.5f,
            rightEyeRotation = -2.5f,

            restingClosure = 0.25f,
            pupilScale = 0.94f,

            focusBrows = 1f,

            bodyY = 0.7f,
            bodyScaleY = 0.997f,

            energy = 0.08f
        )

        MascotMood.ACCOMPLISHED -> MascotExpression(
            leftEyeWidth = 25f,
            leftEyeHeight = 21f,
            rightEyeWidth = 25f,
            rightEyeHeight = 21f,

            restingClosure = 0.1f,

            happyEyes = 1f,
            cheeks = 1f,
            sparkles = 1f,

            bodyY = -1f,

            energy = 0.50f
        )

        MascotMood.OVERWHELMED -> MascotExpression(
            leftEyeWidth = 30f,
            leftEyeHeight = 31f,

            rightEyeWidth = 24f,
            rightEyeHeight = 27f,

            leftEyeRotation = -3f,
            rightEyeRotation = 4f,

            restingClosure = 0.04f,
            pupilScale = 1.05f,

            worryBrows = 1f,

            sweat = 1f,
            stress = 1f,

            bodyY = 1.5f,
            bodyRotation = -0.4f,
            bodyScaleY = 0.985f,

            energy = 0.85f
        )

        MascotMood.SUGGESTING -> MascotExpression(
            leftEyeWidth = 27f,
            leftEyeHeight = 27f,
            rightEyeWidth = 27f,
            rightEyeHeight = 27f,

            pupilScale = 0.92f,

            suggestion = 1f,

            bodyY = -1f,
            bodyRotation = 0.45f,

            energy = 0.30f
        )

        MascotMood.EXCITED -> MascotExpression(
            leftEyeWidth = 29f,
            leftEyeHeight = 29f,
            rightEyeWidth = 29f,
            rightEyeHeight = 29f,

            pupilScale = 1.08f,

            cheeks = 0.85f,
            sparkles = 0.35f,

            bodyScaleX = 1.015f,
            bodyScaleY = 1.025f,

            energy = 0.75f
        )

        MascotMood.SLEEPY -> MascotExpression(
            leftEyeHeight = 24f,
            rightEyeHeight = 24f,

            restingClosure = 0.66f,
            pupilScale = 0.92f,

            bodyY = 2f,
            bodyRotation = -0.7f,
            bodyScaleY = 0.992f,

            energy = 0.02f
        )

        MascotMood.ENERGETIC -> MascotExpression(
            leftEyeWidth = 29f,
            leftEyeHeight = 29f,
            rightEyeWidth = 29f,
            rightEyeHeight = 29f,

            pupilScale = 1.08f,

            cheeks = 0.65f,

            bodyScaleX = 1.02f,
            bodyScaleY = 1.03f,

            energy = 1f
        )

        MascotMood.STARGAZING -> MascotExpression(
            leftEyeWidth = 27.5f,
            leftEyeHeight = 28f,
            rightEyeWidth = 27.5f,
            rightEyeHeight = 28f,

            pupilScale = 0.85f,

            sparkles = 0.45f,

            bodyRotation = -1f,

            energy = 0.10f
        )
    }
}

@Composable
fun DayLoopMascot(
    modifier: Modifier = Modifier,
    mood: MascotMood = MascotMood.NEUTRAL,
    lookingDown: Boolean = false,
    reducedMotion: Boolean = false
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val accent = MaterialTheme.colorScheme.primary

    val frame = remember {
        PathParser().parsePathString(
            """
            M217.025,270.145
            C217.025,270.145 220.272,288.459 230.764,302.548
            C218.113,320.778 193.171,332.038 170.982,332.038
            C139.483,332.038 95.684,307.441 95.258,256.313
            C94.832,205.186 139.677,180.362 170.982,180.362
            C205.32,180.362 246.706,207.45 246.706,255.811
            C246.706,281.06 266.136,304.445 294.838,304.445
            C321.784,304.445 342.968,282.946 342.968,256
            C342.968,226.689 317.709,208.182 294.838,208.182
            C258.987,208.182 248.941,241.782 248.941,241.782
            C248.941,241.782 245.2,222.203 235.202,209.665
            C248.88,193.464 269.224,180.362 294.838,180.362
            C338.187,180.362 370.562,216.292 370.562,256.086
            C370.562,289.181 345.028,331.81 294.838,331.81
            C263.729,331.81 221.382,310.377 219.114,256.313
            C219.114,230.967 198.925,207.955 170.982,207.955
            C146.709,207.955 122.851,227.421 122.851,256.313
            C122.851,279.441 141.826,304.192 169.409,304.192
            C207.868,304.192 217.025,270.145 217.025,270.145 Z
            """.trimIndent()
        ).toPath()
    }

    /*
     * =========================================================================
     * INTERPOLATED MOOD POSE
     * =========================================================================
     */

    val target = expressionFor(mood)

    val poseSpec: FiniteAnimationSpec<Float> =
        if (reducedMotion) {
            snap()
        } else {
            spring(
                dampingRatio = 0.78f,
                stiffness = 430f
            )
        }

    val leftEyeWidth by animateFloatAsState(
        targetValue = target.leftEyeWidth,
        animationSpec = poseSpec,
        label = "Left eye width"
    )

    val leftEyeHeight by animateFloatAsState(
        targetValue = target.leftEyeHeight,
        animationSpec = poseSpec,
        label = "Left eye height"
    )

    val rightEyeWidth by animateFloatAsState(
        targetValue = target.rightEyeWidth,
        animationSpec = poseSpec,
        label = "Right eye width"
    )

    val rightEyeHeight by animateFloatAsState(
        targetValue = target.rightEyeHeight,
        animationSpec = poseSpec,
        label = "Right eye height"
    )

    val leftEyeRotation by animateFloatAsState(
        targetValue = target.leftEyeRotation,
        animationSpec = poseSpec,
        label = "Left eye rotation"
    )

    val rightEyeRotation by animateFloatAsState(
        targetValue = target.rightEyeRotation,
        animationSpec = poseSpec,
        label = "Right eye rotation"
    )

    val restingClosure by animateFloatAsState(
        targetValue = target.restingClosure,
        animationSpec = poseSpec,
        label = "Resting closure"
    )

    val pupilScale by animateFloatAsState(
        targetValue = target.pupilScale,
        animationSpec = poseSpec,
        label = "Pupil scale"
    )

    val focusBrows by animateFloatAsState(
        targetValue = target.focusBrows,
        animationSpec = poseSpec,
        label = "Focus brows"
    )

    val worryBrows by animateFloatAsState(
        targetValue = target.worryBrows,
        animationSpec = poseSpec,
        label = "Worry brows"
    )

    val happyEyes by animateFloatAsState(
        targetValue = target.happyEyes,
        animationSpec = poseSpec,
        label = "Happy eyes"
    )

    val cheekAmount by animateFloatAsState(
        targetValue = target.cheeks,
        animationSpec = poseSpec,
        label = "Cheeks"
    )

    val sparkleAmount by animateFloatAsState(
        targetValue = target.sparkles,
        animationSpec = poseSpec,
        label = "Sparkles"
    )

    val suggestionAmount by animateFloatAsState(
        targetValue = target.suggestion,
        animationSpec = poseSpec,
        label = "Suggestion"
    )

    val sweatAmount by animateFloatAsState(
        targetValue = target.sweat,
        animationSpec = poseSpec,
        label = "Sweat"
    )

    val stressAmount by animateFloatAsState(
        targetValue = target.stress,
        animationSpec = poseSpec,
        label = "Stress"
    )

    val energy by animateFloatAsState(
        targetValue = target.energy,
        animationSpec = poseSpec,
        label = "Energy"
    )

    val baseBodyY by animateFloatAsState(
        targetValue = target.bodyY,
        animationSpec = poseSpec,
        label = "Body Y"
    )

    val baseBodyRotation by animateFloatAsState(
        targetValue = target.bodyRotation,
        animationSpec = poseSpec,
        label = "Body rotation"
    )

    val baseBodyScaleX by animateFloatAsState(
        targetValue = target.bodyScaleX,
        animationSpec = poseSpec,
        label = "Body scale X"
    )

    val baseBodyScaleY by animateFloatAsState(
        targetValue = target.bodyScaleY,
        animationSpec = poseSpec,
        label = "Body scale Y"
    )

    /*
     * =========================================================================
     * CONTEXTUAL LOOK-DOWN
     * =========================================================================
     */

    val lookDownAmount by animateFloatAsState(
        targetValue = if (lookingDown) 1f else 0f,
        animationSpec =
            if (reducedMotion) {
                snap()
            } else {
                spring(
                    dampingRatio = 0.82f,
                    stiffness = 500f
                )
            },
        label = "Looking down"
    )

    /*
     * =========================================================================
     * ONE-SHOT REACTION
     * =========================================================================
     *
     * This is intentionally separate from the looping idle animation.
     *
     * Accomplished therefore celebrates once when entered instead of jumping
     * forever.
     */

    val reaction = remember {
        Animatable(0f)
    }

    LaunchedEffect(
        mood,
        reducedMotion
    ) {
        reaction.stop()

        if (reducedMotion) {
            reaction.snapTo(0f)
            return@LaunchedEffect
        }

        reaction.snapTo(0f)

        when (mood) {

            MascotMood.ACCOMPLISHED -> {
                reaction.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 90,
                        easing = FastOutSlowInEasing
                    )
                )

                reaction.animateTo(
                    targetValue = -0.65f,
                    animationSpec = tween(
                        durationMillis = 130
                    )
                )

                reaction.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }

            MascotMood.SUGGESTING -> {
                reaction.animateTo(
                    targetValue = 0.65f,
                    animationSpec = tween(110)
                )

                reaction.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = 0.55f,
                        stiffness = 400f
                    )
                )
            }

            MascotMood.EXCITED -> {
                reaction.animateTo(
                    targetValue = -0.7f,
                    animationSpec = tween(120)
                )

                reaction.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = 0.45f,
                        stiffness = 380f
                    )
                )
            }

            MascotMood.ENERGETIC -> {
                reaction.animateTo(
                    targetValue = -1f,
                    animationSpec = tween(100)
                )

                reaction.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = 0.38f,
                        stiffness = 430f
                    )
                )
            }

            MascotMood.OVERWHELMED -> {
                reaction.animateTo(
                    targetValue = 0.45f,
                    animationSpec = tween(80)
                )

                reaction.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = 0.72f,
                        stiffness = 520f
                    )
                )
            }

            else -> Unit
        }
    }

    /*
     * =========================================================================
     * IDLE CLOCK
     * =========================================================================
     */

    val phase = if (reducedMotion) {
        0f
    } else {
        val transition = rememberInfiniteTransition(
            label = "DayLoop mascot idle"
        )

        val duration = when (mood) {
            MascotMood.NEUTRAL -> 7600
            MascotMood.FOCUSED -> 9000
            MascotMood.ACCOMPLISHED -> 6200
            MascotMood.OVERWHELMED -> 5200
            MascotMood.SUGGESTING -> 6800
            MascotMood.EXCITED -> 4600
            MascotMood.SLEEPY -> 12000
            MascotMood.ENERGETIC -> 5000
            MascotMood.STARGAZING -> 9000
        }

        val time by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = duration,
                    easing = LinearEasing
                )
            ),
            label = "Mascot idle phase"
        )

        time
    }

    /*
     * =========================================================================
     * ANIMATION HELPERS
     * =========================================================================
     */

    fun circularDistance(
        a: Float,
        b: Float
    ): Float {
        val direct = abs(a - b)
        return min(direct, 1f - direct)
    }

    fun pulseAt(
        center: Float,
        width: Float
    ): Float {
        val distance = circularDistance(
            phase,
            center
        )

        val value =
            (
                    1f -
                            distance / width
                    ).coerceIn(
                    0f,
                    1f
                )

        return value *
                value *
                (3f - 2f * value)
    }

    fun blinkAmount(
        vararg moments: Float,
        width: Float = 0.022f
    ): Float {
        var amount = 0f

        moments.forEach { moment ->
            amount = maxOf(
                amount,
                pulseAt(
                    moment,
                    width
                )
            )
        }

        return amount
    }

    fun smoothStep(
        value: Float
    ): Float {
        val t = value.coerceIn(
            0f,
            1f
        )

        return t *
                t *
                (3f - 2f * t)
    }

    fun mix(
        start: Float,
        end: Float,
        fraction: Float
    ): Float {
        return start +
                (end - start) *
                fraction
    }

    fun moveBetween(
        startValue: Float,
        endValue: Float,
        startTime: Float,
        endTime: Float
    ): Float {
        if (phase <= startTime) {
            return startValue
        }

        if (phase >= endTime) {
            return endValue
        }

        val progress =
            (phase - startTime) /
                    (endTime - startTime)

        return mix(
            startValue,
            endValue,
            smoothStep(progress)
        )
    }

    fun gazeStep(
        holdA: Float,
        moveAB: Float,
        holdB: Float,
        moveBC: Float,
        a: Float,
        b: Float,
        c: Float
    ): Float {
        return when {

            phase < holdA -> a

            phase < moveAB -> {
                mix(
                    a,
                    b,
                    smoothStep(
                        (phase - holdA) /
                                (moveAB - holdA)
                    )
                )
            }

            phase < holdB -> b

            phase < moveBC -> {
                mix(
                    b,
                    c,
                    smoothStep(
                        (phase - holdB) /
                                (moveBC - holdB)
                    )
                )
            }

            else -> c
        }
    }

    /*
     * =========================================================================
     * BLINK LANGUAGE
     * =========================================================================
     */

    val blink = when (mood) {

        MascotMood.NEUTRAL -> {
            blinkAmount(
                0.31f,
                0.67f,
                0.715f
            )
        }

        MascotMood.FOCUSED -> {
            blinkAmount(
                0.39f,
                0.78f,
                width = 0.019f
            )
        }

        MascotMood.ACCOMPLISHED -> 0f

        MascotMood.OVERWHELMED -> {
            blinkAmount(
                0.12f,
                0.27f,
                0.43f,
                0.61f,
                0.81f,
                0.855f,
                width = 0.017f
            )
        }

        MascotMood.SUGGESTING -> {
            blinkAmount(
                0.18f,
                0.59f
            )
        }

        MascotMood.EXCITED -> {
            blinkAmount(
                0.52f,
                width = 0.017f
            )
        }

        MascotMood.SLEEPY -> {
            blinkAmount(
                0.38f,
                0.72f,
                width = 0.052f
            )
        }

        MascotMood.ENERGETIC -> {
            blinkAmount(
                0.44f,
                width = 0.015f
            )
        }

        MascotMood.STARGAZING -> {
            blinkAmount(
                0.52f,
                width = 0.018f
            )
        }
    }

    val closure =
        (
                restingClosure +
                        blink *
                        (1f - restingClosure)
                ).coerceIn(
                0f,
                1f
            )

    /*
     * =========================================================================
     * GAZE LANGUAGE
     * =========================================================================
     */

    var targetLeftGazeX = 0f
    var targetRightGazeX = 0f

    var targetLeftGazeY = 0f
    var targetRightGazeY = 0f

    when (mood) {

        MascotMood.NEUTRAL -> {
            val gaze = when {

                phase < 0.18f -> -4f

                phase < 0.23f -> {
                    moveBetween(
                        -4f,
                        7f,
                        0.18f,
                        0.23f
                    )
                }

                phase < 0.49f -> 7f

                phase < 0.54f -> {
                    moveBetween(
                        7f,
                        1f,
                        0.49f,
                        0.54f
                    )
                }

                phase < 0.77f -> 1f

                phase < 0.83f -> {
                    moveBetween(
                        1f,
                        -4f,
                        0.77f,
                        0.83f
                    )
                }

                else -> -4f
            }

            targetLeftGazeX = gaze
            targetRightGazeX = gaze

            targetLeftGazeY =
                sin(
                    phase *
                            2f *
                            PI
                ).toFloat() * 1.1f

            targetRightGazeY =
                targetLeftGazeY
        }

        MascotMood.FOCUSED -> {
            targetLeftGazeX =
                3.5f +
                        sin(
                            phase *
                                    2f *
                                    PI
                        ).toFloat() * 0.55f

            targetRightGazeX =
                -3.5f +
                        sin(
                            phase *
                                    2f *
                                    PI
                        ).toFloat() * 0.55f

            targetLeftGazeY = 3f
            targetRightGazeY = 3f
        }

        MascotMood.ACCOMPLISHED -> {
            targetLeftGazeX = 0f
            targetRightGazeX = 0f

            targetLeftGazeY = -1f
            targetRightGazeY = -1f
        }

        MascotMood.OVERWHELMED -> {
            when {

                phase < 0.14f -> {
                    targetLeftGazeX = -10f
                    targetRightGazeX = 7f

                    targetLeftGazeY = -3f
                    targetRightGazeY = 4f
                }

                phase < 0.19f -> {
                    targetLeftGazeX =
                        moveBetween(
                            -10f,
                            10f,
                            0.14f,
                            0.19f
                        )

                    targetRightGazeX =
                        moveBetween(
                            7f,
                            -5f,
                            0.14f,
                            0.19f
                        )

                    targetLeftGazeY =
                        moveBetween(
                            -3f,
                            2f,
                            0.14f,
                            0.19f
                        )

                    targetRightGazeY =
                        moveBetween(
                            4f,
                            -5f,
                            0.14f,
                            0.19f
                        )
                }

                phase < 0.41f -> {
                    targetLeftGazeX = 10f
                    targetRightGazeX = -5f

                    targetLeftGazeY = 2f
                    targetRightGazeY = -5f
                }

                phase < 0.47f -> {
                    targetLeftGazeX =
                        moveBetween(
                            10f,
                            -6f,
                            0.41f,
                            0.47f
                        )

                    targetRightGazeX =
                        moveBetween(
                            -5f,
                            8f,
                            0.41f,
                            0.47f
                        )

                    targetLeftGazeY =
                        moveBetween(
                            2f,
                            5f,
                            0.41f,
                            0.47f
                        )

                    targetRightGazeY =
                        moveBetween(
                            -5f,
                            -2f,
                            0.41f,
                            0.47f
                        )
                }

                phase < 0.72f -> {
                    targetLeftGazeX = -6f
                    targetRightGazeX = 8f

                    targetLeftGazeY = 5f
                    targetRightGazeY = -2f
                }

                else -> {
                    targetLeftGazeX = -10f
                    targetRightGazeX = 7f

                    targetLeftGazeY = -3f
                    targetRightGazeY = 4f
                }
            }
        }

        MascotMood.SUGGESTING -> {
            val glance =
                gazeStep(
                    holdA = 0.18f,
                    moveAB = 0.24f,
                    holdB = 0.58f,
                    moveBC = 0.64f,
                    a = 1f,
                    b = 5f,
                    c = -2f
                )

            targetLeftGazeX = glance
            targetRightGazeX = glance

            /*
             * Curious upward glance.
             */
            targetLeftGazeY = -4f
            targetRightGazeY = -4f
        }

        MascotMood.EXCITED -> {
            val glance =
                gazeStep(
                    holdA = 0.16f,
                    moveAB = 0.20f,
                    holdB = 0.43f,
                    moveBC = 0.48f,
                    a = -6f,
                    b = 8f,
                    c = 1f
                )

            targetLeftGazeX = glance
            targetRightGazeX = glance

            targetLeftGazeY =
                -2f +
                        sin(
                            phase *
                                    4f *
                                    PI
                        ).toFloat() * 1.5f

            targetRightGazeY =
                targetLeftGazeY
        }

        MascotMood.SLEEPY -> {
            targetLeftGazeX =
                sin(
                    phase *
                            PI
                ).toFloat() * 1.2f

            targetRightGazeX =
                targetLeftGazeX

            targetLeftGazeY = 5f
            targetRightGazeY = 5f
        }

        MascotMood.ENERGETIC -> {
            val glance =
                gazeStep(
                    holdA = 0.12f,
                    moveAB = 0.17f,
                    holdB = 0.38f,
                    moveBC = 0.44f,
                    a = 6f,
                    b = -7f,
                    c = 3f
                )

            targetLeftGazeX = glance
            targetRightGazeX = glance

            targetLeftGazeY =
                sin(
                    phase *
                            4f *
                            PI
                ).toFloat() * 2f

            targetRightGazeY =
                targetLeftGazeY
        }

        MascotMood.STARGAZING -> {
            targetLeftGazeY = -12f
            targetRightGazeY = -12f

            val skyDrift =
                sin(
                    phase *
                            2f *
                            PI
                ).toFloat() * 5f

            targetLeftGazeX = skyDrift
            targetRightGazeX = skyDrift
        }
    }

    /*
     * A short spring smooths gaze changes and therefore also smooths
     * transitions between mood-specific gaze behaviours.
     */

    val gazeSpec: FiniteAnimationSpec<Float> =
        if (reducedMotion) {
            snap()
        } else {
            spring(
                dampingRatio = 0.88f,
                stiffness = 900f
            )
        }

    val animatedLeftGazeX by animateFloatAsState(
        targetValue = targetLeftGazeX,
        animationSpec = gazeSpec,
        label = "Left gaze X"
    )

    val animatedRightGazeX by animateFloatAsState(
        targetValue = targetRightGazeX,
        animationSpec = gazeSpec,
        label = "Right gaze X"
    )

    val animatedLeftGazeY by animateFloatAsState(
        targetValue = targetLeftGazeY,
        animationSpec = gazeSpec,
        label = "Left gaze Y"
    )

    val animatedRightGazeY by animateFloatAsState(
        targetValue = targetRightGazeY,
        animationSpec = gazeSpec,
        label = "Right gaze Y"
    )

    /*
     * Contextual gaze is layered over emotional gaze.
     *
     * lookingDown no longer teleports the pupils.
     */

    val leftGazeX =
        animatedLeftGazeX *
                (1f - lookDownAmount)

    val rightGazeX =
        animatedRightGazeX *
                (1f - lookDownAmount)

    val leftGazeY =
        mix(
            animatedLeftGazeY,
            12f,
            lookDownAmount
        )

    val rightGazeY =
        mix(
            animatedRightGazeY,
            12f,
            lookDownAmount
        )

    /*
     * =========================================================================
     * BODY LANGUAGE
     * =========================================================================
     */

    var bodyX = 0f
    var bodyY = baseBodyY

    var bodyRotation =
        baseBodyRotation

    var bodyScaleX =
        baseBodyScaleX

    var bodyScaleY =
        baseBodyScaleY

    if (!reducedMotion) {

        /*
         * Shared breathing motion.
         *
         * Energetic states breathe less visibly because their stronger
         * motion masks it.
         */
        val breath =
            sin(
                phase *
                        2f *
                        PI
            ).toFloat()

        bodyY +=
            breath *
                    0.8f *
                    (1f - energy * 0.55f)

        when (mood) {

            MascotMood.NEUTRAL -> {
                bodyScaleY *=
                    1f +
                            breath *
                            0.0035f
            }

            MascotMood.FOCUSED -> {
                bodyY +=
                    sin(
                        phase *
                                2f *
                                PI
                    ).toFloat() * 0.2f

                bodyRotation +=
                    sin(
                        phase *
                                2f *
                                PI
                    ).toFloat() * 0.12f
            }

            MascotMood.ACCOMPLISHED -> {
                /*
                 * Accomplished idle is calm after the one-shot celebration.
                 */
                bodyY +=
                    sin(
                        phase *
                                2f *
                                PI
                    ).toFloat() * 0.65f
            }

            MascotMood.OVERWHELMED -> {
                /*
                 * Two frequencies prevent mechanical vibration.
                 */
                bodyX +=
                    (
                            sin(
                                phase *
                                        22f *
                                        PI
                            ) * 1.2 +
                                    sin(
                                        phase *
                                                34f *
                                                PI
                                    ) * 0.35
                            ).toFloat()

                bodyY +=
                    cos(
                        phase *
                                18f *
                                PI
                    ).toFloat() * 0.55f

                bodyRotation +=
                    sin(
                        phase *
                                20f *
                                PI
                    ).toFloat() * 0.45f
            }

            MascotMood.SUGGESTING -> {
                /*
                 * Gentle curious lean rather than repetitive bouncing.
                 */
                bodyRotation +=
                    sin(
                        phase *
                                2f *
                                PI
                    ).toFloat() * 0.55f

                bodyY +=
                    sin(
                        phase *
                                2f *
                                PI
                    ).toFloat() * 1.1f
            }

            MascotMood.EXCITED -> {
                /*
                 * Small persistent buoyancy.
                 * The large movement happens only on state entry.
                 */
                bodyY -=
                    abs(
                        sin(
                            phase *
                                    2f *
                                    PI
                        )
                    ).toFloat() * 2.4f

                bodyRotation +=
                    sin(
                        phase *
                                4f *
                                PI
                    ).toFloat() * 0.4f
            }

            MascotMood.SLEEPY -> {
                bodyRotation +=
                    sin(
                        phase *
                                PI
                    ).toFloat() * 0.65f

                bodyY +=
                    sin(
                        phase *
                                PI
                    ).toFloat() * 0.45f
            }

            MascotMood.ENERGETIC -> {
                bodyY -=
                    abs(
                        sin(
                            phase *
                                    2f *
                                    PI
                        )
                    ).toFloat() * 3f

                bodyRotation +=
                    sin(
                        phase *
                                4f *
                                PI
                    ).toFloat() * 0.7f
            }

            MascotMood.STARGAZING -> {
                bodyX +=
                    sin(
                        phase *
                                2f *
                                PI
                    ).toFloat() * 2f

                bodyY +=
                    cos(
                        phase *
                                2f *
                                PI
                    ).toFloat() * 0.6f
            }
        }

        /*
         * ------------------------------------------------------------
         * One-shot reaction layered above idle animation.
         * ------------------------------------------------------------
         */

        when (mood) {

            MascotMood.ACCOMPLISHED -> {
                bodyY +=
                    reaction.value *
                            10f

                bodyScaleX *=
                    1f +
                            reaction.value *
                            0.07f

                bodyScaleY *=
                    1f -
                            reaction.value *
                            0.06f
            }

            MascotMood.SUGGESTING -> {
                bodyY -=
                    reaction.value *
                            3f

                bodyRotation +=
                    reaction.value *
                            1.2f
            }

            MascotMood.EXCITED -> {
                bodyY +=
                    reaction.value *
                            10f

                bodyScaleX *=
                    1f -
                            reaction.value *
                            0.025f

                bodyScaleY *=
                    1f -
                            reaction.value *
                            0.055f
            }

            MascotMood.ENERGETIC -> {
                bodyY +=
                    reaction.value *
                            12f

                bodyScaleX *=
                    1f -
                            reaction.value *
                            0.035f

                bodyScaleY *=
                    1f -
                            reaction.value *
                            0.065f
            }

            MascotMood.OVERWHELMED -> {
                bodyX +=
                    reaction.value *
                            3f

                bodyScaleX *=
                    1f -
                            reaction.value *
                            0.025f
            }

            else -> Unit
        }
    }

    /*
     * =========================================================================
     * DRAW
     * =========================================================================
     */

    Canvas(
        modifier = modifier
    ) {
        val mascotScale =
            min(
                size.width,
                size.height
            ) / 513f

        withTransform({
            translate(
                left =
                    (
                            size.width -
                                    513f * mascotScale
                            ) / 2f,

                top =
                    (
                            size.height -
                                    513f * mascotScale
                            ) / 2f
            )

            scale(
                scaleX = mascotScale,
                scaleY = mascotScale,
                pivot = Offset.Zero
            )
        }) {
            withTransform({
                translate(
                    left = bodyX,
                    top = bodyY
                )

                rotate(
                    degrees = bodyRotation,
                    pivot = Offset(
                        256.5f,
                        256.5f
                    )
                )

                scale(
                    scaleX = bodyScaleX,
                    scaleY = bodyScaleY,
                    pivot = Offset(
                        256.5f,
                        256.5f
                    )
                )
            }) {

                /*
                 * Original infinity body.
                 */
                withTransform({
                    translate(
                        left = -8.702412f,
                        top = -35.173267f
                    )

                    scale(
                        scaleX = 1.136509f,
                        scaleY = 1.136509f,
                        pivot = Offset.Zero
                    )
                }) {
                    drawPath(
                        path = frame,
                        color = ink
                    )
                }

                val leftCx = 187.146f
                val rightCx = 334.348f
                val eyeCy = 257.942f

                /*
                 * ------------------------------------------------------------
                 * ORDINARY EYES
                 * ------------------------------------------------------------
                 *
                 * They smoothly fade away as accomplished happy eyes enter.
                 */

                val ordinaryEyeAlpha =
                    (
                            1f -
                                    happyEyes
                            ).coerceIn(
                            0f,
                            1f
                        )

                drawCutoutEye(
                    cx = leftCx,
                    cy = eyeCy,
                    radiusX = leftEyeWidth,
                    radiusY = leftEyeHeight,
                    gazeX = leftGazeX,
                    gazeY = leftGazeY,
                    closure = closure,
                    rotation = leftEyeRotation,
                    pupilRadius = 8f * pupilScale,
                    colour = ink,
                    alpha = ordinaryEyeAlpha
                )

                drawCutoutEye(
                    cx = rightCx,
                    cy = eyeCy,
                    radiusX = rightEyeWidth,
                    radiusY = rightEyeHeight,
                    gazeX = rightGazeX,
                    gazeY = rightGazeY,
                    closure = closure,
                    rotation = rightEyeRotation,
                    pupilRadius = 8f * pupilScale,
                    colour = ink,
                    alpha = ordinaryEyeAlpha
                )

                /*
                 * ------------------------------------------------------------
                 * FOCUSED BROWS
                 * ------------------------------------------------------------
                 */

                drawFocusedBrows(
                    leftCx = leftCx,
                    rightCx = rightCx,
                    cy = eyeCy,
                    colour = ink,
                    alpha = focusBrows
                )

                /*
                 * ------------------------------------------------------------
                 * OVERWHELMED BROWS
                 * ------------------------------------------------------------
                 */

                drawOverwhelmedBrows(
                    leftCx = leftCx,
                    rightCx = rightCx,
                    cy = eyeCy,
                    colour = ink,
                    alpha = worryBrows
                )

                /*
                 * ------------------------------------------------------------
                 * HAPPY CRESCENT EYES
                 * ------------------------------------------------------------
                 */

                drawHappyEye(
                    cx = leftCx,
                    cy = eyeCy,
                    rotation = 3.5f,
                    colour = ink,
                    alpha = happyEyes
                )

                drawHappyEye(
                    cx = rightCx,
                    cy = eyeCy,
                    rotation = -3.5f,
                    colour = ink,
                    alpha = happyEyes
                )

                /*
                 * ------------------------------------------------------------
                 * CHEEK ACCENTS
                 * ------------------------------------------------------------
                 */

                drawHappyCheeks(
                    leftCx = leftCx,
                    rightCx = rightCx,
                    cy = eyeCy,
                    colour = accent,
                    alpha = cheekAmount
                )

                /*
                 * ------------------------------------------------------------
                 * GENERAL SPARKLES
                 * ------------------------------------------------------------
                 */

                val twinkle =
                    if (reducedMotion) {
                        1f
                    } else {
                        0.82f +
                                0.18f *
                                sin(
                                    phase *
                                            6f *
                                            PI
                                ).toFloat()
                    }

                val generalSparkleAlpha =
                    (
                            sparkleAmount *
                                    twinkle
                            ).coerceIn(
                            0f,
                            1f
                        )

                drawSparkle(
                    center = Offset(
                        132f,
                        193f
                    ),
                    radius = 12f,
                    colour = accent,
                    alpha = generalSparkleAlpha
                )

                drawSparkle(
                    center = Offset(
                        391f,
                        214f
                    ),
                    radius = 9f,
                    colour = accent,
                    alpha =
                        generalSparkleAlpha *
                                0.90f
                )

                drawSparkle(
                    center = Offset(
                        371f,
                        166f
                    ),
                    radius = 6f,
                    colour = accent,
                    alpha =
                        generalSparkleAlpha *
                                0.70f
                )

                /*
                 * ------------------------------------------------------------
                 * SUGGESTION / IDEA SPARKLE
                 * ------------------------------------------------------------
                 */

                val suggestionPulse =
                    if (reducedMotion) {
                        1f
                    } else {
                        0.78f +
                                pulseAt(
                                    center = 0.28f,
                                    width = 0.22f
                                ) * 0.22f
                    }

                val suggestionLift =
                    if (reducedMotion) {
                        0f
                    } else {
                        sin(
                            phase *
                                    2f *
                                    PI
                        ).toFloat() * 2.2f
                    }

                drawSuggestionSparkle(
                    center = Offset(
                        256.5f,
                        150f + suggestionLift
                    ),
                    radius = 23f,
                    colour = accent,
                    alpha =
                        (
                                suggestionAmount *
                                        suggestionPulse
                                ).coerceIn(
                                0f,
                                1f
                            )
                )

                /*
                 * ------------------------------------------------------------
                 * OVERWHELMED ACCESSORIES
                 * ------------------------------------------------------------
                 */

                val sweatMovement =
                    if (reducedMotion) {
                        0f
                    } else {
                        sin(
                            phase *
                                    2f *
                                    PI
                        ).toFloat() * 3f
                    }

                drawSweatDrop(
                    center = Offset(
                        385f,
                        208f + sweatMovement
                    ),
                    colour = accent,
                    alpha = sweatAmount
                )

                drawStressMarks(
                    origin = Offset(
                        123f,
                        207f
                    ),
                    colour = accent,
                    alpha = stressAmount
                )

                /*
                 * ------------------------------------------------------------
                 * SLEEPY ACCESSORY
                 * ------------------------------------------------------------
                 */

                drawSleepMarks(
                    origin = Offset(
                        388f,
                        188f
                    ),
                    colour = accent,
                    alpha =
                        if (mood == MascotMood.SLEEPY) {
                            1f
                        } else {
                            0f
                        }
                )

                /*
                 * ------------------------------------------------------------
                 * ENERGETIC MOTION ACCENTS
                 * ------------------------------------------------------------
                 */

                drawEnergyMarks(
                    origin = Offset(
                        111f,
                        207f
                    ),
                    colour = accent,
                    alpha =
                        if (mood == MascotMood.ENERGETIC) {
                            0.80f
                        } else {
                            0f
                        }
                )
            }
        }
    }
}

/*
 * ============================================================================
 * EYES
 * ============================================================================
 */

private fun DrawScope.drawCutoutEye(
    cx: Float,
    cy: Float,
    radiusX: Float,
    radiusY: Float,
    gazeX: Float,
    gazeY: Float,
    closure: Float,
    rotation: Float = 0f,
    pupilRadius: Float = 8f,
    colour: Color,
    alpha: Float = 1f
) {
    if (alpha <= 0.001f) {
        return
    }

    val eye =
        Path().apply {
            addOval(
                Rect(
                    left = cx - radiusX,
                    top = cy - radiusY,
                    right = cx + radiusX,
                    bottom = cy + radiusY
                )
            )
        }

    val pupilCenterX =
        cx +
                gazeX +
                13f

    val pupilCenterY =
        cy +
                gazeY -
                4f

    val pupil =
        Path().apply {
            addOval(
                Rect(
                    left =
                        pupilCenterX -
                                pupilRadius,

                    top =
                        pupilCenterY -
                                pupilRadius,

                    right =
                        pupilCenterX +
                                pupilRadius,

                    bottom =
                        pupilCenterY +
                                pupilRadius
                )
            )
        }

    val visibleEye =
        Path.combine(
            PathOperation.Difference,
            eye,
            pupil
        )

    val opening =
        (
                radiusY *
                        (1f - closure)
                ).coerceAtLeast(
                0.8f
            )

    rotate(
        degrees = rotation,
        pivot = Offset(
            cx,
            cy
        )
    ) {
        clipRect(
            left = cx - radiusX - 3f,
            top = cy - opening,
            right = cx + radiusX + 3f,
            bottom = cy + opening
        ) {
            drawPath(
                path = visibleEye,
                color =
                    colour.copy(
                        alpha =
                            colour.alpha *
                                    alpha.coerceIn(
                                        0f,
                                        1f
                                    )
                    )
            )
        }
    }
}

/*
 * ============================================================================
 * ACCOMPLISHED / HAPPY
 * ============================================================================
 */

private fun DrawScope.drawHappyEye(
    cx: Float,
    cy: Float,
    rotation: Float,
    colour: Color,
    alpha: Float
) {
    if (alpha <= 0.001f) {
        return
    }

    val eye =
        Path().apply {
            moveTo(
                cx - 23f,
                cy + 4f
            )

            quadraticTo(
                cx,
                cy - 17f,
                cx + 23f,
                cy + 4f
            )
        }

    rotate(
        degrees = rotation,
        pivot = Offset(
            cx,
            cy
        )
    ) {
        drawPath(
            path = eye,
            color =
                colour.copy(
                    alpha =
                        colour.alpha *
                                alpha.coerceIn(
                                    0f,
                                    1f
                                )
                ),
            style = Stroke(
                width = 9.5f,
                cap = StrokeCap.Round
            )
        )
    }
}

private fun DrawScope.drawHappyCheeks(
    leftCx: Float,
    rightCx: Float,
    cy: Float,
    colour: Color,
    alpha: Float
) {
    if (alpha <= 0.001f) {
        return
    }

    fun cheek(
        start: Offset,
        end: Offset,
        opacity: Float,
        width: Float
    ) {
        drawLine(
            color =
                colour.copy(
                    alpha =
                        colour.alpha *
                                alpha *
                                opacity
                ),
            start = start,
            end = end,
            strokeWidth = width,
            cap = StrokeCap.Round
        )
    }

    cheek(
        start = Offset(
            leftCx - 35f,
            cy + 29f
        ),
        end = Offset(
            leftCx - 29f,
            cy + 22f
        ),
        opacity = 0.75f,
        width = 4f
    )

    cheek(
        start = Offset(
            leftCx - 28f,
            cy + 34f
        ),
        end = Offset(
            leftCx - 23f,
            cy + 27f
        ),
        opacity = 0.55f,
        width = 3.5f
    )

    cheek(
        start = Offset(
            rightCx + 35f,
            cy + 29f
        ),
        end = Offset(
            rightCx + 29f,
            cy + 22f
        ),
        opacity = 0.75f,
        width = 4f
    )

    cheek(
        start = Offset(
            rightCx + 28f,
            cy + 34f
        ),
        end = Offset(
            rightCx + 23f,
            cy + 27f
        ),
        opacity = 0.55f,
        width = 3.5f
    )
}

/*
 * ============================================================================
 * SPARKLES
 * ============================================================================
 */

private fun DrawScope.drawSparkle(
    center: Offset,
    radius: Float,
    colour: Color,
    alpha: Float
) {
    if (alpha <= 0.001f) {
        return
    }

    val sparkle =
        Path().apply {
            moveTo(
                center.x,
                center.y - radius
            )

            quadraticTo(
                center.x + radius * 0.18f,
                center.y - radius * 0.18f,
                center.x + radius,
                center.y
            )

            quadraticTo(
                center.x + radius * 0.18f,
                center.y + radius * 0.18f,
                center.x,
                center.y + radius
            )

            quadraticTo(
                center.x - radius * 0.18f,
                center.y + radius * 0.18f,
                center.x - radius,
                center.y
            )

            quadraticTo(
                center.x - radius * 0.18f,
                center.y - radius * 0.18f,
                center.x,
                center.y - radius
            )

            close()
        }

    drawPath(
        path = sparkle,
        color =
            colour.copy(
                alpha =
                    colour.alpha *
                            alpha.coerceIn(
                                0f,
                                1f
                            )
            )
    )
}

private fun DrawScope.drawSuggestionSparkle(
    center: Offset,
    radius: Float,
    colour: Color,
    alpha: Float
) {
    if (alpha <= 0.001f) {
        return
    }

    val visibleAlpha =
        alpha.coerceIn(
            0f,
            1f
        )

    /*
     * Soft outer halo.
     */
    drawCircle(
        color =
            colour.copy(
                alpha =
                    colour.alpha *
                            visibleAlpha *
                            0.16f
            ),
        radius = radius * 1.8f,
        center = center
    )

    /*
     * Middle halo.
     */
    drawCircle(
        color =
            colour.copy(
                alpha =
                    colour.alpha *
                            visibleAlpha *
                            0.10f
            ),
        radius = radius * 2.45f,
        center = center
    )

    drawSparkle(
        center = center,
        radius = radius,
        colour = colour,
        alpha = visibleAlpha
    )
}

/*
 * ============================================================================
 * FOCUSED
 * ============================================================================
 */

private fun DrawScope.drawFocusedBrows(
    leftCx: Float,
    rightCx: Float,
    cy: Float,
    colour: Color,
    alpha: Float
) {
    if (alpha <= 0.001f) {
        return
    }

    val browColour =
        colour.copy(
            alpha =
                colour.alpha *
                        alpha.coerceIn(
                            0f,
                            1f
                        )
        )

    drawLine(
        color = browColour,
        start = Offset(
            leftCx - 23f,
            cy - 37f
        ),
        end = Offset(
            leftCx + 17f,
            cy - 31f
        ),
        strokeWidth = 6f,
        cap = StrokeCap.Round
    )

    drawLine(
        color = browColour,
        start = Offset(
            rightCx + 23f,
            cy - 37f
        ),
        end = Offset(
            rightCx - 17f,
            cy - 31f
        ),
        strokeWidth = 6f,
        cap = StrokeCap.Round
    )
}

/*
 * ============================================================================
 * OVERWHELMED
 * ============================================================================
 */

private fun DrawScope.drawOverwhelmedBrows(
    leftCx: Float,
    rightCx: Float,
    cy: Float,
    colour: Color,
    alpha: Float
) {
    if (alpha <= 0.001f) {
        return
    }

    val browColour =
        colour.copy(
            alpha =
                colour.alpha *
                        alpha.coerceIn(
                            0f,
                            1f
                        )
        )

    drawLine(
        color = browColour,
        start = Offset(
            leftCx - 24f,
            cy - 43f
        ),
        end = Offset(
            leftCx + 18f,
            cy - 34f
        ),
        strokeWidth = 5.5f,
        cap = StrokeCap.Round
    )

    drawLine(
        color = browColour,
        start = Offset(
            rightCx - 18f,
            cy - 35f
        ),
        end = Offset(
            rightCx + 23f,
            cy - 44f
        ),
        strokeWidth = 5.5f,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawSweatDrop(
    center: Offset,
    colour: Color,
    alpha: Float
) {
    if (alpha <= 0.001f) {
        return
    }

    val drop =
        Path().apply {
            moveTo(
                center.x,
                center.y - 14f
            )

            cubicTo(
                center.x + 3f,
                center.y - 7f,
                center.x + 10f,
                center.y,
                center.x + 10f,
                center.y + 6f
            )

            cubicTo(
                center.x + 10f,
                center.y + 13f,
                center.x + 5f,
                center.y + 18f,
                center.x,
                center.y + 18f
            )

            cubicTo(
                center.x - 6f,
                center.y + 18f,
                center.x - 10f,
                center.y + 13f,
                center.x - 10f,
                center.y + 6f
            )

            cubicTo(
                center.x - 10f,
                center.y,
                center.x - 3f,
                center.y - 7f,
                center.x,
                center.y - 14f
            )

            close()
        }

    drawPath(
        path = drop,
        color =
            colour.copy(
                alpha =
                    colour.alpha *
                            alpha.coerceIn(
                                0f,
                                1f
                            ) *
                            0.90f
            )
    )
}

private fun DrawScope.drawStressMarks(
    origin: Offset,
    colour: Color,
    alpha: Float
) {
    if (alpha <= 0.001f) {
        return
    }

    fun mark(
        start: Offset,
        end: Offset,
        opacity: Float,
        width: Float
    ) {
        drawLine(
            color =
                colour.copy(
                    alpha =
                        colour.alpha *
                                alpha *
                                opacity
                ),
            start = start,
            end = end,
            strokeWidth = width,
            cap = StrokeCap.Round
        )
    }

    mark(
        start = Offset(
            origin.x,
            origin.y
        ),
        end = Offset(
            origin.x - 11f,
            origin.y - 14f
        ),
        opacity = 0.9f,
        width = 4.5f
    )

    mark(
        start = Offset(
            origin.x + 11f,
            origin.y - 6f
        ),
        end = Offset(
            origin.x + 5f,
            origin.y - 23f
        ),
        opacity = 0.75f,
        width = 4f
    )

    mark(
        start = Offset(
            origin.x - 11f,
            origin.y + 9f
        ),
        end = Offset(
            origin.x - 24f,
            origin.y + 1f
        ),
        opacity = 0.60f,
        width = 3.5f
    )
}

/*
 * ============================================================================
 * SLEEPY
 * ============================================================================
 */

private fun DrawScope.drawSleepMarks(
    origin: Offset,
    colour: Color,
    alpha: Float
) {
    if (alpha <= 0.001f) {
        return
    }

    val markColour =
        colour.copy(
            alpha =
                colour.alpha *
                        alpha.coerceIn(
                            0f,
                            1f
                        ) *
                        0.65f
        )

    val smallZ =
        Path().apply {
            moveTo(
                origin.x,
                origin.y
            )

            lineTo(
                origin.x + 13f,
                origin.y
            )

            lineTo(
                origin.x,
                origin.y + 14f
            )

            lineTo(
                origin.x + 13f,
                origin.y + 14f
            )
        }

    drawPath(
        path = smallZ,
        color = markColour,
        style = Stroke(
            width = 3.5f,
            cap = StrokeCap.Round
        )
    )

    val largeOrigin =
        Offset(
            origin.x + 18f,
            origin.y - 24f
        )

    val largeZ =
        Path().apply {
            moveTo(
                largeOrigin.x,
                largeOrigin.y
            )

            lineTo(
                largeOrigin.x + 18f,
                largeOrigin.y
            )

            lineTo(
                largeOrigin.x,
                largeOrigin.y + 19f
            )

            lineTo(
                largeOrigin.x + 18f,
                largeOrigin.y + 19f
            )
        }

    drawPath(
        path = largeZ,
        color =
            markColour.copy(
                alpha =
                    markColour.alpha *
                            0.80f
            ),
        style = Stroke(
            width = 4f,
            cap = StrokeCap.Round
        )
    )
}

/*
 * ============================================================================
 * ENERGETIC
 * ============================================================================
 */

private fun DrawScope.drawEnergyMarks(
    origin: Offset,
    colour: Color,
    alpha: Float
) {
    if (alpha <= 0.001f) {
        return
    }

    val visible =
        alpha.coerceIn(
            0f,
            1f
        )

    drawLine(
        color =
            colour.copy(
                alpha =
                    colour.alpha *
                            visible *
                            0.85f
            ),
        start = Offset(
            origin.x,
            origin.y
        ),
        end = Offset(
            origin.x - 16f,
            origin.y - 13f
        ),
        strokeWidth = 4f,
        cap = StrokeCap.Round
    )

    drawLine(
        color =
            colour.copy(
                alpha =
                    colour.alpha *
                            visible *
                            0.62f
            ),
        start = Offset(
            origin.x - 3f,
            origin.y + 13f
        ),
        end = Offset(
            origin.x - 23f,
            origin.y + 8f
        ),
        strokeWidth = 3.5f,
        cap = StrokeCap.Round
    )

    drawLine(
        color =
            colour.copy(
                alpha =
                    colour.alpha *
                            visible *
                            0.45f
            ),
        start = Offset(
            origin.x + 8f,
            origin.y - 14f
        ),
        end = Offset(
            origin.x,
            origin.y - 30f
        ),
        strokeWidth = 3f,
        cap = StrokeCap.Round
    )
}

/*
 * ============================================================================
 * PREVIEWS
 * ============================================================================
 *
 * These previews deliberately avoid DayLoopTheme, Surface, padding and dp.
 *
 * They therefore depend only on Compose itself and the mascot.
 */

@Preview(
    name = "Mascot - Neutral",
    showBackground = true,
    widthDp = 300,
    heightDp = 300
)
@Composable
private fun DayLoopMascotNeutralPreview() {
    MaterialTheme {
        DayLoopMascot(
            modifier = Modifier.fillMaxSize(),
            mood = MascotMood.NEUTRAL,
            reducedMotion = false
        )
    }
}

@Preview(
    name = "Mascot - Focused",
    showBackground = true,
    widthDp = 300,
    heightDp = 300
)
@Composable
private fun DayLoopMascotFocusedPreview() {
    MaterialTheme {
        DayLoopMascot(
            modifier = Modifier.fillMaxSize(),
            mood = MascotMood.FOCUSED,
            reducedMotion = false
        )
    }
}

@Preview(
    name = "Mascot - Accomplished",
    showBackground = true,
    widthDp = 300,
    heightDp = 300
)
@Composable
private fun DayLoopMascotAccomplishedPreview() {
    MaterialTheme {
        DayLoopMascot(
            modifier = Modifier.fillMaxSize(),
            mood = MascotMood.ACCOMPLISHED,
            reducedMotion = false
        )
    }
}

@Preview(
    name = "Mascot - Overwhelmed",
    showBackground = true,
    widthDp = 300,
    heightDp = 300
)
@Composable
private fun DayLoopMascotOverwhelmedPreview() {
    MaterialTheme {
        DayLoopMascot(
            modifier = Modifier.fillMaxSize(),
            mood = MascotMood.OVERWHELMED,
            reducedMotion = false
        )
    }
}

@Preview(
    name = "Mascot - Suggesting",
    showBackground = true,
    widthDp = 300,
    heightDp = 300
)
@Composable
private fun DayLoopMascotSuggestingPreview() {
    MaterialTheme {
        DayLoopMascot(
            modifier = Modifier.fillMaxSize(),
            mood = MascotMood.SUGGESTING,
            reducedMotion = false
        )
    }
}

@Preview(
    name = "Mascot - Excited",
    showBackground = true,
    widthDp = 300,
    heightDp = 300
)
@Composable
private fun DayLoopMascotExcitedPreview() {
    MaterialTheme {
        DayLoopMascot(
            modifier = Modifier.fillMaxSize(),
            mood = MascotMood.EXCITED,
            reducedMotion = false
        )
    }
}

@Preview(
    name = "Mascot - Sleepy",
    showBackground = true,
    widthDp = 300,
    heightDp = 300
)
@Composable
private fun DayLoopMascotSleepyPreview() {
    MaterialTheme {
        DayLoopMascot(
            modifier = Modifier.fillMaxSize(),
            mood = MascotMood.SLEEPY,
            reducedMotion = false
        )
    }
}

@Preview(
    name = "Mascot - Energetic",
    showBackground = true,
    widthDp = 300,
    heightDp = 300
)
@Composable
private fun DayLoopMascotEnergeticPreview() {
    MaterialTheme {
        DayLoopMascot(
            modifier = Modifier.fillMaxSize(),
            mood = MascotMood.ENERGETIC,
            reducedMotion = false
        )
    }
}

@Preview(
    name = "Mascot - Stargazing",
    showBackground = true,
    widthDp = 300,
    heightDp = 300
)
@Composable
private fun DayLoopMascotStargazingPreview() {
    MaterialTheme {
        DayLoopMascot(
            modifier = Modifier.fillMaxSize(),
            mood = MascotMood.STARGAZING,
            reducedMotion = false
        )
    }
}

@Preview(
    name = "Mascot - Looking Down",
    showBackground = true,
    widthDp = 300,
    heightDp = 300
)
@Composable
private fun DayLoopMascotLookingDownPreview() {
    MaterialTheme {
        DayLoopMascot(
            modifier = Modifier.fillMaxSize(),
            mood = MascotMood.NEUTRAL,
            lookingDown = true,
            reducedMotion = false
        )
    }
}

@Preview(
    name = "Mascot - Reduced Motion",
    showBackground = true,
    widthDp = 300,
    heightDp = 300
)
@Composable
private fun DayLoopMascotReducedMotionPreview() {
    MaterialTheme {
        DayLoopMascot(
            modifier = Modifier.fillMaxSize(),
            mood = MascotMood.OVERWHELMED,
            reducedMotion = true
        )
    }
}