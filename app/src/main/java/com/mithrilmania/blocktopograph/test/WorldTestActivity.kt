package com.mithrilmania.blocktopograph.test

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.mithrilmania.blocktopograph.BaseActivity
import com.mithrilmania.blocktopograph.MIME_TYPE_DEFAULT
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.databinding.ActivityWorldTestBinding
import com.mithrilmania.blocktopograph.nbt.old.convert.NBTConstants
import com.mithrilmania.blocktopograph.util.ByteArrayMatcher
import com.mithrilmania.blocktopograph.util.FileCreator
import com.mithrilmania.blocktopograph.util.LEVEL_DB_TAG
import com.mithrilmania.blocktopograph.util.VIEW_DOCUMENT_FLAG
import com.mithrilmania.blocktopograph.util.errorAndPop
import com.mithrilmania.blocktopograph.util.lenientHexToByteArray
import com.mithrilmania.blocktopograph.util.upcoming
import com.mithrilmania.blocktopograph.world.WorldStorage
import com.mithrilmania.blocktopograph.world.await
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WorldTestActivity : BaseActivity(), TextWatcher {
    private lateinit var binding: ActivityWorldTestBinding
    private lateinit var selectOutput: ActivityResultLauncher<FileCreator.Options?>
    private val model by viewModels<WorldTestModel>()
    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        val model = this.model
        var storage: Deferred<WorldStorage?> = CompletableDeferred(model.handler?.storage)
        if (model.handler === null) {
            if (model.init(this, this.intent)) {
                storage = this.lifecycleScope.async(Dispatchers.IO) {
                    this@WorldTestActivity.model.handler?.open(this@WorldTestActivity)
                }
            } else {
                Toast.makeText(this, "Invalid world", Toast.LENGTH_SHORT).show()
                this.finish()
                return
            }
        }
        val binding = ActivityWorldTestBinding.inflate(this.layoutInflater)
        this.setContentView(binding.root)
        binding.toolbar.let {
            this.setSupportActionBar(it)
            it.setNavigationOnClickListener {
                this.finish()
            }
        }
        val onSwitchType = View.OnClickListener { v -> model.checked = v.id }
        binding.type.check(model.checked)
        binding.plainText.setOnClickListener(onSwitchType)
        binding.hexText.setOnClickListener(onSwitchType)
        binding.key.apply {
            editText?.apply {
                setText(model.text)
                addTextChangedListener(this@WorldTestActivity)
            }
            setEndIconOnClickListener {
                val pattern = this@WorldTestActivity.getDBKey() ?: return@setEndIconOnClickListener
                val isPlainText = binding.type.checkedButtonId == R.id.plain_text
                this@WorldTestActivity.lifecycleScope.launch(Dispatchers.Default) {
                    val display = ArrayList<String>()
                    val values = ArrayList<String>()
                    val iterator = storage.await { it.db.iterator() } ?: return@launch
                    val failure = ByteArrayMatcher.computeFailure(pattern)
                    try {
                        iterator.seekToFirst()
                        while (iterator.hasNext()) {
                            val entry = iterator.next()
                            val key = entry.key
                            if (ByteArrayMatcher.contains(key, pattern, failure)) {
                                values.add(entry.value.toHexString())
                                display.add(
                                    if (isPlainText) {
                                        key.toString(Charsets.UTF_8)
                                    } else {
                                        key.toHexString()
                                    }
                                )
                            }
                        }
                    } catch (e: Throwable) {
                        iterator.close()
                        errorAndPop("Failed to collect keys", e, LEVEL_DB_TAG)
                        return@launch
                    }
                    iterator.close()
                    val onClick = DialogInterface.OnClickListener { dialog, which ->
                        MaterialAlertDialogBuilder(this@WorldTestActivity)
                            .setMessage(values.getOrNull(which) ?: "")
                            .show()
                    }
                    val entries = display.toArray(arrayOfNulls<String>(display.size))
                    withContext(Dispatchers.Main) {
                        MaterialAlertDialogBuilder(this@WorldTestActivity)
                            .setItems(entries, onClick)
                            .show()
                    }
                }
            }
        }

        binding.fix.setOnClickListener {
            this.upcoming()
        }

        binding.query.setOnClickListener {
            val key = this.getDBKey() ?: return@setOnClickListener
            this.lifecycleScope.launch(Dispatchers.Default) {
                val db = storage.await(WorldStorage::db) ?: return@launch
                val value: String
                try {
                    value = db[key]?.toHexString() ?: return@launch
                } catch (e: Throwable) {
                    errorAndPop("Failed to query value with key $key", e, LEVEL_DB_TAG)
                    return@launch
                }
                withContext(Dispatchers.Main) {
                    MaterialAlertDialogBuilder(this@WorldTestActivity).setMessage(value).show()
                }
            }
        }

        binding.export.setOnClickListener {
            this.selectOutput.launch(null)
        }
        this.binding = binding
        this.selectOutput = this.registerForActivityResult(FileCreator) { uri ->
            if (uri == null) return@registerForActivityResult
            val key = this.getDBKey() ?: return@registerForActivityResult
            this.lifecycleScope.launch(Dispatchers.IO) {
                val db = storage.await(WorldStorage::db) ?: return@launch
                val stream = this@WorldTestActivity.contentResolver.openOutputStream(uri)
                    ?: return@launch
                try {
                    stream.write(db[key])
                } catch (e: Throwable) {
                    stream.close()
                    errorAndPop("Failed to query and export value with key $key", e, LEVEL_DB_TAG)
                    return@launch
                }
                stream.close()
                val resources = this@WorldTestActivity.resources
                withContext(Dispatchers.Main) {
                    Snackbar.make(
                        binding.bottomBar,
                        resources.getString(R.string.world_test_export_done),
                        Snackbar.LENGTH_INDEFINITE
                    ).setAction(resources.getString(R.string.world_test_open_file)) {
                        this@WorldTestActivity.startActivity(
                            Intent()
                                .setAction(Intent.ACTION_VIEW)
                                .setFlags(VIEW_DOCUMENT_FLAG)
                                .setDataAndType(uri, MIME_TYPE_DEFAULT)
                        )
                    }.apply {
                        ViewCompat.setOnApplyWindowInsetsListener(view.apply {
                            fitsSystemWindows = false
                            updateLayoutParams<ViewGroup.MarginLayoutParams> {
                                bottomMargin =
                                    resources.getDimensionPixelSize(R.dimen.large_content_padding)
                            }
                        }, null)
                    }.setGestureInsetBottomIgnored(true).show()
                }
            }
        }
    }

    private fun getDBKey(): ByteArray? {
        val text = this.binding.key.editText?.text?.toString()
        if (text.isNullOrBlank()) return null
        if (this.model.checked == R.id.plain_text) return text.toByteArray(NBTConstants.CHARSET)
        try {
            return text.lenientHexToByteArray()
        } catch (e: IllegalArgumentException) {
        }
        return null
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

    override fun afterTextChanged(s: Editable?) {
        this.model.text = s.toString()
    }

    override fun applyContentInsets(window: View, insets: Insets) {
        window.updatePadding(
            top = 0,
            bottom = insets.bottom,
            left = insets.left,
            right = insets.right
        )
    }
}