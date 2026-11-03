package com.example.gateopener

import android.Manifest
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class MainActivity : AppCompatActivity() {

    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    private var isLightThemeSelected: Boolean = false

    private lateinit var tvPermissionStatus: TextView
    private lateinit var ivPreviewBg: ImageView
    private lateinit var tvPreviewTitle: TextView
    private lateinit var previewBtnOpenGate: TextView
    private lateinit var etPhoneNumber: TextInputEditText
    private lateinit var etWidgetTitle: TextInputEditText
    private lateinit var btnThemeDark: MaterialButton
    private lateinit var btnThemeLight: MaterialButton

    private val requestCallPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        updatePermissionBanner()
    }

    private val pickBackgroundLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            handleSelectedImage(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            setResult(Activity.RESULT_CANCELED)
        }

        tvPermissionStatus = findViewById(R.id.tv_permission_status)
        ivPreviewBg = findViewById(R.id.iv_preview_bg)
        tvPreviewTitle = findViewById(R.id.tv_preview_title)
        previewBtnOpenGate = findViewById(R.id.preview_btn_open_gate)
        etPhoneNumber = findViewById(R.id.et_phone_number)
        etWidgetTitle = findViewById(R.id.et_widget_title)
        btnThemeDark = findViewById(R.id.btn_theme_dark)
        btnThemeLight = findViewById(R.id.btn_theme_light)

        val btnChooseBg = findViewById<MaterialButton>(R.id.btn_choose_bg)
        val btnResetBg = findViewById<MaterialButton>(R.id.btn_reset_bg)
        val btnSave = findViewById<MaterialButton>(R.id.btn_save)
        val btnTestCall = findViewById<MaterialButton>(R.id.btn_test_call)

        etPhoneNumber.setText(GatePrefs.getPhoneNumber(this))
        etWidgetTitle.setText(GatePrefs.getWidgetTitle(this))
        isLightThemeSelected = GatePrefs.isLightWidgetTheme(this)

        refreshPreview()
        updateThemeSelectorButtons()
        updatePermissionBanner()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestCallPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
        }

        tvPermissionStatus.setOnClickListener {
            requestCallPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
        }

        btnThemeDark.setOnClickListener {
            isLightThemeSelected = false
            GatePrefs.setLightWidgetTheme(this, false)
            updateThemeSelectorButtons()
            refreshPreview()
            GateWidgetProvider.updateAllWidgets(this)
        }

        btnThemeLight.setOnClickListener {
            isLightThemeSelected = true
            GatePrefs.setLightWidgetTheme(this, true)
            updateThemeSelectorButtons()
            refreshPreview()
            GateWidgetProvider.updateAllWidgets(this)
        }

        etWidgetTitle.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val title = s?.toString()?.trim().orEmpty()
                if (title.isNotEmpty()) {
                    tvPreviewTitle.text = title
                    tvPreviewTitle.visibility = View.VISIBLE
                } else {
                    tvPreviewTitle.visibility = View.GONE
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnChooseBg.setOnClickListener {
            pickBackgroundLauncher.launch("image/*")
        }

        btnResetBg.setOnClickListener {
            GatePrefs.clearCustomBg(this)
            refreshPreview()
            GateWidgetProvider.updateAllWidgets(this)
            Toast.makeText(this, "Восстановлен стандартный 3D-фон ворот", Toast.LENGTH_SHORT).show()
        }

        btnSave.setOnClickListener {
            saveSettingsAndFinishIfWidgetConfig()
        }

        val testCallListener = View.OnClickListener {
            val phone = etPhoneNumber.text?.toString()?.trim().orEmpty()
            if (phone.isEmpty()) {
                Toast.makeText(this, "Сначала введите номер телефона ворот", Toast.LENGTH_SHORT).show()
                return@OnClickListener
            }
            GatePrefs.setPhoneNumber(this, phone)
            GatePrefs.setWidgetTitle(this, etWidgetTitle.text?.toString()?.trim().orEmpty())
            GatePrefs.setLightWidgetTheme(this, isLightThemeSelected)
            GateWidgetProvider.updateAllWidgets(this)

            startActivity(Intent(this, CallActionActivity::class.java))
        }

        btnTestCall.setOnClickListener(testCallListener)
        previewBtnOpenGate.setOnClickListener(testCallListener)
    }

    override fun onResume() {
        super.onResume()
        updatePermissionBanner()
    }

    private fun updateThemeSelectorButtons() {
        if (!isLightThemeSelected) {
            // Активна тёмная кнопка
            btnThemeDark.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#22252A"))
            btnThemeDark.setTextColor(Color.WHITE)
            btnThemeDark.strokeWidth = 0

            btnThemeLight.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#E0E4E8"))
            btnThemeLight.setTextColor(Color.parseColor("#455A64"))
            btnThemeLight.strokeWidth = 0
        } else {
            // Активна светлая кнопка
            btnThemeLight.backgroundTintList = ColorStateList.valueOf(Color.WHITE)
            btnThemeLight.setTextColor(Color.parseColor("#1A1C1E"))
            btnThemeLight.strokeColor = ColorStateList.valueOf(Color.parseColor("#2E7D32"))
            btnThemeLight.strokeWidth = 4

            btnThemeDark.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#E0E4E8"))
            btnThemeDark.setTextColor(Color.parseColor("#455A64"))
            btnThemeDark.strokeWidth = 0
        }
    }

    private fun updatePermissionBanner() {
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            tvPermissionStatus.text = getString(R.string.permission_granted)
            tvPermissionStatus.setBackgroundColor(Color.parseColor("#E8F5E9"))
            tvPermissionStatus.setTextColor(Color.parseColor("#2E7D32"))
        } else {
            tvPermissionStatus.text = getString(R.string.permission_missing)
            tvPermissionStatus.setBackgroundColor(Color.parseColor("#FFF3E0"))
            tvPermissionStatus.setTextColor(Color.parseColor("#E65100"))
        }
    }

    private fun handleSelectedImage(uri: Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    val saved = GatePrefs.saveCustomBgBitmap(this, bitmap)
                    if (saved) {
                        refreshPreview()
                        GateWidgetProvider.updateAllWidgets(this)
                        Toast.makeText(this, "Фоновое изображение обновлено", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Не удалось загрузить изображение: ${e.localizedMessage}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun refreshPreview() {
        val customBitmap = GatePrefs.loadWidgetBgBitmap(this)
        if (customBitmap != null) {
            ivPreviewBg.setImageBitmap(customBitmap)
        } else {
            ivPreviewBg.setImageResource(R.drawable.default_gate_bg)
        }

        val title = etWidgetTitle.text?.toString()?.trim().orEmpty()
        if (title.isNotEmpty()) {
            tvPreviewTitle.text = title
            tvPreviewTitle.visibility = View.VISIBLE
        } else {
            tvPreviewTitle.visibility = View.GONE
        }

        if (isLightThemeSelected) {
            previewBtnOpenGate.setBackgroundResource(R.drawable.widget_button_light_bg)
            previewBtnOpenGate.setTextColor(ContextCompat.getColor(this, R.color.widget_btn_light_text))
        } else {
            previewBtnOpenGate.setBackgroundResource(R.drawable.widget_button_dark_bg)
            previewBtnOpenGate.setTextColor(ContextCompat.getColor(this, R.color.widget_btn_dark_text))
        }
    }

    private fun saveSettingsAndFinishIfWidgetConfig() {
        val phone = etPhoneNumber.text?.toString()?.trim().orEmpty()
        if (phone.isEmpty()) {
            etPhoneNumber.error = "Введите номер телефона ворот"
            return
        }

        val title = etWidgetTitle.text?.toString()?.trim().orEmpty()
        GatePrefs.setPhoneNumber(this, phone)
        GatePrefs.setWidgetTitle(this, title)
        GatePrefs.setLightWidgetTheme(this, isLightThemeSelected)

        GateWidgetProvider.updateAllWidgets(this)
        Toast.makeText(this, "Настройки сохранены, виджет обновлён!", Toast.LENGTH_SHORT).show()

        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            val resultValue = Intent().apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            setResult(Activity.RESULT_OK, resultValue)
            finish()
        }
    }
}
