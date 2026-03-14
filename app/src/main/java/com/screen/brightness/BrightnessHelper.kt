package com.screen.brightness

import android.content.ContentResolver
import android.content.Context
import android.provider.Settings

object BrightnessHelper {

    private const val MAX_BRIGHTNESS = 255
    private var savedBrightness = -1
    private var savedAutoBrightness = false
    var isMaxBrightness = false
        private set

    fun init(context: Context) {
        isMaxBrightness = AppPrefs.getIsMaxBrightness(context)
        savedBrightness = AppPrefs.getSavedBrightness(context)
        savedAutoBrightness = AppPrefs.getSavedAutoBrightness(context)
    }

    fun getCurrentBrightness(contentResolver: ContentResolver): Int {
        return try {
            Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        } catch (e: Settings.SettingNotFoundException) {
            128
        }
    }

    fun isAutoBrightness(contentResolver: ContentResolver): Boolean {
        return try {
            Settings.System.getInt(
                contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE
            ) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
        } catch (e: Settings.SettingNotFoundException) {
            false
        }
    }

    fun setMaxBrightness(context: Context) {
        val contentResolver = context.contentResolver
        savedBrightness = getCurrentBrightness(contentResolver)
        savedAutoBrightness = isAutoBrightness(contentResolver)

        AppPrefs.saveBrightnessState(context, savedBrightness, savedAutoBrightness)

        Settings.System.putInt(
            contentResolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE,
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
        )
        Settings.System.putInt(
            contentResolver,
            Settings.System.SCREEN_BRIGHTNESS,
            MAX_BRIGHTNESS
        )
        isMaxBrightness = true
    }

    fun restoreBrightness(context: Context) {
        val contentResolver = context.contentResolver
        val brightness = if (savedBrightness >= 0) savedBrightness else 128

        Settings.System.putInt(
            contentResolver,
            Settings.System.SCREEN_BRIGHTNESS,
            brightness
        )

        if (savedAutoBrightness) {
            Settings.System.putInt(
                contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
            )
        }
        isMaxBrightness = false
        AppPrefs.clearBrightnessState(context)
    }

    fun toggleBrightness(context: Context) {
        if (isMaxBrightness) {
            restoreBrightness(context)
        } else {
            setMaxBrightness(context)
        }
    }
}
