package dev.ahmeddawood.elghassala.settings

import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

/**
 * Opening cash per day: who started the day and how much each had on them (in piasters).
 * Kept in DataStore, not Room, so there is no schema change and imported or older days
 * simply have no entry. Map of dayId -> (personId -> cents).
 */
class OpeningCashStore(private val settings: SettingsRepository) {

    private val key = stringPreferencesKey("opening_cash")

    val all: Flow<Map<Long, Map<Long, Long>>> = settings.rawFlow().map { parse(it[key]) }

    suspend fun set(dayId: Long, cash: Map<Long, Long>) {
        settings.editRaw { prefs ->
            val m = parse(prefs[key]).toMutableMap()
            if (cash.isEmpty()) m.remove(dayId) else m[dayId] = cash
            prefs[key] = encode(m)
        }
    }

    companion object {
        fun parse(json: String?): Map<Long, Map<Long, Long>> {
            if (json.isNullOrBlank()) return emptyMap()
            return runCatching {
                val o = JSONObject(json)
                o.keys().asSequence().mapNotNull { dk ->
                    val day = dk.toLongOrNull() ?: return@mapNotNull null
                    val p = o.optJSONObject(dk) ?: return@mapNotNull null
                    day to p.keys().asSequence().mapNotNull { pk -> pk.toLongOrNull()?.let { it to p.optLong(pk) } }.toMap()
                }.toMap()
            }.getOrDefault(emptyMap())
        }

        fun encode(map: Map<Long, Map<Long, Long>>): String {
            val o = JSONObject()
            map.forEach { (day, people) ->
                val p = JSONObject()
                people.forEach { (id, c) -> p.put(id.toString(), c) }
                o.put(day.toString(), p)
            }
            return o.toString()
        }

        /** Total cash the group set out with. */
        fun pool(cash: Map<Long, Long>): Long = cash.values.filter { it > 0 }.sum()
    }
}
