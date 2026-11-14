package br.com.araujosamuel.cieloticketsales.data.repository

import br.com.araujosamuel.cieloticketsales.domain.model.Event

/**
 * Implementação com eventos mockados (hardcoded).
 *
 * Decisão: o teste técnico especifica explicitamente que não será avaliada
 * a construção ou não de um backend de apoio para o app. Por isso os eventos
 * são fornecidos de forma estática para focar no que é avaliado:
 * fluxo de compra, integração com Cielo e qualidade do código.
 */
class EventRepositoryImpl : EventRepository {

    private val events = listOf(
        Event(
            id = "evt_001",
            name = "Show: Ana Castela",
            description = "Grande show de música sertaneja com a maior artista do momento. Palco com telões, iluminação especial e muito sertanejo.",
            date = "15/02/2025",
            location = "Arena São Paulo — SP",
            priceInCents = 8000,
            availableTickets = 200,
            category = "Música"
        ),
        Event(
            id = "evt_002",
            name = "Festival Gastronômico SP",
            description = "Os melhores sabores da cidade reunidos em um só lugar. Mais de 50 restaurantes, música ao vivo e experiências gastronômicas únicas.",
            date = "22/02/2025",
            location = "Parque Ibirapuera — SP",
            priceInCents = 3500,
            availableTickets = 500,
            category = "Gastronomia"
        ),
        Event(
            id = "evt_003",
            name = "Peça: O Auto da Compadecida",
            description = "Clássico do teatro brasileiro com direção premiada. Elenco renomado em espetáculo imperdível para toda a família.",
            date = "01/03/2025",
            location = "Teatro Municipal — SP",
            priceInCents = 5000,
            availableTickets = 150,
            category = "Teatro"
        ),
        Event(
            id = "evt_004",
            name = "SP Night Run 10km",
            description = "Corrida noturna pelo centro histórico de São Paulo. Percurso de 10km com iluminação especial e kit do corredor incluso.",
            date = "08/03/2025",
            location = "Centro Histórico — SP",
            priceInCents = 12000,
            availableTickets = 1000,
            category = "Esporte"
        ),
        Event(
            id = "evt_005",
            name = "Expo Tech 2025",
            description = "O maior evento de tecnologia e inovação do Brasil. Palestras, workshops, demos de produtos e networking com referências do setor.",
            date = "15/03/2025",
            location = "Expo Center Norte — SP",
            priceInCents = 2500,
            availableTickets = 5000,
            category = "Tecnologia"
        )
    )

    override suspend fun getEvents(): List<Event> = events

    override suspend fun getEventById(id: String): Event? = events.find { it.id == id }
}
