package br.com.araujosamuel.cieloticketsales.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Cores baseadas na identidade visual da Cielo (azul)
private val CieloBlue = Color(0xFF003087)
private val CieloCyan = Color(0xFF00AEEF)
private val LightColorScheme = lightColorScheme(
    primary = CieloBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E4FF),
    onPrimaryContainer = CieloBlue,
    secondary = CieloCyan,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD6F4FF),
    onSecondaryContainer = Color(0xFF001F28),
    tertiary = Color(0xFF7B5800),
    tertiaryContainer = Color(0xFFFFDEA0),
    onTertiaryContainer = Color(0xFF271900),
    error = Color(0xFFB3261E),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFFEFBFF),
    onBackground = Color(0xFF1B1B1F),
    surface = Color(0xFFFEFBFF),
    onSurface = Color(0xFF1B1B1F),
    surfaceVariant = Color(0xFFE4E2EC),
    onSurfaceVariant = Color(0xFF47464F)
)

@Composable
fun CieloTicketSalesTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
