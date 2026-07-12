package com.example.expirytracker1.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import java.util.UUID

data class PantryItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val quantity: String,
    val expiryDate: String,
    val daysLeft: Int,
    val icon: ImageVector,
    val statusColor: Color,
    val category: String,
    val addedTimestamp: Long = System.currentTimeMillis()
)
