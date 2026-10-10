package com.doodlecat.wallpaper.core

import android.content.Context
import android.content.SharedPreferences

/**
 * Tiny preference store. No androidx.preference dependency: the settings UI is
 * hand-built and minimalist, and the engine only needs primitive reads + a listener.
 */
class Prefs private constructor(private val sp: SharedPreferences) {

    enum class Size(val fraction: Float) {
        SMALL(0.16f), MEDIUM(0.22f), LARGE(0.29f);
        companion object { fun of(i: Int) = entries.getOrElse(i) { MEDIUM } }
    }

    enum class Speed(val baseMoveMs: Long) {
        SLOW(640L), NORMAL(470L), FAST(330L);
        companion object { fun of(i: Int) = entries.getOrElse(i) { NORMAL } }
    }

    val size: Size get() = Size.of(sp.getInt(KEY_SIZE, 1))
    val speed: Speed get() = Speed.of(sp.getInt(KEY_SPEED, 1))
    val rareEvents: Boolean get() = sp.getBoolean(KEY_RARE, true)
    val sleepingZ: Boolean get() = sp.getBoolean(KEY_Z, true)
    val randomPose: Boolean get() = sp.getBoolean(KEY_POSE, true)
    val charcoalBackground: Boolean get() = sp.getBoolean(KEY_BG, false)

    val backgroundColor: Int get() = if (charcoalBackground) 0xFF0B0B0C.toInt() else 0xFF000000.toInt()

    fun registerListener(l: SharedPreferences.OnSharedPreferenceChangeListener) =
        sp.registerOnSharedPreferenceChangeListener(l)

    fun unregisterListener(l: SharedPreferences.OnSharedPreferenceChangeListener) =
        sp.unregisterOnSharedPreferenceChangeListener(l)

    /* ---- writes (settings UI) ---- */
    fun putSize(s: Size) = sp.edit().putInt(KEY_SIZE, s.ordinal).apply()
    fun putSpeed(s: Speed) = sp.edit().putInt(KEY_SPEED, s.ordinal).apply()
    fun putRareEvents(on: Boolean) = sp.edit().putBoolean(KEY_RARE, on).apply()
    fun putSleepingZ(on: Boolean) = sp.edit().putBoolean(KEY_Z, on).apply()
    fun putRandomPose(on: Boolean) = sp.edit().putBoolean(KEY_POSE, on).apply()
    fun putCharcoalBackground(on: Boolean) = sp.edit().putBoolean(KEY_BG, on).apply()

    companion object {
        private const val FILE = "doodle_cat_prefs"
        private const val KEY_SIZE = "cat_size"
        private const val KEY_SPEED = "move_speed"
        private const val KEY_RARE = "rare_events"
        private const val KEY_Z = "sleeping_z"
        private const val KEY_POSE = "random_pose"
        private const val KEY_BG = "charcoal_bg"

        @Volatile private var instance: Prefs? = null

        fun get(context: Context): Prefs =
            instance ?: synchronized(this) {
                instance ?: Prefs(
                    context.applicationContext
                        .getSharedPreferences(FILE, Context.MODE_PRIVATE)
                ).also { instance = it }
            }
    }
}
