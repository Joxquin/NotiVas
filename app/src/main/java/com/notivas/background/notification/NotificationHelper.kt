package com.notivas.background.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.notivas.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val notificationIdGenerator = AtomicInteger((System.currentTimeMillis() % 100000).toInt())

    companion object {
        const val CHANNEL_REMINDERS_ID = "assignment_reminders"
        const val CHANNEL_REMINDERS_NAME = "Recordatorios de Tareas"

        const val CHANNEL_GENERAL_ID = "general_notifications"
        const val CHANNEL_GENERAL_NAME = "Avisos Generales"
    }

    fun showNotification(
        title: String,
        message: String,
        channelId: String = CHANNEL_REMINDERS_ID,
        notificationId: Int? = null
    ) {
        createChannels()

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setGroup("ASSIGNMENT_REMINDERS")
            .build()

        val id = notificationId ?: notificationIdGenerator.incrementAndGet()
        notificationManager.notify(id, notification)
        
        // Generar notificación resumen para agrupar múltiples alertas
        val summaryNotification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setStyle(NotificationCompat.InboxStyle().setSummaryText("Recordatorios Activos"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setGroup("ASSIGNMENT_REMINDERS")
            .setGroupSummary(true)
            .setAutoCancel(true)
            .build()
            
        notificationManager.notify(99999, summaryNotification)
    }

    private fun createChannels() {
        val reminderChannel = NotificationChannel(
            CHANNEL_REMINDERS_ID,
            CHANNEL_REMINDERS_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Canal prioritario para recordatorios de entrega de tareas y evaluaciones"
        }

        val generalChannel = NotificationChannel(
            CHANNEL_GENERAL_ID,
            CHANNEL_GENERAL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Avisos y alertas generales de la aplicación"
        }

        notificationManager.createNotificationChannel(reminderChannel)
        notificationManager.createNotificationChannel(generalChannel)
    }
}
