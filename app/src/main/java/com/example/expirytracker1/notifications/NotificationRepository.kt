package com.example.expirytracker1.notifications

import androidx.compose.runtime.mutableStateListOf
import com.example.expirytracker1.screens.NotificationItem
import com.example.expirytracker1.screens.NotificationType
import java.util.UUID

object NotificationRepository {
    private val _notifications = mutableStateListOf<NotificationItem>()
    val notifications: List<NotificationItem> get() = _notifications

    fun addNotification(title: String, message: String, type: NotificationType) {
        // Prevent duplicate notifications for the same item/message within a short time if needed
        if (_notifications.any { it.title == title && it.message == message && !it.isRead }) return

        _notifications.add(0, NotificationItem(
            id = UUID.randomUUID().toString(),
            title = title,
            message = message,
            type = type,
            timestamp = System.currentTimeMillis(),
            isRead = false
        )
        )
    }

    fun markAsRead(id: String) {
        val index = _notifications.indexOfFirst { it.id == id }
        if (index != -1) {
            val item = _notifications[index]
            _notifications[index] = item.copy(isRead = true)
        }
    }

    fun deleteSelected(ids: List<String>) {
        _notifications.removeAll { ids.contains(it.id) }
    }

    fun clearAll() {
        _notifications.clear()
    }
}
