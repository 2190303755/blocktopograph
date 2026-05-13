package com.mithrilmania.blocktopograph.test.v2

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixNormal
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.ui.component.IconButton
import com.mithrilmania.blocktopograph.ui.component.TooltipBox
import com.mithrilmania.blocktopograph.ui.theme.setThemedContent
import com.mithrilmania.blocktopograph.util.upcoming
import com.mithrilmania.blocktopograph.world.WorldModel
import com.mithrilmania.blocktopograph.world.WorldStorage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

class WorldTestActivity : ComponentActivity() {
    private val viewModel by viewModels<WorldModel>()

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.enableEdgeToEdge()
        val model = this.viewModel
        var storage: Deferred<WorldStorage?> = CompletableDeferred(model.handler?.storage)
        if (model.handler === null) {
            if (model.init(this, this.intent)) {
                storage = this.lifecycleScope.async(Dispatchers.IO) {
                    this@WorldTestActivity.viewModel.handler?.open(this@WorldTestActivity)
                }
            } else {
                Toast.makeText(this, "Invalid world", Toast.LENGTH_SHORT).show()
                this.finish()
                return
            }
        }
        this.setThemedContent {
            val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    TopAppBar(
                        scrollBehavior = scrollBehavior,
                        title = {
                            Text(
                                stringResource(R.string.nbt_editor)
                            )
                        },
                        actions = {
                            TooltipBox("repair") { tooltip ->
                                IconButton(
                                    Icons.Filled.AutoFixNormal,
                                    tooltip,
                                ) {
                                    upcoming()
                                }
                            }
                        }
                    )
                }
            ) { padding ->
                LazyColumn(
                    contentPadding = padding,
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(rememberScrollState())
                ) {
                }
            }
        }
    }
}