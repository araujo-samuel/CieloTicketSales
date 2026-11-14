package br.com.araujosamuel.cieloticketsales.presentation.eventlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.araujosamuel.cieloticketsales.domain.model.Event
import br.com.araujosamuel.cieloticketsales.domain.usecase.GetEventsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EventListUiState(
    val events: List<Event> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class EventListViewModel(
    private val getEventsUseCase: GetEventsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(EventListUiState(isLoading = true))
    val uiState: StateFlow<EventListUiState> = _uiState.asStateFlow()

    init {
        loadEvents()
    }

    fun loadEvents() {
        viewModelScope.launch {
            _uiState.value = EventListUiState(isLoading = true)
            runCatching { getEventsUseCase() }
                .onSuccess { events ->
                    _uiState.value = EventListUiState(events = events)
                }
                .onFailure { error ->
                    _uiState.value = EventListUiState(error = error.message ?: "Erro ao carregar eventos")
                }
        }
    }

    class Factory(private val getEventsUseCase: GetEventsUseCase) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            EventListViewModel(getEventsUseCase) as T
    }
}
