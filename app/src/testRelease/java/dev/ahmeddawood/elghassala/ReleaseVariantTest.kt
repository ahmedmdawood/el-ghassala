package dev.ahmeddawood.elghassala

import org.junit.Assert.*
import org.junit.Test

class ReleaseVariantTest {
    @Test fun releaseIdentityAndDebugFlag() {
        assertFalse(BuildConfig.DEBUG)
        assertEquals("release", BuildConfig.BUILD_TYPE)
        assertEquals("dev.ahmeddawood.elghassala", BuildConfig.APPLICATION_ID)
        assertEquals(6, BuildConfig.VERSION_CODE)
        assertEquals("1.2.2", BuildConfig.VERSION_NAME)
    }
}
