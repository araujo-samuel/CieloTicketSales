package br.com.araujosamuel.cieloticketsales.domain.usecase

import br.com.araujosamuel.cieloticketsales.data.repository.PurchaseRepository
import br.com.araujosamuel.cieloticketsales.domain.model.Purchase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPurchasesUseCase @Inject constructor(
    private val purchaseRepository: PurchaseRepository
) {
    operator fun invoke(): Flow<List<Purchase>> = purchaseRepository.getAllPurchases()

    suspend fun findById(id: String): Purchase? = purchaseRepository.findById(id)
}
