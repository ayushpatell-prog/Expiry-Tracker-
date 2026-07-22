package com.example.expirytracker1.network

import com.example.expirytracker1.BuildConfig
import com.google.genai.Client

object GeminiService {

    private val client = Client.builder()
        .apiKey(BuildConfig.GEMINI_API_KEY)
        .build()

    suspend fun askGemini(prompt: String): String {
        return try {
            val response = client.models.generateContent(
                "gemini-2.5-flash",
                prompt,
                null
            )

            response.text() ?: "No response"

        } catch (e: Exception) {
            e.printStackTrace()

            "Unable to generate AI suggestions. Please check your internet connection or try again later."
        }
    }
}