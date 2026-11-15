package br.com.araujosamuel.cieloticketsales

import android.net.Uri
import android.util.Base64
import br.com.araujosamuel.cieloticketsales.data.cielo.CieloDeepLinkService
import br.com.araujosamuel.cieloticketsales.data.cielo.model.CieloPaymentRequest
import com.google.gson.Gson
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Testes para CieloDeepLinkService.
 *
 * Estratégia:
 * - [buildPaymentRequestBase64] é testada diretamente (método `internal`) para verificar
 *   o JSON sem depender de android.net.Uri.Builder, que não está disponível no ambiente JVM.
 * - [parsePaymentResponse] usa [mockk] para simular Uri com parâmetros controlados,
 *   eliminando a dependência de Uri.parse().
 *
 * Cenários críticos cobertos:
 * 1. JSON do request tem formato correto (value em reais, items, installments=0)
 * 2. SKU derivado do orderId (prevenção de duplicidade identificável)
 * 3. urlCallback é a constante correta
 * 4. Parsing de sucesso (Order.status="PAID" → statusCode=1, campos extraídos)
 * 5. Parsing de cancelamento pelo usuário (error.code=1 → statusCode=2)
 * 6. Parsing de erro genérico (error.code=2 → statusCode=3)
 * 7. URI sem parâmetros retorna erro (statusCode=3)
 */
class CieloDeepLinkServiceTest {

    private lateinit var service: CieloDeepLinkService
    private val gson = Gson()

    @Before
    fun setUp() {
        // Mock de android.util.Base64 para rodar em JVM (sem Android runtime)
        mockkStatic(Base64::class)
        every { Base64.encodeToString(any(), any()) } answers {
            java.util.Base64.getEncoder().encodeToString(firstArg<ByteArray>())
        }
        every { Base64.decode(any<String>(), any()) } answers {
            // getMimeDecoder lida com quebras de linha (\n) no Base64,
            // replicando o comportamento de Base64.DEFAULT no Android
            java.util.Base64.getMimeDecoder().decode(firstArg<String>())
        }

        service = CieloDeepLinkService(gson)
    }

    // ─── Testes de buildPaymentRequestBase64 ─────────────────────────────────

    @Test
    fun `buildPaymentRequestBase64 deve gerar JSON decodificavel`() {
        val base64 = service.buildPaymentRequestBase64(
            orderId = "uuid-123",
            amountInCents = 5000
        )
        val decoded = String(java.util.Base64.getDecoder().decode(base64), Charsets.UTF_8)
        val request = gson.fromJson(decoded, CieloPaymentRequest::class.java)
        assertNull("JSON deve ser parseável sem erros", null) // chegou aqui = sem exceção
        assertEquals("CREDITO_AVISTA", request.paymentCode)
    }

    @Test
    fun `buildPaymentRequestBase64 deve ter value em reais como String`() {
        // R$80,00 = 8000 centavos → "80.00"
        val base64 = service.buildPaymentRequestBase64(
            orderId = "uuid-123",
            amountInCents = 8000
        )
        val decoded = String(java.util.Base64.getDecoder().decode(base64), Charsets.UTF_8)
        assertTrue("value deve ser '80.00' (reais, não centavos)", decoded.contains("\"value\":\"80.00\""))
    }

    @Test
    fun `buildPaymentRequestBase64 deve ter installments 0 para pagamento a vista`() {
        val base64 = service.buildPaymentRequestBase64(
            orderId = "uuid-123",
            amountInCents = 5000
        )
        val decoded = String(java.util.Base64.getDecoder().decode(base64), Charsets.UTF_8)
        val request = gson.fromJson(decoded, CieloPaymentRequest::class.java)
        assertEquals("installments deve ser 0 para à vista (não 1)", 0, request.installments)
    }

