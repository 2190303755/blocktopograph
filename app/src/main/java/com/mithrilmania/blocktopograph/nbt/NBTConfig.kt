package com.mithrilmania.blocktopograph.nbt

import androidx.lifecycle.MutableLiveData
import net.benwoodworth.knbt.NbtCompression
import net.benwoodworth.knbt.NbtVariant

class NBTConfig {
    val stringify: MutableLiveData<Boolean?> = MutableLiveData<Boolean?>(null)
    val variant: MutableLiveData<NbtVariant?> = MutableLiveData<NbtVariant?>(null)
    val compression: MutableLiveData<NbtCompression?> = MutableLiveData<NbtCompression?>(null)
    var header: Boolean? = null
    var format: Boolean? = null
    fun isCompleted() =
        (stringify.value != null) && (variant.value != null) && (compression.value != null)

    fun mayHasHeader() =
        (stringify.value == false) && (variant.value == NbtVariant.Bedrock) && (compression.value == NbtCompression.None)
}