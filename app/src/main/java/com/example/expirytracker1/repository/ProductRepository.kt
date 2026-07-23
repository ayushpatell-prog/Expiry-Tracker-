package com.example.expirytracker1.repository

import com.example.expirytracker1.api.OpenFoodFactsApi
import com.example.expirytracker1.data.PantryItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ProductRepository {
    private val firestore = FirebaseFirestore.getInstance().apply {
        try {
            val settings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                .build()
            this.firestoreSettings = settings
        } catch (e: Exception) {
            android.util.Log.e("ProductRepository", "Error setting Firestore settings", e)
        }
    }
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
        val collection = getItemsCollection() ?: throw Exception("User not logged in")
        collection.document(item.id).set(item).await()
    }

    suspend fun deleteProduct(id: String) {
        val collection = getItemsCollection() ?: throw Exception("User not logged in")
        collection.document(id).delete().await()
    }

    fun getItemsFlow(): Flow<List<PantryItem>> {
        val collection = getItemsCollection() ?: return emptyFlow()
        return collection.snapshots().map { snapshot ->
            snapshot.toObjects(PantryItem::class.java)
        }
    }
}
