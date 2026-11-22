package dev.ahmeddawood.elghassala.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.ahmeddawood.elghassala.R
import dev.ahmeddawood.elghassala.domain.Money
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

/** What the end-of-day celebration shows. */
data class DayCelebration(
    val dayTitle: String,
    val trips: Int,
    val totalCents: Long,
    val message: String,
)

private class Confetti(
    val x: Float, val delay: Float, val speed: Float, val size: Float,
    val color: Color, val phase: Float, val spin: Float,
)

private val confettiColors = listOf(
    Color(0xFFD9A441), Color(0xFF0E7C66), Color(0xFFFFFFFF),
    Color(0xFF4FD1B0), Color(0xFFF2C76B), Color(0xFFE76F51),
)

@Composable
fun CelebrationOverlay(celebration: DayCelebration, onDismiss: () -> Unit) {
    val fall = remember { Animatable(0f) }
    val pop = remember { Animatable(0f) }
    val textIn = remember { Animatable(0f) }
    val pieces = remember {
        val r = Random(42)
        List(120) {
            Confetti(
                x = r.nextFloat(), delay = r.nextFloat() * 0.45f,
                speed = 0.75f + r.nextFloat() * 0.6f, size = 8f + r.nextFloat() * 10f,
                color = confettiColors[r.nextInt(confettiColors.size)],
                phase = r.nextFloat() * 6.28f, spin = 120f + r.nextFloat() * 480f,
            )
        }
    }
    LaunchedEffect(Unit) {
        launch { fall.animateTo(1f, tween(4300, easing = LinearEasing)) }
        launch { pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) }
        launch { delay(350); textIn.animateTo(1f, tween(600)) }
        delay(5500)
        onDismiss()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xE6062E27))
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onDismiss() },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val t = fall.value
            pieces.forEach { p ->
                val local = ((t - p.delay) / (1f - p.delay)).coerceIn(0f, 1f)
                if (local > 0f && local < 1f) {
                    val y = -30f + local * p.speed * (size.height + 60f)
                    val x = p.x * size.width + sin(local * 7f + p.phase) * 46f
                    rotate(degrees = local * p.spin, pivot = Offset(x, y)) {
                        drawRect(color = p.color, topLeft = Offset(x, y), size = Size(p.size, p.size * 0.55f))
                    }
                }
            }
        }
        Column(
            Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                Modifier
                    .size(112.dp)
                    .graphicsLayer { scaleX = pop.value; scaleY = pop.value }
                    .clip(CircleShape)
                    .background(Color(0xFFD9A441)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = Color(0xFF062E27), modifier = Modifier.size(64.dp))
            }
            Spacer(Modifier.height(24.dp))
            Column(
                Modifier.graphicsLayer { alpha = textIn.value; translationY = (1f - textIn.value) * 40f },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    stringResource(R.string.day_celebration_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White, textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    celebration.message,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFF2C76B), textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    "${celebration.dayTitle}  •  ${stringResource(R.string.celebration_trips, celebration.trips)}",
                    style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.9f),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.celebration_total, Money.formatWithCurrency(celebration.totalCents)),
                    style = MaterialTheme.typography.titleLarge, color = Color.White,
                )
                Spacer(Modifier.height(18.dp))
                Text(
                    stringResource(R.string.celebration_tap),
                    style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f),
                )
            }
        }
    }
}
