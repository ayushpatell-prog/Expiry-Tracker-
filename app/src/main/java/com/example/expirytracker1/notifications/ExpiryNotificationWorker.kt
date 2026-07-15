package com.example.expirytracker1.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class ExpiryNotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val title = inputData.getString("title") ?: "Expiry Alert"
        val message = inputData.getString("message") ?: "You have items expiring soon!"
        
        NotificationHelper.showNotification(
            applicationContext,
            title,
            message,
            (System.currentTimeMillis() % 10000).toInt()
        )
        
        return Result.success()
    }
}
