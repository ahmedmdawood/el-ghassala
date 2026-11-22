package dev.ahmeddawood.elghassala.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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

/** Color personalities. TEAL is the original El Ghassala look. */
enum class AppPalette { TEAL, SUNSET, OCEAN, ROSE, MIDNIGHT }

private fun light(
    primary: Long, container: Long, onContainer: Long, secondary: Long, secContainer: Long,
    tertiary: Long, tertContainer: Long, bg: Long, surfaceVar: Long, low: Long, high: Long, outline: Long,
) = LightScheme.copy(
    primary = Color(primary), primaryContainer = Color(container), onPrimaryContainer = Color(onContainer),
    secondary = Color(secondary), secondaryContainer = Color(secContainer),
    tertiary = Color(tertiary), tertiaryContainer = Color(tertContainer),
    background = Color(bg), surfaceVariant = Color(surfaceVar),
    surfaceContainerLow = Color(low), surfaceContainerHigh = Color(high), outline = Color(outline),
)

private fun dark(
    primary: Long, onPrimary: Long, container: Long, onContainer: Long, secondary: Long, secContainer: Long,
    tertiary: Long, tertContainer: Long, bg: Long, surface: Long, surfaceVar: Long, low: Long, high: Long, outline: Long,
) = DarkScheme.copy(
    primary = Color(primary), onPrimary = Color(onPrimary), primaryContainer = Color(container), onPrimaryContainer = Color(onContainer),
    secondary = Color(secondary), secondaryContainer = Color(secContainer),
    tertiary = Color(tertiary), tertiaryContainer = Color(tertContainer),
    background = Color(bg), surface = Color(surface), surfaceVariant = Color(surfaceVar),
    surfaceContainerLow = Color(low), surfaceContainerHigh = Color(high), outline = Color(outline),
)

fun schemeFor(palette: AppPalette, darkTheme: Boolean): ColorScheme = when (palette) {
    AppPalette.TEAL -> if (darkTheme) DarkScheme else LightScheme
    AppPalette.SUNSET -> if (darkTheme)
        dark(0xFFF2A65A, 0xFF3A1D03, 0xFF7A3E0C, 0xFFFFE0C2, 0xFFE8B98F, 0xFF553A22, 0xFFE9C46A, 0xFF5A4410,
            0xFF171009, 0xFF21170E, 0xFF30221A, 0xFF1C130C, 0xFF2B1E14, 0xFF7A5C45)
    else light(0xFFC4601A, 0xFFFFE0C2, 0xFF4A2204, 0xFF8A5A3A, 0xFFF6E1D1, 0xFF9A7A12, 0xFFF3E4AE, 0xFFFBF5EF,
        0xFFF1E3D8, 0xFFF7EDE4, 0xFFF0E3D8, 0xFFB59A86)
    AppPalette.OCEAN -> if (darkTheme)
        dark(0xFF7FB8FF, 0xFF06223F, 0xFF16406E, 0xFFD3E6FF, 0xFF9FBBD8, 0xFF22394F, 0xFF7FE3D6, 0xFF124E48,
            0xFF0A1118, 0xFF111A24, 0xFF1B2836, 0xFF0E1620, 0xFF182331, 0xFF4B647C)
    else light(0xFF1769C2, 0xFFD3E6FF, 0xFF05274A, 0xFF4A6A8A, 0xFFDCE8F5, 0xFF0F8F82, 0xFFC3F0EA, 0xFFF3F7FC,
        0xFFE0EAF5, 0xFFEAF1F9, 0xFFE1EAF4, 0xFF8AA3BD)
    AppPalette.ROSE -> if (darkTheme)
        dark(0xFFF29BB5, 0xFF45091F, 0xFF7B2744, 0xFFFFD9E4, 0xFFDDA9B9, 0xFF4F2A36, 0xFFE5C07B, 0xFF52401A,
            0xFF170C10, 0xFF22141A, 0xFF331F27, 0xFF1D1015, 0xFF2D1B22, 0xFF7C4E5D)
    else light(0xFFC2395F, 0xFFFFD9E4, 0xFF4A0A20, 0xFF8A4A5E, 0xFFF7DFE6, 0xFFA77A12, 0xFFF6E5B5, 0xFFFDF4F6,
        0xFFF3E0E6, 0xFFF9EBEF, 0xFFF3E1E7, 0xFFB98B99)
    AppPalette.MIDNIGHT -> if (darkTheme)
        dark(0xFFB4A7FF, 0xFF1A0E52, 0xFF352A84, 0xFFE4DEFF, 0xFFB5ADE0, 0xFF2B2847, 0xFFFFC46B, 0xFF4F3606,
            0xFF000000, 0xFF0B0B10, 0xFF17171F, 0xFF07070B, 0xFF13131A, 0xFF4A4A63)
    else light(0xFF5A48D6, 0xFFE4DEFF, 0xFF1D1160, 0xFF5E5A8C, 0xFFE6E3F5, 0xFFB07E1E, 0xFFF6E3B8, 0xFFF6F5FC,
        0xFFE6E4F4, 0xFFEEEDF8, 0xFFE7E5F4, 0xFF9A98BD)
}

/** Fades every key color to its new value, so changing theme or palette glides instead of snapping. */
@Composable
private fun animated(target: ColorScheme): ColorScheme {
    @Composable
    fun a(c: Color): Color = animateColorAsState(c, tween(550), label = "c").value
    return target.copy(
        primary = a(target.primary), onPrimary = a(target.onPrimary),
        primaryContainer = a(target.primaryContainer), onPrimaryContainer = a(target.onPrimaryContainer),
        secondary = a(target.secondary), onSecondary = a(target.onSecondary),
        secondaryContainer = a(target.secondaryContainer), onSecondaryContainer = a(target.onSecondaryContainer),
        tertiary = a(target.tertiary), onTertiary = a(target.onTertiary),
        tertiaryContainer = a(target.tertiaryContainer), onTertiaryContainer = a(target.onTertiaryContainer),
        background = a(target.background), onBackground = a(target.onBackground),
        surface = a(target.surface), onSurface = a(target.onSurface),
        surfaceVariant = a(target.surfaceVariant), onSurfaceVariant = a(target.onSurfaceVariant),
        surfaceContainerLow = a(target.surfaceContainerLow), surfaceContainerHigh = a(target.surfaceContainerHigh),
        outline = a(target.outline), error = a(target.error), onError = a(target.onError),
    )
}

@Composable
fun ElGhassalaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    palette: AppPalette = AppPalette.TEAL,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = animated(schemeFor(palette, darkTheme)),
        typography = ElGhassalaTypography,
        content = content,
    )
}
