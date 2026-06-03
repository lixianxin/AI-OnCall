package github.leavesczy.compose_chat.protocol

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.vector.ImageVector

@Stable
data class TabExtension(
    val titleBar: TitleBarExtension? = null,
    val fab: FabExtension? = null,
    val bottomPanel: BottomPanel? = null
)

@Stable
data class TitleBarExtension(
    val rightIcon: ImageVector? = null,
    val rightText: String? = null,
    val menuItems: List<MenuItem> = emptyList()
) {

    val isConflict: Boolean
        get() = rightIcon != null && menuItems.isNotEmpty()

}

@Stable
data class MenuItem(
    val id: String,
    val label: String,
    val onClick: () -> Unit
)

@Stable
data class FabExtension(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit
)

@Stable
data class BottomPanel(
    val content: @Composable () -> Unit,
    val defaultHeight: Int
)

