package ovh.plrapps.mapcompose.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration

/**
 * Limit the rate at which a [block] is called.
 * The [block] execution is triggered upon reception of [Unit] from the returned [SendChannel].
 *
 * @param wait The time in ms between each [block] call.
 *
 * @author P.Laurence
 */

fun CoroutineScope.throttle(wait: Duration, block: suspend () -> Unit): SendChannel<Unit> {
    val channel = Channel<Unit>(capacity = Channel.CONFLATED)
    launch {
        for (it in channel) {
            block()
            delay(wait)
        }
    }
    return channel
}