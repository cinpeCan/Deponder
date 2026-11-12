package com.cinpe.deponder.internal

import android.view.Choreographer
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Suspends until the next display VSYNC frame and returns the unboxed `frameTimeNanos` primitive [Long].
 *
 * Unlike `Flow<Long>`, direct `suspend` invocation avoids `FlowCollector<Long>` autoboxing and
 * `SafeCollector` overhead on 60–144Hz animation hot paths.
 */
internal suspend fun awaitChoreographerFrameNanos(): Long =
    suspendCancellableCoroutine { cont ->
        val choreographer = Choreographer.getInstance()
        val callback = Choreographer.FrameCallback { frameTimeNanos ->
            if (cont.isActive) {
                cont.resume(frameTimeNanos)
            }
        }
        choreographer.postFrameCallback(callback)
        cont.invokeOnCancellation {
            choreographer.removeFrameCallback(callback)
        }
    }
