package com.mithrilmania.blocktopograph.editor.nbt

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mithrilmania.blocktopograph.nbt.EMPTY_COMPOUND
import com.mithrilmania.blocktopograph.nbt.ParsedNBT
import com.mithrilmania.blocktopograph.storage.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.benwoodworth.knbt.NbtCompound
import net.benwoodworth.knbt.NbtCompression
import net.benwoodworth.knbt.NbtVariant

class NBTEditorModel : ViewModel() {
    val modified: MutableLiveData<Boolean> = MutableLiveData(false)
    val loading: MutableLiveData<Boolean> = MutableLiveData(false)
    val stringify: MutableLiveData<Boolean> = MutableLiveData(false)
    val prettify: MutableLiveData<Boolean> = MutableLiveData(true)
    val version: MutableLiveData<Byte?> = MutableLiveData()
    val variant: MutableLiveData<NbtVariant> = MutableLiveData(NbtVariant.Bedrock)
    val compression: MutableLiveData<NbtCompression> = MutableLiveData(NbtCompression.None)
    val source: MutableLiveData<File?> = MutableLiveData()
    val data: MutableLiveData<NbtCompound> = MutableLiveData(EMPTY_COMPOUND)
    val undo: ArrayDeque<Operation> = ArrayDeque()
    val redo: ArrayDeque<Operation> = ArrayDeque()
    var name: String = ""

    suspend fun accept(parsed: ParsedNBT) {
        name = parsed.name
        withContext(Dispatchers.Main) {
            undo.clear()
            redo.clear()
            loading.value = false
            stringify.value = parsed.snbt
            variant.value = parsed.variant
            compression.value = parsed.compression
            version.value = parsed.version
            data.value = parsed.data
        }
    }

    fun reset() {
        name = ""
        undo.clear()
        redo.clear()
        source.value = null
        loading.value = false
        stringify.value = true
        variant.value = NbtVariant.Bedrock
        compression.value = NbtCompression.None
        version.value = null
        data.value = EMPTY_COMPOUND
    }
}