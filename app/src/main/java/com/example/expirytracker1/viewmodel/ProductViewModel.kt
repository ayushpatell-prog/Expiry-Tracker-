package com.example.expirytracker1.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Icecream
import androidx.compose.material.icons.filled.LocalDrink
import androidx.lifecycle.ViewModel
import com.example.expirytracker1.data.PantryItem

class ProductViewModel : ViewModel() {
    // Shared list of products using mutableStateListOf for automatic UI updates
    private val _products = mutableStateListOf<PantryItem>()
    val products: List<PantryItem> get() = _products

    init {
        // Initial mock data
        addInitialMockData()
    }

    private fun addInitialMockData() {
        _products.addAll(
            listOf(
                PantryItem(
                    name = "Organic Broccoli",
                    quantity = "2",
                    expiryDate = "24 Oct",
                    daysLeft = 5,
                    icon = Icons.Default.Eco,
                    statusColor = Color(0xFFD32F2F),
                    category = "Vegetables"
                ),
                PantryItem(
                    name = "Baby Carrots",
                    quantity = "1 bag",
                    expiryDate = "30 Oct",
                    daysLeft = 11,
                    icon = Icons.Default.BakeryDining,
                    statusColor = Color(0xFF4CAF50),
                    category = "Vegetables"
                ),
                PantryItem(
                    name = "Whole Milk",
                    quantity = "1/2 Gal",
                    expiryDate = "22 Oct",
                    daysLeft = 3,
                    icon = Icons.Default.LocalDrink,
                    statusColor = Color(0xFFD32F2F),
                    category = "Dairy"
                ),
                PantryItem(
                    name = "Greek Yogurt",
                    quantity = "2 cups",
                    expiryDate = "15 Oct",
                    daysLeft = 1,
                    icon = Icons.Default.Icecream,
                    statusColor = Color(0xFFFBC02D),
                    category = "Dairy"
                )
            )
        )
    }

    fun addProduct(item: PantryItem) {
        _products.add(item)
    }

    fun deleteProduct(item: PantryItem) {
        _products.removeIf { it.id == item.id }
    }
    
    fun deleteProductById(id: String) {
        _products.removeIf { it.id == id }
    }
}
