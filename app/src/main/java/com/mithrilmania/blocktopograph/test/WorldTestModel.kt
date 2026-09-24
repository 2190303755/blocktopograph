package com.mithrilmania.blocktopograph.test

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mithrilmania.blocktopograph.editor.nbt.ConfiguredNBTSource
import com.mithrilmania.blocktopograph.world.World
import com.mithrilmania.blocktopograph.world.WorldModel


class Pattern(val bytes: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return bytes.contentEquals((other as Pattern).bytes)
    }

    override fun hashCode(): Int = this.bytes.contentHashCode()
}

class WorldTestModel(world: World) : WorldModel(world) {
    var exporting: LDBEntry? by mutableStateOf(null)
    var editing: MutableState<ConfiguredNBTSource?> = mutableStateOf(null)
    var isHexed: Boolean by mutableStateOf(false)
    val plainInput: TextFieldState = TextFieldState()
    val hexedInput: TextFieldState = TextFieldState()
    val entries: MutableList<LDBEntry> = mutableStateListOf()
    val snackbar: SnackbarHostState = SnackbarHostState()
}