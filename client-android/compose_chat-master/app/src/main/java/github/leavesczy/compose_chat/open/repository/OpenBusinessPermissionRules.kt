package github.leavesczy.compose_chat.open.repository

import github.leavesczy.compose_chat.open.model.OpenAnnouncementItem
import github.leavesczy.compose_chat.open.model.OpenAnnouncementScope
import github.leavesczy.compose_chat.open.model.OpenApprovalItem
import github.leavesczy.compose_chat.open.model.OpenApprovalStatus
import github.leavesczy.compose_chat.open.model.OpenBusinessPermissions
import github.leavesczy.compose_chat.open.model.OpenBusinessTabIds
import github.leavesczy.compose_chat.open.model.OpenCalendarEvent
import github.leavesczy.compose_chat.open.model.OpenCalendarVisibility
import github.leavesczy.compose_chat.open.model.OpenTeamProfile
import github.leavesczy.compose_chat.open.model.OpenTeamRoles

object OpenBusinessPermissionRules {

    fun isAdmin(profile: OpenTeamProfile): Boolean {
        return profile.globalRole == OpenTeamRoles.Admin ||
            profile.permissions.contains(OpenBusinessPermissions.TeamManage)
    }

    fun isManagerOf(profile: OpenTeamProfile, teamId: String?): Boolean {
        if (teamId.isNullOrBlank()) {
            return false
        }
        return profile.memberships.any { membership ->
            membership.teamId == teamId && membership.teamRole == OpenTeamRoles.Manager
        }
    }

    fun belongsToTeam(profile: OpenTeamProfile, teamId: String?): Boolean {
        if (teamId.isNullOrBlank()) {
            return false
        }
        return profile.memberships.any { membership -> membership.teamId == teamId }
    }

    fun canViewTab(profile: OpenTeamProfile, tabId: String): Boolean {
        return when (tabId) {
            OpenBusinessTabIds.CompanyIntro -> hasPermission(profile, OpenBusinessPermissions.CompanyRead)
            OpenBusinessTabIds.Announcements -> hasPermission(profile, OpenBusinessPermissions.AnnouncementRead)
            OpenBusinessTabIds.Fun -> hasPermission(profile, OpenBusinessPermissions.FunRead)
            OpenBusinessTabIds.Approval -> hasPermission(profile, OpenBusinessPermissions.ApprovalRead)
            OpenBusinessTabIds.Calendar -> hasPermission(profile, OpenBusinessPermissions.CalendarRead)
            OpenBusinessTabIds.PermissionAdmin -> hasPermission(profile, OpenBusinessPermissions.AdminManage)
            OpenBusinessTabIds.Debug -> hasPermission(profile, OpenBusinessPermissions.DebugRead)
            OpenBusinessTabIds.AiOnCall -> hasPermission(profile, OpenBusinessPermissions.AiOnCall)
            else -> false
        }
    }

    fun canManageTeams(profile: OpenTeamProfile): Boolean {
        return isAdmin(profile) && hasPermission(profile, OpenBusinessPermissions.TeamManage)
    }

    fun canReadTeamMembers(profile: OpenTeamProfile, teamId: String): Boolean {
        return isAdmin(profile) ||
            (isManagerOf(profile, teamId) && hasPermission(profile, OpenBusinessPermissions.TeamMemberRead))
    }

    fun canCreateApproval(profile: OpenTeamProfile, teamId: String?): Boolean {
        return hasPermission(profile, OpenBusinessPermissions.ApprovalCreate) &&
            (isAdmin(profile) || belongsToTeam(profile, teamId ?: profile.currentTeamId))
    }

    fun canViewApproval(profile: OpenTeamProfile, approval: OpenApprovalItem): Boolean {
        return when {
            isAdmin(profile) && hasPermission(profile, OpenBusinessPermissions.ApprovalAll) -> true
            approval.applicantId == profile.userId -> true
            isManagerOf(profile, approval.teamId) -> true
            else -> false
        }
    }

    fun canApprove(profile: OpenTeamProfile, approval: OpenApprovalItem): Boolean {
        return approval.status == OpenApprovalStatus.Pending &&
            hasPermission(profile, OpenBusinessPermissions.ApprovalApprove) &&
            (isAdmin(profile) || isManagerOf(profile, approval.teamId))
    }

    fun canCancelApproval(profile: OpenTeamProfile, approval: OpenApprovalItem): Boolean {
        return approval.status == OpenApprovalStatus.Pending && approval.applicantId == profile.userId
    }

    fun canViewCalendarEvent(profile: OpenTeamProfile, event: OpenCalendarEvent): Boolean {
        return when {
            isAdmin(profile) && hasPermission(profile, OpenBusinessPermissions.CalendarAll) -> true
            event.visibility == OpenCalendarVisibility.Company -> true
            event.creatorId == profile.userId -> true
            event.participantIds.contains(profile.userId) -> true
            event.visibility == OpenCalendarVisibility.Team -> belongsToTeam(profile, event.teamId)
            event.visibility == OpenCalendarVisibility.Participants -> event.participantIds.contains(profile.userId)
            else -> false
        }
    }

    fun canCreateCalendarEvent(profile: OpenTeamProfile, teamId: String?, visibility: String): Boolean {
        if (!hasPermission(profile, OpenBusinessPermissions.CalendarCreate)) {
            return false
        }
        return when {
            isAdmin(profile) -> true
            visibility == OpenCalendarVisibility.Company -> false
            else -> isManagerOf(profile, teamId ?: profile.currentTeamId)
        }
    }

    fun canManageCalendarEvent(profile: OpenTeamProfile, event: OpenCalendarEvent): Boolean {
        return hasPermission(profile, OpenBusinessPermissions.CalendarManage) &&
            (isAdmin(profile) || isManagerOf(profile, event.teamId))
    }

    fun canViewAnnouncement(profile: OpenTeamProfile, announcement: OpenAnnouncementItem): Boolean {
        return when {
            isAdmin(profile) -> true
            announcement.scope == OpenAnnouncementScope.Company -> true
            announcement.publisherId == profile.userId -> true
            announcement.scope == OpenAnnouncementScope.Team -> belongsToTeam(profile, announcement.teamId)
            else -> false
        }
    }

    fun canPublishAnnouncement(profile: OpenTeamProfile, teamId: String?, scope: String): Boolean {
        if (!hasPermission(profile, OpenBusinessPermissions.AnnouncementWrite)) {
            return false
        }
        return when {
            isAdmin(profile) -> true
            scope == OpenAnnouncementScope.Company -> false
            else -> isManagerOf(profile, teamId ?: profile.currentTeamId)
        }
    }

    fun canManageAnnouncement(profile: OpenTeamProfile, announcement: OpenAnnouncementItem): Boolean {
        return hasPermission(profile, OpenBusinessPermissions.AnnouncementWrite) &&
            (isAdmin(profile) || isManagerOf(profile, announcement.teamId))
    }

    private fun hasPermission(profile: OpenTeamProfile, permission: String): Boolean {
        return profile.permissions.contains(permission)
    }

}

