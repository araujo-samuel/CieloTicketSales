package br.com.araujosamuel.cieloticketsales.data.cielo.model

import com.google.gson.annotations.SerializedName

/**
 * Resposta de sucesso do Cielo Smart após processamento do pagamento.
 * Decodificada de Base64 JSON recebido no parâmetro "response" da URI de callback.
 *
 * statusCode:
 *   1 = Autorizado/Aprovado
 *   2 = Cancelado
 */
data class CieloPaymentResponse(
    @SerializedName("statusCode")
    val statusCode: Int = 0,

    @SerializedName("authCode")
    val authCode: String? = null,

    @SerializedName("cieloCode")
    val cieloCode: String? = null,

    @SerializedName("brand")
    val brand: String? = null,

    @SerializedName("amount")
    val amount: Int? = null,

    @SerializedName("installments")
    val installments: Int? = null,

    /** Preenchido internamente quando a resposta vem do parâmetro "error" */
    val errorReason: String? = null
)

/**
 * Payload de erro retornado pelo Cielo Smart.
 * Decodificado de Base64 JSON recebido no parâmetro "error" da URI de callback.
 *
 * Códigos:
 *   1 = Cancelado pelo usuário
 *   2 = Erro genérico
 *   3 = Falha no processamento do pagamento
 *   4 = Problema de autenticação
 */
data class CieloPaymentError(
    @SerializedName("code")
    val code: Int = 0,

    @SerializedName("reason")
    val reason: String = ""
)
