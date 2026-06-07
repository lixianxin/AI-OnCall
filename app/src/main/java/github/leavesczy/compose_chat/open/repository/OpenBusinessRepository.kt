package github.leavesczy.compose_chat.open.repository

import github.leavesczy.compose_chat.open.model.ApprovalItemDto
import github.leavesczy.compose_chat.open.model.ApprovalSummaryResponse
import github.leavesczy.compose_chat.open.model.CalendarEventDto
import github.leavesczy.compose_chat.open.model.CalendarSummaryResponse
import github.leavesczy.compose_chat.open.model.DebugPermissionDto
import github.leavesczy.compose_chat.open.model.TabManifest
import github.leavesczy.compose_chat.open.network.OpenApiClient
import github.leavesczy.compose_chat.open.network.OpenApiResult
import github.leavesczy.compose_chat.open.network.OpenJsonParser

class OpenBusinessRepository(
    private val apiClient: OpenApiClient = OpenApiClient()
) {

    suspend fun approvalSummary(): OpenApiResult<ApprovalSummaryResponse> {
        return apiClient.get(path = "/business/approval/summary").map(OpenJsonParser::parseApprovalSummary)
    }

    suspend fun approvalItems(status: String = "all"): OpenApiResult<List<ApprovalItemDto>> {
        return apiClient.get(path = "/business/approval/items?status=$status").map(OpenJsonParser::parseApprovalItems)
    }

    suspend fun approvalDetail(itemId: String): OpenApiResult<ApprovalItemDto> {
        return apiClient.get(path = "/business/approval/items/$itemId").map(OpenJsonParser::parseApprovalItem)
    }

    suspend fun calendarSummary(): OpenApiResult<CalendarSummaryResponse> {
        return apiClient.get(path = "/business/calendar/summary").map(OpenJsonParser::parseCalendarSummary)
    }

    suspend fun calendarEvents(date: String): OpenApiResult<List<CalendarEventDto>> {
        return apiClient.get(path = "/business/calendar/events?date=$date").map(OpenJsonParser::parseCalendarEvents)
    }

    suspend fun calendarDetail(eventId: String): OpenApiResult<CalendarEventDto> {
        return apiClient.get(path = "/business/calendar/events/$eventId").map(OpenJsonParser::parseCalendarEvent)
    }

    suspend fun debugPermissions(): OpenApiResult<List<DebugPermissionDto>> {
        return apiClient.get(path = "/debug/permissions").map(OpenJsonParser::parseDebugPermissions)
    }

    suspend fun debugSampleTabs(): OpenApiResult<List<TabManifest>> {
        return apiClient.get(path = "/debug/sample-tabs").map(OpenJsonParser::parseTabs)
    }

}
