package br.com.araujosamuel.cieloticketsales.domain.model

/**
 * Entidade de domínio que representa um evento disponível para venda de ingressos.
 * Eventos são fornecidos de forma estática (sem backend) conforme o escopo do teste técnico.
 */
data class Event(
    val id: String,
    val name: String,
    val description: String,
    val date: String,
    val location: String,
    /** Preço unitário em centavos para evitar aritmética de ponto flutuante */
    val priceInCents: Int,
    val availableTickets: Int,
    val category: String = ""
)
