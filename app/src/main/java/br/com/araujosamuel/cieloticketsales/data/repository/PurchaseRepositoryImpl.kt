package br.com.araujosamuel.cieloticketsales.data.repository

import br.com.araujosamuel.cieloticketsales.data.local.dao.PurchaseDao
import br.com.araujosamuel.cieloticketsales.data.local.entity.toEntity
import br.com.araujosamuel.cieloticketsales.domain.model.Purchase
import br.com.araujosamuel.cieloticketsales.domain.model.PurchaseStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PurchaseRepositoryImpl(
    private val dao: PurchaseDao
) : PurchaseRepository {

    override suspend fun savePurchase(purchase: Purchase) {
        dao.insert(purchase.toEntity())
    }

    override suspend fun findById(id: String): Purchase? =
        dao.findById(id)?.toDomain()

    override suspend fun findPendingByEventId(eventId: String): Purchase? =
        dao.findPendingByEventId(eventId)?.toDomain()

    override fun getAllPurchases(): Flow<List<Purchase>> =
        dao.getAllPurchases().map { entities -> entities.map { it.toDomain() } }

    override suspend fun updateResult(
        id: String,
        status: PurchaseStatus,
        authCode: String?,
        cieloCode: String?,
        cardBrand: String?,
        errorReason: String?
    ) {
        dao.updateResult(
            id = id,
            status = status.name,
            authCode = authCode,
            cieloCode = cieloCode,
            cardBrand = cardBrand,
            errorReason = errorReason
        )
    }
}
