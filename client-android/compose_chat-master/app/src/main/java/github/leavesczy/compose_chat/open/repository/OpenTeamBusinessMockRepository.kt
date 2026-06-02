package github.leavesczy.compose_chat.open.repository

import github.leavesczy.compose_chat.open.model.OpenAnnouncementItem
import github.leavesczy.compose_chat.open.model.OpenAnnouncementScope
import github.leavesczy.compose_chat.open.model.OpenApprovalItem
import github.leavesczy.compose_chat.open.model.OpenApprovalStatus
import github.leavesczy.compose_chat.open.model.OpenApprovalTypes
import github.leavesczy.compose_chat.open.model.OpenBusinessPermissions
import github.leavesczy.compose_chat.open.model.OpenCalendarEvent
import github.leavesczy.compose_chat.open.model.OpenCalendarParticipant
import github.leavesczy.compose_chat.open.model.OpenCalendarVisibility
import github.leavesczy.compose_chat.open.model.OpenTeamDto
import github.leavesczy.compose_chat.open.model.OpenTeamMemberDto
import github.leavesczy.compose_chat.open.model.OpenTeamMembership
import github.leavesczy.compose_chat.open.model.OpenTeamProfile
import github.leavesczy.compose_chat.open.model.OpenTeamRoles
import java.time.OffsetDateTime
import java.time.ZoneOffset

class OpenTeamBusinessMockRepository {

    fun currentProfile(account: String?): OpenTeamProfile {
        val normalizedAccount = when (account.orEmpty().ifBlank { "product-employee" }) {
            "opentab-admin" -> "admin"
            "opentab-demo" -> "product-employee"
            "opentab-guest" -> "operation-employee"
            else -> account.orEmpty().ifBlank { "product-employee" }
        }
        return profiles.firstOrNull { profile ->
            profile.account == normalizedAccount
        } ?: profiles.first { profile -> profile.account == "product-employee" }
    }

    fun profiles(): List<OpenTeamProfile> {
        return profiles
    }

    fun teams(profile: OpenTeamProfile): List<OpenTeamDto> {
        return if (OpenBusinessPermissionRules.isAdmin(profile)) {
            teams
        } else {
            teams.filter { team -> OpenBusinessPermissionRules.belongsToTeam(profile, team.teamId) }
        }
    }

    fun teamMembers(profile: OpenTeamProfile, teamId: String): List<OpenTeamMemberDto> {
        if (!OpenBusinessPermissionRules.canReadTeamMembers(profile, teamId)) {
            return emptyList()
        }
        return members.filter { member -> member.teamId == teamId && member.enabled }
    }

    fun approvalItems(
        profile: OpenTeamProfile,
        scope: String = "visible",
        teamId: String? = null
    ): List<OpenApprovalItem> {
        val source = approvals.filter { approval ->
            teamId == null || approval.teamId == teamId
        }
        return source.filter { approval ->
            when (scope) {
                "mine" -> approval.applicantId == profile.userId
                "pending" -> approval.status == OpenApprovalStatus.Pending &&
                    OpenBusinessPermissionRules.canApprove(profile, approval)
                "all" -> OpenBusinessPermissionRules.isAdmin(profile) &&
                    OpenBusinessPermissionRules.canViewApproval(profile, approval)
                else -> OpenBusinessPermissionRules.canViewApproval(profile, approval)
            }
        }.sortedByDescending { item -> item.createdAt }
    }

    fun approvalDetail(profile: OpenTeamProfile, approvalId: String): OpenApprovalItem? {
        return approvals.firstOrNull { approval -> approval.id == approvalId }
            ?.takeIf { approval -> OpenBusinessPermissionRules.canViewApproval(profile, approval) }
    }

    fun createApproval(
        profile: OpenTeamProfile,
        type: String,
        title: String,
        reason: String,
        form: Map<String, String>
    ): OpenApprovalItem? {
        val teamId = profile.currentTeamId ?: if (OpenBusinessPermissionRules.isAdmin(profile)) {
            "team-product"
        } else {
            return null
        }
        if (!OpenBusinessPermissionRules.canCreateApproval(profile = profile, teamId = teamId)) {
            return null
        }
        val approver = managerProfile(teamId = teamId) ?: return null
        val now = nowText()
        val item = OpenApprovalItem(
            id = "approval-${teamId}-${System.currentTimeMillis()}",
            teamId = teamId,
            teamName = teamName(teamId = teamId),
            type = type,
            title = title.ifBlank { "新的审批申请" },
            applicantId = profile.userId,
            applicant = profile.displayName,
            approverId = approver.userId,
            approver = approver.displayName,
            status = OpenApprovalStatus.Pending,
            form = form,
            reason = reason.ifBlank { "未填写原因" },
            comment = null,
            createdAt = now,
            updatedAt = now
        )
        approvals.add(index = 0, element = item)
        return item
    }

