package com.mithrilmania.blocktopograph.texture

import java.io.File
import java.util.concurrent.atomic.AtomicLong
import java.util.zip.ZipFile

const val TERRAIN_ATLAS_PATH = "textures/terrain_texture.json"
const val ITEM_ATLAS_PATH = "textures/item_texture.json"

/** Both atlases of one pack; a missing one stays empty. */
data class PackAtlases(
    val blocks: TextureAtlasJson = TextureAtlasJson(),
    val items: TextureAtlasJson = TextureAtlasJson(),
)

/** A missing entry or a broken parse both give an empty index: one bad pack must not break the stack. */
fun readAtlas(zip: ZipFile, entryName: String): TextureAtlasJson {
    val entry = zip.getEntry(entryName) ?: return TextureAtlasJson()
    return runCatching {
        zip.getInputStream(entry).use { input ->
            lenientTextureJson.decodeFromString<TextureAtlasJson>(input.readBytes().decodeToString())
        }
    }.getOrElse { TextureAtlasJson() }
}

/**
 * Texture name -> path (no extension) for one kind. Names without a namespace land in [shared],
 * packs that declare resource_pack_name land in [dedicated].
 */
class TexturePathTable {
    val shared: MutableMap<String, String> = HashMap()
    val dedicated: MutableMap<String, MutableMap<String, String>> = HashMap()

    /** No dedicated table for [namespace] means [shared] answers; "namespace:name" first, then the bare name. */
    fun find(namespace: String, name: String): String? =
        (dedicated[namespace] ?: shared).let { table ->
            table["$namespace:$name"] ?: table[name]
        }
}

/**
 * Merged tables for blocks and items. [version] bumps on every rebuild so that anything cached under
 * the old stack misses; it travels only inside the request, nothing reads it globally.
 */
class TextureLookup(
    val version: Long = nextVersion(),
) {
    val blocks = TexturePathTable()
    val items = TexturePathTable()

    companion object {
        private val VERSION = AtomicLong(0)

        /** Process-wide monotonic; the image cache dies with the process, so values never repeat. */
        fun nextVersion(): Long = VERSION.incrementAndGet()

        /** Stand-in until the first build; the UI collects it. */
        val EMPTY = TextureLookup(nextVersion())
    }
}

/** Packs are merged in order, so the first one to claim a name keeps it. */
internal fun mergeAtlasInto(table: TexturePathTable, atlas: TextureAtlasJson) {
    val target = atlas.resourcePackName.let { namespace ->
        if (namespace.isBlank() || namespace == "vanilla") table.shared
        else table.dedicated.getOrPut(namespace) { HashMap() }
    }
    for ((key, entry) in atlas.textureData) {
        val base = entry.basePath() ?: continue
        target.putIfAbsent(key, base)
    }
}

/** Input order is priority, highest first. */
fun buildTextureLookupFromAtlases(packAtlases: Iterable<PackAtlases>): TextureLookup {
    val lookup = TextureLookup()
    for (pack in packAtlases) {
        mergeAtlasInto(lookup.blocks, pack.blocks)
        mergeAtlasInto(lookup.items, pack.items)
    }
    return lookup
}

/** Does not open or close anything; the caller owns the zips. */
fun buildTextureLookupFromZips(zips: Iterable<ZipFile>): TextureLookup =
    buildTextureLookupFromAtlases(zips.map { zip ->
        PackAtlases(
            blocks = readAtlas(zip, TERRAIN_ATLAS_PATH),
            items = readAtlas(zip, ITEM_ATLAS_PATH),
        )
    })

/**
 * Opens each pack only long enough to read its atlases. Deliberately separate from the resolver's
 * long-lived handles, so a rebuild never fights with in-flight reads.
 */
fun buildTextureLookupFromPacks(packs: Iterable<ResourcePack>, directory: File): TextureLookup =
    buildTextureLookupFromAtlases(packs.map { pack ->
        val file = File(pack.zipPath).let { if (it.isAbsolute) it else File(directory, it.path) }
        runCatching {
            ZipFile(file).use { zip ->
                PackAtlases(
                    blocks = readAtlas(zip, TERRAIN_ATLAS_PATH),
                    items = readAtlas(zip, ITEM_ATLAS_PATH),
                )
            }
        }.getOrElse { PackAtlases() }
    })
