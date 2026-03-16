package com.screen.brightness

import android.content.Context
import android.content.SharedPreferences

object AppPrefs {

    private const val PREFS_NAME = "screen_brightness_prefs"
    private const val PREFS_VERSION_KEY = "prefs_version"
    private const val CURRENT_PREFS_VERSION = 2

    // 亮度状态
    private const val KEY_SAVED_BRIGHTNESS = "saved_brightness"
    private const val KEY_SAVED_AUTO_BRIGHTNESS = "saved_auto_brightness"
    private const val KEY_IS_MAX_BRIGHTNESS = "is_max_brightness"

    // 亮度按钮 - 外观
    private const val KEY_BG_COLOR = "btn_bg_color"
    private const val KEY_FG_COLOR = "btn_fg_color"
    private const val KEY_ICON_ALPHA = "btn_icon_alpha"
    private const val KEY_BG_ALPHA = "btn_bg_alpha"
    private const val KEY_ICON_TYPE = "btn_icon_type"
    private const val KEY_CUSTOM_SVG = "btn_custom_svg"
    private const val KEY_CUSTOM_SVG_NAME = "btn_custom_svg_name"
    private const val KEY_SIZE = "btn_size"

    // 亮度按钮 - 位置
    private const val KEY_POS_Y = "btn_pos_y"
    private const val KEY_SNAP_SIDE = "btn_snap_side"

    // 旧版透明度（用于迁移）
    private const val KEY_ALPHA_LEGACY = "btn_alpha"

    // 返回按钮
    private const val KEY_BACK_ENABLED = "back_btn_enabled"
    private const val KEY_BACK_BG_COLOR = "back_btn_bg_color"
    private const val KEY_BACK_FG_COLOR = "back_btn_fg_color"
    private const val KEY_BACK_ICON_ALPHA = "back_btn_icon_alpha"
    private const val KEY_BACK_BG_ALPHA = "back_btn_bg_alpha"
    private const val KEY_BACK_SIZE = "back_btn_size"
    private const val KEY_BACK_POS_Y = "back_btn_pos_y"
    private const val KEY_BACK_SNAP_SIDE = "back_btn_snap_side"

    // 亮度按钮默认值 - bgColor 不含透明度，透明度由 SeekBar 独立控制
    const val DEFAULT_BG_COLOR = 0xFF333333.toInt()
    const val DEFAULT_FG_COLOR = 0xFFFFFFFF.toInt()
    const val DEFAULT_ICON_ALPHA = 180
    const val DEFAULT_BG_ALPHA = 128
    const val DEFAULT_ICON_TYPE = "sun"
    const val DEFAULT_SIZE = 56
    const val DEFAULT_POS_Y = 200
    const val DEFAULT_SNAP_SIDE = 0 // 0=左, 1=右

    // 返回按钮默认值
    const val DEFAULT_BACK_ENABLED = false
    const val DEFAULT_BACK_BG_COLOR = 0xFF333333.toInt()
    const val DEFAULT_BACK_FG_COLOR = 0xFFFFFFFF.toInt()
    const val DEFAULT_BACK_ICON_ALPHA = 180
    const val DEFAULT_BACK_BG_ALPHA = 128
    const val DEFAULT_BACK_SIZE = 48
    const val DEFAULT_BACK_POS_Y = 400
    const val DEFAULT_BACK_SNAP_SIDE = 0

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun migrateIfNeeded(context: Context) {
        val p = prefs(context)
        val version = p.getInt(PREFS_VERSION_KEY, 1)
        if (version < CURRENT_PREFS_VERSION) {
            migrateV1ToV2(p)
            p.edit().putInt(PREFS_VERSION_KEY, CURRENT_PREFS_VERSION).apply()
        }
    }

    private fun migrateV1ToV2(p: SharedPreferences) {
        val editor = p.edit()

        // 旧 btn_alpha (Float 0.2~1.0) → 新双 alpha (Int 0~255)
        if (p.contains(KEY_ALPHA_LEGACY)) {
            val oldAlpha = p.getFloat(KEY_ALPHA_LEGACY, 0.6f)
            val alpha255 = (oldAlpha * 255).toInt().coerceIn(0, 255)
            editor.putInt(KEY_ICON_ALPHA, alpha255)
            editor.putInt(KEY_BG_ALPHA, alpha255)
            editor.remove(KEY_ALPHA_LEGACY)
        }

        // 旧 bgColor 含透明通道 → 提取为不透明色 + 独立 bgAlpha
        if (p.contains(KEY_BG_COLOR)) {
            val oldColor = p.getInt(KEY_BG_COLOR, DEFAULT_BG_COLOR)
            val alphaChannel = (oldColor ushr 24) and 0xFF
            if (alphaChannel < 0xFF) {
                val opaqueColor = oldColor or 0xFF000000.toInt()
                editor.putInt(KEY_BG_COLOR, opaqueColor)
                if (!p.contains(KEY_BG_ALPHA)) {
                    editor.putInt(KEY_BG_ALPHA, alphaChannel)
                }
            }
        }

        editor.apply()
    }

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

