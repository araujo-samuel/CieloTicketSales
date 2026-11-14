package br.com.araujosamuel.cieloticketsales.data.repository

import br.com.araujosamuel.cieloticketsales.domain.model.Purchase
import br.com.araujosamuel.cieloticketsales.domain.model.PurchaseStatus
import kotlinx.coroutines.flow.Flow

interface PurchaseRepository {
    suspend fun savePurchase(purchase: Purchase)
    suspend fun findById(id: String): Purchase?
    suspend fun findPendingByEventId(eventId: String): Purchase?
    fun getAllPurchases(): Flow<List<Purchase>>
    suspend fun updateResult(
        id: String,
        status: PurchaseStatus,
        authCode: String?,
        cieloCode: String?,
        cardBrand: String?,
        errorReason: String?
    )
}
