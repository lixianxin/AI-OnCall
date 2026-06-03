package github.leavesczy.compose_chat.open.tab

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Sailing
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GeneratingTokens
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.vector.ImageVector
import github.leavesczy.compose_chat.open.model.EntryType
import github.leavesczy.compose_chat.open.model.TabManifest

@Stable
data class OpenTabItem(
    val manifest: TabManifest,
    val openState: OpenTabState,
    val icon: ImageVector,
    val source: OpenTabSource
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

@Stable
enum class OpenTabSource {
    Remote,
    ClientBuiltIn,
    LocalMock
}

object OpenTabRegistry {

    private val nativeRoutes = setOf(
        "/company",
        "/company-intro",
        "/announcements",
        "/approval",
        "/calendar",
        "/fun",
        "/permission-admin",
        "/finance",
        "/ai-oncall"
    )

    fun supportsRoute(route: String): Boolean {
        return route in nativeRoutes || route == "/docs" || route == "/bilibili" || route.startsWith("/custom-")
    }

    fun supportsEntryType(entryType: EntryType): Boolean {
        return entryType == EntryType.Native || entryType == EntryType.Web || entryType == EntryType.External
    }

    fun iconOf(icon: String?): ImageVector {
        return when (icon) {
            "approval" -> Icons.Filled.Sailing
            "calendar" -> Icons.Rounded.WbSunny
            "company" -> Icons.Rounded.Groups
            "announcement" -> Icons.Rounded.Campaign
            "fun" -> Icons.Rounded.EmojiEmotions
            "admin" -> Icons.Rounded.AdminPanelSettings
            "finance" -> Icons.Rounded.ColorLens
            "docs", "web" -> Icons.Filled.Menu
            "custom" -> Icons.Rounded.ColorLens
            "video", "bilibili" -> Icons.Filled.SmartDisplay
            "ai", "oncall", "ai-oncall" -> Icons.Filled.MoreVert
            "shape-star" -> Icons.Rounded.Star
            "shape-bolt" -> Icons.Rounded.Bolt
            "shape-gem" -> Icons.Rounded.GeneratingTokens
            "shape-extension" -> Icons.Rounded.Extension
            "shape-widgets" -> Icons.Rounded.Widgets
            "shape-bubble" -> Icons.Rounded.Campaign
            "shape-spark" -> Icons.Rounded.ColorLens
            "shape-category" -> Icons.Rounded.Category
            "shape-globe" -> Icons.Rounded.Language
            "shape-heart" -> Icons.Rounded.Favorite
            else -> Icons.Filled.Menu
        }
    }

}
