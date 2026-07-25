package com.example.expirytracker1.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AssistantViewModel : ViewModel() {
    private val generativeModel = Firebase.ai(backend = GenerativeBackend.googleAI())
        .generativeModel("gemini-3.6-flash")

    private val _recipe = MutableStateFlow<String?>(null)
    val recipe = _recipe.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun getRecipeSuggestions(products: List<Pair<String, String>>) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val itemsList = products.joinToString(", ") { "${it.first} (${it.second})" }
                val prompt = if (products.size > 1) {
                    "I have multiple products that are about to expire: $itemsList. " +
                    "Please suggest 2-3 simple and professional recipes that COMBINE as many of these ingredients as possible. " +
                    "If they cannot be combined reasonably, provide separate suggestions. " +
                    "Format the output strictly as follows for each recipe:\n" +
                    "**Dish Name**\n" +
                    "**Description:** [A short, elegant description]\n" +
                    "**Key Ingredients:** [List of main items]\n\n" +
                    "Use double asterisks for bolding as shown above."
                } else {
                    val (productName, category) = products.first()
                    "Give me 3 simple and professional recipe suggestions using $productName (Category: $category). " +
                    "Format the output strictly as follows for each recipe:\n" +
                    "**Dish Name**\n" +
                    "**Description:** [A short, elegant description]\n" +
                    "**Key Ingredients:** [List of main items]\n\n" +
                    "Use double asterisks for bolding as shown above."
                }
                
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
