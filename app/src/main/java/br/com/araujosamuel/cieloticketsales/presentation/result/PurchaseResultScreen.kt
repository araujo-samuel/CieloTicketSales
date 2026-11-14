package br.com.araujosamuel.cieloticketsales.presentation.result

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.com.araujosamuel.cieloticketsales.domain.model.Purchase
import br.com.araujosamuel.cieloticketsales.domain.model.PurchaseStatus
import br.com.araujosamuel.cieloticketsales.util.QrCodeGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseResultScreen(
    viewModel: PurchaseResultViewModel,
    onBackToHome: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Resultado da Compra") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
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
                    PurchaseResultContent(
                        purchase = uiState.purchase!!,
                        onBackToHome = onBackToHome
                    )
                }

                else -> {
                    Text(
                        text = uiState.error ?: "Erro ao exibir resultado",
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
private fun PurchaseResultContent(purchase: Purchase, onBackToHome: () -> Unit) {
    val (statusIcon, statusText, statusColor) = when (purchase.status) {
        PurchaseStatus.APPROVED -> Triple(
            "✅",
            "Pagamento Aprovado!",
            MaterialTheme.colorScheme.primary
        )
        PurchaseStatus.CANCELLED -> Triple(
            "🚫",
            "Pagamento Cancelado",
            MaterialTheme.colorScheme.secondary
        )
        PurchaseStatus.DENIED -> Triple(
            "❌",
            "Pagamento Negado",
            MaterialTheme.colorScheme.error
        )
        PurchaseStatus.PENDING -> Triple(
            "⏳",
            "Aguardando Processamento",
            MaterialTheme.colorScheme.tertiary
        )
    }

    // Gera QR Code para compras aprovadas
    val qrCodeBitmap: Bitmap? = remember(purchase.id, purchase.status) {
        if (purchase.status == PurchaseStatus.APPROVED) {
            val content = QrCodeGenerator.buildTicketContent(
                purchaseId = purchase.id,
                eventName = purchase.eventName,
                quantity = purchase.quantity,
                authCode = purchase.authCode
            )
            QrCodeGenerator.generate(content)
        } else null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Ícone e status
        Text(text = statusIcon, style = MaterialTheme.typography.displayMedium)
        Text(
            text = statusText,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = statusColor,
            textAlign = TextAlign.Center
        )

        // QR Code do ingresso (apenas para aprovados)
        if (qrCodeBitmap != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Seu ingresso",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Image(
                    bitmap = qrCodeBitmap.asImageBitmap(),
                    contentDescription = "QR Code do ingresso para ${purchase.eventName}",
                    modifier = Modifier
                        .size(220.dp)
                        .padding(12.dp)
                )
            }
        }

        // Resumo da compra
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Detalhes da compra",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                HorizontalDivider()
                ResultRow("Evento", purchase.eventName)
                ResultRow("Ingressos", "${purchase.quantity}")
                ResultRow("Total pago", purchase.formattedTotal())
                ResultRow("Nº do pedido", "#${purchase.id.takeLast(8).uppercase()}")

                if (purchase.status == PurchaseStatus.APPROVED) {
                    purchase.authCode?.let { ResultRow("Código de autorização", it) }
                    purchase.cieloCode?.let { ResultRow("Código Cielo", it) }
                    purchase.cardBrand?.let { ResultRow("Bandeira", it) }
                }

                if (purchase.errorReason != null) {
                    HorizontalDivider()
                    Text(
                        text = "Motivo: ${purchase.errorReason}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onBackToHome,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Voltar ao início", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun ResultRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
    }
}
