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

class KeyFeedback(private val context: Context) {
    private var generator: ToneGenerator? = null
    fun sound(style: Int) {
        val audio = context.getSystemService(AudioManager::class.java)
        if (audio.ringerMode != AudioManager.RINGER_MODE_NORMAL) return
        if (generator == null) generator = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 35) }.getOrNull()
        generator?.startTone(when (style) {
            1 -> ToneGenerator.TONE_DTMF_1
            2 -> ToneGenerator.TONE_PROP_ACK
            else -> ToneGenerator.TONE_PROP_BEEP
        }, 40)
    }
    fun vibrate(duration: Int): String? {
        if (Settings.System.getInt(context.contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) == 0)
            return "ဖုန်း၏ Touch vibration ပိတ်ထားသည်။ Sound settings မှာ ဖွင့်ပါ။"
        val vibrator = if (Build.VERSION.SDK_INT >= 31)
            context.getSystemService(VibratorManager::class.java).defaultVibrator
        else context.getSystemService(Vibrator::class.java)
        if (!vibrator.hasVibrator()) return "ဒီဖုန်းမှာ vibrator မရှိပါ။"
        val effect = VibrationEffect.createOneShot(duration.toLong(), VibrationEffect.DEFAULT_AMPLITUDE)
        if (Build.VERSION.SDK_INT >= 33) {
            vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH))
        } else {
            vibrator.vibrate(effect, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).build())
        }
        return null
    }
    fun release() { generator?.release(); generator = null }
}