    fun approveApproval(profile: OpenTeamProfile, approvalId: String, comment: String): OpenApprovalItem? {
        return updateApprovalDecision(
            profile = profile,
            approvalId = approvalId,
            status = OpenApprovalStatus.Approved,
            comment = comment.ifBlank { "审批通过" }
        )
    }

    fun rejectApproval(profile: OpenTeamProfile, approvalId: String, comment: String): OpenApprovalItem? {
        return updateApprovalDecision(
            profile = profile,
            approvalId = approvalId,
            status = OpenApprovalStatus.Rejected,
            comment = comment.ifBlank { "审批驳回" }
        )
    }

    fun calendarEvents(
        profile: OpenTeamProfile,
        scope: String = "visible",
        teamId: String? = null
    ): List<OpenCalendarEvent> {
        val source = calendarEvents.filter { event ->
            teamId == null || event.teamId == teamId
        }
        return source.filter { event ->
            when (scope) {
                "mine" -> event.creatorId == profile.userId || event.participantIds.contains(profile.userId)
                "team" -> OpenBusinessPermissionRules.belongsToTeam(profile, event.teamId) ||
                    (OpenBusinessPermissionRules.isAdmin(profile) && event.teamId == teamId)
                "all" -> OpenBusinessPermissionRules.isAdmin(profile)
                else -> OpenBusinessPermissionRules.canViewCalendarEvent(profile, event)
            }
        }.sortedBy { event -> event.startTime }
    }

    fun calendarDetail(profile: OpenTeamProfile, eventId: String): OpenCalendarEvent? {
        return calendarEvents.firstOrNull { event -> event.eventId == eventId }
            ?.takeIf { event -> OpenBusinessPermissionRules.canViewCalendarEvent(profile, event) }
    }

    fun createCalendarEvent(
        profile: OpenTeamProfile,
        title: String,
        description: String,
        startTime: String,
        endTime: String,
        location: String,
        visibility: String,
        teamId: String? = null,
        participantIds: List<String>? = null
    ): OpenCalendarEvent? {
        val targetTeamId = if (visibility == OpenCalendarVisibility.Company) {
            null
        } else {
            teamId ?: profile.currentTeamId ?: if (OpenBusinessPermissionRules.isAdmin(profile)) "team-product" else null
        }
        if (!OpenBusinessPermissionRules.canCreateCalendarEvent(profile = profile, teamId = targetTeamId, visibility = visibility)) {
            return null
        }
        val finalParticipantIds = when (visibility) {
            OpenCalendarVisibility.Company -> profiles.map { it.userId }
            OpenCalendarVisibility.Team -> participantIds?.takeIf { ids -> ids.isNotEmpty() }
                ?: members.filter { member -> member.teamId == targetTeamId }.map { it.userId }
            else -> participantIds?.takeIf { ids -> ids.isNotEmpty() } ?: listOf(profile.userId)
        }.distinct()
        val item = OpenCalendarEvent(
            eventId = "event-${System.currentTimeMillis()}",
            teamId = targetTeamId,
            teamName = targetTeamId?.let(::teamName),
            title = title.ifBlank { "新的团队日程" },
            description = description.ifBlank { "暂无说明" },
            startTime = startTime.ifBlank { nowText() },
            endTime = endTime.ifBlank { nowText() },
            location = location.ifBlank { null },
            visibility = visibility,
            creatorId = profile.userId,
            creator = profile.displayName,
            participantIds = finalParticipantIds,
            participants = finalParticipantIds.mapNotNull(::participantOf)
        )
        calendarEvents.add(element = item)
        return item
    }

    fun updateCalendarEvent(
        profile: OpenTeamProfile,
        eventId: String,
        title: String,
        description: String,
        startTime: String,
        endTime: String,
        location: String,
        participantIds: List<String>? = null
    ): OpenCalendarEvent? {
        val index = calendarEvents.indexOfFirst { event -> event.eventId == eventId }
        if (index == -1) {
            return null
        }
        val old = calendarEvents[index]
        if (!OpenBusinessPermissionRules.canManageCalendarEvent(profile = profile, event = old)) {
            return null
        }
        val finalParticipantIds = participantIds?.takeIf { ids -> ids.isNotEmpty() }?.distinct() ?: old.participantIds
        val updated = old.copy(
            title = title.ifBlank { old.title },
            description = description.ifBlank { old.description },
            startTime = startTime.ifBlank { old.startTime },
            endTime = endTime.ifBlank { old.endTime },
            location = location.ifBlank { old.location },
            participantIds = finalParticipantIds,
            participants = finalParticipantIds.mapNotNull(::participantOf)
        )
        calendarEvents[index] = updated
        return updated
    }

