package dev.ahmeddawood.elghassala.domain

/**
 * App memory: a rider who is on a given route a lot earns a star on that route.
 * Pure and testable. Routes match on trimmed, case-insensitive origin and destination.
 */
object RouteStars {
    const val MIN_RIDES = 3

    data class Ride(val origin: String, val destination: String, val personIds: List<Long>)

    private fun same(a: String, b: String) = a.trim().equals(b.trim(), ignoreCase = true)

    fun counts(rides: List<Ride>, origin: String, destination: String): Map<Long, Int> {
        if (origin.isBlank() || destination.isBlank()) return emptyMap()
        val out = HashMap<Long, Int>()
        rides.filter { same(it.origin, origin) && same(it.destination, destination) }
            .forEach { r -> r.personIds.distinct().forEach { out[it] = (out[it] ?: 0) + 1 } }
        return out
    }

    /** People who earned a star on this route: at least [MIN_RIDES] rides and within 70% of the top rider. */
    fun stars(rides: List<Ride>, origin: String, destination: String): Set<Long> {
        val c = counts(rides, origin, destination)
        val max = c.values.maxOrNull() ?: return emptySet()
        if (max < MIN_RIDES) return emptySet()
        return c.filterValues { it >= MIN_RIDES && it * 10 >= max * 7 }.keys
    }
}
