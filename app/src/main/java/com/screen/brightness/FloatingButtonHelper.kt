package com.screen.brightness

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView

data class ButtonConfig(
    val sizeDp: Int,
    val bgColor: Int,
    val fgColor: Int,
    val iconAlpha: Int,
    val bgAlpha: Int,
    val posY: Int,
    val snapSide: Int,
    val iconDrawable: Drawable?,
    val isCustomIcon: Boolean,
    val windowType: Int
)

class FloatingButtonHelper(
    private val context: Context,
    private val windowManager: WindowManager,
    private var config: ButtonConfig,
    private val onClick: () -> Unit,
    private val onLongClick: (() -> Unit)? = null,
    private val onPositionSaved: (posY: Int, snapSide: Int) -> Unit
) {

    companion object {
        private const val MOVE_THRESHOLD = 10
        private const val LONG_PRESS_DELAY = 500L
    }

    private var floatingButton: ImageView? = null
    private lateinit var layoutParams: WindowManager.LayoutParams
    private var screenWidth = 0
    private var screenHeight = 0
    private var currentButtonSize = 0
    private val handler = Handler(Looper.getMainLooper())

    fun isCreated(): Boolean = floatingButton != null

    fun create() {
        updateScreenSize()
        currentButtonSize = dpToPx(config.sizeDp)
        val iconPadding = (currentButtonSize * 0.22f).toInt()

        val button = ImageView(context).apply {
            setImageDrawable(config.iconDrawable)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(iconPadding, iconPadding, iconPadding, iconPadding)
            background = createBgDrawable()
            imageAlpha = config.iconAlpha
            alpha = 1.0f
        }
        applyIconTint(button)

        val startX = if (config.snapSide == 1) screenWidth - currentButtonSize else 0

        layoutParams = WindowManager.LayoutParams(
            currentButtonSize,
            currentButtonSize,
            config.windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = startX
            y = config.posY
        }

        setupTouchListener(button)
        windowManager.addView(button, layoutParams)
        floatingButton = button
    }

    fun refresh(newConfig: ButtonConfig) {
        val button = floatingButton ?: return
        config = newConfig

        val newButtonSize = dpToPx(config.sizeDp)
        button.setImageDrawable(config.iconDrawable)
        button.background = createBgDrawable()
        button.imageAlpha = config.iconAlpha
        button.alpha = 1.0f

        if (newButtonSize != currentButtonSize) {
            currentButtonSize = newButtonSize
            layoutParams.width = currentButtonSize
            layoutParams.height = currentButtonSize
            val iconPadding = (currentButtonSize * 0.22f).toInt()
            button.setPadding(iconPadding, iconPadding, iconPadding, iconPadding)
            clampPosition()
        }

        applyIconTint(button)
        windowManager.updateViewLayout(button, layoutParams)
    }

    fun destroy() {
        val button = floatingButton ?: return
        handler.removeCallbacksAndMessages(null)
        windowManager.removeView(button)
        floatingButton = null
    }

    fun updateIconTint(overrideColor: Int? = null) {
        val button = floatingButton ?: return
        if (overrideColor != null && !config.isCustomIcon) {
            button.setColorFilter(overrideColor, PorterDuff.Mode.SRC_IN)
        } else {
            applyIconTint(button)
        }
    }

    private fun createBgDrawable(): GradientDrawable {
        val colorWithAlpha = (config.bgAlpha shl 24) or (config.bgColor and 0x00FFFFFF)
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(colorWithAlpha)
        }
    }

    private fun applyIconTint(button: ImageView) {
        if (config.isCustomIcon) {
            button.colorFilter = null
        } else {
            button.setColorFilter(config.fgColor, PorterDuff.Mode.SRC_IN)
        }
    }

    private fun setupTouchListener(button: ImageView) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false
        var longPressTriggered = false
        var longPressRunnable: Runnable? = null

        button.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    longPressTriggered = false

                    if (onLongClick != null) {
                        longPressRunnable = Runnable {
                            if (!isDragging) {
                                longPressTriggered = true
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                onLongClick.invoke()
                            }
                        }
                        handler.postDelayed(longPressRunnable!!, LONG_PRESS_DELAY)
                    }
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    if (dx * dx + dy * dy > MOVE_THRESHOLD * MOVE_THRESHOLD) {
                        isDragging = true
                        longPressRunnable?.let { handler.removeCallbacks(it) }
                    }
                    layoutParams.x = (initialX + dx.toInt())
                        .coerceIn(0, screenWidth - currentButtonSize)
                    layoutParams.y = (initialY + dy.toInt())
                        .coerceIn(0, screenHeight - currentButtonSize)
                    windowManager.updateViewLayout(button, layoutParams)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    longPressRunnable?.let { handler.removeCallbacks(it) }
                    if (!isDragging && !longPressTriggered) {
                        onClick()
                    } else if (isDragging) {
                        snapToEdge(button)
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    longPressRunnable?.let { handler.removeCallbacks(it) }
                    true
                }
                else -> false
            }
        }
    }

    private fun clampPosition() {
        layoutParams.x = layoutParams.x.coerceIn(0, screenWidth - currentButtonSize)
        layoutParams.y = layoutParams.y.coerceIn(0, screenHeight - currentButtonSize)
    }

    private fun snapToEdge(button: ImageView) {
        val centerX = layoutParams.x + currentButtonSize / 2
        val targetX = if (centerX < screenWidth / 2) 0 else screenWidth - currentButtonSize
        val snapSide = if (targetX == 0) 0 else 1

        ValueAnimator.ofInt(layoutParams.x, targetX).apply {
            duration = 250
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                layoutParams.x = animation.animatedValue as Int
                if (floatingButton != null) {
                    windowManager.updateViewLayout(button, layoutParams)
                }
            }
        }.start()

        onPositionSaved(layoutParams.y, snapSide)
    }

    private fun updateScreenSize() {
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getMetrics(metrics)
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels
    }

    private fun dpToPx(dp: Int): Int =
        (dp * context.resources.displayMetrics.density).toInt()
}
