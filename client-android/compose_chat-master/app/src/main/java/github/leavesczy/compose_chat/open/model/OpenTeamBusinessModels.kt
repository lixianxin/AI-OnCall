package github.leavesczy.compose_chat.open.model

object OpenTeamRoles {
    const val Admin = "admin"
    const val Manager = "manager"
    const val Employee = "employee"
}

object OpenBusinessPermissions {
    const val TeamManage = "team.manage"
    const val TeamAllRead = "team.all.read"
    const val TeamMemberRead = "team.member.read"
    const val CompanyRead = "tab.company.read"
    const val AnnouncementRead = "tab.announcement.read"
    const val AnnouncementWrite = "tab.announcement.write"
    const val FunRead = "tab.fun.read"
    const val ApprovalRead = "tab.approval.read"
    const val ApprovalCreate = "tab.approval.create"
    const val ApprovalApprove = "tab.approval.approve"
    const val ApprovalAll = "tab.approval.all"
    const val CalendarRead = "tab.calendar.read"
    const val CalendarCreate = "tab.calendar.create"
    const val CalendarManage = "tab.calendar.manage"
    const val CalendarAll = "tab.calendar.all"
    const val AdminManage = "tab.admin.manage"
    const val DebugRead = "tab.debug.read"
    const val AiOnCall = "ai.oncall"
}

object OpenBusinessTabIds {
    const val CompanyIntro = "company-intro"
    const val Announcements = "announcements"
    const val Fun = "fun"
    const val Approval = "approval"
    const val Calendar = "calendar"
    const val PermissionAdmin = "permission-admin"
    const val Debug = "debug"
    const val AiOnCall = "ai-oncall"
}

object OpenApprovalStatus {
    const val Pending = "pending"
    const val Approved = "approved"
    const val Rejected = "rejected"
    const val Canceled = "canceled"
    const val Cancelled = "cancelled"
}

object OpenApprovalTypes {
    const val Leave = "leave"
    const val Expense = "expense"
    const val Overtime = "overtime"
    const val Purchase = "purchase"
    const val Outing = "outing"
}

object OpenCalendarVisibility {
    const val Company = "company"
    const val Team = "team"
    const val Participants = "participants"
}

object OpenAnnouncementScope {
    const val Company = "company"
    const val Team = "team"
}

data class OpenTeamProfile(
    val userId: String,
    val account: String,
    val displayName: String,
    val globalRole: String?,
    val currentTeamId: String?,
    val memberships: List<OpenTeamMembership>,
    val permissions: List<String>
)

data class OpenTeamMembership(
    val teamId: String,
    val teamName: String,
    val teamRole: String
)

data class OpenTeamDto(
    val teamId: String,
    val teamName: String,
    val description: String,
    val memberCount: Int,
    val managerCount: Int,
    val enabled: Boolean,
    val createdAt: String,
    val updatedAt: String
)

data class OpenTeamMemberDto(
    val userId: String,
    val account: String,
    val displayName: String,
    val teamId: String,
    val teamName: String,
    val teamRole: String,
    val joinedAt: String,
    val enabled: Boolean
)

data class OpenApprovalItem(
    val id: String,
    val teamId: String,
    val teamName: String,
    val type: String,
    val title: String,
    val applicantId: String,
    val applicant: String,
    val approverId: String,
    val approver: String,
    val status: String,
    val form: Map<String, String>,
    val reason: String,
    val comment: String?,
    val createdAt: String,
    val updatedAt: String?,
    val amount: Int? = null,
)

data class OpenCalendarParticipant(
    val userId: String,
    val displayName: String
)

data class OpenCalendarEvent(
    val eventId: String,
    val teamId: String?,
    val teamName: String?,
    val title: String,
    val description: String,
    val startTime: String,
    val endTime: String,
    val location: String?,
    val visibility: String,
    val creatorId: String,
    val creator: String,
    val participantIds: List<String>,
    val participants: List<OpenCalendarParticipant>
)

data class OpenAnnouncementItem(
    val announcementId: String,
    val scope: String,
    val teamId: String?,
    val teamName: String?,
    val title: String,
    val content: String,
    val publisherId: String,
    val publisher: String,
    val pinned: Boolean,
    val createdAt: String,
    val updatedAt: String?
)
