package com.screen.brightness

import android.content.Context
import android.content.SharedPreferences

object AppPrefs {

    private const val PREFS_NAME = "screen_brightness_prefs"

    // 亮度状态
    private const val KEY_SAVED_BRIGHTNESS = "saved_brightness"
    private const val KEY_SAVED_AUTO_BRIGHTNESS = "saved_auto_brightness"
    private const val KEY_IS_MAX_BRIGHTNESS = "is_max_brightness"

    // 按钮外观
    private const val KEY_BG_COLOR = "btn_bg_color"
    private const val KEY_FG_COLOR = "btn_fg_color"
    private const val KEY_ALPHA = "btn_alpha"
    private const val KEY_ICON = "btn_icon"
    private const val KEY_SIZE = "btn_size"

    // 按钮位置
    private const val KEY_POS_Y = "btn_pos_y"
    private const val KEY_SNAP_SIDE = "btn_snap_side"

    // 默认值
    const val DEFAULT_BG_COLOR = 0x80333333.toInt()
    const val DEFAULT_FG_COLOR = 0xFFFFFFFF.toInt()
    const val DEFAULT_ALPHA = 0.6f
    const val DEFAULT_ICON = "☀"
    const val DEFAULT_SIZE = 56
    const val DEFAULT_POS_Y = 200
    const val DEFAULT_SNAP_SIDE = 0 // 0=左, 1=右

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // --- 亮度状态 ---

    fun saveBrightnessState(context: Context, brightness: Int, autoBrightness: Boolean) {
        prefs(context).edit()
            .putInt(KEY_SAVED_BRIGHTNESS, brightness)
            .putBoolean(KEY_SAVED_AUTO_BRIGHTNESS, autoBrightness)
            .putBoolean(KEY_IS_MAX_BRIGHTNESS, true)
            .apply()
    }

    fun clearBrightnessState(context: Context) {
        prefs(context).edit()
            .putBoolean(KEY_IS_MAX_BRIGHTNESS, false)
            .apply()
    }

    fun getSavedBrightness(context: Context): Int =
        prefs(context).getInt(KEY_SAVED_BRIGHTNESS, -1)

    fun getSavedAutoBrightness(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SAVED_AUTO_BRIGHTNESS, false)

    fun getIsMaxBrightness(context: Context): Boolean =
        prefs(context).getBoolean(KEY_IS_MAX_BRIGHTNESS, false)

    // --- 按钮外观 ---

    fun getBgColor(context: Context): Int =
        prefs(context).getInt(KEY_BG_COLOR, DEFAULT_BG_COLOR)

    fun setBgColor(context: Context, color: Int) {
        prefs(context).edit().putInt(KEY_BG_COLOR, color).apply()
    }

    fun getFgColor(context: Context): Int =
        prefs(context).getInt(KEY_FG_COLOR, DEFAULT_FG_COLOR)

    fun setFgColor(context: Context, color: Int) {
        prefs(context).edit().putInt(KEY_FG_COLOR, color).apply()
    }

    fun getAlpha(context: Context): Float =
        prefs(context).getFloat(KEY_ALPHA, DEFAULT_ALPHA)

    fun setAlpha(context: Context, alpha: Float) {
        prefs(context).edit().putFloat(KEY_ALPHA, alpha).apply()
    }

    fun getIcon(context: Context): String =
        prefs(context).getString(KEY_ICON, DEFAULT_ICON) ?: DEFAULT_ICON

    fun setIcon(context: Context, icon: String) {
        prefs(context).edit().putString(KEY_ICON, icon).apply()
    }

    fun getSize(context: Context): Int =
        prefs(context).getInt(KEY_SIZE, DEFAULT_SIZE)

    fun setSize(context: Context, size: Int) {
        prefs(context).edit().putInt(KEY_SIZE, size).apply()
    }

    // --- 按钮位置 ---

    fun getPosY(context: Context): Int =
        prefs(context).getInt(KEY_POS_Y, DEFAULT_POS_Y)

    fun setPosY(context: Context, y: Int) {
        prefs(context).edit().putInt(KEY_POS_Y, y).apply()
    }

    fun getSnapSide(context: Context): Int =
        prefs(context).getInt(KEY_SNAP_SIDE, DEFAULT_SNAP_SIDE)

    fun setSnapSide(context: Context, side: Int) {
        prefs(context).edit().putInt(KEY_SNAP_SIDE, side).apply()
    }
}
