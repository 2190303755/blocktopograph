package com.mithrilmania.blocktopograph.world

import org.iq80.leveldb.DB
import org.iq80.leveldb.ReadOptions

val NO_CACHE_OPTION: ReadOptions = ReadOptions().fillCache(false)

/**
 * Get without filling cache as [GlobalKey] rarely shares prefixes with other keys
 */
operator fun DB.get(key: GlobalKey): ByteArray? = this[key.bytes, NO_CACHE_OPTION]

enum class GlobalKey(@JvmField val key: String) {
    AUTONOMOUS_ENTITIES("AutonomousEntities"),
    BIOME_DATA("BiomeData"),
    BIOME_REGISTRY("BiomeIdsTable"),
    DIMENSION_REGISTRY("DimensionNameIdTable"),
    DYNAMIC_PROPERTIES("DynamicProperties"),
    CHUNK_METAS("LevelChunkMetaDataDictionary"),
    PORTALS("portals"),
    FLAT_LAYERS("game_flatworldlayers"),
    SCOREBOARD("scoreboard"),
    LOCAL_PLAYER("~local_player");

    @JvmField
    internal val bytes: ByteArray = key.toByteArray(Charsets.UTF_8)
}