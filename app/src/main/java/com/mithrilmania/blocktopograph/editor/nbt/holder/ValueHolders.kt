package com.mithrilmania.blocktopograph.editor.nbt.holder

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
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
import com.mithrilmania.blocktopograph.editor.nbt.node.applyIndent

sealed class ValueHolder<T : ViewBinding>(
    adapter: NBTAdapter,
    parent: ViewGroup,
    binding: (LayoutInflater, ViewGroup, Boolean) -> T
) : NodeHolder<T>(
    parent.context,
    binding(LayoutInflater.from(parent.context), parent, false)
), TextWatcher {
    protected val model: NBTEditorModel = adapter.model
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
}

class ByteHolder(
    adapter: NBTAdapter,
    parent: ViewGroup
) : ValueHolder<TagByteLayoutBinding>(adapter, parent, TagByteLayoutBinding::inflate) {
    private var node: ByteNode? = null
    private var value: Byte = 0

    init {
        this.binding.byteField.addTextChangedListener(this)
    }

    override fun bind(adapter: NBTAdapter, node: NBTNode) {
        if (node is ByteNode) {
            this.node = node
            this.value = node.data
            this.binding.apply {
                applyIndent(node, this@ByteHolder.context)
                tagName.text = node.name
                byteField.setText(node.data.toString())
            }
        }
    }

    override fun afterTextChanged(s: Editable) {
        val raw = s.toString()
        try {
            val value = raw.toByte()
            if (value != this.value) {
                this.model.modified.value = true
                this.value = value
            }
        } catch (_: NumberFormatException) {
            this.binding.byteField.error = this.context.getString(R.string.x_is_invalid).format(raw)
        }
    }
}

class ShortHolder(
    adapter: NBTAdapter,
    parent: ViewGroup
) : ValueHolder<TagShortLayoutBinding>(adapter, parent, TagShortLayoutBinding::inflate) {
    private var node: ShortNode? = null
    private var value: Short = 0

    init {
        this.binding.shortField.addTextChangedListener(this)
    }

    override fun bind(adapter: NBTAdapter, node: NBTNode) {
        if (node is ShortNode) {
            this.node = node
            this.value = node.data
            this.binding.apply {
                applyIndent(node, this@ShortHolder.context)
                tagName.text = node.name
                shortField.setText(node.data.toString())
            }
        }
    }

    override fun afterTextChanged(s: Editable) {
        val raw = s.toString()
        try {
            val value = raw.toShort()
            if (value != this.value) {
                this.model.modified.value = true
                this.value = value
            }
        } catch (_: NumberFormatException) {
            this.binding.shortField.error =
                this.context.getString(R.string.x_is_invalid).format(raw)
        }
    }
}

class IntHolder(
    adapter: NBTAdapter,
    parent: ViewGroup
) : ValueHolder<TagIntLayoutBinding>(adapter, parent, TagIntLayoutBinding::inflate) {
    private var node: IntNode? = null
    private var value: Int = 0

    init {
        this.binding.intField.addTextChangedListener(this)
    }

    override fun bind(adapter: NBTAdapter, node: NBTNode) {
        if (node is IntNode) {
            this.node = node
            this.value = node.data
            this.binding.apply {
                applyIndent(node, this@IntHolder.context)
                tagName.text = node.name
                intField.setText(node.data.toString())
            }
        }
    }

    override fun afterTextChanged(s: Editable) {
        val raw = s.toString()
        try {
            val value = raw.toInt()
            if (value != this.value) {
                this.model.modified.value = true
                this.value = value
            }
        } catch (_: NumberFormatException) {
            this.binding.intField.error = this.context.getString(R.string.x_is_invalid).format(raw)
        }
    }
}

class LongHolder(
    adapter: NBTAdapter,
    parent: ViewGroup
) : ValueHolder<TagLongLayoutBinding>(adapter, parent, TagLongLayoutBinding::inflate) {
    private var node: LongNode? = null
    private var value: Long = 0

    init {
        this.binding.longField.addTextChangedListener(this)
    }

    override fun bind(adapter: NBTAdapter, node: NBTNode) {
        if (node is LongNode) {
            this.node = node
            this.value = node.data
            this.binding.apply {
                applyIndent(node, this@LongHolder.context)
                tagName.text = node.name
                longField.setText(node.data.toString())
            }
        }
    }

    override fun afterTextChanged(s: Editable) {
        val raw = s.toString()
        try {
            val value = raw.toLong()
            if (value != this.value) {
                this.model.modified.value = true
                this.value = value
            }
        } catch (_: NumberFormatException) {
            this.binding.longField.error = this.context.getString(R.string.x_is_invalid).format(raw)
        }
    }
}

class FloatHolder(
    adapter: NBTAdapter,
    parent: ViewGroup
) : ValueHolder<TagFloatLayoutBinding>(adapter, parent, TagFloatLayoutBinding::inflate) {
    private var node: FloatNode? = null
    private var value: Float = 0.0F

    init {
        this.binding.floatField.addTextChangedListener(this)
    }

    override fun bind(adapter: NBTAdapter, node: NBTNode) {
        if (node is FloatNode) {
            this.node = node
            this.value = node.data
            this.binding.apply {
                applyIndent(node, this@FloatHolder.context)
                tagName.text = node.name
                floatField.setText(node.data.toString())
            }
        }
    }

    override fun afterTextChanged(s: Editable) {
        val raw = s.toString()
        try {
            val value = raw.toFloat()
            if (value != this.value) {
                this.model.modified.value = true
                this.value = value
            }
        } catch (_: NumberFormatException) {
            this.binding.floatField.error =
                this.context.getString(R.string.x_is_invalid).format(raw)
        }
    }
}

class DoubleHolder(
    adapter: NBTAdapter,
    parent: ViewGroup
) : ValueHolder<TagDoubleLayoutBinding>(adapter, parent, TagDoubleLayoutBinding::inflate) {
    private var node: DoubleNode? = null
    private var value: Double = 0.0

    init {
        this.binding.doubleField.addTextChangedListener(this)
    }

    override fun bind(adapter: NBTAdapter, node: NBTNode) {
        if (node is DoubleNode) {
            this.node = node
            this.value = node.data
            this.binding.apply {
                applyIndent(node, this@DoubleHolder.context)
                tagName.text = node.name
                doubleField.setText(node.data.toString())
            }
        }
    }

    override fun afterTextChanged(s: Editable) {
        val raw = s.toString()
        try {
            val value = raw.toDouble()
            if (value != this.value) {
                this.model.modified.value = true
                this.value = value
            }
        } catch (_: NumberFormatException) {
            this.binding.doubleField.error =
                this.context.getString(R.string.x_is_invalid).format(raw)
        }
    }
}

class StringHolder(
    adapter: NBTAdapter,
    parent: ViewGroup
) : ValueHolder<TagStringLayoutBinding>(adapter, parent, TagStringLayoutBinding::inflate) {
    private var node: StringNode? = null
    private var value: String = ""

    init {
        this.binding.stringField.addTextChangedListener(this)
    }

    override fun bind(adapter: NBTAdapter, node: NBTNode) {
        if (node is StringNode) {
            this.node = node
            this.value = node.data
            this.binding.apply {
                applyIndent(node, this@StringHolder.context)
                tagName.text = node.name
                stringField.setText(node.data.toString())
            }
        }
    }

    override fun afterTextChanged(s: Editable) {
        val value = s.toString()
        if (value != this.value) {
            this.model.modified.value = true
            this.value = value
        }
    }
}