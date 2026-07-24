package com.example.expirytracker1.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expirytracker1.api.Recipe
import com.example.expirytracker1.repository.OpenAIRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RecipeViewModel : ViewModel() {
    private val repository = OpenAIRepository()

    private val _recipes = MutableStateFlow<List<Recipe>>(emptyList())
    val recipes = _recipes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun getRecipeSuggestions(
        productName: String,
        category: String,
        quantity: String,
        expiryDate: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val result = repository.getRecipeSuggestions(productName, category, quantity, expiryDate)
                if (result != null) {
                    _recipes.value = result
                } else {
                    _error.value = "Failed to get recipe suggestions. Please try again."
                }
            } catch (e: Exception) {
                _error.value = "An error occurred: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
