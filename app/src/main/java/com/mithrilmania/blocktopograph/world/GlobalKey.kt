package com.mithrilmania.blocktopograph.world

import org.iq80.leveldb.DB

operator fun DB.get(key: GlobalKey): ByteArray? = this[key.bytes]

enum class GlobalKey(@JvmField val key: String) {
    PORTALS("portals"),
    FLAT_LAYERS("game_flatworldlayers"),
    SCOREBOARD("scoreboard"),
    AUTONOMOUS_ENTITIES("AutonomousEntities"),
    BIOME_DATA("BiomeData"),
    BIOME_REGISTRY("BiomeIdsTable"),
    DIMENSION_REGISTRY("DimensionNameIdTable"),
    DYNAMIC_PROPERTIES("DynamicProperties"),
    CHUNK_METAS("LevelChunkMetaDataDictionary");

    @JvmField
    internal val bytes: ByteArray = key.toByteArray(Charsets.UTF_8)
}