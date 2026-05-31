package github.leavesczy.compose_chat.open.repository

import github.leavesczy.compose_chat.open.model.ApprovalSummaryResponse
import github.leavesczy.compose_chat.open.network.OpenApiClient
import github.leavesczy.compose_chat.open.network.OpenApiResult
import github.leavesczy.compose_chat.open.network.OpenJsonParser

class OpenBusinessRepository(
    private val apiClient: OpenApiClient = OpenApiClient()
) {

    suspend fun approvalSummary(): OpenApiResult<ApprovalSummaryResponse> {
        return apiClient.get(path = "/business/approval/summary").map(OpenJsonParser::parseApprovalSummary)
    }

}
