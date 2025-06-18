package com.mithrilmania.blocktopograph.editor.nbt.holder

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import androidx.viewbinding.ViewBinding
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.databinding.TagByteLayoutBinding
import com.mithrilmania.blocktopograph.databinding.TagDoubleLayoutBinding
import com.mithrilmania.blocktopograph.databinding.TagFloatLayoutBinding
import com.mithrilmania.blocktopograph.databinding.TagIntLayoutBinding
import com.mithrilmania.blocktopograph.databinding.TagLongLayoutBinding
import com.mithrilmania.blocktopograph.databinding.TagShortLayoutBinding
import com.mithrilmania.blocktopograph.databinding.TagStringLayoutBinding
import com.mithrilmania.blocktopograph.editor.nbt.NBTAdapter
import com.mithrilmania.blocktopograph.editor.nbt.NBTEditorModel
import com.mithrilmania.blocktopograph.editor.nbt.node.ByteNode
import com.mithrilmania.blocktopograph.editor.nbt.node.DoubleNode
import com.mithrilmania.blocktopograph.editor.nbt.node.FloatNode
import com.mithrilmania.blocktopograph.editor.nbt.node.IntNode
import com.mithrilmania.blocktopograph.editor.nbt.node.LongNode
import com.mithrilmania.blocktopograph.editor.nbt.node.NBTNode
import com.mithrilmania.blocktopograph.editor.nbt.node.ShortNode
import com.mithrilmania.blocktopograph.editor.nbt.node.StringNode

sealed class ValueHolder<V : ViewBinding, T : NBTNode>(
    adapter: NBTAdapter,
    parent: ViewGroup,
    binding: (LayoutInflater, ViewGroup, Boolean) -> V
) : NodeHolder<V, T>(
    binding(LayoutInflater.from(parent.context), parent, false)
), TextWatcher {
    protected val model: NBTEditorModel = adapter.model
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    fun markAsInvalid(box: EditText, value: Any) {
        box.error = this.context.getString(R.string.x_is_invalid, value)
    }
}

class ByteHolder(
    adapter: NBTAdapter,
    parent: ViewGroup
) : ValueHolder<TagByteLayoutBinding, ByteNode>(adapter, parent, TagByteLayoutBinding::inflate) {
    init {
        this.binding.byteField.addTextChangedListener(this)
    }

    override fun bind(node: NBTNode) {
        this.node = node as? ByteNode ?: return
        this.binding.apply {
            tagName.text = node.name
            byteField.setText(node.value.toString())
        }
    }

    override fun rename(name: String) {
        this.binding.tagName.text = name
        this.node?.name = name
    }

    override fun afterTextChanged(input: Editable) {
        val node = this.node ?: return
        val raw = input.toString()
        try {
            val value = raw.toByte()
            if (value == node.value) return
            this.model.modified.value = true
            node.value = value
        } catch (_: NumberFormatException) {
            this.markAsInvalid(this.binding.byteField, raw)
        }
    }
}

class ShortHolder(
    adapter: NBTAdapter,
    parent: ViewGroup
) : ValueHolder<TagShortLayoutBinding, ShortNode>(adapter, parent, TagShortLayoutBinding::inflate) {
    init {
        this.binding.shortField.addTextChangedListener(this)
    }

    override fun bind(node: NBTNode) {
        this.node = node as? ShortNode ?: return
        this.binding.apply {
            tagName.text = node.name
            shortField.setText(node.value.toString())
        }
    }

    override fun rename(name: String) {
        this.binding.tagName.text = name
        this.node?.name = name
    }

    override fun afterTextChanged(input: Editable) {
        val node = this.node ?: return
        val raw = input.toString()
        try {
            val value = raw.toShort()
            if (value == node.value) return
            this.model.modified.value = true
            node.value = value
        } catch (_: NumberFormatException) {
            this.markAsInvalid(this.binding.shortField, raw)
        }
    }
}

