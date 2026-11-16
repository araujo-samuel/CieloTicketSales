package br.com.araujosamuel.cieloticketsales.domain.usecase

import br.com.araujosamuel.cieloticketsales.data.cielo.model.CieloPaymentResponse
import br.com.araujosamuel.cieloticketsales.data.repository.PurchaseRepository
import br.com.araujosamuel.cieloticketsales.domain.model.PurchaseStatus
import javax.inject.Inject

/**
 * Mapeia a resposta Cielo para PurchaseStatus e persiste o resultado no DB.
 *
 * Mapeamento de statusCode:
 *   1 → APPROVED  (autorizado)
 *   2 → CANCELLED (cancelado pelo usuário ou pelo terminal)
 *   outro → DENIED (falha no processamento)
 */
class UpdatePurchaseResultUseCase @Inject constructor(
    private val purchaseRepository: PurchaseRepository
) {
    suspend operator fun invoke(purchaseId: String, response: CieloPaymentResponse) {
        val status = when (response.statusCode) {
            1 -> PurchaseStatus.APPROVED
            2 -> PurchaseStatus.CANCELLED
            else -> PurchaseStatus.DENIED
        }

        purchaseRepository.updateResult(
            id = purchaseId,
            status = status,
            authCode = response.authCode,
            cieloCode = response.cieloCode,
            cardBrand = response.brand,
            errorReason = response.errorReason
        )
    }
}
