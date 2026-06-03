package github.leavesczy.compose_chat.open.tab

import androidx.compose.runtime.Composable
import github.leavesczy.compose_chat.open.config.OpenApiConfig
import github.leavesczy.compose_chat.protocol.ContainerInterface
import github.leavesczy.compose_chat.protocol.RegistrationResult
import github.leavesczy.compose_chat.protocol.TabDefinition
import github.leavesczy.compose_chat.protocol.TabErrors
import github.leavesczy.compose_chat.protocol.TabLifecycle

data class RegisteredOpenTab(
    val definition: TabDefinition,
    val lifecycle: TabLifecycle,
    val page: @Composable () -> Unit
)

object OpenTabContainer : ContainerInterface {

    private val registeredTabs = linkedMapOf<String, RegisteredOpenTab>()
    private var currentRoute: String? = null

    override fun registerTab(
        definition: TabDefinition,
        lifecycle: TabLifecycle,
        page: @Composable () -> Unit
    ): RegistrationResult {
        val failedResult = validate(definition = definition)
        if (failedResult != null) {
            return failedResult
        }
        registeredTabs[definition.id] = RegisteredOpenTab(
            definition = definition,
            lifecycle = lifecycle,
            page = page
        )
        return RegistrationResult.Success
    }

    override fun switchTab(route: String): Boolean {
        val registeredTab = findByRoute(route = route) ?: return false
        currentRoute = registeredTab.definition.route
        return true
    }

    override fun unregisterTab(tabId: String): Boolean {
        val removed = registeredTabs.remove(tabId) ?: return false
        if (currentRoute == removed.definition.route) {
            currentRoute = null
        }
        return true
    }

    override fun getRegisteredTabs(): List<TabDefinition> {
        return registeredTabs.values.map { tab -> tab.definition }
    }

    override fun getContainerVersion(): Int {
        return OpenApiConfig.CONTAINER_VERSION
    }

    fun getRegisteredOpenTabs(): List<RegisteredOpenTab> {
        return registeredTabs.values.toList()
    }

    fun findById(tabId: String): RegisteredOpenTab? {
        return registeredTabs[tabId]
    }

    fun findByRoute(route: String): RegisteredOpenTab? {
        return registeredTabs.values.firstOrNull { tab -> tab.definition.route == route }
    }

    fun supportsRoute(route: String): Boolean {
        return findByRoute(route = route) != null
    }

    private fun validate(definition: TabDefinition): RegistrationResult.Failed? {
        if (
            definition.id.isBlank() ||
            definition.displayName.isBlank() ||
            definition.route.isBlank() ||
            !definition.route.startsWith("/")
        ) {
            return RegistrationResult.Failed(errorCode = TabErrors.MISSING_REQUIRED_FIELD)
        }
        if (definition.minContainerVersion > getContainerVersion()) {
            return RegistrationResult.Failed(errorCode = TabErrors.CONTAINER_VERSION_TOO_LOW)
        }
        val duplicateId = registeredTabs.containsKey(definition.id)
        val duplicateRoute = registeredTabs.values.any { tab ->
            tab.definition.route == definition.route && tab.definition.id != definition.id
        }
        if (duplicateId || duplicateRoute) {
            return RegistrationResult.Failed(errorCode = TabErrors.DUPLICATE_ID)
        }
        if (definition.extension?.titleBar?.isConflict == true) {
            return RegistrationResult.Failed(
                errorCode = TabErrors.MISSING_REQUIRED_FIELD,
                message = "标题栏右侧图标和菜单不能同时配置"
            )
        }
        return null
    }

}

