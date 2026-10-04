package com.mithrilmania.blocktopograph.storage

import coil3.map.Mapper
import coil3.request.Options
import com.mithrilmania.blocktopograph.world.FILE_WORLD_ICON
import com.mithrilmania.blocktopograph.world.WorldDetail

class WorldDetailMapper : Mapper<WorldDetail, Any> {
    override fun map(data: WorldDetail, options: Options): Any? =
        data.location.resolve(FILE_WORLD_ICON, options.context)?.location
}