package dev.ahmeddawood.elghassala.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ahmeddawood.elghassala.domain.Money
import dev.ahmeddawood.elghassala.ui.theme.NegativeRed
import dev.ahmeddawood.elghassala.ui.theme.PositiveGreen

/** Signature gradient used across headers and hero cards. */
@Composable
fun ghassalaGradient(): Brush = Brush.linearGradient(
    colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.75f),
    )
)

/** Button with a satisfying press bounce on top of the ripple. */
@Composable
fun PressScaleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "pressScale",
    )
    Button(
        onClick = onClick,
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale },
        enabled = enabled,
        interactionSource = interactionSource,
        content = content,
    )
}

/** Card that dips and lifts when pressed. */
@Composable
fun PressScaleCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "cardScale",
    )
    Card(
        onClick = onClick,
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        interactionSource = interactionSource,
        content = content,
    )
}

/** Money amount that animates when it changes. */
@Composable
fun AnimatedMoney(
    cents: Long,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: Int = 22,
    fontWeight: FontWeight = FontWeight.Bold,
) {
    AnimatedContent(
        targetState = cents,
        transitionSpec = {
            (slideInVertically { it / 2 } + fadeIn()) togetherWith (slideOutVertically { -it / 2 } + fadeOut())
        },
        label = "money",
        modifier = modifier,
    ) { value ->
        Text(
            text = Money.shown(value),
            color = color,
            fontSize = fontSize.sp,
            fontWeight = fontWeight,
        )
    }
}

/** Hero gradient header block used on the main sections. */
@Composable
fun GradientHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    content: (@Composable () -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(ghassalaGradient())
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.headlineMedium)
                    if (subtitle != null) {
                        Text(
                            subtitle,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                trailing?.invoke()
            }
            if (content != null) {
                Spacer(Modifier.height(14.dp))
                content()
            }
        }
    }
}

/** Friendly empty state with a soft icon panel. */
@Composable
fun EmptyState(icon: ImageVector, message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Colored balance text: green when positive, red when negative. */
@Composable
fun BalanceText(cents: Long, fontSize: Int = 15) {
    val color = when {
        cents > 0 -> PositiveGreen
        cents < 0 -> NegativeRed
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(Money.shown(cents), color = color, fontSize = fontSize.sp, fontWeight = FontWeight.SemiBold)
}
