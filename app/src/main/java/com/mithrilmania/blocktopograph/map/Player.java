package com.mithrilmania.blocktopograph.map;

import android.content.Context;

import androidx.annotation.NonNull;

import com.mithrilmania.blocktopograph.R;
import com.mithrilmania.blocktopograph.util.math.DimensionVector3;
import com.mithrilmania.blocktopograph.world.StandaloneKey;

import java.util.Objects;

@Deprecated
public class Player {
    private final String dbName;
    private DimensionVector3<Float> position;

    private Player(String dbName) {
        this.dbName = dbName;
    }

    @NonNull

    public static Player localPlayer() {
        return new Player(null);
    }

    @NonNull
    public static Player networkPlayer(String dbName) {
        return new Player(dbName);
    }

    public boolean isLocal() {
        return this.dbName == null;
    }

    public String getDbName() {
        var name = this.dbName;
        return name == null ? StandaloneKey.LOCAL_PLAYER.literal : name;
    }

    public DimensionVector3<Float> getPosition() {
        return position;
    }

    public String getPositionDescription(Context context) {
        if (position == null)
            return context.getString(R.string.map_locator_player_pos_unknown);
        return context.getString(R.string.player_position_desc,
                Math.round(position.x), Math.round(position.y),
                Math.round(position.z), position.dimension.getDisplayName(context.getResources()));
    }

    public void setPosition(DimensionVector3<Float> position) {
        this.position = position;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        return Objects.equals(dbName, ((Player) o).dbName);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(dbName);
    }
}
