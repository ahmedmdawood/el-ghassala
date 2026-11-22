package dev.ahmeddawood.elghassala.domain

/** Playful awards computed from the user's own trips. Pure and testable. */
object Awards {
    enum class Kind { TRIPS_KING, GENEROUS, ROUTE_REGULAR }

    data class Trip(
        val origin: String,
        val destination: String,
        val paidByPerson: Map<Long, Long>,
        val personIds: List<Long>,
    )

    data class Award(val kind: Kind, val personId: Long, val value: Long)

    fun compute(trips: List<Trip>): List<Award> {
        if (trips.isEmpty()) return emptyList()
        val out = mutableListOf<Award>()

        val rides = HashMap<Long, Long>()
        trips.forEach { t -> t.personIds.distinct().forEach { rides[it] = (rides[it] ?: 0L) + 1 } }
        rides.maxWithOrNull(compareBy({ it.value }, { -it.key }))?.takeIf { it.value >= 2 }
            ?.let { out += Award(Kind.TRIPS_KING, it.key, it.value) }

        val paid = HashMap<Long, Long>()
        trips.forEach { t -> t.paidByPerson.forEach { (p, c) -> paid[p] = (paid[p] ?: 0L) + c } }
        paid.maxWithOrNull(compareBy({ it.value }, { -it.key }))?.takeIf { it.value > 0 }
            ?.let { out += Award(Kind.GENEROUS, it.key, it.value) }

        val perRoute = HashMap<Triple<String, String, Long>, Long>()
        trips.forEach { t ->
            t.personIds.distinct().forEach { p ->
                val k = Triple(t.origin.trim().lowercase(), t.destination.trim().lowercase(), p)
                perRoute[k] = (perRoute[k] ?: 0L) + 1
            }
        }
        perRoute.maxWithOrNull(compareBy({ it.value }, { -it.key.third }))?.takeIf { it.value >= 3 }
            ?.let { out += Award(Kind.ROUTE_REGULAR, it.key.third, it.value) }
        return out
    }
}
