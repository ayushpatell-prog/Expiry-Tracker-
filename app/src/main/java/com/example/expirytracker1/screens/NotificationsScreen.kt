package com.example.expirytracker1.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expirytracker1.ui.theme.TextGray
import java.text.SimpleDateFormat
import java.util.*

data class NotificationItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val type: NotificationType = NotificationType.EXPIRY
)

enum class NotificationType {
    EXPIRY, REMINDER, SYSTEM
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(onNavigateBack: () -> Unit) {
    // Mock data for notifications
    val notifications = remember {
        mutableStateListOf(
            NotificationItem(
                title = "Greek Yogurt Expiring",
                message = "Your Greek Yogurt expires tomorrow! Don't forget to use it.",
                type = NotificationType.EXPIRY
            ),
            NotificationItem(
                title = "Whole Milk Reminder",
                message = "Whole Milk will expire in 3 days.",
                type = NotificationType.REMINDER
            ),
            NotificationItem(
                title = "Welcome to Expiry Tracker",
                message = "Start adding items to your pantry to get notified before they expire.",
                type = NotificationType.SYSTEM,
                isRead = true
            )
        )
    }

    var isEditMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<String>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    if (isEditMode) {
                        Text("${selectedIds.size} Selected", fontWeight = FontWeight.Bold)
                    } else {
                        Text("Notifications", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    if (isEditMode) {
                        IconButton(onClick = { 
                            isEditMode = false
                            selectedIds.clear()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel selection")
                        }
                    } else {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (notifications.isNotEmpty()) {
                        if (isEditMode) {
                            // Select All / Deselect All
                            val allSelected = selectedIds.size == notifications.size
                            TextButton(onClick = {
                                if (allSelected) {
                                    selectedIds.clear()
                                } else {
                                    selectedIds.clear()
                                    selectedIds.addAll(notifications.map { it.id })
                                }
                            }) {
                                Text(if (allSelected) "Deselect All" else "Select All")
                            }

                            IconButton(
                                onClick = { 
                                    notifications.removeAll { selectedIds.contains(it.id) }
                                    selectedIds.clear()
                                    isEditMode = false
                                },
                                enabled = selectedIds.isNotEmpty()
                            ) {
                                Icon(
                                    Icons.Outlined.Delete, 
                                    contentDescription = "Delete selected", 
                                    tint = if (selectedIds.isNotEmpty()) MaterialTheme.colorScheme.error else TextGray
                                )
                            }
                        } else {
                            IconButton(onClick = { isEditMode = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit notifications")
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (notifications.isEmpty()) {
            EmptyNotificationsState()
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                items(notifications, key = { it.id }) { notification ->
                    val isSelected = selectedIds.contains(notification.id)
                    NotificationCard(
                        notification = notification,
                        isEditMode = isEditMode,
                        isSelected = isSelected,
                        onSelect = {
                            if (isSelected) {
                                selectedIds.remove(notification.id)
                            } else {
                                selectedIds.add(notification.id)
                            }
                        }
                    )
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
fun NotificationCard(
    notification: NotificationItem,
    isEditMode: Boolean,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isEditMode) { onSelect() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            } else if (notification.isRead) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (notification.isRead && !isSelected) 0.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isEditMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onSelect() },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        when (notification.type) {
                            NotificationType.EXPIRY -> Color(0xFFD32F2F).copy(alpha = 0.1f)
                            NotificationType.REMINDER -> Color(0xFFFBC02D).copy(alpha = 0.1f)
                            NotificationType.SYSTEM -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (notification.type) {
                        NotificationType.EXPIRY -> Icons.Outlined.NotificationsActive
                        NotificationType.REMINDER -> Icons.Default.Notifications
                        NotificationType.SYSTEM -> Icons.Default.Notifications
                    },
                    contentDescription = null,
                    tint = when (notification.type) {
                        NotificationType.EXPIRY -> Color(0xFFD32F2F)
                        NotificationType.REMINDER -> Color(0xFFFBC02D)
                        NotificationType.SYSTEM -> MaterialTheme.colorScheme.primary
                    }
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = dateFormat.format(Date(notification.timestamp)),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun EmptyNotificationsState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = TextGray.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No notifications yet",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "We'll notify you when your items are close to expiry.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}
