package com.screen.brightness

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.os.IBinder
import android.view.WindowManager
import androidx.core.app.NotificationCompat

class FloatingButtonService : Service() {

    companion object {
        private const val CHANNEL_ID = "floating_button_channel"
        private const val NOTIFICATION_ID = 1
        const val ACTION_REFRESH = "com.screen.brightness.ACTION_REFRESH"
    }

    private var helper: FloatingButtonHelper? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        BrightnessHelper.init(this)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        createFloatingButton()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_REFRESH) {
            refreshButtonAppearance()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        helper?.destroy()
        if (BrightnessHelper.isMaxBrightness) {
            BrightnessHelper.restoreBrightness(this)
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.channel_description)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun buildConfig(): ButtonConfig {
        val isMax = BrightnessHelper.isMaxBrightness
        val fgColor = if (!IconHelper.isCustomIcon(this) && isMax) {
            Color.YELLOW
        } else {
            AppPrefs.getFgColor(this)
        }
        return ButtonConfig(
            sizeDp = AppPrefs.getSize(this),
            bgColor = AppPrefs.getBgColor(this),
            fgColor = fgColor,
            iconAlpha = AppPrefs.getIconAlpha(this),
            bgAlpha = AppPrefs.getBgAlpha(this),
            posY = AppPrefs.getPosY(this),
            snapSide = AppPrefs.getSnapSide(this),
            iconDrawable = IconHelper.loadIconDrawable(this),
            isCustomIcon = IconHelper.isCustomIcon(this),
            windowType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        )
    }

    private fun createFloatingButton() {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        helper = FloatingButtonHelper(
            context = this,
            windowManager = wm,
            config = buildConfig(),
            onClick = ::onButtonClick,
            onPositionSaved = { posY, snapSide ->
                AppPrefs.setPosY(this, posY)
                AppPrefs.setSnapSide(this, snapSide)
            }
        )
        helper?.create()
    }

    private fun refreshButtonAppearance() {
        helper?.refresh(buildConfig())
    }

    private fun onButtonClick() {
        BrightnessHelper.toggleBrightness(this)
        val tintColor = if (BrightnessHelper.isMaxBrightness) Color.YELLOW else null
        helper?.updateIconTint(tintColor)
    }
}
