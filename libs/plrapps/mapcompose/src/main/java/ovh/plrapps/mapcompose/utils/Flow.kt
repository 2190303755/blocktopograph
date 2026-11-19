package ovh.plrapps.mapcompose.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

fun <T> Flow<T>.throttle(wait: Long) = channelFlow {
    val helper = Channel<T>(capacity = Channel.CONFLATED)
    val sender = launch {
        for (e in helper) {
            send(e)
            delay(wait)
        }
    }
    collect(helper::send)
    helper.close()
    sender.join()
}

fun <T, M> StateFlow<T>.map(
    coroutineScope : CoroutineScope,
    mapper : (value : T) -> M
) : StateFlow<M> = map { mapper(it) }.stateIn(
    coroutineScope,
    SharingStarted.Eagerly,
    mapper(value)
)