package com.example.gateopener

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews

class GateWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, GateWidgetProvider::class.java))
            for (id in ids) {
                updateAppWidget(context, manager, id)
            }
        }

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_gate)

            // 1. Установка фонового изображения (пользовательского или очищенного 3D-фона ворот)
            val customBitmap = GatePrefs.loadWidgetBgBitmap(context)
            if (customBitmap != null) {
                views.setImageViewBitmap(R.id.widget_bg_image, customBitmap)
            } else {
                views.setImageViewResource(R.id.widget_bg_image, R.drawable.default_gate_bg)
            }

            // 2. Установка необязательной подписи в левом верхнем углу
            val title = GatePrefs.getWidgetTitle(context)
            if (title.isNotEmpty()) {
                views.setTextViewText(R.id.widget_title_text, title)
                views.setViewVisibility(R.id.widget_title_text, View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.widget_title_text, View.GONE)
            }

            // 3. Выбор темы кнопки на виджете (Тёмная или Светлая)
            val isLight = GatePrefs.isLightWidgetTheme(context)
            if (isLight) {
                views.setViewVisibility(R.id.btn_open_gate_light, View.VISIBLE)
                views.setViewVisibility(R.id.btn_open_gate_dark, View.GONE)
            } else {
                views.setViewVisibility(R.id.btn_open_gate_dark, View.VISIBLE)
                views.setViewVisibility(R.id.btn_open_gate_light, View.GONE)
            }

            // 4. Привязка нажатия текстовой кнопки «Открыть ворота» к прямому телефонному вызову через CallActionActivity
            val callIntent = Intent(context, CallActionActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val callPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                callIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_open_gate_dark, callPendingIntent)
            views.setOnClickPendingIntent(R.id.btn_open_gate_light, callPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
