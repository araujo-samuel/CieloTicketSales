package br.com.araujosamuel.cieloticketsales.presentation.eventdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.araujosamuel.cieloticketsales.data.repository.EventRepository
import br.com.araujosamuel.cieloticketsales.domain.model.Event
import br.com.araujosamuel.cieloticketsales.domain.usecase.CreatePurchaseOrderUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EventDetailUiState(
    val event: Event? = null,
    val quantity: Int = 1,
    val isLoading: Boolean = false,
    val error: String? = null,
    val navigateToPurchaseId: String? = null
) {
    val totalInCents: Int get() = (event?.priceInCents ?: 0) * quantity
}

class EventDetailViewModel(
    private val eventId: String,
    private val eventRepository: EventRepository,
    private val createPurchaseOrderUseCase: CreatePurchaseOrderUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(EventDetailUiState(isLoading = true))
    val uiState: StateFlow<EventDetailUiState> = _uiState.asStateFlow()

    init {
        loadEvent()
    }

    private fun loadEvent() {
        viewModelScope.launch {
            runCatching { eventRepository.getEventById(eventId) }
                .onSuccess { event ->
                    _uiState.value = EventDetailUiState(event = event)
                }
                .onFailure { error ->
                    _uiState.value = EventDetailUiState(error = error.message ?: "Evento não encontrado")
                }
        }
    }

    fun incrementQuantity() {
        val current = _uiState.value
        val max = current.event?.availableTickets ?: 10
        if (current.quantity < minOf(max, 10)) {
            _uiState.value = current.copy(quantity = current.quantity + 1)
        }
    }

    fun decrementQuantity() {
        val current = _uiState.value
        if (current.quantity > 1) {
            _uiState.value = current.copy(quantity = current.quantity - 1)
        }
    }

    fun onBuyClicked() {
        val state = _uiState.value
        val event = state.event ?: return

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true)
            runCatching {
                createPurchaseOrderUseCase(event, state.quantity)
            }.onSuccess { purchase ->
                _uiState.value = state.copy(
                    isLoading = false,
                    navigateToPurchaseId = purchase.id
                )
            }.onFailure { error ->
                _uiState.value = state.copy(
                    isLoading = false,
                    error = error.message ?: "Erro ao criar pedido"
                )
            }
        }
    }

    fun onNavigationConsumed() {
        _uiState.value = _uiState.value.copy(navigateToPurchaseId = null)
    }

    class Factory(
        private val eventId: String,
        private val eventRepository: EventRepository,
        private val createPurchaseOrderUseCase: CreatePurchaseOrderUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            EventDetailViewModel(eventId, eventRepository, createPurchaseOrderUseCase) as T
    }
}
