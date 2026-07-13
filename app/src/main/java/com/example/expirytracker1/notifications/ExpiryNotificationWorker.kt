package com.example.expirytracker1.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.expirytracker1.viewmodel.ProductViewModel

class ExpiryNotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        // In a real app, we would fetch products from a database.
        // For this mock-up, we'll simulate checking items.
        
        NotificationHelper.showNotification(
            applicationContext,
            "Expiry Alert",
            "You have items expiring soon! Check your inventory.",
            1001
        )
        
        return Result.success()
    }
}
