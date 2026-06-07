package github.leavesczy.compose_chat.open.network

import github.leavesczy.compose_chat.open.model.ApprovalItemDto
import github.leavesczy.compose_chat.open.model.ApprovalSummaryResponse
import github.leavesczy.compose_chat.open.model.CalendarEventDto
import github.leavesczy.compose_chat.open.model.CalendarSummaryResponse
import github.leavesczy.compose_chat.open.model.DebugDatabaseDto
import github.leavesczy.compose_chat.open.model.DebugPermissionDto
import github.leavesczy.compose_chat.open.model.DebugStatusResponse
import github.leavesczy.compose_chat.open.model.EntryType
import github.leavesczy.compose_chat.open.model.FabExtensionDto
import github.leavesczy.compose_chat.open.model.HealthResponse
import github.leavesczy.compose_chat.open.model.LoginResponse
import github.leavesczy.compose_chat.open.model.MeResponse
import github.leavesczy.compose_chat.open.model.OnCallMessageDto
import github.leavesczy.compose_chat.open.model.OnCallSessionDto
import github.leavesczy.compose_chat.open.model.MenuItemDto
import github.leavesczy.compose_chat.open.model.OnCallToolEvent
import github.leavesczy.compose_chat.open.model.OpenAnnouncementItem
import github.leavesczy.compose_chat.open.model.OpenAnnouncementScope
import github.leavesczy.compose_chat.open.model.OpenApprovalItem
import github.leavesczy.compose_chat.open.model.OpenCalendarEvent
import github.leavesczy.compose_chat.open.model.OpenCalendarParticipant
import github.leavesczy.compose_chat.open.model.OpenCalendarVisibility
import github.leavesczy.compose_chat.open.model.OpenTeamDto
import github.leavesczy.compose_chat.open.model.OpenTeamMemberDto
import github.leavesczy.compose_chat.open.model.OpenTeamMembership
import github.leavesczy.compose_chat.open.model.SemanticVersionDto
import github.leavesczy.compose_chat.open.model.SuccessResponse
import github.leavesczy.compose_chat.open.model.TabExtensionDto
import github.leavesczy.compose_chat.open.model.TabManifest
import github.leavesczy.compose_chat.open.model.TabMutationResponse
import github.leavesczy.compose_chat.open.model.TabVisibilityRequest
import github.leavesczy.compose_chat.open.model.TeamDto
import github.leavesczy.compose_chat.open.model.TitleBarExtensionDto
import org.json.JSONArray
import org.json.JSONObject

object OpenJsonParser {

    fun parseHealth(json: String): HealthResponse {
        val obj = JSONObject(json)
        return HealthResponse(
            status = obj.optString("status"),
            service = obj.optString("service"),
            mode = obj.optString("mode"),
            serverTime = obj.optString("serverTime")
        )
    }

    fun parseLogin(json: String): LoginResponse {
        val obj = JSONObject(json)
        return LoginResponse(
            token = obj.optString("token"),
            userId = obj.optString("userId").ifBlank { null },
            displayName = obj.optString("displayName"),
            permissions = obj.optJSONArray("permissions").toStringList()
        )
    }

    fun parseMe(json: String): MeResponse {
        val obj = JSONObject(json)
        val teamObj = obj.optJSONObject("team")
        return MeResponse(
            userId = obj.optString("userId"),
            displayName = obj.optString("displayName"),
            globalRole = obj.optString("globalRole").ifBlank { null },
            currentTeamId = obj.optString("currentTeamId").ifBlank { null },
            memberships = obj.optJSONArray("memberships").mapObjects(::parseTeamMembership),
            permissions = obj.optJSONArray("permissions").toStringList(),
            team = teamObj?.let {
                TeamDto(
                    id = it.optString("id"),
                    name = it.optString("name")
                )
            }
        )
    }

    fun parseDebugStatus(json: String): DebugStatusResponse {
        val obj = JSONObject(json)
        val databaseObj = obj.optJSONObject("database")
        return DebugStatusResponse(
            serverTime = obj.optString("serverTime"),
            apiVersion = obj.optString("apiVersion"),
            mockMode = obj.optBoolean("mockMode"),
            sseAvailable = obj.optBoolean("sseAvailable"),
            tabCount = obj.optInt("tabCount"),
            database = databaseObj?.let {
                DebugDatabaseDto(
                    enabled = it.optBoolean("enabled"),
                    type = it.optString("type")
                )
            }
        )
    }

