package com.mithrilmania.blocktopograph.test

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.mithrilmania.blocktopograph.BaseActivity
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.databinding.ActivityWorldTestBinding
import com.mithrilmania.blocktopograph.nbt.convert.NBTConstants
import com.mithrilmania.blocktopograph.util.ByteArrayMatcher
import com.mithrilmania.blocktopograph.util.ConvertUtil
import com.mithrilmania.blocktopograph.util.FileCreator
import com.mithrilmania.blocktopograph.util.VIEW_DOCUMENT_FLAG
import com.mithrilmania.blocktopograph.util.logError
import com.mithrilmania.blocktopograph.util.popError
import com.mithrilmania.blocktopograph.util.upcoming
import com.mithrilmania.blocktopograph.world.WorldStorage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

class WorldTestActivity : BaseActivity(), TextWatcher {
    private lateinit var binding: ActivityWorldTestBinding
    private lateinit var model: WorldTestModel
    private lateinit var selectOutput: ActivityResultLauncher<Uri?>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val model = ViewModelProvider(this)[WorldTestModel::class]
        var storage: Deferred<WorldStorage?> =
            CompletableDeferred<WorldStorage?>(model.handler?.storage)
        if (model.handler == null) {
            try {
                model.init(this, this.intent)
                this.lifecycleScope.launch(Dispatchers.IO) {
                    storage = model.handler!!.open(this, this@WorldTestActivity)
                }
            } catch (e: Throwable) {
                Toast.makeText(this, "Failed to open world", Toast.LENGTH_SHORT).show()
                logError("Failed to open world", e)
                this.finish()
                return
            }
        }
        this.model = model
        val binding = ActivityWorldTestBinding.inflate(this.layoutInflater)
        this.setContentView(binding.root)
        binding.toolbar.let {
            this.setSupportActionBar(it)
            it.setNavigationOnClickListener {
                this.finish()
            }
        }
        val onSwitchType = object : View.OnClickListener {
            override fun onClick(v: View) {
                model.checked = v.id
            }
        }
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
                    val iterator = storage.await()?.db?.iterator() ?: return@launch
                    try {
                        iterator.seekToFirst()
                        val failure = ByteArrayMatcher.computeFailure(pattern)
                        while (iterator.hasNext()) {
                            val entry = iterator.next()
                            val key = entry.key
                            if (ByteArrayMatcher.contains(key, pattern, failure)) {
                                values.add(ConvertUtil.bytesToHexStr(entry.value))
                                if (isPlainText) {
                                    display.add(String(key, NBTConstants.CHARSET))
                                } else {
                                    display.add(ConvertUtil.bytesToHexStr(key))
                                }
                            }
                        }
                    } catch (e: Throwable) {
                        iterator.close()
                        popError(e)
                        return@launch
                    }
                    iterator.close()
                    val onClick = object : DialogInterface.OnClickListener {
                        override fun onClick(dialog: DialogInterface?, which: Int) {
                            MaterialAlertDialogBuilder(this@WorldTestActivity)
                                .setMessage(values.getOrNull(which) ?: "")
                                .show()
                        }
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
                val db = storage.await()?.db ?: return@launch
                var value: String
                try {
                    value = ConvertUtil.bytesToHexStr(db[key] ?: return@launch)
                } catch (e: Throwable) {
                    popError(e)
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
        this.selectOutput = registerForActivityResult(FileCreator) { uri ->
            if (uri == null) return@registerForActivityResult
            val key = this.getDBKey() ?: return@registerForActivityResult
            this.lifecycleScope.launch(Dispatchers.IO) {
                val db = storage.await()?.db ?: return@launch
                val stream = this@WorldTestActivity.contentResolver.openOutputStream(uri)
                    ?: return@launch
                try {
                    stream.write(db[key])
                } catch (e: Throwable) {
                    stream.close()
                    popError(e)
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
                                .setDataAndType(uri, "application/octet-stream")
                        )
                    }.apply {
                        ViewCompat.setOnApplyWindowInsetsListener(view.apply {
                            fitsSystemWindows = false
                            updateLayoutParams<ViewGroup.MarginLayoutParams> {
                                bottomMargin =
                                    resources.getDimension(R.dimen.large_content_padding).toInt()
                            }
                        }, null)
                    }.setGestureInsetBottomIgnored(true).show()
                }
            }
        }
    }

    private fun getDBKey(): ByteArray? {
        val text = this.binding.key.editText?.text?.toString() ?: return null
        if (text.isBlank()) return null
        return if (
            this.model.checked == R.id.plain_text
        ) text.toByteArray(NBTConstants.CHARSET)
        else ConvertUtil.hexStringToBytes(text)
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
    }

    override fun afterTextChanged(s: Editable?) {
        this.model.text = s.toString()
    }

    override fun updateDecorViewPadding(decorView: View, systemBars: Insets, ime: Insets) {
        decorView.updatePadding(
            top = 0,
            bottom = max(systemBars.bottom, ime.bottom),
            left = systemBars.left,
            right = systemBars.right
        )
    }
}