class IntHolder(
    adapter: NBTAdapter,
    parent: ViewGroup
) : ValueHolder<TagIntLayoutBinding, IntNode>(adapter, parent, TagIntLayoutBinding::inflate) {
    init {
        this.binding.intField.addTextChangedListener(this)
    }

    override fun bind(node: NBTNode) {
        this.node = node as? IntNode ?: return
        this.binding.apply {
            tagName.text = node.name
            intField.setText(node.value.toString())
        }
    }

    override fun rename(name: String) {
        this.binding.tagName.text = name
        this.node?.name = name
    }

    override fun afterTextChanged(input: Editable) {
        val node = this.node ?: return
        val raw = input.toString()
        try {
            val value = raw.toInt()
            if (value == node.value) return
            this.model.modified.value = true
            node.value = value
        } catch (_: NumberFormatException) {
            this.markAsInvalid(this.binding.intField, raw)
        }
    }
}

class LongHolder(
    adapter: NBTAdapter,
    parent: ViewGroup
) : ValueHolder<TagLongLayoutBinding, LongNode>(adapter, parent, TagLongLayoutBinding::inflate) {
    init {
        this.binding.longField.addTextChangedListener(this)
    }

    override fun bind(node: NBTNode) {
        this.node = node as? LongNode ?: return
        this.binding.apply {
            tagName.text = node.name
            longField.setText(node.value.toString())
        }
    }

    override fun rename(name: String) {
        this.binding.tagName.text = name
        this.node?.name = name
    }

    override fun afterTextChanged(input: Editable) {
        val node = this.node ?: return
        val raw = input.toString()
        try {
            val value = raw.toLong()
            if (value == node.value) return
            this.model.modified.value = true
            node.value = value
        } catch (_: NumberFormatException) {
            this.markAsInvalid(this.binding.longField, raw)
        }
    }
}

class FloatHolder(
    adapter: NBTAdapter,
    parent: ViewGroup
) : ValueHolder<TagFloatLayoutBinding, FloatNode>(adapter, parent, TagFloatLayoutBinding::inflate) {
    init {
        this.binding.floatField.addTextChangedListener(this)
    }

    override fun bind(node: NBTNode) {
        this.node = node as? FloatNode ?: return
        this.binding.apply {
            tagName.text = node.name
            floatField.setText(node.value.toString())
        }
    }

    override fun rename(name: String) {
        this.binding.tagName.text = name
        this.node?.name = name
    }

    override fun afterTextChanged(input: Editable) {
        val node = this.node ?: return
        val raw = input.toString()
        try {
            val value = raw.toFloat()
            if (value == node.value) return
            this.model.modified.value = true
            node.value = value
        } catch (_: NumberFormatException) {
            this.markAsInvalid(this.binding.floatField, raw)
        }
    }
}

class DoubleHolder(
    adapter: NBTAdapter,
    parent: ViewGroup
) : ValueHolder<TagDoubleLayoutBinding, DoubleNode>(
    adapter,
    parent,
    TagDoubleLayoutBinding::inflate
) {
    init {
        this.binding.doubleField.addTextChangedListener(this)
    }

    override fun bind(node: NBTNode) {
        this.node = node as? DoubleNode ?: return
        this.binding.apply {
            tagName.text = node.name
            doubleField.setText(node.value.toString())
        }
    }

    override fun rename(name: String) {
        this.binding.tagName.text = name
        this.node?.name = name
    }

    override fun afterTextChanged(input: Editable) {
        val node = this.node ?: return
        val raw = input.toString()
        try {
            val value = raw.toDouble()
            if (value == node.value) return
            this.model.modified.value = true
            node.value = value
        } catch (_: NumberFormatException) {
            this.markAsInvalid(this.binding.doubleField, raw)
        }
    }
}

class StringHolder(
    adapter: NBTAdapter,
    parent: ViewGroup
) : ValueHolder<TagStringLayoutBinding, StringNode>(
    adapter,
    parent,
    TagStringLayoutBinding::inflate
) {
    init {
        this.binding.stringField.addTextChangedListener(this)
    }

    override fun rename(name: String) {
        this.binding.tagName.text = name
        this.node?.name = name
    }

    override fun bind(node: NBTNode) {
        this.node = node as? StringNode ?: return
        this.binding.apply {
            tagName.text = node.name
            stringField.setText(node.value.toString())
        }
    }

    override fun afterTextChanged(input: Editable) {
        val node = this.node ?: return
        val value = input.toString()
        if (value == node.value) return
        this.model.modified.value = true
        node.value = value
    }
}