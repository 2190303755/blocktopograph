package ovh.plrapps.mapcompose.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * So long as the returned [SendChannel] receives [T] elements, the provided [collector] function isn't
 * executed until a time-span of [timeoutMillis] elapses.
 * When [collector] is executed, it's provided with the last [T] value sent to the channel.
 */
@OptIn(FlowPreview::class)
fun <T> CoroutineScope.debounce(
    timeoutMillis: Long,
    collector: FlowCollector<T>
): SendChannel<T> {
    val channel = Channel<T>(capacity = Channel.CONFLATED)
    val flow = channel.receiveAsFlow().debounce(timeoutMillis)
    launch {
        flow.collect(collector)
    }

    return channel
}