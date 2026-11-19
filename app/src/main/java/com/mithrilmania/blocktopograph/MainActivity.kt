package com.mithrilmania.blocktopograph

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.rememberUpdatedState
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.metadata
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.mithrilmania.blocktopograph.ui.AppNavKey
import com.mithrilmania.blocktopograph.ui.AppNavKey.CreateWorld
import com.mithrilmania.blocktopograph.ui.AppNavKey.NBTEditor
import com.mithrilmania.blocktopograph.ui.AppNavKey.SAFManager
import com.mithrilmania.blocktopograph.ui.AppNavKey.ScanningManager
import com.mithrilmania.blocktopograph.ui.AppNavKey.Settings
import com.mithrilmania.blocktopograph.ui.AppNavKey.StorageExplorer
import com.mithrilmania.blocktopograph.ui.AppNavKey.WorkspaceManager
import com.mithrilmania.blocktopograph.ui.AppNavKey.WorldEditor
import com.mithrilmania.blocktopograph.ui.AppNavKey.WorldList
import com.mithrilmania.blocktopograph.ui.SharedViewModel
import com.mithrilmania.blocktopograph.ui.navigateBack
import com.mithrilmania.blocktopograph.ui.screen.CreateWorldScreen
import com.mithrilmania.blocktopograph.ui.screen.WorldListScreen
import com.mithrilmania.blocktopograph.ui.screen.nbt.NBTEditorScreen
import com.mithrilmania.blocktopograph.ui.screen.nbt.resolveNBTEditor
import com.mithrilmania.blocktopograph.ui.screen.settings.SAFManagerScreen
import com.mithrilmania.blocktopograph.ui.screen.settings.ScanningManagerScreen
import com.mithrilmania.blocktopograph.ui.screen.settings.SettingsScreen
import com.mithrilmania.blocktopograph.ui.screen.settings.WorkspaceManagerScreen
import com.mithrilmania.blocktopograph.ui.screen.world.StorageExplorerScreen
import com.mithrilmania.blocktopograph.ui.screen.world.WorldEditorScreen
import com.mithrilmania.blocktopograph.ui.theme.BlocktopographCompatTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        this.installSplashScreen()
        this.enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        this.setContent {
            BlocktopographCompatTheme {
                val shared = viewModel {
                    val intent = this@MainActivity.intent
                    SharedViewModel(
                        when (intent?.action) {
                            Intent.ACTION_VIEW -> resolveNBTEditor(intent)
                            Intent.ACTION_APPLICATION_PREFERENCES -> Settings
                            else -> null
                        }
                    )
                }
                NavDisplay(
                    entryDecorators = listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator()
                    ),
                    backStack = shared.backStack,
                    transitionSpec = {
                        slideIntoContainer(
                            AnimatedContentTransitionScope.SlideDirection.Start
                        ) togetherWith ExitTransition.KeepUntilTransitionsFinished
                    },
                    popTransitionSpec = {
                        EnterTransition.None togetherWith slideOutOfContainer(
                            AnimatedContentTransitionScope.SlideDirection.End
                        )
                    },
                    predictivePopTransitionSpec = {
                        EnterTransition.None togetherWith slideOutOfContainer(
                            AnimatedContentTransitionScope.SlideDirection.End
                        )
                    },
                    entryProvider = { key ->
                        val onNavigate: (AppNavKey) -> Unit = { shared.backStack.add(it) }
                        when (key) {
                            is NBTEditor -> NavEntry(key = key, metadata = metadata {
                                if (key.crossfade) {
                                    put(NavDisplay.TransitionKey) {
                                        fadeIn() togetherWith fadeOut()
                                    }
                                }
                            }) {
                                val backStack = shared.backStack
                                NBTEditorScreen(
                                    spec = key,
                                    onNew = {
                                        backStack[
                                            backStack.lastIndex
                                        ] = NBTEditor(null, crossfade = true)
                                    },
                                    onExit = {
                                        backStack.navigateBack()
                                    }
                                )
                            }

                            WorldList -> NavEntry(key) {
                                WorldListScreen(
                                    rememberUpdatedState(key == shared.backStack.lastOrNull()),
                                    onNavigate
                                )
                            }

                            CreateWorld -> NavEntry(key) {
                                CreateWorldScreen {
                                    shared.backStack.navigateBack()
                                }
                            }

                            WorkspaceManager -> NavEntry(key) {
                                WorkspaceManagerScreen()
                            }

                            SAFManager -> NavEntry(key) {
                                SAFManagerScreen()
                            }

                            Settings -> NavEntry(key) {
                                SettingsScreen(onNavigate)
                            }

                            ScanningManager -> NavEntry(key) {
                                ScanningManagerScreen()
                            }

                            is WorldEditor -> NavEntry(key = key, metadata = metadata {
                                put(NavDisplay.TransitionKey) {
                                    slideIntoContainer(
                                        AnimatedContentTransitionScope.SlideDirection.Up
                                    ) togetherWith ExitTransition.KeepUntilTransitionsFinished
                                }
                                put(NavDisplay.PopTransitionKey) {
                                    EnterTransition.None togetherWith slideOutOfContainer(
                                        AnimatedContentTransitionScope.SlideDirection.Down
                                    )
                                }
                                put(NavDisplay.PredictivePopTransitionKey) {
                                    EnterTransition.None togetherWith slideOutOfContainer(
                                        AnimatedContentTransitionScope.SlideDirection.Down
                                    )
                                }
                            }) {
                                WorldEditorScreen(key.world, onNavigate)
                            }

                            is StorageExplorer -> NavEntry(key) {
                                StorageExplorerScreen(key.storage, onNavigate)
                            }
                        }
                    }
                )
            }
        }
    }
}
