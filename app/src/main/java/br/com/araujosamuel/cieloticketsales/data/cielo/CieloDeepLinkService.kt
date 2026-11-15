package br.com.araujosamuel.cieloticketsales.data.cielo

import android.net.Uri
import android.util.Base64
import br.com.araujosamuel.cieloticketsales.data.cielo.model.CieloOrderItem
import br.com.araujosamuel.cieloticketsales.data.cielo.model.CieloOrderResponse
import br.com.araujosamuel.cieloticketsales.data.cielo.model.CieloPaymentError
import br.com.araujosamuel.cieloticketsales.data.cielo.model.CieloPaymentRequest
import br.com.araujosamuel.cieloticketsales.data.cielo.model.CieloPaymentResponse
import com.google.gson.Gson

/**
 * Serviço responsável por montar e interpretar a integração com Cielo Smart via Deep Link.
 *
 * Decisão técnica: Deep Link foi escolhido em vez do SDK local LIO porque:
 * 1. O SDK local está sendo descontinuado pela Cielo (migração obrigatória para Cielo Smart)
 * 2. Deep Link é a abordagem recomendada para novos apps na plataforma Cielo Smart
 * 3. Não exige dependência de AAR externo — menor acoplamento
 * 4. Funciona com o emulador Cielo sem configuração adicional
 *
 * Fluxo:
 *   1. Monta JSON do pedido → codifica em Base64
 *   2. Lança intent com URI: lio://payment?request={base64}&urlCallback=order://response
 *   3. Cielo Smart processa e chama de volta via ResponseActivity
 *   4. ResponseActivity decodifica o resultado e atualiza a compra no DB
 */
