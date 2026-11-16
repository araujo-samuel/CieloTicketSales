package br.com.araujosamuel.cieloticketsales.presentation.checkout

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    viewModel: CheckoutViewModel,
    onBack: () -> Unit,
    onNavigateToResult: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Coleta efeitos colaterais: lançar o app Cielo ou reportar que não está instalado
    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is CheckoutSideEffect.LaunchCieloPayment -> {
                    val intent = Intent(Intent.ACTION_VIEW, effect.uri).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    val resolveInfo = context.packageManager.queryIntentActivities(intent, 0)
                    if (resolveInfo.isNullOrEmpty()) {
                        viewModel.processIntent(CheckoutIntent.CieloNotFound)
                    } else {
                        context.startActivity(intent)
                    }
                }
                is CheckoutSideEffect.CieloNotInstalled -> {
                    viewModel.processIntent(CheckoutIntent.CieloNotFound)
                }
            }
        }
    }

    LaunchedEffect(uiState.navigateToResultId) {
        uiState.navigateToResultId?.let { id ->
            viewModel.processIntent(CheckoutIntent.NavigationConsumed)
            onNavigateToResult(id)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Confirmar Compra") },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !uiState.isAwaitingCielo) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                uiState.purchase != null -> {
                    val purchase = uiState.purchase!!

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Resumo da compra
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Resumo do pedido",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                HorizontalDivider()
                                SummaryRow("Evento", purchase.eventName)
                                SummaryRow("Ingressos", "${purchase.quantity}")
                                SummaryRow("Valor total", purchase.formattedTotal())
                                SummaryRow("Pedido", "#${purchase.id.takeLast(8).uppercase()}")
                            }
                        }

                        // Forma de pagamento
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "Forma de pagamento",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                val paymentOptions = listOf(
                                    "CREDITO_AVISTA" to "Crédito à vista",
                                    "DEBITO_AVISTA" to "Débito à vista"
                                )

                                Column(modifier = Modifier.selectableGroup()) {
                                    paymentOptions.forEach { (code, label) ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .selectable(
                                                    selected = uiState.selectedPaymentCode == code,
                                                    onClick = { viewModel.processIntent(CheckoutIntent.SelectPaymentCode(code)) },
                                                    role = Role.RadioButton,
                                                    enabled = !uiState.isAwaitingCielo
                                                )
                                                .padding(vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = uiState.selectedPaymentCode == code,
                                                onClick = null,
                                                enabled = !uiState.isAwaitingCielo
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(label, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Aviso enquanto aguarda Cielo
                        if (uiState.isAwaitingCielo) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Text(
                                        text = "Aguardando conclusão do pagamento na Cielo...",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }

                        uiState.error?.let { error ->
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        // Botão principal
                        Button(
                            onClick = { viewModel.processIntent(CheckoutIntent.InitiatePayment) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            enabled = !uiState.isAwaitingCielo
                        ) {
                            Text(
                                text = if (uiState.isAwaitingCielo) "Aguardando Cielo..." else "Pagar com Cielo Smart",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }

                else -> {
                    Text(
                        text = uiState.error ?: "Pedido não encontrado",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
