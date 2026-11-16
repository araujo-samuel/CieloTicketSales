package br.com.araujosamuel.cieloticketsales.domain.usecase

import br.com.araujosamuel.cieloticketsales.data.repository.EventRepository
import br.com.araujosamuel.cieloticketsales.domain.model.Event
import javax.inject.Inject

/**
 * Busca um evento pelo ID.
 * Mantém a separação de responsabilidades: ViewModels acessam apenas Use Cases,
 * nunca repositórios diretamente.
 */
class GetEventByIdUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke(eventId: String): Event? =
        eventRepository.getEventById(eventId)
}
