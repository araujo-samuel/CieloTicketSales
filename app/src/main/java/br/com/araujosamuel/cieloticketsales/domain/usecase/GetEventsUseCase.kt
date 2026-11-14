package br.com.araujosamuel.cieloticketsales.domain.usecase

import br.com.araujosamuel.cieloticketsales.data.repository.EventRepository
import br.com.araujosamuel.cieloticketsales.domain.model.Event

class GetEventsUseCase(
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke(): List<Event> = eventRepository.getEvents()
}
