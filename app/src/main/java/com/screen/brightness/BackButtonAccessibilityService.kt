package com.screen.brightness

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat

class BackButtonAccessibilityService : AccessibilityService() {

    companion object {
        const val ACTION_REFRESH = "com.screen.brightness.ACTION_REFRESH_BACK"
        var instance: BackButtonAccessibilityService? = null
            private set
    }

    private var helper: FloatingButtonHelper? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        if (AppPrefs.isBackEnabled(this)) {
            createButton()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_REFRESH) {
            refreshButton()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        helper?.destroy()
        instance = null
        super.onDestroy()
    }

    fun refreshButton() {
        if (!AppPrefs.isBackEnabled(this)) {
            helper?.destroy()
            helper = null
            return
        }
        if (helper?.isCreated() == true) {
            helper?.refresh(buildConfig())
        } else {
            createButton()
        }
    }

    private fun createButton() {
        helper?.destroy()
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        helper = FloatingButtonHelper(
            context = this,
            windowManager = wm,
            config = buildConfig(),
            onClick = { performGlobalAction(GLOBAL_ACTION_BACK) },
            onLongClick = { performGlobalAction(GLOBAL_ACTION_HOME) },
            onPositionSaved = { posY, snapSide ->
                AppPrefs.setBackPosY(this, posY)
                AppPrefs.setBackSnapSide(this, snapSide)
            }
        )
        helper?.create()
    }

    private fun buildConfig(): ButtonConfig {
        return ButtonConfig(
            sizeDp = AppPrefs.getBackSize(this),
            bgColor = AppPrefs.getBackBgColor(this),
            fgColor = AppPrefs.getBackFgColor(this),
            iconAlpha = AppPrefs.getBackIconAlpha(this),
            bgAlpha = AppPrefs.getBackBgAlpha(this),
            posY = AppPrefs.getBackPosY(this),
            snapSide = AppPrefs.getBackSnapSide(this),
            iconDrawable = ContextCompat.getDrawable(this, R.drawable.ic_back)?.mutate(),
            isCustomIcon = false,
            windowType = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        )
    }
}
