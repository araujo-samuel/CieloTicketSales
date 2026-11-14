package br.com.araujosamuel.cieloticketsales.domain.usecase

import br.com.araujosamuel.cieloticketsales.data.repository.PurchaseRepository
import br.com.araujosamuel.cieloticketsales.domain.model.Event
import br.com.araujosamuel.cieloticketsales.domain.model.Purchase
import br.com.araujosamuel.cieloticketsales.domain.model.PurchaseStatus
import java.util.UUID

/**
 * Cria uma nova ordem de compra ou reutiliza uma existente no estado PENDING.
 *
 * Estratégia de idempotência:
 * 1. Verifica se já existe uma compra PENDING para o evento no DB local
 * 2. Se existir, retorna a mesma compra → o usuário pode retomar sem gerar nova cobrança
 * 3. Se não existir, cria uma nova com UUID único
 *
 * O UUID gerado é o orderId enviado ao Cielo, o que garante que reenvios
 * da mesma transação (ex: app fechado e reaberto) não gerem cobranças duplicadas.
 */
class CreatePurchaseOrderUseCase(
    private val purchaseRepository: PurchaseRepository
) {
    suspend operator fun invoke(event: Event, quantity: Int): Purchase {
        require(quantity > 0) { "Quantidade deve ser maior que zero" }
        require(quantity <= event.availableTickets) {
            "Quantidade ($quantity) maior que ingressos disponíveis (${event.availableTickets})"
        }

        // Idempotência: reutiliza ordem PENDING existente para o mesmo evento
        purchaseRepository.findPendingByEventId(event.id)?.let { existing ->
            return existing
        }

        val purchase = Purchase(
            id = UUID.randomUUID().toString(),
            eventId = event.id,
            eventName = event.name,
            quantity = quantity,
            totalAmountInCents = event.priceInCents * quantity,
            status = PurchaseStatus.PENDING,
            createdAt = System.currentTimeMillis()
        )

        purchaseRepository.savePurchase(purchase)
        return purchase
    }
}
