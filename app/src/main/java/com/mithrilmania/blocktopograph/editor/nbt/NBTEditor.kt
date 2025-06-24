package com.mithrilmania.blocktopograph.editor.nbt

import android.content.Context
import android.content.DialogInterface.BUTTON_NEUTRAL
import android.content.DialogInterface.BUTTON_POSITIVE
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.databinding.DialogInsertNodeBinding
import com.mithrilmania.blocktopograph.databinding.DialogRenameNodeBinding
import com.mithrilmania.blocktopograph.databinding.DialogSelectTypeBinding
import com.mithrilmania.blocktopograph.editor.nbt.node.ByteArrayNode
import com.mithrilmania.blocktopograph.editor.nbt.node.ByteNode
import com.mithrilmania.blocktopograph.editor.nbt.node.CompoundNode
import com.mithrilmania.blocktopograph.editor.nbt.node.DoubleNode
import com.mithrilmania.blocktopograph.editor.nbt.node.FloatNode
import com.mithrilmania.blocktopograph.editor.nbt.node.IntArrayNode
import com.mithrilmania.blocktopograph.editor.nbt.node.IntNode
import com.mithrilmania.blocktopograph.editor.nbt.node.ListNode
import com.mithrilmania.blocktopograph.editor.nbt.node.LongArrayNode
import com.mithrilmania.blocktopograph.editor.nbt.node.LongNode
import com.mithrilmania.blocktopograph.editor.nbt.node.MapNode
import com.mithrilmania.blocktopograph.editor.nbt.node.NBTNode
import com.mithrilmania.blocktopograph.editor.nbt.node.RootNode
import com.mithrilmania.blocktopograph.editor.nbt.node.ShortNode
import com.mithrilmania.blocktopograph.editor.nbt.node.StringNode
import com.mithrilmania.blocktopograph.editor.nbt.node.TagListNode
import com.mithrilmania.blocktopograph.editor.nbt.node.registerTo
import com.mithrilmania.blocktopograph.nbt.util.EMPTY_COMPOUND
import com.mithrilmania.blocktopograph.nbt.TAG_BYTE
import com.mithrilmania.blocktopograph.nbt.TAG_BYTE_ARRAY
import com.mithrilmania.blocktopograph.nbt.TAG_COMPOUND
import com.mithrilmania.blocktopograph.nbt.TAG_DOUBLE
import com.mithrilmania.blocktopograph.nbt.TAG_FLOAT
import com.mithrilmania.blocktopograph.nbt.TAG_INT
import com.mithrilmania.blocktopograph.nbt.TAG_INT_ARRAY
import com.mithrilmania.blocktopograph.nbt.TAG_LIST
import com.mithrilmania.blocktopograph.nbt.TAG_LONG
import com.mithrilmania.blocktopograph.nbt.TAG_LONG_ARRAY
import com.mithrilmania.blocktopograph.nbt.TAG_SHORT
import com.mithrilmania.blocktopograph.nbt.TAG_STRING
import com.mithrilmania.blocktopograph.util.clipboard
import com.mithrilmania.blocktopograph.util.toast
import com.mithrilmania.blocktopograph.util.upcoming
import net.benwoodworth.knbt.NbtTag

fun ListNode.notifyMovedChildren() {
    this.getChildren().forEachIndexed { index, node ->
        index.toString().let {
            if (it != node.name) {
                node.name = it
            }
        }
    }
    this.tree.reloadAsync()
}

inline fun <T> RootNode<*>.register(
    key: String,
    value: T,
    crossinline factory: (RootNode<*>, Int, String, T) -> NBTNode
) = this.tree.register(key) { uid, name -> factory(this, uid, name, value) }

