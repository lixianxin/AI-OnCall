package github.leavesczy.compose_chat.open.model

data class TabManifest(
    val id: String,
    val displayName: String,
    val description: String?,
    val icon: String?,
    val route: String,
    val entryType: EntryType,
    val entryUri: String?,
    val version: SemanticVersionDto,
    val minContainerVersion: Int,
    val permissions: List<String>,
    val enabled: Boolean,
    val sortOrder: Int,
    val extension: TabExtensionDto?,
    val extraConfig: Map<String, String>,
    val visibility: TabVisibilityRequest? = null
)

data class SemanticVersionDto(
    val major: Int,
    val minor: Int,
    val patch: Int
)

enum class EntryType {
    Native,
    Web,
    Hybrid,
    External,
    Unknown;

    companion object {

        fun from(value: String?): EntryType {
            return when (value?.lowercase()) {
                "native" -> Native
                "web" -> Web
                "hybrid" -> Hybrid
                "external" -> External
                else -> Unknown
            }
        }

    }

}

data class TabExtensionDto(
    val titleBar: TitleBarExtensionDto?,
    val fab: FabExtensionDto?
)

data class TitleBarExtensionDto(
    val rightText: String?,
    val menuItems: List<MenuItemDto>
)

data class MenuItemDto(
    val id: String,
    val label: String
)

data class FabExtensionDto(
    val id: String,
    val icon: String?,
    val label: String
)
