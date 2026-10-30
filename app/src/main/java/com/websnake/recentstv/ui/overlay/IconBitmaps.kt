package com.websnake.recentstv.ui.overlay

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.core.graphics.drawable.toBitmap

/** Reuse immutable rendered pixels, never mutable Painter state. Bounded to 2 MiB. */
internal object IconBitmaps {
    private val images = object : LruCache<Drawable, Bitmap>(2 * 1024 * 1024) {
        override fun sizeOf(key: Drawable, value: Bitmap) = value.allocationByteCount.coerceAtLeast(1)
    }
    private var initialized = false

    @Synchronized
    fun initialize(context: Context) {
        if (initialized) return
        initialized = true
        context.applicationContext.registerComponentCallbacks(object : ComponentCallbacks2 {
            override fun onConfigurationChanged(newConfig: Configuration) { images.evictAll() }
            override fun onLowMemory() { images.evictAll() }
            override fun onTrimMemory(level: Int) {
                if (level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW ||
                    level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL ||
                    level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) images.evictAll()
            }
        })
    }

    fun prepare(drawables: List<Drawable>) {
        // Cloneable app icons can be rendered on the existing loader worker safely.
        drawables.filter { it.constantState != null }.forEach { get(it) }
    }

    @Synchronized
    fun get(drawable: Drawable): Bitmap {
        images.get(drawable)?.let { return it }
        val cap = 160
        val iw = drawable.intrinsicWidth.takeIf { it > 0 } ?: cap
        val ih = drawable.intrinsicHeight.takeIf { it > 0 } ?: cap
        val scale = minOf(1f, cap.toFloat() / maxOf(iw, ih))
        val source = drawable.constantState?.newDrawable()?.mutate() ?: drawable
        val image = source.toBitmap((iw * scale).toInt().coerceAtLeast(1), (ih * scale).toInt().coerceAtLeast(1))
        images.put(drawable, image)
        // Do not recycle evictions: an existing composition can still be drawing them.
        return image
    }
}
