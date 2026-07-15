package com.example.expirytracker1.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.expirytracker1.api.ProductData
import com.example.expirytracker1.data.PantryItem
import com.example.expirytracker1.notifications.NotificationHelper
import com.example.expirytracker1.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProductViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ProductRepository()
    
    private val _products = mutableStateListOf<PantryItem>()
    val products: List<PantryItem> get() = _products

    private val _scannedProduct = MutableStateFlow<ProductData?>(null)
    val scannedProduct = _scannedProduct.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    init {
        loadProducts()
    }

    private fun loadProducts() {
        repository.getItems { items ->
            _products.clear()
            _products.addAll(items)
        }
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

    fun addProduct(item: PantryItem) {
        viewModelScope.launch {
            repository.saveProduct(item)
            NotificationHelper.scheduleExpiryReminder(getApplication(), item)
        }
    }

    fun deleteProduct(item: PantryItem) {
        viewModelScope.launch {
            repository.deleteProduct(item.id)
        }
    }
    
    fun deleteProductById(id: String) {
        viewModelScope.launch {
            repository.deleteProduct(id)
        }
    }
}
