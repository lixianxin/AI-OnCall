package github.leavesczy.compose_chat.protocol

import androidx.compose.runtime.Composable

interface ContainerInterface {

    fun registerTab(
        definition: TabDefinition,
        lifecycle: TabLifecycle,
        page: @Composable () -> Unit
    ): RegistrationResult

    fun switchTab(route: String): Boolean

    fun unregisterTab(tabId: String): Boolean

    fun getRegisteredTabs(): List<TabDefinition>

    fun getContainerVersion(): Int

}

sealed class RegistrationResult {

    data object Success : RegistrationResult()

    data class Failed(
        val errorCode: Int,
        val message: String = TabErrors.description(errorCode)
    ) : RegistrationResult()

    val isSuccess: Boolean
        get() = this is Success

    val isFailed: Boolean
        get() = this is Failed

}

