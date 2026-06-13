package com.mithrilmania.blocktopograph.world.chunk

/**
 * [Reference from Tommaso Checchi (/u/mojang_tommo), MCPE developer](https://www.reddit.com/r/MCPE/comments/5cw2tm/level_format_changes_in_mcpe_0171_100/d9zv9s8/)
 *
 * [基岩版LevelDB格式](https://zh.minecraft.wiki/w/User:Miemiemethod/%E5%9F%BA%E5%B2%A9%E7%89%88LevelDB%E6%A0%BC%E5%BC%8F#%E7%8B%AC%E7%AB%8B%E9%94%AE%E5%90%8D)
 */
enum class ChunkTag(@JvmField val dataID: Byte) {
    DATA_3D(0x2B.toByte()),
    VERSION(0x2C.toByte()),
    DATA_2D(0x2D.toByte()),
    DATA_2D_LEGACY(0x2E.toByte()),
    SUB_CHUNK_PREFIX(0x2F.toByte()),
    LEGACY_TERRAIN(0x30.toByte()),
    BLOCK_ENTITY(0x31.toByte()),
    ENTITY(0x32.toByte()),
    PENDING_TICKS(0x33.toByte()),
    LEGACY_BLOCK_EXTRA_DATA(0x34.toByte()),
    BIOME_STATE(0x35.toByte()),
    FINALIZED_STATE(0x36.toByte()),
    CONVERSION_DATA(0x37.toByte()),
    BORDER_BLOCKS(0x38.toByte()),
    HARDCODED_SPAWNERS(0x39.toByte()),
    RANDOM_TICKS(0x3A.toByte()),
    CHECKSUMS(0x3B.toByte()),
    GENERATION_SEED(0x3C.toByte()),
    GENERATED_PRE_CAVE_AND_CLIFFS_BLENDING(0x3D.toByte()),
    BLENDING_BIOME_HEIGHT(0x3E.toByte()),
    METADATA_HASH(0x3F.toByte()),
    BLENDING_DATA(0x40.toByte()),
    ACTOR_DIGEST_VERSION(0x41.toByte()),
    VERSION_ENCHANT(0x6E.toByte()),
    VERSION_MARK_INSERT(0x6F.toByte()),
    LEGACY_VERSION(0x76.toByte()),
    AABB_VOLUMES(0x77.toByte())
}