package br.com.araujosamuel.cieloticketsales.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import br.com.araujosamuel.cieloticketsales.domain.model.Purchase
import br.com.araujosamuel.cieloticketsales.domain.model.PurchaseStatus

/**
 * Entidade Room para persistência de compras.
 * Separada do modelo de domínio para desacoplar a camada de dados da camada de domínio.
 */
@Entity(tableName = "purchases")
data class PurchaseEntity(
    @PrimaryKey
    val id: String,
    val eventId: String,
    val eventName: String,
    val quantity: Int,
    val totalAmountInCents: Int,
    /** Armazenado como String (nome do enum) para simplicidade */
    val status: String,
    val createdAt: Long,
    val authCode: String?,
    val cieloCode: String?,
    val cardBrand: String?,
    val errorReason: String?,
    val paymentCode: String
) {
    fun toDomain(): Purchase = Purchase(
        id = id,
        eventId = eventId,
        eventName = eventName,
        quantity = quantity,
        totalAmountInCents = totalAmountInCents,
        status = PurchaseStatus.valueOf(status),
        createdAt = createdAt,
        authCode = authCode,
        cieloCode = cieloCode,
        cardBrand = cardBrand,
        errorReason = errorReason,
        paymentCode = paymentCode
    )
}

fun Purchase.toEntity(): PurchaseEntity = PurchaseEntity(
    id = id,
    eventId = eventId,
    eventName = eventName,
    quantity = quantity,
    totalAmountInCents = totalAmountInCents,
    status = status.name,
    createdAt = createdAt,
    authCode = authCode,
    cieloCode = cieloCode,
    cardBrand = cardBrand,
    errorReason = errorReason,
    paymentCode = paymentCode
)
