package github.leavesczy.compose_chat.open.tab

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Sailing
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.vector.ImageVector
import github.leavesczy.compose_chat.open.model.EntryType
import github.leavesczy.compose_chat.open.model.TabManifest

@Stable
data class OpenTabItem(
    val manifest: TabManifest,
    val openState: OpenTabState,
    val icon: ImageVector
) {
    val id: String
        get() = manifest.id

    val displayName: String
        get() = manifest.displayName
}

@Stable
enum class OpenTabState {
    Openable,
    Disabled,
    PermissionDenied,
    VersionIncompatible,
    RouteUnsupported,
    EntryUnsupported,
    InvalidConfig
}

object OpenTabRegistry {

    private val nativeRoutes = setOf(
        "/approval",
        "/calendar",
        "/finance",
        "/ai-oncall"
    )

    fun supportsRoute(route: String): Boolean {
        return route in nativeRoutes || route == "/docs"
    }

    fun supportsEntryType(entryType: EntryType): Boolean {
        return entryType == EntryType.Native || entryType == EntryType.Web || entryType == EntryType.External
    }

    fun iconOf(icon: String?): ImageVector {
        return when (icon) {
            "approval" -> Icons.Filled.Sailing
            "calendar" -> Icons.Rounded.WbSunny
            "finance" -> Icons.Rounded.ColorLens
            "docs" -> Icons.Filled.Menu
            "ai", "oncall", "ai-oncall" -> Icons.Filled.MoreVert
            else -> Icons.Filled.Menu
        }
    }

}