    fun parseApprovalSummary(json: String): ApprovalSummaryResponse {
        val obj = JSONObject(json)
        val items = obj.optJSONArray("items").mapObjects(::parseApprovalItem)
        return ApprovalSummaryResponse(
            pendingCount = obj.optInt("pendingCount"),
            approvedToday = obj.optInt("approvedToday"),
            items = items
        )
    }

    fun parseApprovalItems(json: String): List<ApprovalItemDto> {
        return JSONArray(json).mapObjects(::parseApprovalItem)
    }

    fun parseApprovalItem(json: String): ApprovalItemDto {
        return parseApprovalItem(JSONObject(json))
    }

    fun parseCalendarSummary(json: String): CalendarSummaryResponse {
        val obj = JSONObject(json)
        return CalendarSummaryResponse(
            todayCount = obj.optInt("todayCount"),
            events = obj.optJSONArray("events").mapObjects(::parseCalendarEvent)
        )
    }

    fun parseCalendarEvents(json: String): List<CalendarEventDto> {
        return JSONArray(json).mapObjects(::parseCalendarEvent)
    }

    fun parseCalendarEvent(json: String): CalendarEventDto {
        return parseCalendarEvent(JSONObject(json))
    }

    fun parseOpenTeams(json: String): List<OpenTeamDto> {
        return JSONArray(json).mapObjects(::parseOpenTeam)
    }

    fun parseOpenTeam(json: String): OpenTeamDto {
        val obj = JSONObject(json)
        return parseOpenTeam(obj.optJSONObject("team") ?: obj.optJSONObject("data") ?: obj)
    }

    fun parseOpenTeamMembers(json: String): List<OpenTeamMemberDto> {
        return JSONArray(json).mapObjects(::parseOpenTeamMember)
    }

    fun parseOpenTeamMember(json: String): OpenTeamMemberDto {
        return parseOpenTeamMember(JSONObject(json))
    }

    fun parseOpenApprovalItems(json: String): List<OpenApprovalItem> {
        return JSONArray(json).mapObjects(::parseOpenApprovalItem)
    }

    fun parseOpenApprovalItem(json: String): OpenApprovalItem {
        return parseOpenApprovalItem(JSONObject(json))
    }

    fun parseOpenCalendarEvents(json: String): List<OpenCalendarEvent> {
        return JSONArray(json).mapObjects(::parseOpenCalendarEvent)
    }

    fun parseOpenCalendarEvent(json: String): OpenCalendarEvent {
        return parseOpenCalendarEvent(JSONObject(json))
    }

    fun parseOpenAnnouncements(json: String): List<OpenAnnouncementItem> {
        return JSONArray(json).mapObjects(::parseOpenAnnouncement)
    }

    fun parseOpenAnnouncement(json: String): OpenAnnouncementItem {
        return parseOpenAnnouncement(JSONObject(json))
    }

    fun parseDebugPermissions(json: String): List<DebugPermissionDto> {
        return JSONArray(json).mapObjects { item ->
            DebugPermissionDto(
                code = item.optString("code"),
                description = item.optString("description")
            )
        }
    }

    fun parseTabs(json: String): List<TabManifest> {
        return JSONArray(json).mapObjects(::parseTabManifest)
    }

    fun parseSuccess(json: String): SuccessResponse {
        val obj = JSONObject(json)
        return SuccessResponse(
            success = obj.optBoolean("success"),
            tabId = obj.optString("tabId").ifBlank { null }
        )
    }

    fun parseTabMutation(json: String): TabMutationResponse {
        val obj = JSONObject(json)
        return TabMutationResponse(
            success = obj.optBoolean("success"),
            tabId = obj.optString("tabId").ifBlank { null },
            tab = obj.optJSONObject("tab")?.let(::parseTabManifest)
        )
    }

    fun parseOnCallDelta(data: String): String {
        return JSONObject(data).optString("text")
    }

