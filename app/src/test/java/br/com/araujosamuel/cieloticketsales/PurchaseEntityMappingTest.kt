package br.com.araujosamuel.cieloticketsales

import br.com.araujosamuel.cieloticketsales.data.local.entity.PurchaseEntity
import br.com.araujosamuel.cieloticketsales.data.local.entity.toEntity
import br.com.araujosamuel.cieloticketsales.domain.model.Purchase
import br.com.araujosamuel.cieloticketsales.domain.model.PurchaseStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Testes para mapeamento entre Purchase (domínio) e PurchaseEntity (Room).
 *
 * Cenários críticos cobertos:
 * 1. Purchase → PurchaseEntity preserva todos os campos
 * 2. PurchaseEntity → Purchase preserva todos os campos
 * 3. Status é armazenado como String e recuperado como enum
 * 4. Campos opcionais nulos são preservados como null
 * 5. formattedTotal() formata corretamente
 */
class PurchaseEntityMappingTest {

    private val testPurchase = Purchase(
        id = "uuid-123",
        eventId = "evt_001",
        eventName = "Show Teste",
        quantity = 2,
        totalAmountInCents = 10000,
        status = PurchaseStatus.APPROVED,
        createdAt = 1700000000000L,
        authCode = "AUTH123",
        cieloCode = "CIELO456",
        cardBrand = "VISA",
        errorReason = null,
        paymentCode = "CREDITO_AVISTA"
    )

    @Test
    fun `Purchase para PurchaseEntity deve preservar todos os campos`() {
        val entity = testPurchase.toEntity()

        assertEquals(testPurchase.id, entity.id)
        assertEquals(testPurchase.eventId, entity.eventId)
        assertEquals(testPurchase.eventName, entity.eventName)
        assertEquals(testPurchase.quantity, entity.quantity)
        assertEquals(testPurchase.totalAmountInCents, entity.totalAmountInCents)
        assertEquals(testPurchase.status.name, entity.status) // status como String
        assertEquals(testPurchase.createdAt, entity.createdAt)
        assertEquals(testPurchase.authCode, entity.authCode)
        assertEquals(testPurchase.cieloCode, entity.cieloCode)
        assertEquals(testPurchase.cardBrand, entity.cardBrand)
        assertNull(entity.errorReason)
        assertEquals(testPurchase.paymentCode, entity.paymentCode)
    }

    @Test
    fun `PurchaseEntity para Purchase deve recuperar status como enum corretamente`() {
        val entity = PurchaseEntity(
            id = "uuid-456",
            eventId = "evt_002",
            eventName = "Festival",
            quantity = 1,
            totalAmountInCents = 3500,
            status = "PENDING",
            createdAt = 1700000000000L,
            authCode = null,
            cieloCode = null,
            cardBrand = null,
            errorReason = null,
            paymentCode = "DEBITO_AVISTA"
        )

        val domain = entity.toDomain()

        assertEquals(PurchaseStatus.PENDING, domain.status)
        assertNull(domain.authCode)
        assertNull(domain.errorReason)
    }

    @Test
    fun `mapeamento de ida e volta deve ser consistente`() {
        val entity = testPurchase.toEntity()
        val recovered = entity.toDomain()

        assertEquals(testPurchase.id, recovered.id)
        assertEquals(testPurchase.status, recovered.status)
        assertEquals(testPurchase.totalAmountInCents, recovered.totalAmountInCents)
        assertEquals(testPurchase.authCode, recovered.authCode)
    }

    @Test
    fun `formattedTotal deve formatar corretamente em reais`() {
        // R$100,00
        val purchase = testPurchase.copy(totalAmountInCents = 10000)
        assertEquals("R$ 100,00", purchase.formattedTotal())
    }

    @Test
    fun `formattedTotal deve formatar centavos corretamente`() {
        // R$12,50
        val purchase = testPurchase.copy(totalAmountInCents = 1250)
        assertEquals("R$ 12,50", purchase.formattedTotal())
    }

    @Test
    fun `formattedTotal deve formatar valor menor que R$1 corretamente`() {
        // R$0,99
        val purchase = testPurchase.copy(totalAmountInCents = 99)
        assertEquals("R$ 0,99", purchase.formattedTotal())
    }
}
