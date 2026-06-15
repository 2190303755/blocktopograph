package com.mithrilmania.blocktopograph.world

import org.iq80.leveldb.DB

operator fun DB.get(key: GlobalKey): ByteArray? = this[key.bytes]

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