package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.core.database.Gallery56Database
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Gallery56PhotoWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = Gallery56Database.getInstance(context)
                val all = db.mediaIndexDao().getAllMediaSnapshot()
                val featured = all.firstOrNull { !it.isVideo && it.isFavorite }
                    ?: all.firstOrNull { !it.isVideo }

                val bmp = featured?.let { entity ->
                    runCatching {
                        context.contentResolver.openInputStream(Uri.parse(entity.contentUri))?.use { stream ->
                            BitmapFactory.decodeStream(
                                stream,
                                null,
                                BitmapFactory.Options().apply { inSampleSize = 4 },
                            )
                        }
                    }.getOrNull()
                }

                val launchIntent = Intent(context, MainActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )

                appWidgetIds.forEach { widgetId ->
                    val views = RemoteViews(context.packageName, R.layout.widget_gallery56_photo).apply {
                        setTextViewText(
                            R.id.widget_title,
                            featured?.displayName ?: "Gallery56 Featured Photo",
                        )
                        if (bmp != null) {
                            setImageViewBitmap(R.id.widget_image, bmp)
                        }
                        setOnClickPendingIntent(R.id.widget_root, pendingIntent)
                    }
                    appWidgetManager.updateAppWidget(widgetId, views)
                }
                bmp?.recycle()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
