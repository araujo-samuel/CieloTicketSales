package br.com.araujosamuel.cieloticketsales

import br.com.araujosamuel.cieloticketsales.data.cielo.model.CieloPaymentResponse
import br.com.araujosamuel.cieloticketsales.data.repository.PurchaseRepository
import br.com.araujosamuel.cieloticketsales.domain.model.PurchaseStatus
import br.com.araujosamuel.cieloticketsales.domain.usecase.UpdatePurchaseResultUseCase
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Testes para UpdatePurchaseResultUseCase.
 *
 * Cenários críticos cobertos:
 * 1. statusCode=1 → PurchaseStatus.APPROVED
 * 2. statusCode=2 → PurchaseStatus.CANCELLED
 * 3. statusCode=3 → PurchaseStatus.DENIED
 * 4. Dados de aprovação (authCode, cieloCode, brand) são persistidos
 * 5. Motivo do erro é persistido para negados/cancelados
 */
class UpdatePurchaseResultUseCaseTest {

    private lateinit var repository: PurchaseRepository
    private lateinit var useCase: UpdatePurchaseResultUseCase

    @Before
    fun setUp() {
        repository = mockk()
        coEvery { repository.updateResult(any(), any(), any(), any(), any(), any()) } returns Unit
        useCase = UpdatePurchaseResultUseCase(repository)
    }

    @Test
    fun `statusCode 1 deve mapear para APPROVED`() = runTest {
        val response = CieloPaymentResponse(
            statusCode = 1,
            authCode = "AUTH123",
            cieloCode = "CIELO456",
            brand = "VISA"
        )

        useCase("purchase-id", response)

        coVerify {
            repository.updateResult(
                id = "purchase-id",
                status = PurchaseStatus.APPROVED,
                authCode = "AUTH123",
                cieloCode = "CIELO456",
                cardBrand = "VISA",
                errorReason = null
            )
        }
    }

    @Test
    fun `statusCode 2 deve mapear para CANCELLED`() = runTest {
        val response = CieloPaymentResponse(
            statusCode = 2,
            errorReason = "Cancelado pelo usuário"
        )

        useCase("purchase-id", response)

        coVerify {
            repository.updateResult(
                id = "purchase-id",
                status = PurchaseStatus.CANCELLED,
                authCode = null,
                cieloCode = null,
                cardBrand = null,
                errorReason = "Cancelado pelo usuário"
            )
        }
    }

    @Test
    fun `statusCode diferente de 1 e 2 deve mapear para DENIED`() = runTest {
        val response = CieloPaymentResponse(
            statusCode = 3,
            errorReason = "Falha no processamento"
        )

        useCase("purchase-id", response)

        coVerify {
            repository.updateResult(
                id = "purchase-id",
                status = PurchaseStatus.DENIED,
                authCode = null,
                cieloCode = null,
                cardBrand = null,
                errorReason = "Falha no processamento"
            )
        }
    }

    @Test
    fun `statusCode 4 (autenticacao) deve mapear para DENIED`() = runTest {
        val response = CieloPaymentResponse(
            statusCode = 4,
            errorReason = "Problema de autenticação"
        )

        useCase("purchase-id", response)

        coVerify {
            repository.updateResult(
                id = "purchase-id",
                status = PurchaseStatus.DENIED,
                authCode = null,
                cieloCode = null,
                cardBrand = null,
                errorReason = "Problema de autenticação"
            )
        }
    }
}
