package com.stormpanda.megingiard.ui

import android.content.Context
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Subtle touch haptics for every interaction in the app.
 *
 * Uses short, low-scale vibration primitives so feedback stays light. Vibrations are sent with
 * [VibrationAttributes.USAGE_TOUCH], so Android scales them by the user's touch-feedback
 * intensity and drops them entirely when touch feedback is off (including via the Vibration tile).
 */
class Haptics internal constructor(context: Context) {
    private val vibrator: Vibrator? =
        context.applicationContext.getSystemService(VibratorManager::class.java)?.defaultVibrator
    private val attributes = VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH)

    private val tapEffect = effect(VibrationEffect.Composition.PRIMITIVE_TICK, TAP_SCALE)
    private val longPressEffect = effect(VibrationEffect.Composition.PRIMITIVE_CLICK, LONG_PRESS_SCALE)
    private val stepEffect = effect(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, STEP_SCALE)

    /** A button, tile, row, toggle or option was activated. */
    fun tap() = play(tapEffect)

    /** A long-press action fired. Slightly firmer than [tap] so the two feel different. */
    fun longPress() = play(longPressEffect)

    /** A slider or stepper moved by one notch. The lightest of the three. */
    fun step() = play(stepEffect)

    private fun effect(primitive: Int, scale: Float): VibrationEffect? {
        val v = vibrator ?: return null
        if (!v.hasVibrator()) return null
        return if (v.areAllPrimitivesSupported(primitive)) {
            VibrationEffect.startComposition().addPrimitive(primitive, scale).compose()
        } else {
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
        }
    }

    private fun play(effect: VibrationEffect?) {
        val v = vibrator ?: return
        effect ?: return
        try {
            v.vibrate(effect, attributes)
        } catch (_: Throwable) {
            // Haptics are cosmetic; never let them break an interaction.
        }
    }

    companion object {
        private const val TAP_SCALE = 0.45f
        private const val LONG_PRESS_SCALE = 0.55f
        private const val STEP_SCALE = 0.35f

        /** Slider haptics fire once per this many percent. */
        const val SLIDER_STEP_PERCENT = 10

        @Volatile private var instance: Haptics? = null

        fun get(context: Context): Haptics =
            instance ?: synchronized(this) {
                instance ?: Haptics(context).also { instance = it }
            }
    }
}

@Composable
fun rememberHaptics(): Haptics {
    val context = LocalContext.current
    return remember(context) { Haptics.get(context) }
}

/** Wraps [action] so it plays a [Haptics.tap] first. */
fun Haptics.tapThen(action: () -> Unit): () -> Unit = {
    tap()
    action()
}

/** Wraps [action] so it plays a [Haptics.longPress] first. */
fun Haptics.longPressThen(action: () -> Unit): () -> Unit = {
    longPress()
    action()
}
