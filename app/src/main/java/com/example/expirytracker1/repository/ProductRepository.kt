package com.example.expirytracker1.repository

import com.example.expirytracker1.api.OpenFoodFactsApi
import com.example.expirytracker1.data.PantryItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ProductRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val api = Retrofit.Builder()
        .baseUrl("https://world.openfoodfacts.org/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OpenFoodFactsApi::class.java)

    private fun getItemsCollection() = auth.currentUser?.uid?.let { uid ->
        firestore.collection("users").document(uid).collection("items")
    }

    suspend fun fetchProductFromApi(barcode: String) = try {
        val response = api.getProduct(barcode)
        if (response.status == 1) response.product else null
    } catch (e: Exception) {
        null
    }

    suspend fun saveProduct(item: PantryItem) {
        getItemsCollection()?.document(item.id)?.set(item)?.await()
    }

    suspend fun deleteProduct(id: String) {
        getItemsCollection()?.document(id)?.delete()?.await()
    }

    fun getItems(onUpdate: (List<PantryItem>) -> Unit) {
        getItemsCollection()?.addSnapshotListener { snapshot, _ ->
            val items = snapshot?.toObjects(PantryItem::class.java) ?: emptyList()
            onUpdate(items)
        }
    }
}
