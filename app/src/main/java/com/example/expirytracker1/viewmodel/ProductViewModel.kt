package com.example.expirytracker1.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.expirytracker1.api.ProductData
import com.example.expirytracker1.data.PantryItem
import com.example.expirytracker1.notifications.NotificationHelper
import com.example.expirytracker1.notifications.NotificationRepository
import com.example.expirytracker1.screens.NotificationType
import com.example.expirytracker1.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ProductViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ProductRepository()
    
    private val _products = MutableStateFlow<List<PantryItem>>(emptyList())
    val products: StateFlow<List<PantryItem>> = _products.asStateFlow()

    private val _scannedProduct = MutableStateFlow<ProductData?>(null)
    val scannedProduct = _scannedProduct.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    init {
        loadProducts()
    }

    private fun loadProducts() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getItemsFlow()
                .catch { e ->
                    _error.value = "Failed to load products: ${e.message}"
                    _isLoading.value = false
                }
                .collect { items ->
                    _products.value = items
                    _isLoading.value = false
                    checkExpirations(items)
                }
        }
    }

    private fun checkExpirations(items: List<PantryItem>) {
        items.forEach { item ->
            generateNotificationIfNecessary(item)
        }
    }

    private fun generateNotificationIfNecessary(item: PantryItem) {
        val title: String
        val message: String
        val type: NotificationType

        when (item.daysLeft) {
            0 -> {
                title = "${item.name} Expires Today!"
                message = "Your ${item.name} reached its expiry date. Use it now!"
                type = NotificationType.EXPIRY
            }
            in 1..3 -> {
                title = "${item.name} Expiring Soon"
                message = "${item.name} will expire in ${item.daysLeft} days."
                type = NotificationType.REMINDER
            }
            else -> return
        }

        // Add to in-app real-time screen repository
        NotificationRepository.addNotification(title, message, type)
        
        // Ensure system tray notification
        NotificationHelper.showNotification(
            getApplication(),
            title,
            message,
            item.id.hashCode()
        )
    }

    fun scanBarcode(barcode: String) {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                val product = repository.fetchProductFromApi(barcode)
                if (product != null) {
                    _scannedProduct.value = product
                } else {
                    // Product not found in database, open manual entry with barcode
                    _scannedProduct.value = ProductData(
                        product_name = "",
                        brands = "",
                        categories = "",
                        image_url = "",
                        code = barcode
                    )
                }
            } catch (e: Exception) {
                // Network or API failure, still allow manual entry
                _scannedProduct.value = ProductData(
                    product_name = "",
                    brands = "",
                    categories = "",
                    image_url = "",
                    code = barcode
                )
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun clearScannedProduct() {
        _scannedProduct.value = null
    }

    fun addProduct(item: PantryItem, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.saveProduct(item)
                NotificationHelper.scheduleExpiryReminder(getApplication(), item)
                onSuccess()
            } catch (e: Exception) {
                _error.value = "Failed to add product: ${e.message}"
                android.util.Log.e("ProductViewModel", "Error adding product", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteProduct(item: PantryItem) {
        viewModelScope.launch {
            try {
                repository.deleteProduct(item.id)
            } catch (e: Exception) {
                _error.value = "Failed to delete product: ${e.message}"
            }
        }
    }
    
    fun deleteProductById(id: String) {
        viewModelScope.launch {
            try {
                repository.deleteProduct(id)
            } catch (e: Exception) {
                _error.value = "Failed to delete product: ${e.message}"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
