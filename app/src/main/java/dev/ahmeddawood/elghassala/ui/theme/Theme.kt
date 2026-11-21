package dev.ahmeddawood.elghassala.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// El Ghassala identity: deep lagoon teal + warm amber. Same premium discipline
// as ZAD (paired light/dark schemes, tinted surfaces) with its own palette.
val Teal = Color(0xFF0E7C66)
val TealDeep = Color(0xFF0A5F50)
val Amber = Color(0xFFD9A441)
val MintLight = Color(0xFF6FD3B6)
val SandLight = Color(0xFFF3F7F5)
val InkDark = Color(0xFF0C1413)

val PositiveGreen = Color(0xFF2E9E6B)
val NegativeRed = Color(0xFFD96A5F)

val LightScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF0E7C66),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCDEDE3),
    onPrimaryContainer = Color(0xFF06332A),
    secondary = Color(0xFF3E7A6A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD7EEE6),
    onSecondaryContainer = Color(0xFF123B31),
    tertiary = Color(0xFFB07E1E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF6E3B8),
    onTertiaryContainer = Color(0xFF4A3205),
    background = Color(0xFFF3F7F5),
    onBackground = Color(0xFF10201B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF10201B),
    surfaceVariant = Color(0xFFE2EFEA),
    onSurfaceVariant = Color(0xFF40564F),
    surfaceContainerLow = Color(0xFFEDF4F1),
    surfaceContainerHigh = Color(0xFFE4EEE9),
    outline = Color(0xFF8AA79E),
    error = Color(0xFFB4372C),
    onError = Color(0xFFFFFFFF),
)

val DarkScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFF6FD3B6),
    onPrimary = Color(0xFF06231B),
    primaryContainer = Color(0xFF155C4C),
    onPrimaryContainer = Color(0xFFCDEDE3),
    secondary = Color(0xFF8FCBB9),
    onSecondary = Color(0xFF0A2620),
    secondaryContainer = Color(0xFF1E443B),
    onSecondaryContainer = Color(0xFFD7EEE6),
    tertiary = Color(0xFFE5B95A),
    onTertiary = Color(0xFF3A2903),
    tertiaryContainer = Color(0xFF55400F),
    onTertiaryContainer = Color(0xFFF6E3B8),
    background = Color(0xFF0C1413),
    onBackground = Color(0xFFE3F2ED),
    surface = Color(0xFF131F1D),
    onSurface = Color(0xFFE3F2ED),
    surfaceVariant = Color(0xFF1D2E2A),
    onSurfaceVariant = Color(0xFFA9C6BC),
    surfaceContainerLow = Color(0xFF101B19),
    surfaceContainerHigh = Color(0xFF1B2A27),
    outline = Color(0xFF4E6B62),
    error = Color(0xFFE8A49D),
    onError = Color(0xFF4A0E08),
)

val ElGhassalaTypography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 23.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 19.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyLarge = TextStyle(fontSize = 15.sp),
    bodyMedium = TextStyle(fontSize = 13.sp),
)

@Composable
fun ElGhassalaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = ElGhassalaTypography,
        content = content,
    )
}