    fun parseOnCallTool(data: String): OnCallToolEvent {
        val obj = JSONObject(data)
        return OnCallToolEvent(
            name = obj.optString("name").ifBlank { "tool" },
            status = obj.optString("status"),
            summary = obj.optString("summary")
        )
    }

    fun parseOnCallDoneMessageId(data: String): String? {
        if (data.isBlank() || data == "{}") {
            return null
        }
        return JSONObject(data).optString("messageId").ifBlank { null }
    }

    fun parseOnCallSessions(json: String): List<OnCallSessionDto> {
        return JSONArray(json).mapObjects(::parseOnCallSession)
    }

    fun parseOnCallSession(json: String): OnCallSessionDto {
        return parseOnCallSession(JSONObject(json))
    }

    fun parseOnCallMessages(json: String): List<OnCallMessageDto> {
        return JSONArray(json).mapObjects(::parseOnCallMessage)
    }

    fun parseOnCallMessage(json: String): OnCallMessageDto {
        return parseOnCallMessage(JSONObject(json))
    }

    fun parseTabManifest(obj: JSONObject): TabManifest {
        return TabManifest(
            id = obj.optString("id"),
            displayName = obj.optString("displayName"),
            description = obj.optString("description").ifBlank { null },
            icon = obj.optString("icon").ifBlank { null },
            route = obj.optString("route"),
            entryType = EntryType.from(obj.optString("entryType")),
            entryUri = obj.optString("entryUri").ifBlank { null },
            version = parseVersion(obj.optJSONObject("version")),
            minContainerVersion = obj.optInt("minContainerVersion", defaultMinContainerVersion),
            permissions = obj.optJSONArray("permissions").toStringList(),
            enabled = obj.optBoolean("enabled", true),
            sortOrder = obj.optInt("sortOrder", Int.MAX_VALUE),
            extension = parseExtension(obj.optJSONObject("extension")),
            extraConfig = obj.optJSONObject("extraConfig").toStringMap(),
            visibility = parseVisibility(obj.optJSONObject("visibility"))
        )
    }

    private fun parseVisibility(obj: JSONObject?): TabVisibilityRequest? {
        obj ?: return null
        return TabVisibilityRequest(
            scope = obj.optString("scope").ifBlank { "self" },
            teamIds = obj.optJSONArray("teamIds").toStringList(),
            userIds = obj.optJSONArray("userIds").toStringList(),
            defaultEnabled = obj.optBoolean("defaultEnabled", true)
        )
    }

    private fun parseApprovalItem(item: JSONObject): ApprovalItemDto {
        return ApprovalItemDto(
            id = item.optString("id"),
            title = item.optString("title"),
            applicant = item.optString("applicant"),
            status = item.optString("status"),
            createdAt = item.optString("createdAt"),
            amount = item.optNullableInt("amount"),
            reason = item.optString("reason").ifBlank { null },
            comment = item.optString("comment").ifBlank { null },
            updatedAt = item.optString("updatedAt").ifBlank { null }
        )
    }

    private fun parseCalendarEvent(item: JSONObject): CalendarEventDto {
        return CalendarEventDto(
            id = item.optString("id"),
            title = item.optString("title"),
            description = item.optString("description").ifBlank { null },
            startTime = item.optString("startTime"),
            endTime = item.optString("endTime"),
            location = item.optString("location").ifBlank { null },
            participants = item.optJSONArray("participants").toStringList()
        )
    }

    private fun parseOpenTeam(item: JSONObject): OpenTeamDto {
        return OpenTeamDto(
            teamId = item.optString("teamId").ifBlank { item.optString("id") },
            teamName = item.optString("teamName").ifBlank { item.optString("name") },
            description = item.optString("description"),
            memberCount = item.optInt("memberCount"),
            managerCount = item.optInt("managerCount"),
            enabled = item.optBoolean("enabled", true),
            createdAt = item.optString("createdAt"),
            updatedAt = item.optString("updatedAt")
        )
    }

    private fun parseTeamMembership(item: JSONObject): OpenTeamMembership {
        return OpenTeamMembership(
            teamId = item.optString("teamId"),
            teamName = item.optString("teamName"),
            teamRole = item.optString("teamRole")
        )
    }

