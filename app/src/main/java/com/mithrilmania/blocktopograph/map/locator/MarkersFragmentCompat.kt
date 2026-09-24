package com.mithrilmania.blocktopograph.map.locator

import android.view.View
import androidx.lifecycle.lifecycleScope
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.databinding.FragLocatorPlayersBinding
import com.mithrilmania.blocktopograph.map.MarkerManager
import com.mithrilmania.blocktopograph.map.Player
import com.mithrilmania.blocktopograph.map.locator.LocatorMarkersFragment.MarkersAdapter
import com.mithrilmania.blocktopograph.map.locator.LocatorPlayersFragment.PlayersAdapter
import com.mithrilmania.blocktopograph.map.marker.AbstractMarker
import com.mithrilmania.blocktopograph.nbt.CompoundTag
import com.mithrilmania.blocktopograph.nbt.io.BedrockNBTInput
import com.mithrilmania.blocktopograph.nbt.io.readAnonymousTypedTag
import com.mithrilmania.blocktopograph.util.math.DimensionVector3
import com.mithrilmania.blocktopograph.util.startsWith
import com.mithrilmania.blocktopograph.world.KeyPrefix
import com.mithrilmania.blocktopograph.world.World
import com.mithrilmania.blocktopograph.world.extractPlayerPosCompat
import com.mithrilmania.blocktopograph.world.resolveLocalPlayerPos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.iq80.leveldb.ReadOptions
import java.lang.ref.WeakReference


fun World.getMarkerManager(): MarkerManager {
    throw UnsupportedOperationException("Not implemented yet")
}

fun loadLocatorMarkers(
    fragment: LocatorMarkersFragment,
    world: World,
    binding: FragLocatorPlayersBinding
) {
    // TODO use viewModelScope
    fragment.lifecycleScope.launch(Dispatchers.IO) {
        val markers: List<AbstractMarker> = try {
            world.getMarkerManager().markers // TODO: use SAF
        } catch (_: Exception) {
            // TODO log
            emptyList()
        }
        withContext(Dispatchers.Main) {
            binding.loading.visibility = View.GONE
            if (markers.isEmpty()) {
                binding.empty.visibility = View.VISIBLE
                binding.empty.setText(R.string.no_custom_markers)
            } else {
                binding.list.setAdapter(
                    MarkersAdapter(WeakReference(fragment), markers)
                )
                binding.list.visibility = View.VISIBLE
            }
        }
    }
}

fun loadPlayerMarkers(
    fragment: LocatorPlayersFragment,
    world: World,
    binding: FragLocatorPlayersBinding
) {
    val context = fragment.context ?: return
    // TODO use viewModelScope
    fragment.lifecycleScope.launch(Dispatchers.IO) {
        val players = mutableListOf<Player>()
        try {
            val localPlayerPos: DimensionVector3<Float>? = world.resolveLocalPlayerPos(context)
            if (localPlayerPos !== null) {
                players.add(Player.localPlayer().apply { position = localPlayerPos })
            }
            world.storage?.db?.iterator(ReadOptions().fillCache(false))?.use { iterator ->
                val prefix = KeyPrefix.ONLINE_PLAYER.bytes
                iterator.seek(prefix)
                while (iterator.hasNext()) {
                    val entry = iterator.next()
                    val key = entry.key
                    if (!key.startsWith(prefix)) break
                    val player = Player.networkPlayer(key.toString(Charsets.UTF_8))
                    players.add(player)
                    player.position = BedrockNBTInput(entry.value)
                        .readAnonymousTypedTag<CompoundTag>()
                        ?.extractPlayerPosCompat()
                        ?: continue
                }
            }
        } catch (_: Exception) {
            // TODO log
        }
        withContext(Dispatchers.Main) {
            binding.loading.visibility = View.GONE
            if (players.isEmpty()) {
                binding.empty.visibility = View.VISIBLE
                binding.empty.setText(R.string.failed_find_player)
            } else {
                binding.list.setAdapter(
                    PlayersAdapter(WeakReference(fragment), players)
                )
                binding.list.visibility = View.VISIBLE
            }
        }
    }
}