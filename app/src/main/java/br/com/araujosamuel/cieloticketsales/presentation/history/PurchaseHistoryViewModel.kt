package br.com.araujosamuel.cieloticketsales.presentation.history

import androidx.lifecycle.ViewModel
import br.com.araujosamuel.cieloticketsales.domain.model.Purchase
import br.com.araujosamuel.cieloticketsales.domain.usecase.GetPurchasesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class PurchaseHistoryViewModel @Inject constructor(
    getPurchasesUseCase: GetPurchasesUseCase
) : ViewModel() {

    val purchases: Flow<List<Purchase>> = getPurchasesUseCase()
}
