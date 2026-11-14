package br.com.araujosamuel.cieloticketsales.data.cielo

import android.net.Uri
import android.util.Base64
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
     * @param orderId UUID da compra — chave de idempotência
     * @param amountInCents valor total em centavos
     * @param paymentCode forma de pagamento (default: CREDITO_AVISTA)
     * @return Uri no formato lio://payment?request={base64}&urlCallback=order://response
     */
    fun buildPaymentUri(
        orderId: String,
        amountInCents: Int,
        paymentCode: String = "CREDITO_AVISTA"
    ): Uri {
        val request = CieloPaymentRequest(
            accessToken = ACCESS_TOKEN,
            clientID = CLIENT_ID,
            paymentCode = paymentCode,
            amount = amountInCents,
            installments = 1,
            email = "",
            orderId = orderId
        )
        val json = gson.toJson(request)
        val base64 = Base64.encodeToString(
            json.toByteArray(Charsets.UTF_8),
            Base64.NO_WRAP
        )
        return Uri.parse("lio://payment?request=$base64&urlCallback=$CALLBACK_URI")
    }

    /**
     * Interpreta a URI de retorno do Cielo Smart.
     *
     * O Cielo Smart pode retornar dois parâmetros na URI:
     * - "response": Base64 JSON com dados do pagamento aprovado/cancelado
     * - "error": Base64 JSON com código e motivo do erro
     *
     * @param uri URI recebida pela ResponseActivity
     * @return [CieloPaymentResponse] com statusCode e dados da transação
     */
    fun parsePaymentResponse(uri: Uri): CieloPaymentResponse {
        val responseBase64 = uri.getQueryParameter("response")
        val errorBase64 = uri.getQueryParameter("error")

        return when {
            responseBase64 != null -> {
                runCatching {
                    val json = String(
                        Base64.decode(responseBase64, Base64.NO_WRAP),
                        Charsets.UTF_8
                    )
                    gson.fromJson(json, CieloPaymentResponse::class.java)
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
                        Base64.decode(errorBase64, Base64.NO_WRAP),
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
