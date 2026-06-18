package com.mithrilmania.blocktopograph.world.chunk

class NoSuchChunkException : NoSuchElementException {
    constructor() : super()
    constructor(message: String) : super(message)
}