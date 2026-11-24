package ovh.plrapps.mapcompose.utils

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

internal suspend fun withRetryImpl(maxRetry: Int, intervalMs: Long, block: suspend () -> Boolean) {
    var cnt = 0
    var res = block()
    while (!res && cnt < maxRetry) {
        delay(intervalMs)
        res = block()
        cnt++
    }
}

internal suspend inline fun withRetry(
    maxRetry: Int,
    intervalMs: Long,
    crossinline block: suspend () -> Unit
) {
    withRetryImpl(maxRetry, intervalMs) {
        try {
            block()
            true
        } catch (e: CancellationException) {
            if (!currentCoroutineContext().isActive) throw e
            false
        } catch (_: Throwable) {
            false
        }
    }
}
