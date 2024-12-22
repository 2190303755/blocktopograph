package com.mithrilmania.blocktopograph.world.impl

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.database.getLongOrNull
import androidx.lifecycle.MutableLiveData
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.nbt.convert.LevelDataConverter
import com.mithrilmania.blocktopograph.nbt.tags.CompoundTag
import com.mithrilmania.blocktopograph.nbt.tags.LongTag
import com.mithrilmania.blocktopograph.nbt.tags.StringTag
import com.mithrilmania.blocktopograph.util.findChild
import com.mithrilmania.blocktopograph.util.getGameMode
import com.mithrilmania.blocktopograph.util.lastPlayedVersion
import com.mithrilmania.blocktopograph.util.queryString
import com.mithrilmania.blocktopograph.world.FILE_LEVEL_DAT
import com.mithrilmania.blocktopograph.world.IWorldLoader
import com.mithrilmania.blocktopograph.world.KEY_LAST_PLAYED_TIME
import com.mithrilmania.blocktopograph.world.KEY_LEVEL_NAME
import com.mithrilmania.blocktopograph.world.KEY_RANDOM_SEED
import com.mithrilmania.blocktopograph.worldlist.WorldItemAdapter

object SAFWorldLoader : IWorldLoader<Uri> {
    override suspend fun loadWorlds(
        adapter: WorldItemAdapter,
        context: Context,
        location: Uri,
        state: MutableLiveData<Boolean>,
        tag: String
    ) {
        state.postValue(true)
        val resolver = context.contentResolver
        val worlds = adapter.model.worlds
        resolver.query(
            DocumentsContract.buildChildDocumentsUriUsingTree(
                location, DocumentsContract.getDocumentId(location)
            ), arrayOf(
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            ), null, null, DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )?.use {
            if (!it.moveToLast()) return@use// idk why ` DESC` doesn't work, so reverse iteration
            forEachCandidate@ do {
                if (it.isNull(0) || DocumentsContract.Document.MIME_TYPE_DIR != it.getString(0)) continue
                val candidate =
                    DocumentsContract.buildDocumentUriUsingTree(location, it.getString(2))
                val config = candidate.findChild(
                    resolver,
                    FILE_LEVEL_DAT
                ) ?: continue
                val compound: CompoundTag? = LevelDataConverter.read(
                    resolver.openInputStream(config)
                )
                val world = SAFWorld(
                    candidate,
                    (compound?.getChildTagByKey(KEY_LEVEL_NAME) as? StringTag)?.value
                        ?: location.queryString(
                            context.contentResolver,
                            DocumentsContract.Document.COLUMN_DISPLAY_NAME
                        ) ?: context.getString(R.string.default_world_name),
                    compound.getGameMode(context),
                    (compound?.getChildTagByKey(KEY_LAST_PLAYED_TIME) as? LongTag)?.value?.let {
                        it * 1000L
                    } ?: it.getLongOrNull(1) ?: 0L,
                    (compound?.getChildTagByKey(KEY_RANDOM_SEED) as? LongTag)?.value?.toString()
                        ?: "",
                    compound.lastPlayedVersion,
                    config,
                    tag
                )
                var index = worlds.size
                while (--index >= 0) {
                    if (world.time < worlds[index].time) {
                        if (++index != worlds.size && world == worlds[index]) continue@forEachCandidate
                        worlds.add(index, world)
                        world.populate(adapter, context)
                        continue@forEachCandidate
                    }
                }
                if (worlds.isEmpty() || worlds[0] != world) {
                    worlds.add(0, world)
                    world.populate(adapter, context)
                }
            } while (it.moveToPrevious())
        }
        state.postValue(false)
    }
}