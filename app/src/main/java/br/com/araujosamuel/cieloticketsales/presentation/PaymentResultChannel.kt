package br.com.araujosamuel.cieloticketsales.presentation

import android.net.Uri
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Canal singleton para entregar o resultado do pagamento Cielo ao CheckoutViewModel.
 *
 * Fluxo:
 *   ResponseActivity → Intent → MainActivity → [emit aqui] → CheckoutViewModel
 *
 * A emissão é feita exclusivamente pela MainActivity (em onNewIntent/onCreate),
 * garantindo fonte única. ResponseActivity apenas transporta a URI via Intent;
 * emitir diretamente nela causaria double emission — o mesmo resultado chegaria
 * duas vezes ao ViewModel, podendo contaminar um checkout futuro via buffer.
 *
 * SharedFlow com extraBufferCapacity=1: garante que o evento não seja perdido
 * caso o ViewModel ainda não esteja coletando no momento da emissão.
 */
object PaymentResultChannel {
    private val _results = MutableSharedFlow<Uri>(extraBufferCapacity = 1)
    val results: SharedFlow<Uri> = _results.asSharedFlow()

    fun emit(uri: Uri): Boolean = _results.tryEmit(uri)
}
