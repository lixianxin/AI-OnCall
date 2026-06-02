package github.leavesczy.compose_chat.open.repository

import github.leavesczy.compose_chat.open.model.MeResponse
import github.leavesczy.compose_chat.open.model.OpenAnnouncementItem
import github.leavesczy.compose_chat.open.model.OpenAnnouncementScope
import github.leavesczy.compose_chat.open.model.OpenApprovalItem
import github.leavesczy.compose_chat.open.model.OpenCalendarEvent
import github.leavesczy.compose_chat.open.model.OpenCalendarVisibility
import github.leavesczy.compose_chat.open.model.OpenTeamDto
import github.leavesczy.compose_chat.open.model.OpenTeamMemberDto
import github.leavesczy.compose_chat.open.model.OpenTeamProfile
import github.leavesczy.compose_chat.open.network.OpenApiClient
import github.leavesczy.compose_chat.open.network.OpenApiResult
import github.leavesczy.compose_chat.open.network.OpenJsonParser
import github.leavesczy.compose_chat.open.session.OpenSessionManager
import org.json.JSONArray
import org.json.JSONObject

class OpenTeamBusinessRepository(
    private val apiClient: OpenApiClient = OpenApiClient(),
    private val sessionManager: OpenSessionManager = OpenSessionManager
) {

    suspend fun currentProfile(): OpenApiResult<OpenTeamProfile> {
        return apiClient.get(path = "/me").map { json ->
            OpenJsonParser.parseMe(json).toTeamProfile(account = sessionManager.lastAccount)
        }
    }

    suspend fun teams(): OpenApiResult<List<OpenTeamDto>> {
        return apiClient.get(path = "/admin/teams").map(OpenJsonParser::parseOpenTeams)
    }

    suspend fun createTeam(
        teamName: String,
        description: String
    ): OpenApiResult<OpenTeamDto> {
        return apiClient.postJson(
            path = "/admin/teams",
            json = JSONObject()
                .put("teamName", teamName)
                .put("description", description)
        ).map(OpenJsonParser::parseOpenTeam)
    }

    suspend fun teamMembers(teamId: String): OpenApiResult<List<OpenTeamMemberDto>> {
        return apiClient.get(path = "/admin/teams/$teamId/members").map(OpenJsonParser::parseOpenTeamMembers)
    }

    suspend fun updateTeamMemberRole(
        teamId: String,
        userId: String,
        teamRole: String
    ): OpenApiResult<Boolean> {
        return apiClient.putJson(
            path = "/admin/teams/$teamId/members/$userId",
            json = JSONObject().put("teamRole", teamRole)
        ).map { true }
    }

    suspend fun addTeamMember(
        teamId: String,
        userId: String,
        teamRole: String
    ): OpenApiResult<Boolean> {
        return apiClient.postJson(
            path = "/admin/teams/$teamId/members",
            json = JSONObject()
                .put("userId", userId)
                .put("teamRole", teamRole)
        ).map { true }
    }

    suspend fun approvalItems(scope: String): OpenApiResult<List<OpenApprovalItem>> {
        return apiClient.get(path = "/business/approval/items?scope=$scope")
            .map(OpenJsonParser::parseOpenApprovalItems)
    }

    suspend fun approvalDetail(approvalId: String): OpenApiResult<OpenApprovalItem> {
        return apiClient.get(path = "/business/approval/items/$approvalId")
            .map(OpenJsonParser::parseOpenApprovalItem)
    }

    suspend fun createApproval(
        teamId: String?,
        type: String,
        title: String,
        reason: String,
        form: Map<String, String>
    ): OpenApiResult<OpenApprovalItem> {
        val body = JSONObject()
            .put("type", type)
            .put("title", title)
            .put("reason", reason)
            .put("form", JSONObject(form))
        if (!teamId.isNullOrBlank()) {
            body.put("teamId", teamId)
        }
        return apiClient.postJson(path = "/business/approval/items", json = body)
            .map(OpenJsonParser::parseOpenApprovalItem)
    }

    suspend fun approveApproval(approvalId: String, comment: String): OpenApiResult<OpenApprovalItem> {
        return updateApproval(path = "/business/approval/items/$approvalId/approve", comment = comment)
    }

    suspend fun rejectApproval(approvalId: String, comment: String): OpenApiResult<OpenApprovalItem> {
        return updateApproval(path = "/business/approval/items/$approvalId/reject", comment = comment)
    }

    suspend fun cancelApproval(approvalId: String, comment: String): OpenApiResult<OpenApprovalItem> {
        return updateApproval(path = "/business/approval/items/$approvalId/cancel", comment = comment)
    }

    suspend fun calendarEvents(scope: String): OpenApiResult<List<OpenCalendarEvent>> {
        return apiClient.get(path = "/business/calendar/events?scope=$scope")
            .map(OpenJsonParser::parseOpenCalendarEvents)
    }

    suspend fun calendarDetail(eventId: String): OpenApiResult<OpenCalendarEvent> {
        return apiClient.get(path = "/business/calendar/events/$eventId")
            .map(OpenJsonParser::parseOpenCalendarEvent)
    }

    suspend fun createCalendarEvent(
        teamId: String?,
        title: String,
        description: String,
        startTime: String,
        endTime: String,
        location: String,
        visibility: String,
        participantIds: List<String>
    ): OpenApiResult<OpenCalendarEvent> {
        val body = calendarBody(
            teamId = teamId,
            title = title,
            description = description,
            startTime = startTime,
            endTime = endTime,
            location = location,
            visibility = visibility,
            participantIds = participantIds
        )
        return apiClient.postJson(path = "/business/calendar/events", json = body)
            .map(OpenJsonParser::parseOpenCalendarEvent)
    }

    suspend fun updateCalendarEvent(
        eventId: String,
        teamId: String?,
        title: String,
        description: String,
        startTime: String,
        endTime: String,
        location: String,
        visibility: String,
        participantIds: List<String>
    ): OpenApiResult<OpenCalendarEvent> {
        val body = calendarBody(
            teamId = teamId,
            title = title,
            description = description,
            startTime = startTime,
            endTime = endTime,
            location = location,
            visibility = visibility,
            participantIds = participantIds
        )
        return apiClient.putJson(path = "/business/calendar/events/$eventId", json = body)
            .map(OpenJsonParser::parseOpenCalendarEvent)
    }

    suspend fun deleteCalendarEvent(eventId: String): OpenApiResult<Boolean> {
        return apiClient.delete(path = "/business/calendar/events/$eventId").map { true }
    }

    suspend fun announcements(scope: String = "visible"): OpenApiResult<List<OpenAnnouncementItem>> {
        return apiClient.get(path = "/business/announcements?scope=$scope")
            .map(OpenJsonParser::parseOpenAnnouncements)
    }

    suspend fun announcementDetail(announcementId: String): OpenApiResult<OpenAnnouncementItem> {
        return apiClient.get(path = "/business/announcements/$announcementId")
            .map(OpenJsonParser::parseOpenAnnouncement)
    }

    suspend fun createAnnouncement(
        teamId: String?,
        scope: String,
        title: String,
        content: String,
        pinned: Boolean
    ): OpenApiResult<OpenAnnouncementItem> {
        return apiClient.postJson(
            path = "/business/announcements",
            json = announcementBody(
                teamId = teamId,
                scope = scope,
                title = title,
                content = content,
                pinned = pinned
            )
        ).map(OpenJsonParser::parseOpenAnnouncement)
    }

    suspend fun updateAnnouncement(
        announcementId: String,
        title: String,
        content: String,
        pinned: Boolean
    ): OpenApiResult<OpenAnnouncementItem> {
        return apiClient.putJson(
            path = "/business/announcements/$announcementId",
            json = JSONObject()
                .put("title", title)
                .put("content", content)
                .put("pinned", pinned)
        ).map(OpenJsonParser::parseOpenAnnouncement)
    }

    suspend fun deleteAnnouncement(announcementId: String): OpenApiResult<Boolean> {
        return apiClient.delete(path = "/business/announcements/$announcementId").map { true }
    }

    private suspend fun updateApproval(path: String, comment: String): OpenApiResult<OpenApprovalItem> {
        return apiClient.postJson(path = path, json = JSONObject().put("comment", comment))
            .map(OpenJsonParser::parseOpenApprovalItem)
    }

    private fun calendarBody(
        teamId: String?,
        title: String,
        description: String,
        startTime: String,
        endTime: String,
        location: String,
        visibility: String,
        participantIds: List<String>
    ): JSONObject {
        val body = JSONObject()
            .put("title", title)
            .put("description", description)
            .put("startTime", startTime)
            .put("endTime", endTime)
            .put("location", location)
            .put("visibility", visibility.ifBlank { OpenCalendarVisibility.Team })
            .put("participantIds", JSONArray(participantIds))
        if (!teamId.isNullOrBlank()) {
            body.put("teamId", teamId)
        }
        return body
    }

    private fun announcementBody(
        teamId: String?,
        scope: String,
        title: String,
        content: String,
        pinned: Boolean
    ): JSONObject {
        val body = JSONObject()
            .put("scope", scope.ifBlank { OpenAnnouncementScope.Team })
            .put("title", title)
            .put("content", content)
            .put("pinned", pinned)
        if (!teamId.isNullOrBlank()) {
            body.put("teamId", teamId)
        }
        return body
    }

}

fun MeResponse.toTeamProfile(account: String): OpenTeamProfile {
    return OpenTeamProfile(
        userId = userId,
        account = account,
        displayName = displayName,
        globalRole = globalRole,
        currentTeamId = currentTeamId,
        memberships = memberships,
        permissions = permissions
    )
}
