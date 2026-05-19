package com.mithrilmania.blocktopograph.util;

import java.nio.charset.StandardCharsets;

public enum SpecialDBEntryType {

    //Who came up with the formatting for these NBT keys is CRAZY
    // (PascalCase, camelCase, snake_case, lowercase, m-prefix(Android), tilde-prefix; it's all there!)
    BIOME_DATA("BiomeData"),
    OVERWORLD("Overworld"),
    M_VILLAGES("mVillages"),
    PORTALS("portals"),
    LOCAL_PLAYER("~local_player"),
    AUTONOMOUS_ENTITIES("AutonomousEntities"),
    DIMENSION_0("dimension0"),
    DIMENSION_1("dimension1"),
    DIMENSION_2("dimension2");

    public final String keyName;
    public final byte[] keyBytes;

    SpecialDBEntryType(String keyName) {
        this.keyName = keyName;
        this.keyBytes = keyName.getBytes(StandardCharsets.UTF_8);
    }
}
