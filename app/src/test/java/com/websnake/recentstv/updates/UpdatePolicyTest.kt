package com.websnake.recentstv.updates

import org.junit.Assert.*
import org.junit.Test

class UpdatePolicyTest {
    @Test fun numericVersionsPreventDowngradesAndHandleDoubleDigits() {
        assertTrue(UpdatePolicy.newer("v1.10.0", "1.9.9"))
        assertTrue(UpdatePolicy.newer("v2.0.0", "1.99.99"))
        assertFalse(UpdatePolicy.newer("v1.0.0", "1.0.1"))
        assertFalse(UpdatePolicy.newer("v1.0.1", "1.0.1"))
    }
    @Test fun unknownAndPrereleaseVersionsAreNotOffered() {
        for (tag in listOf("latest", "v1.0.2-beta", "v999999999999.0.0", "v1.0", "v1.0.2/path"))
            assertFalse(UpdatePolicy.newer(tag, "1.0.1"))
        assertFalse(UpdatePolicy.newer("v1.0.2", "1.0.1-migration"))
    }
    @Test fun sameVersionBuildUpdatesRequireStrictlyHigherCode() {
        assertFalse(UpdatePolicy.offer("v1.1.0", "1.0.1", 48L, 48L))
        assertTrue(UpdatePolicy.offer("v1.1.0", "1.0.1", 49L, 48L))
        assertTrue(UpdatePolicy.offer("v1.1.0", "1.1.0", 49L, 48L))
        assertTrue(UpdatePolicy.offer("v1.0.1", "1.0.1", 19L, 18L))
        assertFalse(UpdatePolicy.offer("v1.0.1", "1.0.1", 19L, 19L))
        assertFalse(UpdatePolicy.offer("v1.0.1", "1.0.1", 18L, 19L))
        assertFalse(UpdatePolicy.offer("v1.0.0", "1.0.1", 99L, 18L))
        assertFalse(UpdatePolicy.offer("v1.0.2", "1.0.1", 18L, 18L))
        assertFalse(UpdatePolicy.offer("v1.0.1", "1.0.1", 0L, 18L))
        assertFalse(UpdatePolicy.offer("v1.0.1", "1.0.1", 2100000001L, 18L))
        assertFalse(UpdatePolicy.offer("v1.0.1-beta", "1.0.1", 19L, 18L))
        assertFalse(UpdatePolicy.offer("v1.0.1", "1.0.1", null, 18L))
        assertTrue(UpdatePolicy.offer("v1.0.2", "1.0.1", null, 18L))
    }
    @Test fun checksumMustMatchExactFilenameAndBeUnique() {
        val hash = "a".repeat(64)
        assertEquals(hash, UpdatePolicy.checksum("$hash  RecentAppsTV-1.0.2.apk\r\n", "RecentAppsTV-1.0.2.apk"))
        assertEquals(hash, UpdatePolicy.checksum("$hash *RecentAppsTV-1.0.2.apk", "RecentAppsTV-1.0.2.apk"))
        assertNull(UpdatePolicy.checksum("$hash  other.apk", "RecentAppsTV-1.0.2.apk"))
        assertNull(UpdatePolicy.checksum("1234  RecentAppsTV-1.0.2.apk", "RecentAppsTV-1.0.2.apk"))
        assertNull(UpdatePolicy.checksum("$hash  app.apk\n$hash  app.apk", "app.apk"))
    }
}
