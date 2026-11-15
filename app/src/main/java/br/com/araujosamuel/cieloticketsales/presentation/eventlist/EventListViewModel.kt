package br.com.araujosamuel.cieloticketsales.presentation.eventlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.araujosamuel.cieloticketsales.domain.model.Event
import br.com.araujosamuel.cieloticketsales.domain.usecase.GetEventsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EventListUiState(
    val events: List<Event> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class EventListIntent {
    object RetryLoad : EventListIntent()
}

@HiltViewModel
class EventListViewModel @Inject constructor(
    private val getEventsUseCase: GetEventsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(EventListUiState(isLoading = true))
    val uiState: StateFlow<EventListUiState> = _uiState.asStateFlow()

    init {
        loadEvents()
    }

    fun processIntent(intent: EventListIntent) {
        when (intent) {
            is EventListIntent.RetryLoad -> loadEvents()
        }
    }

    private fun loadEvents() {
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
}
