package br.com.araujosamuel.cieloticketsales.data.cielo.model

import com.google.gson.annotations.SerializedName

/**
 * Payload JSON enviado ao Cielo Smart via Deep Link.
 * É serializado para JSON e então encodado em Base64 antes de compor a URI.
 *
 * URI final: lio://payment?request={base64}&urlCallback=order://response
 *
 * Ref: https://github.com/DeveloperCielo/LIO-SDK-Sample-Integracao-Local
 */
data class CieloPaymentRequest(
    @SerializedName("accessToken")
    val accessToken: String,

    @SerializedName("clientID")
    val clientID: String,

    /**
     * Código da forma de pagamento.
     * Valores suportados: CREDITO_AVISTA, DEBITO_AVISTA, CREDITO_PARCELADO_LOJA, PIX, etc.
     */
    @SerializedName("paymentCode")
    val paymentCode: String,

    /** Valor total em centavos */
    @SerializedName("amount")
    val amount: Int,

    @SerializedName("installments")
    val installments: Int = 1,

    @SerializedName("email")
    val email: String = "",

    /**
     * Chave de idempotência: UUID gerado localmente.
     * Enviado ao Cielo para que reenvios do mesmo pedido não gerem cobranças duplicadas.
     */
    @SerializedName("orderId")
    val orderId: String
)
