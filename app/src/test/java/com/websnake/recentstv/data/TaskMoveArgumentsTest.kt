package com.websnake.recentstv.data

import org.junit.Assert.*
import org.junit.Test

class TaskMoveArgumentsTest {
    @Test fun shellIdentityAndTaskIdOccupyTheCorrectSignatureSlots() {
        val types = arrayOf(Any::class.java, String::class.java, Integer.TYPE, Integer.TYPE, Any::class.java)
        assertArrayEquals(arrayOf(null, "com.android.shell", 123, 0, null), TaskMoveArguments.create(types, 123, 2000))
    }
    @Test fun extraAttributionAndRootIdentityDoNotReceiveTheClientPackage() {
        val types = arrayOf(Any::class.java, String::class.java, String::class.java, Integer.TYPE, Integer.TYPE, java.lang.Boolean.TYPE)
        assertArrayEquals(arrayOf(null, "com.android.shell", null, 456, 0, false), TaskMoveArguments.create(types, 456, 2000))
        assertArrayEquals(arrayOf(null, null, null, 456, 0, false), TaskMoveArguments.create(types, 456, 0))
    }
}
