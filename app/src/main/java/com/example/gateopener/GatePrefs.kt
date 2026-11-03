package com.example.gateopener

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import java.io.File
import java.io.FileOutputStream

object GatePrefs {
    private const val PREFS_NAME = "gate_widget_prefs"
    private const val KEY_PHONE = "phone_number"
    private const val KEY_TITLE = "widget_title"
    private const val KEY_LIGHT_THEME = "widget_light_theme"
    private const val CUSTOM_BG_FILENAME = "custom_gate_bg.png"

    fun getPhoneNumber(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_PHONE, "") ?: ""
    }

    fun setPhoneNumber(context: Context, phone: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PHONE, phone.trim())
            .apply()
    }

    fun getWidgetTitle(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_TITLE, "") ?: ""
    }

    fun setWidgetTitle(context: Context, title: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TITLE, title.trim())
            .apply()
    }

    /**
     * Возвращает true, если выбрана светлая кнопка на виджете; false — если тёмная (по умолчанию).
     */
    fun isLightWidgetTheme(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_LIGHT_THEME, false)
    }

    fun setLightWidgetTheme(context: Context, isLight: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_LIGHT_THEME, isLight)
            .apply()
    }

    fun getCustomBgFile(context: Context): File {
        return File(context.filesDir, CUSTOM_BG_FILENAME)
    }

    fun clearCustomBg(context: Context) {
        val file = getCustomBgFile(context)
        if (file.exists()) {
            file.delete()
        }
    }

    /**
     * Масштабирует и обрезает выбранное пользователем изображение под пропорции виджета 4x2 (800x400 px)
     * со скругленными углами, чтобы избежать ошибки превышения лимита памяти Binder в RemoteViews.
     */
    fun saveCustomBgBitmap(context: Context, source: Bitmap): Boolean {
        return try {
            val targetWidth = 800
            val targetHeight = 400

            val scaled = centerCropBitmap(source, targetWidth, targetHeight)
            val rounded = getRoundedCornerBitmap(scaled, 48f)

            val file = getCustomBgFile(context)
            FileOutputStream(file).use { out ->
                rounded.compress(Bitmap.CompressFormat.PNG, 95, out)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun loadWidgetBgBitmap(context: Context): Bitmap? {
        val file = getCustomBgFile(context)
        if (file.exists()) {
            return BitmapFactory.decodeFile(file.absolutePath)
        }
        return null
    }

    private fun centerCropBitmap(src: Bitmap, targetW: Int, targetH: Int): Bitmap {
        val srcW = src.width.toFloat()
        val srcH = src.height.toFloat()
        val scale = maxOf(targetW / srcW, targetH / srcH)

        val scaledW = scale * srcW
        val scaledH = scale * srcH
        val left = (targetW - scaledW) / 2f
        val top = (targetH - scaledH) / 2f

        val targetRect = RectF(left, top, left + scaledW, top + scaledH)
        val dest = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(dest)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(src, null, targetRect, paint)
        return dest
    }

    private fun getRoundedCornerBitmap(bitmap: Bitmap, cornerRadiusPx: Float): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = Rect(0, 0, bitmap.width, bitmap.height)
        val rectF = RectF(rect)

        canvas.drawARGB(0, 0, 0, 0)
        canvas.drawRoundRect(rectF, cornerRadiusPx, cornerRadiusPx, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)
        return output
    }
}
