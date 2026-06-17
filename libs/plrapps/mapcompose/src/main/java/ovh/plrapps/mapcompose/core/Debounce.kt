package ovh.plrapps.mapcompose.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlin.time.Duration

/**
 * So long as the returned [MutableSharedFlow] receives [T] elements, the provided [collector] function isn't
 * executed until a time-span of [timeout] elapses.
 * When [collector] is executed, it's provided with the last [T] value sent to the flow.
 */
@OptIn(FlowPreview::class)
fun <T> CoroutineScope.debounce(
    timeout: Duration,
    collector: FlowCollector<T>
): MutableSharedFlow<T> {
    val flow = MutableSharedFlow<T>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    launch {
        flow.debounce(timeout).collect(collector)
    }
    return flow
}