    private fun parseOpenTeamMember(item: JSONObject): OpenTeamMemberDto {
        return OpenTeamMemberDto(
            userId = item.optString("userId"),
            account = item.optString("account"),
            displayName = item.optString("displayName"),
            teamId = item.optString("teamId"),
            teamName = item.optString("teamName"),
            teamRole = item.optString("teamRole"),
            joinedAt = item.optString("joinedAt"),
            enabled = item.optBoolean("enabled", true)
        )
    }

    private fun parseOpenApprovalItem(item: JSONObject): OpenApprovalItem {
        val form = item.optJSONObject("form").toStringMap()
        return OpenApprovalItem(
            id = item.optString("id"),
            teamId = item.optString("teamId"),
            teamName = item.optString("teamName"),
            type = item.optString("type").ifBlank { "leave" },
            title = item.optString("title"),
            applicantId = item.optString("applicantId"),
            applicant = item.displayNameOf(
                idKey = "applicantId",
                primaryKeys = arrayOf("applicantName", "applicant", "creatorName", "creator")
            ),
            approverId = item.optString("approverId"),
            approver = item.displayNameOf(
                idKey = "approverId",
                primaryKeys = arrayOf("approverName", "approver", "managerName", "manager")
            ),
            status = item.optString("status"),
            form = form,
            reason = item.optString("reason").ifBlank { item.optString("summary") },
            comment = item.optString("comment").ifBlank { null },
            createdAt = item.optString("createdAt"),
            updatedAt = item.optString("updatedAt").ifBlank { null }
        )
    }

    private fun parseOpenCalendarEvent(item: JSONObject): OpenCalendarEvent {
        val participants = item.optJSONArray("participants").toCalendarParticipants()
        val participantIds = item.optJSONArray("participantIds").toStringList()
            .ifEmpty { participants.map { participant -> participant.userId } }
        return OpenCalendarEvent(
            eventId = item.optString("id").ifBlank { item.optString("eventId") },
            teamId = item.optString("teamId").ifBlank { null },
            teamName = item.optString("teamName").ifBlank { null },
            title = item.optString("title"),
            description = item.optString("description"),
            startTime = item.optString("startTime"),
            endTime = item.optString("endTime"),
            location = item.optString("location").ifBlank { null },
            visibility = item.optString("visibility").ifBlank { OpenCalendarVisibility.Team },
            creatorId = item.optString("creatorId"),
            creator = item.displayNameOf(
                idKey = "creatorId",
                primaryKeys = arrayOf("creatorName", "creator", "publisherName", "publisher")
            ),
            participantIds = participantIds,
            participants = participants
        )
    }

    private fun parseOpenAnnouncement(item: JSONObject): OpenAnnouncementItem {
        return OpenAnnouncementItem(
            announcementId = item.optString("id").ifBlank { item.optString("announcementId") },
            scope = item.optString("scope").ifBlank { OpenAnnouncementScope.Team },
            teamId = item.optString("teamId").ifBlank { null },
            teamName = item.optString("teamName").ifBlank { null },
            title = item.optString("title"),
            content = item.optString("content"),
            publisherId = item.optString("publisherId"),
            publisher = item.displayNameOf(
                idKey = "publisherId",
                primaryKeys = arrayOf("publisherName", "publisher", "creatorName", "creator")
            ),
            pinned = item.optBoolean("pinned"),
            createdAt = item.optString("createdAt"),
            updatedAt = item.optString("updatedAt").ifBlank { null }
        )
    }

    private fun parseOnCallSession(item: JSONObject): OnCallSessionDto {
        return OnCallSessionDto(
            sessionId = item.optString("sessionId"),
            title = item.optString("title"),
            createdAt = item.optString("createdAt"),
            updatedAt = item.optString("updatedAt").ifBlank { null },
            messageCount = item.optNullableInt("messageCount")
        )
    }

    private fun parseOnCallMessage(item: JSONObject): OnCallMessageDto {
        return OnCallMessageDto(
            messageId = item.optString("messageId"),
            sessionId = item.optString("sessionId"),
            role = item.optString("role"),
            content = item.optString("content"),
            contentType = item.optString("contentType"),
            createdAt = item.optString("createdAt")
        )
    }

