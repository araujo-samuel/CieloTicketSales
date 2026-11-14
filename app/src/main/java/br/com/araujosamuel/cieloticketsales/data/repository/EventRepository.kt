package br.com.araujosamuel.cieloticketsales.data.repository

import br.com.araujosamuel.cieloticketsales.domain.model.Event

interface EventRepository {
    suspend fun getEvents(): List<Event>
    suspend fun getEventById(id: String): Event?
}
