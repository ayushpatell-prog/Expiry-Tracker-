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
            val diff = expiryTimestamp - System.currentTimeMillis()
            if (diff <= 0) return 0
            // Round up to ensure 1.1 days shows as 2 days
            return kotlin.math.ceil(diff.toDouble() / (1000 * 60 * 60 * 24)).toInt()
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
            daysLeft < 3 -> Color(0xFFD32F2F) // Red
            daysLeft < 7 -> Color(0xFFFBC02D) // Yellow/Orange
            else -> Color(0xFF4CAF50) // Green
        }
}
