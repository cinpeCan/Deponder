package com.cinpe.deponder.internal

import android.view.Choreographer
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Emits [Choreographer] VSYNC `frameTimeNanos` on each display frame while collected.
 *
 * Schedules the next VSYNC callback only after the collector finishes processing the current frame,
 * naturally preventing frame backlog without channel buffering overhead.
 */
internal fun choreographerFrameFlow(): Flow<Long> = flow {
    val choreographer = Choreographer.getInstance()
    while (currentCoroutineContext().isActive) {
        val frameTimeNanos = suspendCancellableCoroutine<Long> { cont ->
            val callback = Choreographer.FrameCallback { frameTime ->
                if (cont.isActive) {
                    cont.resume(frameTime)
                }
            }
            choreographer.postFrameCallback(callback)
            cont.invokeOnCancellation {
                choreographer.removeFrameCallback(callback)
            }
        }
        emit(frameTimeNanos)
    }
}
