package com.websnake.recentstv.data

/** The remote binder checks its caller's package, not the client application's package. */
internal object TaskMoveArguments {
    fun create(types: Array<Class<*>>, taskId: Int, remoteUid: Int): Array<Any?> {
        var firstInt = true
        var firstString = true
        val callingPackage = if (remoteUid == 2000) "com.android.shell" else null
        return types.map { type ->
            when (type) {
                Int::class.javaPrimitiveType -> (if (firstInt) taskId else 0).also { firstInt = false }
                String::class.java -> (if (firstString) callingPackage else null).also { firstString = false }
                Boolean::class.javaPrimitiveType -> false
                Long::class.javaPrimitiveType -> 0L
                else -> null
            }
        }.toTypedArray()
    }
}