inline fun Int.registerTo(
    parent: RootNode<*>,
    key: () -> String
): NBTNode? = when (this) {
    TAG_BYTE -> parent.register(key(), 0, ::ByteNode)
    TAG_SHORT -> parent.register(key(), 0, ::ShortNode)
    TAG_INT -> parent.register(key(), 0, ::IntNode)
    TAG_LONG -> parent.register(key(), 0, ::LongNode)
    TAG_FLOAT -> parent.register(key(), 0.0F, ::FloatNode)
    TAG_DOUBLE -> parent.register(key(), 0.0, ::DoubleNode)
    TAG_BYTE_ARRAY -> parent.register(key(), emptyList(), ::ByteArrayNode)
    TAG_STRING -> parent.register(key(), "", ::StringNode)
    TAG_LIST -> parent.register(key(), emptyList(), ::TagListNode)
    TAG_COMPOUND -> parent.register(key(), EMPTY_COMPOUND, ::CompoundNode)
    TAG_INT_ARRAY -> parent.register(key(), emptyList(), ::IntArrayNode)
    TAG_LONG_ARRAY -> parent.register(key(), emptyList(), ::LongArrayNode)
    else -> null
}

inline fun Context.renameNode(
    name: String,
    parent: MapNode?,
    crossinline callback: (String, String) -> Unit
) {
    val binding = DialogRenameNodeBinding.inflate(LayoutInflater.from(this))
    binding.text.setText(name)
    val handler = if (parent === null) null else object : TextWatcher {
        var positive: Button? = null
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            val key = s.toString()
            if (key == name || !parent.containsKey(key)) {
                this.positive?.isEnabled = true
                binding.box.error = null
            } else {
                this.positive?.isEnabled = false
                binding.box.formatError(R.string.error_duplicate_key, key)
            }
        }
    }.also {
        binding.text.addTextChangedListener(it)
    }
    MaterialAlertDialogBuilder(this)
        .setTitle(R.string.action_rename)
        .setView(binding.root)
        .setNegativeButton(R.string.option_negative, null)
        .setNeutralButton(R.string.action_paste, null)
        .setPositiveButton(R.string.option_positive) { dialog, _ ->
            callback(name, binding.text.text.toString())
        }.showAnyModify {
            handler?.positive = getButton(BUTTON_POSITIVE)
            getButton(BUTTON_NEUTRAL).setOnClickListener click@{
                it.context.apply {
                    val item = clipboard?.primaryClip?.getItemAt(0)
                    if (item === null) {
                        toast(R.string.toast_empty_clipboard)
                    } else {
                        binding.text.setText(item.coerceToText(this))
                    }
                }
            }
        }
}

inline fun Context.appendNode(
    parent: TagListNode,
    crossinline callback: (NBTNode) -> Unit
) {
    val holder = object {
        var selected = -1
        var positive: Button? = null
    }
    val binding = DialogSelectTypeBinding.inflate(LayoutInflater.from(this))
    binding.tagType.setOnItemClickListener { view, child, pos, id ->
        holder.selected = pos + 1
        holder.positive?.isEnabled = true
    }
    MaterialAlertDialogBuilder(this)
        .setTitle(R.string.action_insert)
        .setNegativeButton(R.string.option_negative, null)
        .setNeutralButton(R.string.action_paste, null)
        .setPositiveButton(R.string.option_positive) click@{ dialog, _ ->
            callback(holder.selected.registerTo(parent) { parent.size.toString() } ?: return@click)
        }.showAnyModify {
            getButton(BUTTON_POSITIVE).apply {
                isEnabled = false
                holder.positive = this
            }
            getButton(BUTTON_NEUTRAL).setOnClickListener {
                it.context.upcoming()
            }
        }
}

