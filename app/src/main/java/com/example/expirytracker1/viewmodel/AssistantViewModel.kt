package com.example.expirytracker1.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expirytracker1.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AssistantViewModel : ViewModel() {
    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    private val _recipe = MutableStateFlow<String?>(null)
    val recipe = _recipe.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun getRecipeSuggestions(productName: String, category: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val prompt = "Give me 3 simple and professional recipe suggestions using $productName (Category: $category). " +
                        "Format the output with bold titles for each recipe and a short description with key ingredients."
                
                val response = generativeModel.generateContent(prompt)
                _recipe.value = response.text
            } catch (e: Exception) {
                _error.value = "Failed to get suggestions: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
