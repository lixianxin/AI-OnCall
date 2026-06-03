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
    val tabCount: Int
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
    val createdAt: String
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
