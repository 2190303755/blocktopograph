package com.mithrilmania.blocktopograph.ui

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.ui.graphics.vector.ImageVector
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.ui.screen.nbt.ConfiguredNBTSource
import com.mithrilmania.blocktopograph.world.LoadedWorld
import org.iq80.leveldb.DB

sealed interface AppNavKey {
    sealed interface SettingsNavKey : AppNavKey {
        val icon: ImageVector

        @get:StringRes
        val title: Int

        @get:StringRes
        val description: Int
    }

    object WorldList : AppNavKey
    object Settings : AppNavKey
    object WorkspaceManager : SettingsNavKey {
        override val icon: ImageVector
            get() = Icons.Filled.FolderCopy
        override val title: Int
            get() = R.string.settings_workspace_manager_title
        override val description: Int
            get() = R.string.settings_workspace_manager_description
    }

    object SAFManager : SettingsNavKey {
        override val icon: ImageVector
            get() = Icons.AutoMirrored.Filled.Rule
        override val title: Int
            get() = R.string.settings_saf_manager_title
        override val description: Int
            get() = R.string.settings_saf_manager_description
    }

    object ScanningManager : SettingsNavKey {
        override val icon: ImageVector
            get() = Icons.Filled.TravelExplore
        override val title: Int
            get() = R.string.settings_scanning_manager_title
        override val description: Int
            get() = R.string.settings_scanning_manager_description
    }

    object ResourcePackManager : SettingsNavKey {
        override val icon: ImageVector
            get() = Icons.Filled.Layers
        override val title: Int
            get() = R.string.settings_resource_pack_manager_title
        override val description: Int
            get() = R.string.settings_resource_pack_manager_description
    }

    object BlockTextureBinding : SettingsNavKey {
        override val icon: ImageVector
            get() = Icons.Filled.GridView
        override val title: Int
            get() = R.string.settings_block_texture_binding_title
        override val description: Int
            get() = R.string.settings_block_texture_binding_description
    }

    object ItemTextureBinding : SettingsNavKey {
        override val icon: ImageVector
            get() = Icons.Filled.Inventory2
        override val title: Int
            get() = R.string.settings_item_texture_binding_title
        override val description: Int
            get() = R.string.settings_item_texture_binding_description
    }

    object CreateWorld : AppNavKey
    class NBTEditor(
        @JvmField val initial: ConfiguredNBTSource?,
        @JvmField val inferable: Boolean = true,
        @JvmField val crossfade: Boolean = false
    ) : AppNavKey

    class WorldEditor(@JvmField val world: LoadedWorld) : AppNavKey

    /**
     * Full-screen LevelDB key browser bound to an opened raw [DB].
     */
    class StorageExplorer(@JvmField val storage: DB) : AppNavKey
}

@JvmField
val BlankNBTEditor = AppNavKey.NBTEditor(null)

fun MutableList<AppNavKey>.navigateBack(): AppNavKey? =
    if (this.size > 1) removeAt(this.lastIndex) else null
