package ovh.plrapps.mapcompose.core

/**
 * Properties container for the computed visible tiles.
 * @param level 0-based level index
 * TODO doc
 */
internal data class VisibleTiles(
    @JvmField val level: Int,
    @JvmField val left: Int,
    @JvmField val top: Int,
    @JvmField val right: Int,
    @JvmField val bottom: Int
)

internal fun VisibleTiles.contains(tile: Tile): Boolean {
    return level == tile.zoom
            && tile.row in this.top..this.bottom
            && tile.col in this.left..this.right
}

internal fun VisibleTiles.intersects(tile: Tile): Boolean {
    return if (level == tile.zoom) {
        tile.row in this.top..this.bottom && tile.col in this.left..this.right
    } else {
        val curMinRow = this.top
        val curMaxRow = this.bottom
        val curMinCol = this.left
        val curMaxCol = this.right

        if (tile.zoom > level) { // User is zooming out
            val dLevel = tile.zoom - level
            val minRowAtLvl = curMinRow.minAtGreaterLevel(dLevel)
            val maxRowAtLvl = curMaxRow.maxAtGreaterLevel(dLevel)

            val minColAtLvl = curMinCol.minAtGreaterLevel(dLevel)
            val maxColAtLvl = curMaxCol.maxAtGreaterLevel(dLevel)
            tile.row in minRowAtLvl..maxRowAtLvl && tile.col in minColAtLvl..maxColAtLvl
        } else { // User is zooming in
            val dLevel = level - tile.zoom
            val minRowAtLvl = tile.row.minAtGreaterLevel(dLevel)
            val maxRowAtLvl = tile.row.maxAtGreaterLevel(dLevel)

            val minColAtLvl = tile.col.minAtGreaterLevel(dLevel)
            val maxColAtLvl = tile.col.maxAtGreaterLevel(dLevel)
            curMinCol <= maxColAtLvl && minColAtLvl <= curMaxCol && curMinRow <= maxRowAtLvl &&
                    minRowAtLvl <= curMaxRow
        }
    }
}

private fun Int.minAtGreaterLevel(n: Int): Int {
    return this shl n
}

private fun Int.maxAtGreaterLevel(n: Int): Int {
    return ((this + 1) shl n) - 1
}
