package github.leavesczy.compose_chat.open.model

data class ApiErrorResponse(
    val code: String,
    val message: String,
    val traceId: String? = null
)

data class HealthResponse(
    val status: String,
    val service: String,
    val mode: String,
    val serverTime: String
)

data class LoginRequest(
    val account: String,
    val password: String
)

data class RegisterRequest(
    val account: String,
    val password: String,
    val displayName: String
)

data class LoginResponse(
    val token: String,
    val userId: String?,
    val displayName: String,
    val permissions: List<String>
)

data class MeResponse(
    val userId: String,
    val displayName: String,
    val globalRole: String?,
    val currentTeamId: String?,
    val memberships: List<OpenTeamMembership>,
    val permissions: List<String>,
    val team: TeamDto?
)

data class TeamDto(
    val id: String,
    val name: String
)

data class DebugStatusResponse(
    val serverTime: String,
    val apiVersion: String,
    val mockMode: Boolean,
    val sseAvailable: Boolean,
    val tabCount: Int,
    val database: DebugDatabaseDto? = null
)

data class ApprovalSummaryResponse(
    val pendingCount: Int,
    val approvedToday: Int,
    val items: List<ApprovalItemDto>
)

data class ApprovalItemDto(
    val id: String,
    val title: String,
    val applicant: String,
    val status: String,
    val createdAt: String,
    val amount: Int? = null,
    val reason: String? = null,
    val comment: String? = null,
    val updatedAt: String? = null
)

data class SuccessResponse(
    val success: Boolean,
    val tabId: String? = null
)

data class CreateCustomWebTabRequest(
    val id: String,
    val displayName: String,
    val description: String,
    val icon: String,
    val route: String,
    val entryUri: String,
    val minContainerVersion: Int = 1,
    val sortOrder: Int? = null,
    val extraConfig: Map<String, String> = emptyMap()
)

data class TabMutationResponse(
    val success: Boolean,
    val tabId: String?,
    val tab: TabManifest?
)

data class OnCallToolEvent(
    val name: String,
    val status: String,
    val summary: String
)
data class CalendarSummaryResponse(
    val todayCount: Int,
    val events: List<CalendarEventDto>
)

data class CalendarEventDto(
    val id: String,
    val title: String,
    val description: String? = null,
    val startTime: String,
    val endTime: String,
    val location: String? = null,
    val participants: List<String> = emptyList()
)

data class DebugDatabaseDto(
    val enabled: Boolean,
    val type: String
)

data class DebugPermissionDto(
    val code: String,
    val description: String,
    val database: String? = null
)

data class OnCallSessionDto(
    val sessionId: String,
    val title: String,
    val createdAt: String,
    val updatedAt: String? = null,
    val messageCount: Int? = null
)

data class OnCallMessageDto(
    val messageId: String,
    val sessionId: String,
    val role: String,
    val content: String,
    val contentType: String,
    val createdAt: String
)

data class UpdateCustomWebTabRequest(
    val displayName: String,
    val description: String,
    val icon: String,
    val entryUri: String,
    val sortOrder: Int? = null
)

data class SemanticVersionDto(
    val major: Int,
    val minor: Int,
    val patch: Int
)

data class TabExtensionDto(
    val titleBar: TitleBarExtensionDto? = null,
    val fab: FabExtensionDto? = null
)

data class TitleBarExtensionDto(
    val rightText: String? = null,
    val menuItems: List<MenuItemDto> = emptyList()
)

data class MenuItemDto(
    val id: String,
    val label: String
)

data class FabExtensionDto(
    val id: String,
    val icon: String? = null,
    val label: String
)

enum class EntryType(val value: String) {
    Native("native"),
    Web("web"),
    External("external"),
    Hybrid("hybrid"),
    Unknown("unknown");

    companion object {
        fun from(value: String): EntryType =
            entries.firstOrNull { it.value == value } ?: Unknown
    }
}