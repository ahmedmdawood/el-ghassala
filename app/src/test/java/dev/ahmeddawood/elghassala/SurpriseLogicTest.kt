package dev.ahmeddawood.elghassala

import dev.ahmeddawood.elghassala.domain.Awards
import dev.ahmeddawood.elghassala.domain.RouteStars
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SurpriseLogicTest {

    private fun ride(vararg ids: Long) = RouteStars.Ride("Home", "Work", ids.toList())

    @Test
    fun star_needsMinimumRides() {
        val rides = listOf(ride(1, 2), ride(1), RouteStars.Ride("Home", "Mall", listOf(1, 1)))
        assertTrue(RouteStars.stars(rides, "Home", "Work").isEmpty())
    }

    @Test
    fun star_goesToFrequentRidersOnThatRouteOnly() {
        val rides = listOf(ride(1, 2), ride(1, 2), ride(1), ride(1), ride(2), RouteStars.Ride("Home", "Mall", listOf(3, 3, 3, 3)))
        // person 1: 4 rides, person 2: 3 rides (>= 70% of 4 is 2.8, so yes), person 3 not on this route
        assertEquals(setOf(1L, 2L), RouteStars.stars(rides, " home ", "WORK"))
        assertEquals(setOf(3L), RouteStars.stars(rides + RouteStars.Ride("Home", "Mall", listOf(3)), "Home", "Mall"))
    }

    @Test
    fun star_ignoresBlankRoute() {
        assertTrue(RouteStars.stars(listOf(ride(1), ride(1), ride(1)), "", "Work").isEmpty())
    }

    @Test
    fun awards_pickKingGenerousAndRegular() {
        val trips = listOf(
            Awards.Trip("A", "B", mapOf(1L to 1000L, 2L to 0L), listOf(1, 2)),
            Awards.Trip("A", "B", mapOf(1L to 500L, 2L to 0L), listOf(1, 2)),
            Awards.Trip("A", "B", mapOf(1L to 0L, 2L to 800L), listOf(1, 2)),
            Awards.Trip("C", "D", mapOf(1L to 0L), listOf(1)),
        )
        val awards = Awards.compute(trips).associateBy { it.kind }
        assertEquals(1L, awards[Awards.Kind.TRIPS_KING]!!.personId)
        assertEquals(4L, awards[Awards.Kind.TRIPS_KING]!!.value)
        assertEquals(1L, awards[Awards.Kind.GENEROUS]!!.personId)
        assertEquals(1500L, awards[Awards.Kind.GENEROUS]!!.value)
        assertEquals(3L, awards[Awards.Kind.ROUTE_REGULAR]!!.value)
    }

    @Test
    fun awards_emptyWhenNoTrips() {
        assertTrue(Awards.compute(emptyList()).isEmpty())
    }
}
