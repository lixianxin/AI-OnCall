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
    val extraConfig: Map<String, String>
)