inline fun Context.replaceNode(
    parent: MapNode,
    node: NBTNode,
    crossinline callback: (NBTNode) -> Unit
) {
    val binding = DialogSelectTypeBinding.inflate(LayoutInflater.from(this))
    val handler = object : AdapterView.OnItemClickListener {
        var selected: Int = node.type
        var positive: Button? = null
        var parsed: NbtTag? = null
        override fun onItemClick(view: AdapterView<*>?, child: View?, position: Int, id: Long) {
            val type = position + 1
            if (this.selected != type) {
                this.selected = type
                this.parsed = null
                this.positive?.isEnabled = type != node.type
            }
        }
    }
    binding.tagType.apply {
        onItemClickListener = handler
        removeTextChangedListener(AnyWatcher)
        setText(
            this@replaceNode.resources.getStringArray(R.array.tag_types).getOrNull(node.type - 1)
        )
    }
    MaterialAlertDialogBuilder(this)
        .setTitle(R.string.action_replace)
        .setView(binding.root)
        .setNegativeButton(R.string.option_negative, null)
        .setNeutralButton(R.string.action_paste, null)
        .setPositiveButton(R.string.option_positive) click@{ dialog, _ ->
            callback(
                handler.parsed?.registerTo(parent, node.name)
                    ?: handler.selected.registerTo(parent) { node.name }
                    ?: return@click
            )
        }.showAnyModify {
            getButton(BUTTON_POSITIVE).apply {
                isEnabled = false
                handler.positive = this
            }
            getButton(BUTTON_NEUTRAL).setOnClickListener {
                it.context.upcoming()
            }
        }
}

inline fun Context.insertNode(
    parent: MapNode,
    crossinline callback: (NBTNode) -> Unit
) {
    val builder = MaterialAlertDialogBuilder(this)
        .setTitle(R.string.action_insert)
        .setNegativeButton(R.string.option_negative, null)
        .setNeutralButton(R.string.action_paste, null)
    val handler = InsertionHandler(parent, this, builder)
    builder.setPositiveButton(R.string.option_positive) click@{ dialog, _ ->
        val name = handler.text.text.toString()
        callback(
            handler.parsed?.registerTo(handler.parent, name)
                ?: handler.selected.registerTo(handler.parent) { name }
                ?: return@click
        )
    }.showAnyModify {
        getButton(BUTTON_POSITIVE).apply {
            isEnabled = false
            handler.positive = this
        }
        getButton(BUTTON_NEUTRAL).setOnClickListener {
            it.context.upcoming()
        }
    }
}

class InsertionHandler(
    val parent: MapNode,
    context: Context,
    builder: MaterialAlertDialogBuilder
) : TextWatcher, AdapterView.OnItemClickListener {
    val text: EditText
    val layout: TextInputLayout
    var selected: Int = -1
    var positive: Button? = null
    var isValidName: Boolean
    var parsed: NbtTag? = null
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

    init {
        val binding = DialogInsertNodeBinding.inflate(LayoutInflater.from(context))
        binding.tagType.onItemClickListener = this
        this.layout = binding.box
        binding.name.let {
            this.text = it
            it.addTextChangedListener(this)
            val key = it.text.toString()
            if (parent.containsKey(key)) {
                this.layout.formatError(R.string.error_duplicate_key, key)
                this.isValidName = false
            } else {
                this.isValidName = true
            }
        }
        builder.setView(binding.root)
    }

    override fun afterTextChanged(s: Editable?) {
        val key = s.toString()
        if (this.parent.containsKey(key)) {
            this.layout.formatError(R.string.error_duplicate_key, key)
            this.isValidName = false
            this.positive?.isEnabled = false
        } else {
            this.layout.error = null
            this.isValidName = true
            if (this.selected != -1) {
                this.positive?.isEnabled = true
            }
        }
    }

    override fun onItemClick(view: AdapterView<*>?, child: View?, position: Int, id: Long) {
        if (this.selected != position + 1) {
            this.selected = position + 1
            this.parsed = null
        }
        if (this.isValidName) {
            this.positive?.isEnabled = true
        }
    }
}

fun TextInputLayout.formatError(@StringRes pattern: Int, arg: String) {
    this.error = this.context.getString(pattern, arg)
}

inline fun MaterialAlertDialogBuilder.showAnyModify(
    action: AlertDialog.() -> Unit
): AlertDialog = this.show().apply {
    window?.decorView?.setOnApplyWindowInsetsListener(null)
    action(this)
}

private object AnyWatcher : TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    override fun afterTextChanged(s: Editable?) {}
    override fun equals(other: Any?) = other is TextWatcher
}