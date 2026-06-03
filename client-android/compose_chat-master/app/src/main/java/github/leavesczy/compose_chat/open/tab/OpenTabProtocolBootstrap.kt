package github.leavesczy.compose_chat.open.tab

import android.os.Bundle
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Map
import github.leavesczy.compose_chat.open.ui.ProtocolGuideTabPage
import github.leavesczy.compose_chat.protocol.FabExtension
import github.leavesczy.compose_chat.protocol.MenuItem
import github.leavesczy.compose_chat.protocol.SemanticVersion
import github.leavesczy.compose_chat.protocol.TabDefinition
import github.leavesczy.compose_chat.protocol.TabExtension
import github.leavesczy.compose_chat.protocol.TabLifecycle
import github.leavesczy.compose_chat.protocol.TitleBarExtension

object OpenTabProtocolBootstrap {

    private const val ProtocolGuideTabId = "protocol-guide"

    fun registerBuiltInTabs() {
        if (OpenTabContainer.findById(tabId = ProtocolGuideTabId) != null) {
            return
        }
        OpenTabContainer.registerTab(
            definition = TabDefinition(
                id = ProtocolGuideTabId,
                displayName = "接入指南",
                icon = Icons.Rounded.Map,
                route = "/protocol-guide",
                version = SemanticVersion.CURRENT,
                minContainerVersion = 1,
                permissions = emptyList(),
                extension = TabExtension(
                    titleBar = TitleBarExtension(
                        rightText = "协议",
                        menuItems = listOf(
                            MenuItem(
                                id = "copy-id",
                                label = "查看 Tab ID",
                                onClick = {
                                    Log.d("OpenTabProtocol", "protocol-guide")
                                }
                            )
                        )
                    ),
                    fab = FabExtension(
                        icon = Icons.Rounded.Map,
                        label = "接入检查",
                        onClick = {
                            Log.d("OpenTabProtocol", "open checklist")
                        }
                    )
                )
            ),
            lifecycle = object : TabLifecycle {
                override fun onCreate(bundle: Bundle?) {
                    Log.d("OpenTabLifecycle", "protocol-guide onCreate")
                }

                override fun onResume() {
                    Log.d("OpenTabLifecycle", "protocol-guide onResume")
                }

                override fun onPause() {
                    Log.d("OpenTabLifecycle", "protocol-guide onPause")
                }

                override fun onDestroy() {
                    Log.d("OpenTabLifecycle", "protocol-guide onDestroy")
                }
            },
            page = {
                ProtocolGuideTabPage()
            }
        )
    }

}

