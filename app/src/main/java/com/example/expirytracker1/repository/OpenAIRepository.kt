package com.example.expirytracker1.repository

import com.example.expirytracker1.BuildConfig
import com.example.expirytracker1.api.*
import com.google.gson.Gson

class OpenAIRepository {
    private val api = OpenAIClient.instance
    private val gson = Gson()

    suspend fun getRecipeSuggestions(
        productName: String,
        category: String,
        quantity: String,
        expiryDate: String
    ): List<Recipe>? {
        val apiKey = "Bearer ${BuildConfig.OPENAI_API_KEY}"
        
        val systemPrompt = "You are a creative chef specializing in reducing food waste. " +
                "Generate exactly 3 simple and professional recipe suggestions using the provided product. " +
                "Respond ONLY in JSON format with a root key 'recipes' containing a list of objects. " +
                "Each object MUST have: 'recipe_name', 'cooking_time', 'difficulty', 'ingredients' (list), " +
                "'instructions' (list), and 'waste_reduction_tip'."

        val userPrompt = "Product: $productName, Category: $category, Quantity: $quantity, Expiry: $expiryDate"

        val request = ChatRequest(
            messages = listOf(
                ChatMessage(role = "system", content = systemPrompt),
                ChatMessage(role = "user", content = userPrompt)
            ),
            responseFormat = ResponseFormat()
        )

        return try {
            val response = api.getChatCompletions(apiKey, request)
            val jsonContent = response.choices.firstOrNull()?.message?.content
            if (jsonContent != null) {
                val recipeList = gson.fromJson(jsonContent, RecipeList::class.java)
                recipeList.recipes
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
