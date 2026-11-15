package br.com.araujosamuel.cieloticketsales.data.cielo.model

import com.google.gson.annotations.SerializedName

/**
 * Resposta interna normalizada após parsear o callback do Cielo Smart.
 * Não mapeada diretamente do JSON — é montada em [CieloDeepLinkService.parsePaymentResponse].
 *
 * statusCode:
 *   1 = Autorizado/Aprovado  (Order.status == "PAID")
 *   2 = Cancelado pelo usuário
 *   3 = Negado / erro de processamento
 */
data class CieloPaymentResponse(
    val statusCode: Int = 0,
    val authCode: String? = null,
    val cieloCode: String? = null,
    val brand: String? = null,
    val amount: Int? = null,
    val installments: Int? = null,
    /** Preenchido quando a resposta indica erro ou cancelamento */
    val errorReason: String? = null
)

/**
 * Estrutura real retornada pelo Cielo Smart no parâmetro "response" da URI de callback.
 *
 * O Cielo Smart retorna um objeto Order (pedido) completo com a lista de transações.
 * Formato confirmado via análise de logcat com o emulador oficial (v1.61.8).
 *
 * Campos relevantes:
 * - [status]: "PAID" para pagamento aprovado; outros valores indicam falha/cancelamento
 * - [payments]: lista de transações; a primeira contém authCode, cieloCode e brand
 */
data class CieloOrderResponse(
    @SerializedName("status")
    val status: String = "",

    @SerializedName("payments")
    val payments: List<CieloOrderPayment> = emptyList()
)

/**
 * Dados de uma transação dentro da resposta Order do Cielo Smart.
 */
data class CieloOrderPayment(
    @SerializedName("authCode")
    val authCode: String? = null,

    @SerializedName("cieloCode")
    val cieloCode: String? = null,

    @SerializedName("brand")
    val brand: String? = null,

    @SerializedName("amount")
    val amount: Int? = null,

    @SerializedName("installments")
    val installments: Int? = null
)

/**
 * Payload de erro retornado pelo Cielo Smart no parâmetro "error" da URI de callback.
 *
 * Códigos conhecidos:
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
