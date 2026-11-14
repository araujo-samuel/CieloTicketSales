package br.com.araujosamuel.cieloticketsales.domain.model

/**
 * Status possíveis de uma compra.
 * Mapeados diretamente dos statusCode da Cielo e dos códigos de erro.
 *
 * PENDING  → compra criada localmente, aguardando resposta do Cielo
 * APPROVED → Cielo retornou statusCode=1 (autorizado)
 * CANCELLED → Cielo retornou statusCode=2 ou código de erro 1 (cancelado pelo usuário)
 * DENIED  → Cielo retornou código de erro 2, 3 ou 4 (falha no processamento)
 */
enum class PurchaseStatus {
    PENDING,
    APPROVED,
    CANCELLED,
    DENIED
}

/**
 * Entidade de domínio que representa uma tentativa de compra de ingressos.
 *
 * O campo [id] é um UUID gerado localmente e usado como chave de idempotência:
 * ele é enviado como orderId na requisição Cielo, permitindo que reenvios do mesmo
 * pedido sejam identificados e não gerem cobranças duplicadas.
 */
data class Purchase(
    /** UUID — chave de idempotência também enviada ao Cielo como orderId */
    val id: String,
    val eventId: String,
    val eventName: String,
    val quantity: Int,
    /** Total em centavos = quantidade × preço unitário */
    val totalAmountInCents: Int,
    val status: PurchaseStatus,
    val createdAt: Long,
    /** Código de autorização retornado pela Cielo (disponível quando APPROVED) */
    val authCode: String? = null,
    /** Código NSU/transação Cielo (disponível quando APPROVED) */
    val cieloCode: String? = null,
    /** Bandeira do cartão (ex: VISA, MASTERCARD) */
    val cardBrand: String? = null,
    /** Motivo do erro/cancelamento (disponível quando DENIED ou CANCELLED) */
    val errorReason: String? = null,
    /** Forma de pagamento escolhida pelo usuário */
    val paymentCode: String = "CREDITO_AVISTA"
) {
    /** Formata o total para exibição em reais */
    fun formattedTotal(): String {
        val reais = totalAmountInCents / 100
        val centavos = totalAmountInCents % 100
        return "R$ %d,%02d".format(reais, centavos)
    }
}
