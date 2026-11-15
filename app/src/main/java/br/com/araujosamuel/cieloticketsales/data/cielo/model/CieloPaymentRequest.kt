package br.com.araujosamuel.cieloticketsales.data.cielo.model

import com.google.gson.annotations.SerializedName

/**
 * Payload JSON enviado ao Cielo Smart via Deep Link.
 *
 * Formato correto descoberto via análise do sample oficial:
 * https://github.com/DeveloperCielo/LIO-SDK-Sample-Integracao-Local
 *
 * Campos críticos:
 * - [value]: valor em REAIS como String (ex: "80.00"), NÃO em centavos
 * - [items]: lista de produtos obrigatória
 * - [installments]: 0 para à vista (não 1)
 *
 * Erros comuns:
 * - Usar `amount` em vez de `value` → "Json inválido, order: null"
 * - Omitir `items` → mesmo erro
 * - Enviar valor em centavos → erro de valor inválido
 */
data class CieloPaymentRequest(
    @SerializedName("accessToken")
    val accessToken: String,

    @SerializedName("clientID")
    val clientID: String,

    @SerializedName("email")
    val email: String = "",

    /**
     * Número de parcelas. Use 0 para pagamentos à vista.
     * Crédito parcelado: número de parcelas (2, 3, 6, 12...)
     */
    @SerializedName("installments")
    val installments: Int = 0,

    /** Lista de itens da compra (obrigatório) */
    @SerializedName("items")
    val items: List<CieloOrderItem>,

    /**
     * Código da forma de pagamento.
     * Valores: CREDITO_AVISTA, DEBITO_AVISTA, CREDITO_PARCELADO_LOJA, PIX
     */
    @SerializedName("paymentCode")
    val paymentCode: String,

    /**
     * Valor total em REAIS como String.
     * Exemplo: "80.00" para R$80,00
     * ATENÇÃO: NÃO é em centavos. Converter: cents / 100.0 → formatar como "%.2f"
     */
    @SerializedName("value")
    val value: String
)

data class CieloOrderItem(
    @SerializedName("name")
    val name: String,

    @SerializedName("quantity")
    val quantity: Int,

    @SerializedName("sku")
    val sku: String,

    @SerializedName("unitOfMeasure")
    val unitOfMeasure: String = "unit",

    /** Preço unitário em REAIS (ex: 80.00 para R$80,00) */
    @SerializedName("unitPrice")
    val unitPrice: Double
)
