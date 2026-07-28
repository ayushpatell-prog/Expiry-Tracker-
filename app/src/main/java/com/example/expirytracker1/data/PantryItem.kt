package com.example.expirytracker1.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Icecream
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SetMeal
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.google.firebase.firestore.Exclude
import java.util.UUID

data class PantryItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val quantity: String = "",
    val expiryDate: String = "",
    val expiryTimestamp: Long = 0L,
    val category: String = "",
    val addedTimestamp: Long = System.currentTimeMillis(),
    val brand: String = "",
    val barcode: String = "",
    val imageUrl: String = "",
    val notes: String = "",
    val purchaseDate: String = "",
    val reminder: String = "1 Day Before"
) {
    @get:Exclude
    val daysLeft: Int
        get() {
            if (expiryTimestamp == 0L) return 0
            
            val today = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            
            val expiry = java.util.Calendar.getInstance().apply {
                timeInMillis = expiryTimestamp
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            
            val diff = expiry.timeInMillis - today.timeInMillis
            return (diff / (1000 * 60 * 60 * 24)).toInt()
        }

    @get:Exclude
    val icon: ImageVector
        get() = when (category) {
            "Dairy" -> Icons.Default.Icecream
            "Vegetables", "Fruits", "Produce" -> Icons.Default.Eco
            "Meat", "SetMeal" -> Icons.Default.SetMeal
            "Beverages", "Drinks" -> Icons.Default.LocalDrink
            "Bakery" -> Icons.Default.BakeryDining
            "Meals", "Food" -> Icons.Default.Restaurant
            else -> Icons.Default.Inventory
        }

    @get:Exclude
    val statusColor: Color
        get() = when {
            daysLeft < 0 -> Color(0xFFD32F2F) // Expired - Dark Red
            daysLeft == 0 -> Color(0xFFE64A19) // Today - Orange Red
            daysLeft < 3 -> Color(0xFFFBC02D) // Soon - Yellow
            else -> Color(0xFF4CAF50) // Safe - Green
        }
}
