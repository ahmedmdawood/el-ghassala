package dev.ahmeddawood.elghassala.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.ahmeddawood.elghassala.R
import dev.ahmeddawood.elghassala.Records
import dev.ahmeddawood.elghassala.domain.Awards
import dev.ahmeddawood.elghassala.domain.Money
import dev.ahmeddawood.elghassala.ui.theme.Amber

/** أوسمة الغسالة: playful badges computed from the user's own trips. Hidden until there is data. */
@Composable
fun AwardsCard(records: Records) {
    val awards = remember(records.transports, records.participants) {
        val parts = records.participants.groupBy { it.transportId }
        Awards.compute(
            records.transports.map { t ->
                val p = parts[t.id].orEmpty()
                Awards.Trip(t.origin, t.destination, p.associate { it.personId to it.paidCents }, p.map { it.personId })
            }
        )
    }
    if (awards.isEmpty()) return
    Column(Modifier.padding(horizontal = 16.dp)) {
        Spacer(Modifier.size(12.dp))
        SectionHeader(Icons.Filled.EmojiEvents, stringResource(R.string.awards_title))
        Spacer(Modifier.size(8.dp))
        SectionCard {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                awards.forEach { a ->
                    val name = records.peopleNames[a.personId] ?: return@forEach
                    val (icon, title, detail) = when (a.kind) {
                        Awards.Kind.TRIPS_KING -> Triple(Icons.Filled.Star, R.string.award_trips_king, stringResource(R.string.award_trips_detail, a.value.toInt()))
                        Awards.Kind.GENEROUS -> Triple(Icons.Filled.Favorite, R.string.award_generous, stringResource(R.string.award_generous_detail, Money.shown(a.value)))
                        Awards.Kind.ROUTE_REGULAR -> Triple(Icons.Filled.Route, R.string.award_regular, stringResource(R.string.award_regular_detail, a.value.toInt()))
                    }
                    AwardRow(icon, stringResource(title), name, detail)
                }
            }
        }
    }
}

@Composable
private fun AwardRow(icon: ImageVector, title: String, name: String, detail: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(44.dp).clip(CircleShape).background(Amber.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = Amber) }
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
