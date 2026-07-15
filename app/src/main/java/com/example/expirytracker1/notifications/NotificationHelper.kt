package com.example.expirytracker1.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.example.expirytracker1.MainActivity
import com.example.expirytracker1.R
import com.example.expirytracker1.data.PantryItem
import java.util.concurrent.TimeUnit

object NotificationHelper {
    private const val CHANNEL_ID = "expiry_notifications"
    private const val CHANNEL_NAME = "Expiry Alerts"
    private const val CHANNEL_DESC = "Notifications for expiring products"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun scheduleExpiryReminder(context: Context, item: PantryItem) {
        val workManager = WorkManager.getInstance(context)
        
        // Calculate delay based on reminder selection
        // For simplicity in this demo, we'll use a fixed short delay or a calculation
        // In production, you'd calculate: item.expiryDate - reminderOffset - currentTime
        
        val data = workDataOf(
            "title" to "FreshKeeper Reminder",
            "message" to "${item.name} expires soon (${item.expiryDate})"
        )

        val reminderWork = OneTimeWorkRequestBuilder<ExpiryNotificationWorker>()
            .setInitialDelay(10, TimeUnit.SECONDS) // Mock delay for testing
            .setInputData(data)
            .build()

        workManager.enqueueUniqueWork(
            item.id,
            ExistingWorkPolicy.REPLACE,
            reminderWork
        )
    }

    fun showNotification(context: Context, title: String, message: String, notificationId: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Using system icon for now
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            try {
                notify(notificationId, builder.build())
            } catch (e: SecurityException) {
                // Handle permission not granted
            }
        }
    }
}
