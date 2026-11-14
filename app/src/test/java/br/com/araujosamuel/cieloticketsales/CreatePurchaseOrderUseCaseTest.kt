package br.com.araujosamuel.cieloticketsales

import br.com.araujosamuel.cieloticketsales.data.repository.PurchaseRepository
import br.com.araujosamuel.cieloticketsales.domain.model.Event
import br.com.araujosamuel.cieloticketsales.domain.model.Purchase
import br.com.araujosamuel.cieloticketsales.domain.model.PurchaseStatus
import br.com.araujosamuel.cieloticketsales.domain.usecase.CreatePurchaseOrderUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

/**
 * Testes para CreatePurchaseOrderUseCase.
 *
 * Cenários críticos cobertos:
 * 1. Criação de nova ordem com UUID único
 * 2. Idempotência: reutiliza ordem PENDING existente para o mesmo evento
 * 3. Cálculo correto do total (quantidade × preço unitário)
 * 4. Validação de quantidade inválida
 */
class CreatePurchaseOrderUseCaseTest {

    private lateinit var repository: PurchaseRepository
    private lateinit var useCase: CreatePurchaseOrderUseCase

    private val testEvent = Event(
        id = "evt_test",
        name = "Show Teste",
        description = "Descrição",
        date = "01/01/2025",
        location = "Local Teste",
        priceInCents = 5000,
        availableTickets = 100
    )

    @Before
    fun setUp() {
        repository = mockk()
        useCase = CreatePurchaseOrderUseCase(repository)
    }

    @Test
    fun `deve criar nova ordem com UUID unico quando nao ha pendente`() = runTest {
        // Nenhuma compra pendente no repositório
        coEvery { repository.findPendingByEventId(testEvent.id) } returns null
        coEvery { repository.savePurchase(any()) } returns Unit

        val result = useCase(testEvent, quantity = 2)

        assertNotNull(result.id)
        assertEquals(testEvent.id, result.eventId)
        assertEquals(testEvent.name, result.eventName)
        assertEquals(2, result.quantity)
        assertEquals(PurchaseStatus.PENDING, result.status)
        // Verifica que savePurchase foi chamado
        coVerify(exactly = 1) { repository.savePurchase(any()) }
    }

    @Test
    fun `deve calcular total corretamente como quantidade vezes preco unitario`() = runTest {
        coEvery { repository.findPendingByEventId(testEvent.id) } returns null
        val capturedPurchase = slot<Purchase>()
        coEvery { repository.savePurchase(capture(capturedPurchase)) } returns Unit

        useCase(testEvent, quantity = 3)

        // 3 × R$50,00 = R$150,00 = 15000 centavos
        assertEquals(15000, capturedPurchase.captured.totalAmountInCents)
    }

    @Test
    fun `deve reutilizar ordem PENDING existente para o mesmo evento (idempotencia)`() = runTest {
        val existingPurchase = Purchase(
            id = "uuid-existente",
            eventId = testEvent.id,
            eventName = testEvent.name,
            quantity = 1,
            totalAmountInCents = 5000,
            status = PurchaseStatus.PENDING,
            createdAt = System.currentTimeMillis()
        )
        // Repositório já tem uma compra PENDING para este evento
        coEvery { repository.findPendingByEventId(testEvent.id) } returns existingPurchase

        val result = useCase(testEvent, quantity = 2)

        // Deve retornar a compra existente, NÃO criar uma nova
        assertEquals("uuid-existente", result.id)
        // savePurchase NÃO deve ser chamado — sem nova cobrança
        coVerify(exactly = 0) { repository.savePurchase(any()) }
    }

    @Test
    fun `deve lancar excecao quando quantidade e zero`() = runTest {
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking {
                useCase(testEvent, quantity = 0)
            }
        }
    }

    @Test
    fun `deve lancar excecao quando quantidade excede ingressos disponiveis`() = runTest {
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking {
                useCase(testEvent, quantity = 101) // 100 disponíveis
            }
        }
    }
}
