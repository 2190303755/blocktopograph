package com.mithrilmania.blocktopograph.world

enum class KeyPrefix(@JvmField val key: String) {
    MAP("map_"),
    TICKING_AREA("tickingarea_"),
    VILLAGE("VILLAGE_"),
    ONLINE_PLAYER("player_server_"),
    CONSOLE_PLAYER("legacy_console_player_"), // TODO: what is this
    PROFILE("player_");

    @JvmField
    internal val bytes: ByteArray = key.toByteArray(Charsets.UTF_8)

    fun expand(size: Int): ByteArray {
        return this.bytes.copyOf(this.bytes.size + size)
    }
}