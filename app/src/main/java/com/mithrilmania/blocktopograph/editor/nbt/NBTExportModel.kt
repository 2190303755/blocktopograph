package com.mithrilmania.blocktopograph.editor.nbt

import android.content.Context
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mithrilmania.blocktopograph.MIME_SNBT
import com.mithrilmania.blocktopograph.MIME_TYPE_DEFAULT
import com.mithrilmania.blocktopograph.storage.File
import com.mithrilmania.blocktopograph.storage.SAFFile
import com.mithrilmania.blocktopograph.util.FileCreator
import net.benwoodworth.knbt.NbtCompression
import net.benwoodworth.knbt.NbtVariant

class NBTExportModel : ViewModel(), ExporterFactory {
    var output: File? = null
    var location: Uri? = null
    var name: String = ""
    var version: Byte? = null
    var header: Boolean = true
    var compression: NbtCompression = NbtCompression.None
    var prettify: Boolean = true
    var littleEndian: Boolean = true
    val stringify: MutableLiveData<Boolean> = MutableLiveData()

    fun isHeaderAvailable() =
        this.version !== null &&
                this.littleEndian &&
                this.compression == NbtCompression.None &&
                this.stringify.value == false

    fun accept(model: NBTEditorModel, context: Context) {
        stringify.value = model.stringify.value != false
        if (model.variant.value == NbtVariant.Java) {
            littleEndian = false
            header = false
        } else {
            littleEndian = true
            header = true
        }
        compression = model.compression.value ?: NbtCompression.None
        version = model.version.value
        val file = model.source.value
        output = file
        if (file === null) return
        name = file.getName(context)
        if (file is SAFFile) {
            this.location = file.uri
        }
    }

    fun saveTo(file: File, context: Context, model: NBTEditorModel) {
        this.output = file
        model.saveFileAsync(file, context, this)
    }

    fun buildOptions() = FileCreator.Options(
        if (this.stringify.value != false) MIME_SNBT else MIME_TYPE_DEFAULT,
        this.location,
        this.name
    )

    override fun createExporter() = if (this.stringify.value == false) NBTOptions(
        if (this.littleEndian) NbtVariant.Bedrock else NbtVariant.Java,
        this.compression,
        if (this.isHeaderAvailable() && this.header) this.version else null
    ) else SNBTOptions(this.prettify)
}