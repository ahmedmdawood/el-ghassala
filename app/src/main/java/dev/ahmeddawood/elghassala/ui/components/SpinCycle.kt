package dev.ahmeddawood.elghassala.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val clothes = listOf(
    Color(0xFFD9A441), Color(0xFFE76F51), Color(0xFF4FD1B0), Color(0xFFF2C76B), Color(0xFFFFFFFF), Color(0xFF8FCBB9),
)

/** The washing-machine drum: a ring with clothes tumbling round. Used as the app's loader. */
@Composable
fun SpinLoader(modifier: Modifier = Modifier, size: Dp = 64.dp, periodMs: Int = 1500) {
    val t = rememberInfiniteTransition(label = "spin")
    val angle by t.animateFloat(
        0f, 360f, infiniteRepeatable(tween(periodMs, easing = LinearEasing), RepeatMode.Restart), label = "angle",
    )
    val primary = MaterialTheme.colorScheme.primary
    val glass = MaterialTheme.colorScheme.primaryContainer
    Canvas(modifier.size(size)) {
        val r = this.size.minDimension / 2f
        val c = Offset(this.size.width / 2f, this.size.height / 2f)
        drawCircle(glass, radius = r * 0.86f, center = c)
        drawCircle(primary, radius = r * 0.92f, center = c, style = Stroke(width = r * 0.14f))
        rotate(angle, pivot = c) {
            clothes.forEachIndexed { i, col ->
                val a = i * (Math.PI * 2 / clothes.size)
                val rad = r * (if (i % 2 == 0) 0.52f else 0.30f)
                val p = Offset(c.x + (cos(a) * rad).toFloat(), c.y + (sin(a) * rad).toFloat())
                drawCircle(col, radius = r * 0.17f, center = p)
            }
        }
        drawLine(primary, Offset(c.x - r * 0.35f, c.y - r * 1.02f), Offset(c.x + r * 0.35f, c.y - r * 1.02f), strokeWidth = r * 0.08f, cap = StrokeCap.Round)
    }
}

private class Piece(val x: Float, val delay: Float, val speed: Float, val size: Float, val color: Color, val phase: Float, val spin: Float)

/** Hidden spin-cycle screen: reached by tapping the version number seven times. */
@Composable
fun SpinCycleScreen(joke: String, onAnother: () -> Unit, onClose: () -> Unit) {
    val fall = remember { Animatable(0f) }
    val pieces = remember {
        val r = Random(7)
        List(140) {
            Piece(r.nextFloat(), r.nextFloat() * 0.5f, 0.7f + r.nextFloat() * 0.7f, 8f + r.nextFloat() * 10f,
                clothes[r.nextInt(clothes.size)], r.nextFloat() * 6.28f, 120f + r.nextFloat() * 480f)
        }
    }
    LaunchedEffect(joke) {
        fall.snapTo(0f)
        fall.animateTo(1f, tween(4500, easing = LinearEasing))
    }
    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF062E27), Color(0xFF0E7C66))))
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onAnother() },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val tt = fall.value
            pieces.forEach { p ->
                val local = ((tt - p.delay) / (1f - p.delay)).coerceIn(0f, 1f)
                if (local > 0f && local < 1f) {
                    val y = -30f + local * p.speed * (size.height + 60f)
                    val x = p.x * size.width + sin(local * 7f + p.phase) * 46f
                    rotate(degrees = local * p.spin, pivot = Offset(x, y)) {
                        drawRect(p.color, Offset(x, y), Size(p.size, p.size * 0.55f))
                    }
                }
            }
        }
        Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            SpinLoader(size = 190.dp, periodMs = 700)
            Spacer(Modifier.height(28.dp))
            Text(joke, style = MaterialTheme.typography.titleLarge, color = Color(0xFFF2C76B), textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            androidx.compose.material3.TextButton(onClick = onClose) {
                Text("✕", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}
