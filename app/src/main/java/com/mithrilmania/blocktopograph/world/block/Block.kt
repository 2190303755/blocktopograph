package com.mithrilmania.blocktopograph.world.block

class Block(
    @JvmField
    val typeId: String,
    @JvmField
    val interpreter: BlockInterpreter
) {
}