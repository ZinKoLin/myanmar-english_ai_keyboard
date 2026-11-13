package com.zrok.myanmaraikeyboard.ui

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager
import android.os.VibrationEffect
import android.os.VibrationAttributes
import android.media.AudioAttributes
import android.provider.Settings
import android.view.View
import android.view.HapticFeedbackConstants

class KeyFeedback(private val context: Context) {
    private var generator: ToneGenerator? = null
    private var generatorVolume = -1
    fun sound(style: Int, key: String = "", volume: Float = 0.5f) {
        val audio = context.getSystemService(AudioManager::class.java)
        if (audio.ringerMode != AudioManager.RINGER_MODE_NORMAL) return
        if (style == 3) {
            val effect = when (key) {
                "⌫" -> AudioManager.FX_KEYPRESS_DELETE
                "↵" -> AudioManager.FX_KEYPRESS_RETURN
                " ", "English", "မြန်မာ" -> AudioManager.FX_KEYPRESS_SPACEBAR
                else -> AudioManager.FX_KEYPRESS_STANDARD
            }
            audio.playSoundEffect(effect, volume.coerceIn(0f, 1f))
            return
        }
        val percent = (volume.coerceIn(0f, 1f) * 100).toInt()
        if (generator == null || generatorVolume != percent) {
            generator?.release()
            generator = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, percent) }.getOrNull()
            generatorVolume = percent
        }
        generator?.startTone(when (style) {
            1 -> ToneGenerator.TONE_DTMF_1
            2 -> ToneGenerator.TONE_PROP_ACK
            else -> ToneGenerator.TONE_PROP_BEEP
        }, 40)
    }
    fun vibrate(duration: Int, view: View? = null, system: Boolean = true): String? {
        if (Settings.System.getInt(context.contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) == 0)
            return "ဖုန်း၏ Touch vibration ပိတ်ထားသည်။ Sound settings မှာ ဖွင့်ပါ။"
        val vibrator = if (Build.VERSION.SDK_INT >= 31)
            context.getSystemService(VibratorManager::class.java).defaultVibrator
        else context.getSystemService(Vibrator::class.java)
        if (!vibrator.hasVibrator()) return "ဒီဖုန်းမှာ vibrator မရှိပါ။"
        if (system && view != null) {
            return if (view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)) null
                else "ဖုန်းစနစ်က keyboard haptic ကို ပိတ်ထားပါသည်။ Touch vibration ကို စစ်ပါ။"
        }
        val strength = duration.coerceIn(1, 100)
        val effect = if (Build.VERSION.SDK_INT >= 30 && vibrator.arePrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_CLICK).all { it })
            VibrationEffect.startComposition().addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, strength / 100f).compose()
        else VibrationEffect.createOneShot(strength.toLong(), VibrationEffect.DEFAULT_AMPLITUDE)
        if (Build.VERSION.SDK_INT >= 33) {
            vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH))
        } else {
            vibrator.vibrate(effect, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).build())
        }
        return null
    }
    fun release() { generator?.release(); generator = null }
}