    // --- 亮度按钮外观 ---

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

    fun getIconAlpha(context: Context): Int =
        prefs(context).getInt(KEY_ICON_ALPHA, DEFAULT_ICON_ALPHA)

    fun setIconAlpha(context: Context, alpha: Int) {
        prefs(context).edit().putInt(KEY_ICON_ALPHA, alpha).apply()
    }

    fun getBgAlpha(context: Context): Int =
        prefs(context).getInt(KEY_BG_ALPHA, DEFAULT_BG_ALPHA)

    fun setBgAlpha(context: Context, alpha: Int) {
        prefs(context).edit().putInt(KEY_BG_ALPHA, alpha).apply()
    }

    fun getIconType(context: Context): String =
        prefs(context).getString(KEY_ICON_TYPE, DEFAULT_ICON_TYPE) ?: DEFAULT_ICON_TYPE

    fun setIconType(context: Context, type: String) {
        prefs(context).edit().putString(KEY_ICON_TYPE, type).apply()
    }

    fun getCustomSvg(context: Context): String? =
        prefs(context).getString(KEY_CUSTOM_SVG, null)

    fun getCustomSvgName(context: Context): String? =
        prefs(context).getString(KEY_CUSTOM_SVG_NAME, null)

    fun setCustomSvg(context: Context, svg: String, name: String) {
        prefs(context).edit()
            .putString(KEY_CUSTOM_SVG, svg)
            .putString(KEY_CUSTOM_SVG_NAME, name)
            .apply()
    }

    fun getSize(context: Context): Int =
        prefs(context).getInt(KEY_SIZE, DEFAULT_SIZE)

    fun setSize(context: Context, size: Int) {
        prefs(context).edit().putInt(KEY_SIZE, size).apply()
    }

    // --- 亮度按钮位置 ---

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

    // --- 返回按钮 ---

    fun isBackEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_BACK_ENABLED, DEFAULT_BACK_ENABLED)

    fun setBackEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_BACK_ENABLED, enabled).apply()
    }

    fun getBackBgColor(context: Context): Int =
        prefs(context).getInt(KEY_BACK_BG_COLOR, DEFAULT_BACK_BG_COLOR)

    fun setBackBgColor(context: Context, color: Int) {
        prefs(context).edit().putInt(KEY_BACK_BG_COLOR, color).apply()
    }

    fun getBackFgColor(context: Context): Int =
        prefs(context).getInt(KEY_BACK_FG_COLOR, DEFAULT_BACK_FG_COLOR)

    fun setBackFgColor(context: Context, color: Int) {
        prefs(context).edit().putInt(KEY_BACK_FG_COLOR, color).apply()
    }

    fun getBackIconAlpha(context: Context): Int =
        prefs(context).getInt(KEY_BACK_ICON_ALPHA, DEFAULT_BACK_ICON_ALPHA)

    fun setBackIconAlpha(context: Context, alpha: Int) {
        prefs(context).edit().putInt(KEY_BACK_ICON_ALPHA, alpha).apply()
    }

    fun getBackBgAlpha(context: Context): Int =
        prefs(context).getInt(KEY_BACK_BG_ALPHA, DEFAULT_BACK_BG_ALPHA)

    fun setBackBgAlpha(context: Context, alpha: Int) {
        prefs(context).edit().putInt(KEY_BACK_BG_ALPHA, alpha).apply()
    }

    fun getBackSize(context: Context): Int =
        prefs(context).getInt(KEY_BACK_SIZE, DEFAULT_BACK_SIZE)

    fun setBackSize(context: Context, size: Int) {
        prefs(context).edit().putInt(KEY_BACK_SIZE, size).apply()
    }

    fun getBackPosY(context: Context): Int =
        prefs(context).getInt(KEY_BACK_POS_Y, DEFAULT_BACK_POS_Y)

    fun setBackPosY(context: Context, y: Int) {
        prefs(context).edit().putInt(KEY_BACK_POS_Y, y).apply()
    }

    fun getBackSnapSide(context: Context): Int =
        prefs(context).getInt(KEY_BACK_SNAP_SIDE, DEFAULT_BACK_SNAP_SIDE)

    fun setBackSnapSide(context: Context, side: Int) {
        prefs(context).edit().putInt(KEY_BACK_SNAP_SIDE, side).apply()
    }
}