    fun deleteCalendarEvent(profile: OpenTeamProfile, eventId: String): Boolean {
        val event = calendarEvents.firstOrNull { item -> item.eventId == eventId } ?: return false
        if (!OpenBusinessPermissionRules.canManageCalendarEvent(profile = profile, event = event)) {
            return false
        }
        return calendarEvents.remove(event)
    }

    fun announcements(
        profile: OpenTeamProfile,
        scope: String = "visible",
        teamId: String? = null
    ): List<OpenAnnouncementItem> {
        val source = announcements.filter { announcement ->
            teamId == null || announcement.teamId == teamId
        }
        return source.filter { announcement ->
            when (scope) {
                "team" -> announcement.scope == OpenAnnouncementScope.Team &&
                    OpenBusinessPermissionRules.belongsToTeam(profile, announcement.teamId)
                "all" -> OpenBusinessPermissionRules.isAdmin(profile)
                else -> OpenBusinessPermissionRules.canViewAnnouncement(profile, announcement)
            }
        }.sortedByDescending { announcement -> announcement.createdAt }
    }

    fun visibleTabIds(profile: OpenTeamProfile): List<String> {
        return targetTabIds.filter { tabId -> OpenBusinessPermissionRules.canViewTab(profile, tabId) }
    }

    companion object {

        val targetTabIds = listOf(
            "company-intro",
            "announcements",
            "fun",
            "approval",
            "calendar",
            "permission-admin",
            "ai-oncall"
        )

        private val productMembership = OpenTeamMembership(
            teamId = "team-product",
            teamName = "产品研发部",
            teamRole = OpenTeamRoles.Employee
        )

        private val operationMembership = OpenTeamMembership(
            teamId = "team-operation",
            teamName = "运营支持部",
            teamRole = OpenTeamRoles.Employee
        )

        private val profiles = listOf(
            OpenTeamProfile(
                userId = "user-admin",
                account = "admin",
                displayName = "系统管理员",
                globalRole = OpenTeamRoles.Admin,
                currentTeamId = null,
                memberships = emptyList(),
                permissions = listOf(
                    OpenBusinessPermissions.TeamManage,
                    OpenBusinessPermissions.TeamAllRead,
                    OpenBusinessPermissions.AdminManage,
                    OpenBusinessPermissions.DebugRead,
                    OpenBusinessPermissions.CompanyRead,
                    OpenBusinessPermissions.AnnouncementRead,
                    OpenBusinessPermissions.AnnouncementWrite,
                    OpenBusinessPermissions.FunRead,
                    OpenBusinessPermissions.ApprovalRead,
                    OpenBusinessPermissions.ApprovalCreate,
                    OpenBusinessPermissions.ApprovalApprove,
                    OpenBusinessPermissions.ApprovalAll,
                    OpenBusinessPermissions.CalendarRead,
                    OpenBusinessPermissions.CalendarCreate,
                    OpenBusinessPermissions.CalendarManage,
                    OpenBusinessPermissions.CalendarAll,
                    OpenBusinessPermissions.AiOnCall
                )
            ),
            OpenTeamProfile(
                userId = "user-product-manager",
                account = "product-manager",
                displayName = "产品主管",
                globalRole = null,
                currentTeamId = "team-product",
                memberships = listOf(productMembership.copy(teamRole = OpenTeamRoles.Manager)),
                permissions = managerPermissions()
            ),
            OpenTeamProfile(
                userId = "user-product-employee",
                account = "product-employee",
                displayName = "产品员工",
                globalRole = null,
                currentTeamId = "team-product",
                memberships = listOf(productMembership),
                permissions = employeePermissions()
            ),
            OpenTeamProfile(
                userId = "user-operation-manager",
                account = "operation-manager",
                displayName = "运营主管",
                globalRole = null,
                currentTeamId = "team-operation",
                memberships = listOf(operationMembership.copy(teamRole = OpenTeamRoles.Manager)),
                permissions = managerPermissions()
            ),
            OpenTeamProfile(
                userId = "user-operation-employee",
                account = "operation-employee",
                displayName = "运营员工",
                globalRole = null,
                currentTeamId = "team-operation",
                memberships = listOf(operationMembership),
                permissions = employeePermissions()
            )
        )

        private val teams = listOf(
            OpenTeamDto(
                teamId = "team-product",
                teamName = "产品研发部",
                description = "负责产品设计、客户端和服务端联调",
                memberCount = 2,
                managerCount = 1,
                enabled = true,
                createdAt = "2026-06-02T09:00:00+08:00",
                updatedAt = "2026-06-02T09:00:00+08:00"
            ),
            OpenTeamDto(
                teamId = "team-operation",
                teamName = "运营支持部",
                description = "负责运营支持、客户协同和活动执行",
                memberCount = 2,
                managerCount = 1,
                enabled = true,
                createdAt = "2026-06-02T09:00:00+08:00",
                updatedAt = "2026-06-02T09:00:00+08:00"
            )
        )

        private val members = profiles.flatMap { profile ->
            profile.memberships.map { membership ->
                OpenTeamMemberDto(
                    userId = profile.userId,
                    account = profile.account,
                    displayName = profile.displayName,
                    teamId = membership.teamId,
                    teamName = membership.teamName,
                    teamRole = membership.teamRole,
                    joinedAt = "2026-06-02T09:10:00+08:00",
                    enabled = true
                )
            }
        }

        private val approvals = mutableListOf(
            OpenApprovalItem(
                id = "approval-product-leave-001",
                teamId = "team-product",
                teamName = "产品研发部",
                type = OpenApprovalTypes.Leave,
                title = "端午调休请假",
                applicantId = "user-product-employee",
                applicant = "产品员工",
                approverId = "user-product-manager",
                approver = "产品主管",
                status = OpenApprovalStatus.Pending,
                form = mapOf("leaveType" to "事假", "days" to "1", "date" to "2026-06-03"),
                reason = "处理个人事务，提前完成当前迭代任务。",
                comment = null,
                createdAt = "2026-06-02T10:20:00+08:00",
                updatedAt = null
            ),
            OpenApprovalItem(
                id = "approval-operation-expense-001",
                teamId = "team-operation",
                teamName = "运营支持部",
                type = OpenApprovalTypes.Expense,
                title = "客户活动物料报销",
                applicantId = "user-operation-employee",
                applicant = "运营员工",
                approverId = "user-operation-manager",
                approver = "运营主管",
                status = OpenApprovalStatus.Pending,
                form = mapOf("amount" to "860", "category" to "活动物料"),
                reason = "线下客户沟通会物料采购费用。",
                comment = null,
                createdAt = "2026-06-02T11:00:00+08:00",
                updatedAt = null
            ),
            OpenApprovalItem(
                id = "approval-product-overtime-001",
                teamId = "team-product",
                teamName = "产品研发部",
                type = OpenApprovalTypes.Overtime,
                title = "接口联调加班申请",
                applicantId = "user-product-employee",
                applicant = "产品员工",
                approverId = "user-product-manager",
                approver = "产品主管",
                status = OpenApprovalStatus.Approved,
                form = mapOf("hours" to "2", "date" to "2026-06-01"),
                reason = "完成审批接口联调验证。",
                comment = "已通过，注意同步测试记录。",
                createdAt = "2026-06-01T18:30:00+08:00",
                updatedAt = "2026-06-01T19:00:00+08:00"
            )
        )

        private val calendarEvents = mutableListOf(
            OpenCalendarEvent(
                eventId = "event-product-weekly",
                teamId = "team-product",
                teamName = "产品研发部",
                title = "产品团队周会",
                description = "同步 Tab 容器、AI 助手和权限隔离开发进度。",
                startTime = "2026-06-03T10:00:00+08:00",
                endTime = "2026-06-03T11:00:00+08:00",
                location = "线上会议室 A",
                visibility = OpenCalendarVisibility.Team,
                creatorId = "user-product-manager",
                creator = "产品主管",
                participantIds = listOf("user-product-manager", "user-product-employee"),
                participants = listOf(
                    OpenCalendarParticipant("user-product-manager", "产品主管"),
                    OpenCalendarParticipant("user-product-employee", "产品员工")
                )
            ),
            OpenCalendarEvent(
                eventId = "event-operation-review",
                teamId = "team-operation",
                teamName = "运营支持部",
                title = "运营活动复盘",
                description = "复盘客户活动执行效果和后续跟进事项。",
                startTime = "2026-06-03T14:00:00+08:00",
                endTime = "2026-06-03T15:00:00+08:00",
                location = "会议室 B",
                visibility = OpenCalendarVisibility.Team,
                creatorId = "user-operation-manager",
                creator = "运营主管",
                participantIds = listOf("user-operation-manager", "user-operation-employee"),
                participants = listOf(
                    OpenCalendarParticipant("user-operation-manager", "运营主管"),
                    OpenCalendarParticipant("user-operation-employee", "运营员工")
                )
            ),
            OpenCalendarEvent(
                eventId = "event-company-townhall",
                teamId = null,
                teamName = null,
                title = "全员项目展示会",
                description = "展示开放式 Tab 容器和 AI OnCall 阶段成果。",
                startTime = "2026-06-04T16:00:00+08:00",
                endTime = "2026-06-04T17:00:00+08:00",
                location = "大会议室",
                visibility = OpenCalendarVisibility.Company,
                creatorId = "user-admin",
                creator = "系统管理员",
                participantIds = profiles.map { it.userId },
                participants = profiles.map { OpenCalendarParticipant(it.userId, it.displayName) }
            )
        )

        private val announcements = listOf(
            OpenAnnouncementItem(
                announcementId = "announcement-company-001",
                scope = OpenAnnouncementScope.Company,
                teamId = null,
                teamName = null,
                title = "AI OnCall 演示准备通知",
                content = "请各团队在本周内完成业务闭环演示数据确认。",
                publisherId = "user-admin",
                publisher = "系统管理员",
                pinned = true,
                createdAt = "2026-06-02T09:30:00+08:00",
                updatedAt = null
            ),
            OpenAnnouncementItem(
                announcementId = "announcement-product-001",
                scope = OpenAnnouncementScope.Team,
                teamId = "team-product",
                teamName = "产品研发部",
                title = "产品团队接口对齐",
                content = "审批、日程、公告字段严格按 2026-06-02 文档推进。",
                publisherId = "user-product-manager",
                publisher = "产品主管",
                pinned = false,
                createdAt = "2026-06-02T10:00:00+08:00",
                updatedAt = null
            ),
            OpenAnnouncementItem(
                announcementId = "announcement-operation-001",
                scope = OpenAnnouncementScope.Team,
                teamId = "team-operation",
                teamName = "运营支持部",
                title = "运营团队演示素材收集",
                content = "请整理客户活动、团队日程和审批样例。",
                publisherId = "user-operation-manager",
                publisher = "运营主管",
                pinned = false,
                createdAt = "2026-06-02T10:15:00+08:00",
                updatedAt = null
            )
        )

        private fun employeePermissions(): List<String> {
            return listOf(
                OpenBusinessPermissions.CompanyRead,
                OpenBusinessPermissions.AnnouncementRead,
                OpenBusinessPermissions.FunRead,
                OpenBusinessPermissions.ApprovalRead,
                OpenBusinessPermissions.ApprovalCreate,
                OpenBusinessPermissions.CalendarRead,
                OpenBusinessPermissions.AiOnCall
            )
        }

        private fun managerPermissions(): List<String> {
            return employeePermissions() + listOf(
                OpenBusinessPermissions.TeamMemberRead,
                OpenBusinessPermissions.AnnouncementWrite,
                OpenBusinessPermissions.ApprovalApprove,
                OpenBusinessPermissions.CalendarCreate,
                OpenBusinessPermissions.CalendarManage
            )
        }

        private fun updateApprovalDecision(
            profile: OpenTeamProfile,
            approvalId: String,
            status: String,
            comment: String
        ): OpenApprovalItem? {
            val index = approvals.indexOfFirst { approval -> approval.id == approvalId }
            if (index == -1) {
                return null
            }
            val old = approvals[index]
            if (!OpenBusinessPermissionRules.canApprove(profile = profile, approval = old)) {
                return null
            }
            val updated = old.copy(
                status = status,
                comment = comment,
                updatedAt = nowText()
            )
            approvals[index] = updated
            return updated
        }

        private fun managerProfile(teamId: String): OpenTeamProfile? {
            return profiles.firstOrNull { profile ->
                profile.memberships.any { membership ->
                    membership.teamId == teamId && membership.teamRole == OpenTeamRoles.Manager
                }
            }
        }

        private fun teamName(teamId: String): String {
            return teams.firstOrNull { team -> team.teamId == teamId }?.teamName ?: teamId
        }

        private fun participantOf(userId: String): OpenCalendarParticipant? {
            return profiles.firstOrNull { profile -> profile.userId == userId }?.let { profile ->
                OpenCalendarParticipant(userId = profile.userId, displayName = profile.displayName)
            }
        }

        private fun nowText(): String {
            return OffsetDateTime.now(ZoneOffset.ofHours(8)).toString()
        }

    }

}