    fun parseError(json: String?): OpenApiResult.Failed {
        if (json.isNullOrBlank()) {
            return OpenApiResult.Failed(code = "NETWORK_ERROR", message = "Empty error body")
        }
        if (!json.trimStart().startsWith("{")) {
            return OpenApiResult.Failed(code = "HTTP_ERROR", message = json.trim())
        }
        return runCatching {
            val obj = JSONObject(json)
            OpenApiResult.Failed(
                code = obj.optString("code", "UNKNOWN_ERROR"),
                message = obj.optString("message", "Unknown error"),
                traceId = obj.optString("traceId").ifBlank { null }
            )
        }.getOrElse { error ->
            OpenApiResult.Failed(code = "UNKNOWN_ERROR", message = error.message ?: "Unknown error")
        }
    }

    private fun parseVersion(obj: JSONObject?): SemanticVersionDto {
        return SemanticVersionDto(
            major = obj?.optInt("major") ?: 1,
            minor = obj?.optInt("minor") ?: 0,
            patch = obj?.optInt("patch") ?: 0
        )
    }

    private fun parseExtension(obj: JSONObject?): TabExtensionDto? {
        if (obj == null) {
            return null
        }
        return TabExtensionDto(
            titleBar = parseTitleBar(obj.optJSONObject("titleBar")),
            fab = parseFab(obj.optJSONObject("fab"))
        )
    }

    private fun parseTitleBar(obj: JSONObject?): TitleBarExtensionDto? {
        if (obj == null) {
            return null
        }
        return TitleBarExtensionDto(
            rightText = obj.optString("rightText").ifBlank { null },
            menuItems = obj.optJSONArray("menuItems").mapObjects { item ->
                MenuItemDto(
                    id = item.optString("id"),
                    label = item.optString("label")
                )
            }
        )
    }

    private fun parseFab(obj: JSONObject?): FabExtensionDto? {
        if (obj == null) {
            return null
        }
        return FabExtensionDto(
            id = obj.optString("id"),
            icon = obj.optString("icon").ifBlank { null },
            label = obj.optString("label")
        )
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) {
            return emptyList()
        }
        return List(length()) { index ->
            val value = opt(index)
            if (value is JSONObject) {
                value.optString("userId").ifBlank {
                    value.optString("displayName").ifBlank { value.toString() }
                }
            } else {
                optString(index)
            }
        }.filter { it.isNotBlank() }
    }

    private fun JSONArray?.toCalendarParticipants(): List<OpenCalendarParticipant> {
        if (this == null) {
            return emptyList()
        }
        return List(length()) { index ->
            val value = opt(index)
            if (value is JSONObject) {
                OpenCalendarParticipant(
                    userId = value.optString("userId").ifBlank { value.optString("id") },
                    displayName = value.optString("displayName").ifBlank {
                        value.optString("name").ifBlank { value.optString("userId") }
                    }
                )
            } else {
                val text = optString(index)
                OpenCalendarParticipant(userId = text, displayName = text)
            }
        }.filter { participant -> participant.userId.isNotBlank() || participant.displayName.isNotBlank() }
    }

    private fun JSONObject?.toStringMap(): Map<String, String> {
        if (this == null) {
            return emptyMap()
        }
        return keys().asSequence().associateWith { key ->
            opt(key)?.toString().orEmpty()
        }
    }

    private fun <T> JSONArray?.mapObjects(mapper: (JSONObject) -> T): List<T> {
        if (this == null) {
            return emptyList()
        }
        return List(length()) { index ->
            mapper(optJSONObject(index) ?: JSONObject())
        }
    }

    private fun JSONObject.optNullableInt(name: String): Int? {
        return if (has(name) && !isNull(name)) {
            optInt(name)
        } else {
            null
        }
    }

    private fun JSONObject.displayNameOf(idKey: String, primaryKeys: Array<String>): String {
        primaryKeys.forEach { key ->
            optString(key).cleanDisplayName()?.let { name ->
                return name
            }
        }
        return optString(idKey).cleanDisplayName() ?: "-"
    }

    private fun String.cleanDisplayName(): String? {
        val value = trim()
        if (value.isBlank() || value == "?" || value == "??" || value == "null") {
            return null
        }
        return value
    }

    private const val defaultMinContainerVersion = 1

}
