package br.com.araujosamuel.cieloticketsales

import android.net.Uri
import android.util.Base64
import br.com.araujosamuel.cieloticketsales.data.cielo.CieloDeepLinkService
import com.google.gson.Gson
import io.mockk.every
import io.mockk.mockkStatic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Testes para CieloDeepLinkService.
 *
 * Cenários críticos cobertos:
 * 1. URI de pagamento tem scheme correto (lio://)
 * 2. Request JSON é codificado em Base64 na URI
 * 3. Parsing de resposta de sucesso (statusCode=1 → APPROVED)
 * 4. Parsing de cancelamento pelo usuário (error code=1)
 * 5. Parsing de erro genérico (error code=2)
 * 6. URI sem parâmetros retorna erro
 */
class CieloDeepLinkServiceTest {

    private lateinit var service: CieloDeepLinkService
    private val gson = Gson()

    @Before
    fun setUp() {
        // Mock do android.util.Base64 para rodar em JVM (sem Android)
        mockkStatic(Base64::class)
        every { Base64.encodeToString(any(), any()) } answers {
            java.util.Base64.getEncoder().withoutPadding()
                .encodeToString(firstArg<ByteArray>())
        }
        every { Base64.decode(any<String>(), any()) } answers {
            java.util.Base64.getDecoder().decode(firstArg<String>())
        }

        service = CieloDeepLinkService(gson)
    }

    @Test
    fun `buildPaymentUri deve ter scheme lio`() {
        val uri = service.buildPaymentUri(
            orderId = "uuid-123",
            amountInCents = 5000
        )
        assertEquals("lio", uri.scheme)
    }

    @Test
    fun `buildPaymentUri deve incluir urlCallback correto`() {
        val uri = service.buildPaymentUri(
            orderId = "uuid-123",
            amountInCents = 5000
        )
        val callback = uri.getQueryParameter("urlCallback")
        assertEquals(CieloDeepLinkService.CALLBACK_URI, callback)
    }

    @Test
    fun `buildPaymentUri deve incluir orderId no request Base64`() {
        val orderId = "meu-uuid-unico"
        val uri = service.buildPaymentUri(
            orderId = orderId,
            amountInCents = 10000
        )

        val base64 = uri.getQueryParameter("request") ?: ""
        val decoded = String(java.util.Base64.getDecoder().decode(base64), Charsets.UTF_8)
        assertTrue("orderId deve estar no request", decoded.contains(orderId))
    }

    @Test
    fun `parsePaymentResponse deve retornar statusCode 1 para pagamento aprovado`() {
        val responseJson = """{"statusCode":1,"authCode":"12345","cieloCode":"ABC","brand":"VISA"}"""
        val base64Response = java.util.Base64.getEncoder().withoutPadding()
            .encodeToString(responseJson.toByteArray())

        val uri = Uri.parse("order://response?response=$base64Response")
        val result = service.parsePaymentResponse(uri)

        assertEquals(1, result.statusCode)
        assertEquals("12345", result.authCode)
        assertEquals("ABC", result.cieloCode)
        assertEquals("VISA", result.brand)
        assertNull(result.errorReason)
    }

    @Test
    fun `parsePaymentResponse deve retornar statusCode 2 quando usuario cancela (error code 1)`() {
        val errorJson = """{"code":1,"reason":"CANCELADO PELO USUÁRIO"}"""
        val base64Error = java.util.Base64.getEncoder().withoutPadding()
            .encodeToString(errorJson.toByteArray())

        val uri = Uri.parse("order://response?error=$base64Error")
        val result = service.parsePaymentResponse(uri)

        // Cancelamento pelo usuário → statusCode=2 (não é negado, é cancelado)
        assertEquals(2, result.statusCode)
        assertEquals("CANCELADO PELO USUÁRIO", result.errorReason)
    }

    @Test
    fun `parsePaymentResponse deve retornar statusCode 3 para erro generico (error code 2)`() {
        val errorJson = """{"code":2,"reason":"ERRO GENÉRICO"}"""
        val base64Error = java.util.Base64.getEncoder().withoutPadding()
            .encodeToString(errorJson.toByteArray())

        val uri = Uri.parse("order://response?error=$base64Error")
        val result = service.parsePaymentResponse(uri)

        assertEquals(3, result.statusCode)
    }

    @Test
    fun `parsePaymentResponse deve retornar erro quando URI nao tem parametros`() {
        val uri = Uri.parse("order://response")
        val result = service.parsePaymentResponse(uri)

        assertEquals(3, result.statusCode)
        assertTrue(result.errorReason?.contains("inválida") == true)
    }
}
