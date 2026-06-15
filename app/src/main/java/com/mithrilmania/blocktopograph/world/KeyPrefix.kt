package com.mithrilmania.blocktopograph.world

enum class KeyPrefix(@JvmField val key: String) {
    VILLAGE("VILLAGE_"),
    CONSOLE_PLAYER("legacy_console_player_"), // TODO: what is this
    MAP("map_"),
    PROFILE("player_"),
    ONLINE_PLAYER("player_server_"),
    TICKING_AREA("tickingarea_");
    @JvmField
    internal val bytes: ByteArray = key.toByteArray(Charsets.UTF_8)

    fun expand(size: Int): ByteArray {
        return this.bytes.copyOf(this.bytes.size + size)
    }
}