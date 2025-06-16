package com.mithrilmania.blocktopograph.editor.nbt

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mithrilmania.blocktopograph.storage.File

class NBTEditorModel : ViewModel() {
    val modified = MutableLiveData(false)
    val reloading = MutableLiveData(false)
    var source: File? = null
}