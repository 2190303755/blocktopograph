package com.mithrilmania.blocktopograph.world

import com.mithrilmania.blocktopograph.storage.VirtualFile
import org.iq80.leveldb.DB
import org.iq80.leveldb.ReadOptions

@JvmField
val NO_CACHE_OPTION: ReadOptions = ReadOptions().fillCache(false)

val StandaloneKey.hexedKey: String get() = this.bytes.toHexString()

/**
 * Get without filling cache as [StandaloneKey] rarely shares prefixes with other keys
 */
operator fun DB.get(key: StandaloneKey): ByteArray? =
    this[key.bytes, NO_CACHE_OPTION]

fun DB.wrapAsFile(key: StandaloneKey): VirtualFile =
    VirtualFile(this, key.literal, key.bytes)

enum class StandaloneKey(@JvmField val literal: String) {
    AUTONOMOUS_ENTITIES("AutonomousEntities"),
    BIOME_DATA("BiomeData"),
    BIOME_REGISTRY("BiomeIdsTable"),
    DIMENSION_REGISTRY("DimensionNameIdTable"),
    DYNAMIC_PROPERTIES("DynamicProperties"),
    CHUNK_METAS("LevelChunkMetaDataDictionary"),
    PORTALS("portals"),
    FLAT_LAYERS("game_flatworldlayers"),
    SCOREBOARD("scoreboard"),
    LEGACY_VILLAGE("mVillage"),
    LOCAL_PLAYER("~local_player");

    @JvmField
    internal val bytes: ByteArray = this.literal.toByteArray(Charsets.UTF_8)
}