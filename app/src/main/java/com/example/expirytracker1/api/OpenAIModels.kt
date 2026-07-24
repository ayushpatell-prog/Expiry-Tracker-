package com.example.expirytracker1.api

import com.google.gson.annotations.SerializedName

data class ChatRequest(
    val model: String = "gpt-4o-mini",
    val messages: List<ChatMessage>,
    @SerializedName("response_format") val responseFormat: ResponseFormat? = null
)

data class ChatMessage(
    val role: String,
    val content: String
)

data class ResponseFormat(
    val type: String = "json_object"
)

data class ChatResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: ChatMessage
)

data class RecipeList(
    val recipes: List<Recipe>
)

data class Recipe(
    @SerializedName("recipe_name") val name: String,
    @SerializedName("cooking_time") val cookingTime: String,
    val difficulty: String,
    val ingredients: List<String>,
    val instructions: List<String>,
    @SerializedName("waste_reduction_tip") val wasteReductionTip: String
)