class CieloDeepLinkService(
    private val gson: Gson = Gson()
) {

    companion object {
        /**
         * Credenciais de teste para uso com o emulador Cielo.
         * Em produção, devem ser obtidas no Developer Portal da Cielo e
         * armazenadas de forma segura (ex: BuildConfig + CI secrets).
         *
         * Ref: https://docs.cielo.com.br/cielo-smart/docs/autenticacao-e-credenciais
         */
        const val CLIENT_ID = "CLIENT_ID_EMULADOR"
        const val ACCESS_TOKEN = "ACCESS_TOKEN_EMULADOR"

        /**
         * URI de callback declarada no AndroidManifest.xml como intent-filter da ResponseActivity.
         * O scheme "order" e host "response" devem coincidir com o que está no Manifest.
         */
        const val CALLBACK_URI = "order://response"

        /**
         * Pacote do serviço Cielo Smart (emulador v1.61.8+).
         * Nota: a versão legada LIO usava "com.ads.lio.uriappclient".
         * O emulador atual usa "br.com.cielosmart.orderservice".
         * A detecção real é feita pelo scheme "lio://" via queryIntentActivities.
         */
        const val CIELO_PACKAGE = "br.com.cielosmart.orderservice"
        const val CIELO_PACKAGE_LEGACY = "com.ads.lio.uriappclient"
    }

    /**
     * Constrói a URI de pagamento para ser lançada via Intent.
     *
     * IMPORTANTE: usa Uri.Builder + appendQueryParameter (NÃO Uri.parse com string direta).
     * O Base64 padrão contém os caracteres '+', '/' e '=' que são especiais em URLs.
     * Uri.parse() não faz percent-encoding dos parâmetros — isso corrompia o JSON
     * no lado do Cielo (erro: "Json inválido, order: null").
     * appendQueryParameter() aplica percent-encoding automático, garantindo que o
     * Cielo decodifique corretamente.
     *
     * Formato correto do JSON (descoberto via análise do sample oficial):
     * - [value]: String em REAIS (ex: "80.00"), NÃO em centavos
     * - [items]: lista de produtos obrigatória
     * - [installments]: 0 para pagamento à vista (NÃO 1)
     *
     * @param orderId UUID da compra — chave de idempotência
     * @param amountInCents valor total em centavos
     * @param paymentCode forma de pagamento (default: CREDITO_AVISTA)
     * @param eventName nome do evento (usado como nome do item)
     * @param quantity quantidade de ingressos
     * @return Uri no formato lio://payment?request={base64_encoded}&urlCallback=order%3A%2F%2Fresponse
     */
    /**
     * Constrói o payload JSON do pedido e retorna a string Base64 pronta para a URI.
     *
     * Exposta como `internal` para permitir testes unitários sem dependência de
     * `android.net.Uri.Builder`, que não está disponível no ambiente JVM dos testes unitários.
     * [buildPaymentUri] delega a esta função e só adiciona a camada de URI.
     */
    internal fun buildPaymentRequestBase64(
        orderId: String,
        amountInCents: Int,
        paymentCode: String = "CREDITO_AVISTA",
        eventName: String = "Ingresso",
        quantity: Int = 1
    ): String {
        val unitPriceReais = (amountInCents.toDouble() / quantity) / 100.0
        val totalReais = "%.2f".format(amountInCents / 100.0)

        val request = CieloPaymentRequest(
            accessToken = ACCESS_TOKEN,
            clientID = CLIENT_ID,
            email = "",
            installments = 0,
            items = listOf(
                CieloOrderItem(
                    name = eventName.ifBlank { "Ingresso" },
                    quantity = quantity,
                    sku = orderId.takeLast(8),
                    unitPrice = unitPriceReais
                )
            ),
            paymentCode = paymentCode,
            value = totalReais
        )
        val json = gson.toJson(request)
        return Base64.encodeToString(json.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    }

    fun buildPaymentUri(
        orderId: String,
        amountInCents: Int,
        paymentCode: String = "CREDITO_AVISTA",
        eventName: String = "Ingresso",
        quantity: Int = 1
    ): Uri {
        val base64 = buildPaymentRequestBase64(orderId, amountInCents, paymentCode, eventName, quantity)
        return Uri.Builder()
            .scheme("lio")
            .authority("payment")
            .appendQueryParameter("request", base64)
            .appendQueryParameter("urlCallback", CALLBACK_URI)
            .build()
    }

    /**
     * Interpreta a URI de retorno do Cielo Smart.
     *
     * O Cielo Smart retorna um objeto Order completo serializado em Base64 no parâmetro "response".
     * A estrutura real (confirmada via logcat com emulador v1.61.8) é:
     *   { "status": "PAID", "payments": [{ "authCode": "...", "cieloCode": "...", "brand": "..." }] }
     *
     * Mapeamento de status:
     *   Order.status == "PAID" → statusCode = 1 (APPROVED)
     *   qualquer outro valor   → statusCode = 2 (CANCELLED)
     *
     * Nota: Base64.DEFAULT é usado para decodificação pois o Cielo emulador envia o Base64
     * com quebras de linha (\n) incorporadas — DEFAULT ignora whitespace, NO_WRAP não.
     *
     * Em caso de erro interno, o parâmetro "error" carrega um JSON { "code": N, "reason": "..." }.
     *
     * @param uri URI recebida pela ResponseActivity (scheme: order://response)
     * @return [CieloPaymentResponse] normalizado com statusCode e dados da transação
     */
    fun parsePaymentResponse(uri: Uri): CieloPaymentResponse {
        val responseBase64 = uri.getQueryParameter("response")
        val errorBase64 = uri.getQueryParameter("error")

        return when {
            responseBase64 != null -> {
                runCatching {
                    val json = String(
                        Base64.decode(responseBase64, Base64.DEFAULT),
                        Charsets.UTF_8
                    )
                    val order = gson.fromJson(json, CieloOrderResponse::class.java)
                    val payment = order.payments.firstOrNull()
                    CieloPaymentResponse(
                        statusCode = if (order.status.equals("PAID", ignoreCase = true)) 1 else 2,
                        authCode = payment?.authCode,
                        cieloCode = payment?.cieloCode,
                        brand = payment?.brand,
                        amount = payment?.amount,
                        installments = payment?.installments
                    )
                }.getOrElse {
                    CieloPaymentResponse(
                        statusCode = 3,
                        errorReason = "Falha ao decodificar resposta: ${it.message}"
                    )
                }
            }

            errorBase64 != null -> {
                runCatching {
                    val json = String(
                        Base64.decode(errorBase64, Base64.DEFAULT),
                        Charsets.UTF_8
                    )
                    val error = gson.fromJson(json, CieloPaymentError::class.java)
                    CieloPaymentResponse(
                        // code=1 → cancelado pelo usuário (statusCode=2), demais → negado (3)
                        statusCode = if (error.code == 1) 2 else 3,
                        errorReason = error.reason
                    )
                }.getOrElse {
                    CieloPaymentResponse(
                        statusCode = 3,
                        errorReason = "Falha ao decodificar erro: ${it.message}"
                    )
                }
            }

            else -> CieloPaymentResponse(
                statusCode = 3,
                errorReason = "Resposta inválida: sem parâmetros na URI de retorno"
            )
        }
    }
}
