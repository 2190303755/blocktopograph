package com.mithrilmania.blocktopograph.editor.nbt

import android.content.Context
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mithrilmania.blocktopograph.MIME_SNBT
import com.mithrilmania.blocktopograph.MIME_TYPE_DEFAULT
import com.mithrilmania.blocktopograph.nbt.io.BedrockOutputBuffer
import com.mithrilmania.blocktopograph.nbt.io.NBTOutput
import com.mithrilmania.blocktopograph.nbt.io.NBTOutputBuffer
import com.mithrilmania.blocktopograph.nbt.io.NBTOutputFactory
import com.mithrilmania.blocktopograph.nbt.util.NBTStringifier
import com.mithrilmania.blocktopograph.storage.File
import com.mithrilmania.blocktopograph.storage.SAFFile
import com.mithrilmania.blocktopograph.util.FileCreator
import java.io.OutputStream
import java.nio.ByteOrder
import java.util.zip.GZIPOutputStream

class NBTExportModel : ViewModel(), NBTOutputFactory {
    var output: File? = null
    var location: Uri? = null
    var name: String = ""
    var version: UInt? = null
    var header: Boolean = true
    var compressed: Boolean = false
    var prettify: Boolean = true
    var littleEndian: Boolean = true
    val stringify: MutableLiveData<Boolean> = MutableLiveData()

    fun isHeaderAvailable() =
        this.version !== null &&
                this.littleEndian &&
                !this.compressed &&
                this.stringify.value == false

    fun accept(model: NBTEditorModel, context: Context) {
        stringify.value = model.stringify
        version = model.version.value
        compressed = model.compressed
        if (model.littleEndian) {
            littleEndian = true
            header = version !== null
        } else {
            littleEndian = false
            header = false
        }
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

    override fun createOutput(stream: OutputStream): NBTOutput {
        if (this.stringify.value != false) {
            val builder = NBTStringifier(indent = if (this.prettify) "    " else "")
            return NBTOutput { name, tag ->
                tag.accept(builder)
                stream.use {
                    it.write(builder.toString().toByteArray(Charsets.UTF_8))
                }
            }
        }
        if (this.littleEndian) {
            if (this.compressed) return NBTOutputBuffer(
                GZIPOutputStream(stream),
                ByteOrder.LITTLE_ENDIAN
            )
            val version = this.version
            if (version === null) return NBTOutputBuffer(stream, ByteOrder.LITTLE_ENDIAN)
            return BedrockOutputBuffer(stream, version)
        }
        return NBTOutputBuffer(
            if (this.compressed) GZIPOutputStream(stream) else stream,
            ByteOrder.BIG_ENDIAN
        )
    }
}