    @Test
    fun `buildPaymentRequestBase64 deve incluir items obrigatorio com nome do evento`() {
        val base64 = service.buildPaymentRequestBase64(
            orderId = "uuid-123",
            amountInCents = 5000,
            eventName = "Show do Rock"
        )
        val decoded = String(java.util.Base64.getDecoder().decode(base64), Charsets.UTF_8)
        assertTrue("campo items deve estar presente no JSON", decoded.contains("\"items\""))
        assertTrue("nome do evento deve estar no item", decoded.contains("Show do Rock"))
    }

    @Test
    fun `buildPaymentRequestBase64 deve incluir sku derivado do orderId para rastreabilidade`() {
        val orderId = "meu-uuid-unico"
        val base64 = service.buildPaymentRequestBase64(
            orderId = orderId,
            amountInCents = 10000
        )
        val decoded = String(java.util.Base64.getDecoder().decode(base64), Charsets.UTF_8)
        val expectedSku = orderId.takeLast(8)
        assertTrue("SKU deve conter os últimos 8 chars do orderId", decoded.contains(expectedSku))
    }

    @Test
    fun `CALLBACK_URI deve ser o scheme e host corretos`() {
        assertEquals("order://response", CieloDeepLinkService.CALLBACK_URI)
    }

    // ─── Testes de parsePaymentResponse ──────────────────────────────────────

    @Test
    fun `parsePaymentResponse deve retornar statusCode 1 para pagamento aprovado`() {
        // Formato real retornado pelo Cielo Smart (confirmado via logcat com emulador v1.61.8)
        val responseJson = """
            {"status":"PAID","payments":[{"authCode":"12345","cieloCode":"ABC","brand":"VISA"}]}
        """.trimIndent()
        val base64Response = java.util.Base64.getEncoder()
            .encodeToString(responseJson.toByteArray())

        val mockUri = mockk<Uri>()
        every { mockUri.getQueryParameter("response") } returns base64Response
        every { mockUri.getQueryParameter("error") } returns null

        val result = service.parsePaymentResponse(mockUri)

        assertEquals(1, result.statusCode)
        assertEquals("12345", result.authCode)
        assertEquals("ABC", result.cieloCode)
        assertEquals("VISA", result.brand)
        assertNull(result.errorReason)
    }

    @Test
    fun `parsePaymentResponse deve retornar statusCode 2 quando usuario cancela (error code 1)`() {
        val errorJson = """{"code":1,"reason":"CANCELADO PELO USUÁRIO"}"""
        val base64Error = java.util.Base64.getEncoder()
            .encodeToString(errorJson.toByteArray())

        val mockUri = mockk<Uri>()
        every { mockUri.getQueryParameter("response") } returns null
        every { mockUri.getQueryParameter("error") } returns base64Error

        val result = service.parsePaymentResponse(mockUri)

        // Cancelamento pelo usuário → statusCode=2 (CANCELLED, não DENIED)
        assertEquals(2, result.statusCode)
        assertEquals("CANCELADO PELO USUÁRIO", result.errorReason)
    }

    @Test
    fun `parsePaymentResponse deve retornar statusCode 3 para erro generico (error code 2)`() {
        val errorJson = """{"code":2,"reason":"ERRO GENÉRICO"}"""
        val base64Error = java.util.Base64.getEncoder()
            .encodeToString(errorJson.toByteArray())

        val mockUri = mockk<Uri>()
        every { mockUri.getQueryParameter("response") } returns null
        every { mockUri.getQueryParameter("error") } returns base64Error

        val result = service.parsePaymentResponse(mockUri)

        assertEquals(3, result.statusCode)
    }

    @Test
    fun `parsePaymentResponse deve retornar erro quando URI nao tem parametros`() {
        val mockUri = mockk<Uri>()
        every { mockUri.getQueryParameter("response") } returns null
        every { mockUri.getQueryParameter("error") } returns null

        val result = service.parsePaymentResponse(mockUri)

        assertEquals(3, result.statusCode)
        assertTrue(result.errorReason?.contains("inválida") == true)
    }
}
