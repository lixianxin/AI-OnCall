package github.leavesczy.compose_chat.protocol

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.vector.ImageVector

@Stable
data class TabDefinition(
    val id: String,
    val displayName: String,
    val icon: ImageVector,
    val route: String,
    val version: SemanticVersion,
    val minContainerVersion: Int = 1,
    val permissions: List<String> = emptyList(),
    val extension: TabExtension? = null
) {

    val fullId: String
        get() = "$id@$version"

    init {
        require(id.isNotBlank()) { "Tab id must not be blank" }
        require(displayName.isNotBlank()) { "displayName must not be blank" }
        require(displayName.length <= 16) {
            "displayName must be <= 16 chars, got ${displayName.length}"
        }
        require(route.startsWith("/")) { "route must start with '/'" }
    }

}

@Stable
data class SemanticVersion(
    val major: Int,
    val minor: Int,
    val patch: Int = 0
) {

    override fun toString(): String = "$major.$minor.$patch"

    companion object {

        val CURRENT = SemanticVersion(major = 1, minor = 0, patch = 0)

    }

}

