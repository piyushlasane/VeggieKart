package com.project.veggiekart.repository

import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.project.veggiekart.model.ProductModel
import com.project.veggiekart.model.UserModel
import kotlinx.coroutines.tasks.await

/**
 * Owns all Firestore reads/writes related to the cart. Keeping this out of the
 * ViewModel means the ViewModel only deals with state/UI concerns, and this class
 * can be swapped for a fake in unit tests or for a different backend later.
 */
class CartRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /** Result of loading the cart: the resolved items and any product IDs that couldn't be resolved. */
    data class CartLoadResult(
        val items: Map<String, Pair<ProductModel, Long>>,
        val unavailableProductIds: List<String>
    )

    private fun userDoc(uid: String) = firestore.collection("users").document(uid)

    private fun productsCollection() = firestore.collection("data")
        .document("stock")
        .collection("products")

    /** Reads the raw {productId -> quantity} map stored on the user document. */
    suspend fun getCartMap(uid: String): Map<String, Long> {
        val snapshot = userDoc(uid).get().await()
        return snapshot.toObject(UserModel::class.java)?.cartItems ?: emptyMap()
    }

    /**
     * Resolves a cart map into full product details, batching reads with whereIn()
     * (max 30 IDs per Firestore query) instead of issuing one read per product.
     */
    suspend fun resolveProducts(cartMap: Map<String, Long>): CartLoadResult {
        if (cartMap.isEmpty()) return CartLoadResult(emptyMap(), emptyList())

        val fetched = mutableMapOf<String, ProductModel>()
        val productIds = cartMap.keys.toList()

        for (batch in productIds.chunked(FIRESTORE_WHERE_IN_LIMIT)) {
            try {
                val snapshot = productsCollection()
                    .whereIn(FieldPath.documentId(), batch)
                    .get()
                    .await()

                for (doc in snapshot.documents) {
                    doc.toObject(ProductModel::class.java)?.let { fetched[doc.id] = it }
                }
            } catch (e: Exception) {
                // Batch failed; its products will be reported as unavailable below.
            }
        }

        val items = mutableMapOf<String, Pair<ProductModel, Long>>()
        val unavailable = mutableListOf<String>()
        for ((productId, quantity) in cartMap) {
            val product = fetched[productId]
            if (product != null) {
                items[productId] = product to quantity
            } else {
                unavailable.add(productId)
            }
        }

        return CartLoadResult(items, unavailable)
    }

    /**
     * Atomically increments the quantity of [productId] by one. Uses a transaction so
     * concurrent calls (e.g. a double-tap) can't both read the same starting quantity
     * and silently overwrite each other's update.
     */
    suspend fun incrementItem(uid: String, productId: String) {
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(userDoc(uid))
            val currentCart = snapshot.toObject(UserModel::class.java)?.cartItems?.toMutableMap()
                ?: mutableMapOf()

            val currentQty = currentCart[productId] ?: 0L
            currentCart[productId] = currentQty + 1

            transaction.update(userDoc(uid), "cartItems", currentCart)
        }.await()
    }

    /** Atomically sets the quantity of [productId] to [newQuantity]. */
    suspend fun setItemQuantity(uid: String, productId: String, newQuantity: Long) {
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(userDoc(uid))
            val currentCart = snapshot.toObject(UserModel::class.java)?.cartItems?.toMutableMap()
                ?: mutableMapOf()

            currentCart[productId] = newQuantity

            transaction.update(userDoc(uid), "cartItems", currentCart)
        }.await()
    }

    /** Atomically removes [productId] from the cart. */
    suspend fun removeItem(uid: String, productId: String) {
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(userDoc(uid))
            val currentCart = snapshot.toObject(UserModel::class.java)?.cartItems?.toMutableMap()
                ?: mutableMapOf()

            currentCart.remove(productId)

            transaction.update(userDoc(uid), "cartItems", currentCart)
        }.await()
    }

    /** Empties the cart. No transaction needed since it's an unconditional overwrite. */
    suspend fun clearCart(uid: String) {
        userDoc(uid).update("cartItems", emptyMap<String, Long>()).await()
    }

    companion object {
        /** Firestore allows at most 30 values in a whereIn() clause. */
        private const val FIRESTORE_WHERE_IN_LIMIT = 30
    }
}