package com.screen.brightness

import android.Manifest
import android.app.ActivityManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.switchmaterial.SwitchMaterial

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var actionButton: Button
    private lateinit var previewButton: ImageView
    private lateinit var backPreviewButton: ImageView
    private lateinit var backSwitch: SwitchMaterial
    private lateinit var backAccessibilityStatus: TextView
    private lateinit var btnGrantAccessibility: Button
    private lateinit var backSettingsContainer: LinearLayout

    // 不透明色，透明度由 SeekBar 独立控制
    private val bgColors = intArrayOf(
        0xFF333333.toInt(), 0xFF000000.toInt(), 0xFF1565C0.toInt(),
        0xFFC62828.toInt(), 0xFF2E7D32.toInt(), 0xFFFF8F00.toInt()
    )

    private val fgColors = intArrayOf(
        0xFFFFFFFF.toInt(), 0xFFFFEB3B.toInt(), 0xFFFF5722.toInt(),
        0xFF4CAF50.toInt(), 0xFF2196F3.toInt(), 0xFF000000.toInt()
    )

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            checkPermissionsAndStart()
        } else {
            updateStatus(getString(R.string.status_notification_denied))
        }
    }

    private val svgPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri ?: return@registerForActivityResult
        importSvg(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppPrefs.migrateIfNeeded(this)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.status_text)
        actionButton = findViewById(R.id.action_button)
        previewButton = findViewById(R.id.preview_button)
        backPreviewButton = findViewById(R.id.back_preview_button)
        backSwitch = findViewById(R.id.back_btn_switch)
        backAccessibilityStatus = findViewById(R.id.back_accessibility_status)
        btnGrantAccessibility = findViewById(R.id.btn_grant_accessibility)
        backSettingsContainer = findViewById(R.id.back_settings_container)

        actionButton.setOnClickListener {
            checkPermissionsAndStart()
        }

        setupBrightnessSettings()
        setupBackButtonSettings()
    }

    override fun onResume() {
        super.onResume()
        checkPermissionsAndStart()
        updateBackButtonUI()
    }

    // --- 权限检查与亮度服务启动 ---

    private fun checkPermissionsAndStart() {
        when {
            !Settings.canDrawOverlays(this) -> {
                updateStatus(getString(R.string.status_overlay_needed))
                actionButton.text = getString(R.string.btn_grant_overlay)
                actionButton.setOnClickListener {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    startActivity(intent)
                }
            }
            !Settings.System.canWrite(this) -> {
                updateStatus(getString(R.string.status_write_settings_needed))
                actionButton.text = getString(R.string.btn_grant_settings)
                actionButton.setOnClickListener {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_WRITE_SETTINGS,
                        Uri.parse("package:$packageName")
                    )
                    startActivity(intent)
                }
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED -> {
                updateStatus(getString(R.string.status_notification_needed))
                actionButton.text = getString(R.string.btn_grant_notification)
                actionButton.setOnClickListener {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            else -> {
                if (!isServiceRunning()) {
                    startFloatingService()
                }
                updateStatus(getString(R.string.status_running))
                actionButton.text = getString(R.string.btn_stop)
                actionButton.setOnClickListener {
                    stopService(Intent(this, FloatingButtonService::class.java))
                    Toast.makeText(this, getString(R.string.toast_stopped), Toast.LENGTH_SHORT)
                        .show()
                    updateStatus(getString(R.string.status_stopped))
                    actionButton.text = getString(R.string.btn_start)
                    actionButton.setOnClickListener {
                        checkPermissionsAndStart()
                    }
                }
            }
        }
    }

    private fun isServiceRunning(): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        @Suppress("DEPRECATION")
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (FloatingButtonService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }

    private fun startFloatingService() {
        startForegroundService(Intent(this, FloatingButtonService::class.java))
    }

    private fun sendRefreshIntent() {
        if (isServiceRunning()) {
            val intent = Intent(this, FloatingButtonService::class.java).apply {
                action = FloatingButtonService.ACTION_REFRESH
            }
            startService(intent)
        }
    }

    private fun updateStatus(text: String) {
        statusText.text = text
    }

    // --- SVG 导入 ---

    private fun importSvg(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri) ?: return
            val svgContent = inputStream.bufferedReader().readText()
            inputStream.close()

            if (!IconHelper.isValidSvg(svgContent)) {
                Toast.makeText(this, getString(R.string.toast_svg_error), Toast.LENGTH_SHORT).show()
                return
            }

            val fileName = getFileName(uri) ?: "custom.svg"
            AppPrefs.setCustomSvg(this, svgContent, fileName)
            AppPrefs.setIconType(this, "custom")

            Toast.makeText(this, getString(R.string.toast_svg_imported), Toast.LENGTH_SHORT).show()
            setupIconRow()
            updatePreview()
            sendRefreshIntent()
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.toast_svg_error), Toast.LENGTH_SHORT).show()
        }
    }

    private fun getFileName(uri: Uri): String? {
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) return it.getString(nameIndex)
            }
        }
        return null
    }

    // --- 亮度按钮设置 ---

    private fun setupBrightnessSettings() {
        updatePreview()
        setupColorRow(findViewById(R.id.bg_color_row), bgColors, AppPrefs.getBgColor(this)) { color ->
            AppPrefs.setBgColor(this, color)
            updatePreview()
            sendRefreshIntent()
        }
        setupColorRow(findViewById(R.id.fg_color_row), fgColors, AppPrefs.getFgColor(this)) { color ->
            AppPrefs.setFgColor(this, color)
            updatePreview()
            sendRefreshIntent()
        }
        setupIconRow()
        setupIconAlphaSeekBar()
        setupBgAlphaSeekBar()
        setupSizeSeekBar()
    }

    private fun updatePreview() {
        val bgColor = AppPrefs.getBgColor(this)
        val fgColor = AppPrefs.getFgColor(this)
        val iconAlpha = AppPrefs.getIconAlpha(this)
        val bgAlpha = AppPrefs.getBgAlpha(this)
        val sizeDp = AppPrefs.getSize(this)
        val sizePx = (sizeDp * resources.displayMetrics.density).toInt()
        val iconPadding = (sizePx * 0.22f).toInt()

        previewButton.setImageDrawable(IconHelper.loadIconDrawable(this))
        val colorWithAlpha = (bgAlpha shl 24) or (bgColor and 0x00FFFFFF)
        previewButton.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(colorWithAlpha)
        }
        previewButton.imageAlpha = iconAlpha
        previewButton.alpha = 1.0f
        previewButton.setPadding(iconPadding, iconPadding, iconPadding, iconPadding)
        previewButton.layoutParams = previewButton.layoutParams.apply {
            width = sizePx
            height = sizePx
        }

        if (IconHelper.isCustomIcon(this)) {
            previewButton.colorFilter = null
        } else {
            previewButton.setColorFilter(fgColor, PorterDuff.Mode.SRC_IN)
        }
    }

    private fun setupColorRow(
        container: LinearLayout,
        colors: IntArray,
        selectedColor: Int,
        onSelect: (Int) -> Unit
    ) {
        container.removeAllViews()
        val circleSize = (36 * resources.displayMetrics.density).toInt()
        val margin = (8 * resources.displayMetrics.density).toInt()

        for (color in colors) {
            val view = View(this).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(color)
                    if (color == selectedColor) {
                        setStroke(
                            (3 * resources.displayMetrics.density).toInt(),
                            ContextCompat.getColor(this@MainActivity, R.color.accent)
                        )
                    }
                }
                layoutParams = LinearLayout.LayoutParams(circleSize, circleSize).apply {
                    setMargins(margin, 0, margin, 0)
                }
                setOnClickListener {
                    onSelect(color)
                    setupColorRow(container, colors, color, onSelect)
                }
            }
            container.addView(view)
        }
    }

    private fun setupIconRow() {
        val container = findViewById<LinearLayout>(R.id.icon_row)
        container.removeAllViews()
        val selectedType = AppPrefs.getIconType(this)
        val iconSize = (36 * resources.displayMetrics.density).toInt()
        val margin = (6 * resources.displayMetrics.density).toInt()
        val padding = (6 * resources.displayMetrics.density).toInt()

        for (type in IconHelper.PRESET_TYPES) {
            val iv = ImageView(this).apply {
                setImageDrawable(IconHelper.getPresetDrawable(this@MainActivity, type))
                setColorFilter(Color.DKGRAY, PorterDuff.Mode.SRC_IN)
                scaleType = ImageView.ScaleType.FIT_CENTER
                setPadding(padding, padding, padding, padding)
                layoutParams = LinearLayout.LayoutParams(iconSize, iconSize).apply {
                    setMargins(margin, 0, margin, 0)
                }
                if (type == selectedType) {
                    setBackgroundColor(Color.parseColor("#30000000"))
                }
                setOnClickListener {
                    AppPrefs.setIconType(this@MainActivity, type)
                    setupIconRow()
                    updatePreview()
                    sendRefreshIntent()
                }
            }
            container.addView(iv)
        }

        if (selectedType == "custom") {
            val customIv = ImageView(this).apply {
                setImageDrawable(IconHelper.loadIconDrawable(this@MainActivity))
                scaleType = ImageView.ScaleType.FIT_CENTER
                setPadding(padding, padding, padding, padding)
                layoutParams = LinearLayout.LayoutParams(iconSize, iconSize).apply {
                    setMargins(margin, 0, margin, 0)
                }
                setBackgroundColor(Color.parseColor("#30000000"))
            }
            container.addView(customIv)
        }

        val importBtn = TextView(this).apply {
            text = getString(R.string.settings_import_svg)
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(padding, padding, padding, padding)
            setOnClickListener {
                svgPickerLauncher.launch("*/*")
            }
        }
        container.addView(importBtn)
    }

    private fun setupIconAlphaSeekBar() {
        val seekBar = findViewById<SeekBar>(R.id.icon_alpha_seekbar)
        seekBar.progress = AppPrefs.getIconAlpha(this)
        seekBar.setOnSeekBarChangeListener(simpleSeekBarListener { progress ->
            AppPrefs.setIconAlpha(this, progress)
            updatePreview()
            sendRefreshIntent()
        })
    }

    private fun setupBgAlphaSeekBar() {
        val seekBar = findViewById<SeekBar>(R.id.bg_alpha_seekbar)
        seekBar.progress = AppPrefs.getBgAlpha(this)
        seekBar.setOnSeekBarChangeListener(simpleSeekBarListener { progress ->
            AppPrefs.setBgAlpha(this, progress)
            updatePreview()
            sendRefreshIntent()
        })
    }

    private fun setupSizeSeekBar() {
        val seekBar = findViewById<SeekBar>(R.id.size_seekbar)
        seekBar.progress = AppPrefs.getSize(this) - 40
        seekBar.setOnSeekBarChangeListener(simpleSeekBarListener { progress ->
            AppPrefs.setSize(this, 40 + progress)
            updatePreview()
            sendRefreshIntent()
        })
    }

    // --- 返回按钮设置 ---

    private fun setupBackButtonSettings() {
        updateBackPreview()
        backSwitch.isChecked = AppPrefs.isBackEnabled(this)
        backSwitch.setOnCheckedChangeListener { _, checked ->
            AppPrefs.setBackEnabled(this, checked)
            updateBackButtonUI()
            sendBackRefreshIntent()
        }

        btnGrantAccessibility.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }

        setupColorRow(
            findViewById(R.id.back_bg_color_row), bgColors,
            AppPrefs.getBackBgColor(this)
        ) { color ->
            AppPrefs.setBackBgColor(this, color)
            updateBackPreview()
            sendBackRefreshIntent()
        }
        setupColorRow(
            findViewById(R.id.back_fg_color_row), fgColors,
            AppPrefs.getBackFgColor(this)
        ) { color ->
            AppPrefs.setBackFgColor(this, color)
            updateBackPreview()
            sendBackRefreshIntent()
        }

        val backIconAlphaSeekBar = findViewById<SeekBar>(R.id.back_icon_alpha_seekbar)
        backIconAlphaSeekBar.progress = AppPrefs.getBackIconAlpha(this)
        backIconAlphaSeekBar.setOnSeekBarChangeListener(simpleSeekBarListener { progress ->
            AppPrefs.setBackIconAlpha(this, progress)
            updateBackPreview()
            sendBackRefreshIntent()
        })

        val backBgAlphaSeekBar = findViewById<SeekBar>(R.id.back_bg_alpha_seekbar)
        backBgAlphaSeekBar.progress = AppPrefs.getBackBgAlpha(this)
        backBgAlphaSeekBar.setOnSeekBarChangeListener(simpleSeekBarListener { progress ->
            AppPrefs.setBackBgAlpha(this, progress)
            updateBackPreview()
            sendBackRefreshIntent()
        })

        val backSizeSeekBar = findViewById<SeekBar>(R.id.back_size_seekbar)
        backSizeSeekBar.progress = AppPrefs.getBackSize(this) - 40
        backSizeSeekBar.setOnSeekBarChangeListener(simpleSeekBarListener { progress ->
            AppPrefs.setBackSize(this, 40 + progress)
            updateBackPreview()
            sendBackRefreshIntent()
        })
    }

    private fun updateBackPreview() {
        val bgColor = AppPrefs.getBackBgColor(this)
        val fgColor = AppPrefs.getBackFgColor(this)
        val iconAlpha = AppPrefs.getBackIconAlpha(this)
        val bgAlpha = AppPrefs.getBackBgAlpha(this)
        val sizeDp = AppPrefs.getBackSize(this)
        val sizePx = (sizeDp * resources.displayMetrics.density).toInt()
        val iconPadding = (sizePx * 0.22f).toInt()

        backPreviewButton.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.ic_back)?.mutate()
        )
        val colorWithAlpha = (bgAlpha shl 24) or (bgColor and 0x00FFFFFF)
        backPreviewButton.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(colorWithAlpha)
        }
        backPreviewButton.imageAlpha = iconAlpha
        backPreviewButton.alpha = 1.0f
        backPreviewButton.setColorFilter(fgColor, PorterDuff.Mode.SRC_IN)
        backPreviewButton.setPadding(iconPadding, iconPadding, iconPadding, iconPadding)
        backPreviewButton.layoutParams = backPreviewButton.layoutParams.apply {
            width = sizePx
            height = sizePx
        }
    }

    private fun updateBackButtonUI() {
        val enabled = AppPrefs.isBackEnabled(this)
        val serviceRunning = isAccessibilityServiceEnabled()

        backSettingsContainer.visibility = if (enabled) View.VISIBLE else View.GONE
        backPreviewButton.visibility = if (enabled) View.VISIBLE else View.GONE

        if (enabled && !serviceRunning) {
            backAccessibilityStatus.text = getString(R.string.back_accessibility_needed)
            backAccessibilityStatus.visibility = View.VISIBLE
            btnGrantAccessibility.visibility = View.VISIBLE
        } else {
            backAccessibilityStatus.visibility = View.GONE
            btnGrantAccessibility.visibility = View.GONE
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedComponent = ComponentName(this, BackButtonAccessibilityService::class.java)
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.split(':').any {
            ComponentName.unflattenFromString(it) == expectedComponent
        }
    }

    private fun sendBackRefreshIntent() {
        BackButtonAccessibilityService.instance?.refreshButton()
    }

    // --- 工具方法 ---

    private fun simpleSeekBarListener(onProgress: (Int) -> Unit): SeekBar.OnSeekBarChangeListener {
        return object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) onProgress(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }
    }
}
