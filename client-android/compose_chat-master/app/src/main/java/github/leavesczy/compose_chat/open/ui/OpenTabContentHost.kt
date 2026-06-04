package github.leavesczy.compose_chat.open.ui

import androidx.activity.compose.BackHandler
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.res.Configuration
import android.os.Bundle
import android.os.SystemClock
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import github.leavesczy.compose_chat.open.model.EntryType
import github.leavesczy.compose_chat.open.model.OpenAnnouncementItem
import github.leavesczy.compose_chat.open.model.OpenAnnouncementScope
import github.leavesczy.compose_chat.open.model.OpenApprovalItem
import github.leavesczy.compose_chat.open.model.OpenApprovalStatus
import github.leavesczy.compose_chat.open.model.OpenApprovalTypes
import github.leavesczy.compose_chat.open.model.OpenBusinessPermissions
import github.leavesczy.compose_chat.open.model.OpenCalendarEvent
import github.leavesczy.compose_chat.open.model.OpenCalendarVisibility
import github.leavesczy.compose_chat.open.model.OpenTeamDto
import github.leavesczy.compose_chat.open.model.OpenTeamMemberDto
import github.leavesczy.compose_chat.open.model.OpenTeamProfile
import github.leavesczy.compose_chat.open.network.OpenApiResult
import github.leavesczy.compose_chat.open.repository.OpenBusinessPermissionRules
import github.leavesczy.compose_chat.open.repository.OpenTeamBusinessMockRepository
import github.leavesczy.compose_chat.open.repository.OpenTeamBusinessRepository
import github.leavesczy.compose_chat.open.session.OpenSessionManager
import github.leavesczy.compose_chat.open.tab.OpenTabContainer
import github.leavesczy.compose_chat.open.tab.OpenTabItem
import github.leavesczy.compose_chat.open.tab.OpenTabState
import github.leavesczy.compose_chat.open.tab.RegisteredOpenTab
import github.leavesczy.compose_chat.open.tab.toErrorCode
import github.leavesczy.compose_chat.ui.theme.AppTheme
import github.leavesczy.compose_chat.protocol.TabErrors
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

private val BusinessZoneId = ZoneId.of("Asia/Shanghai")
private val BusinessDateTextFormatter = DateTimeFormatter.ofPattern("yyyy\u5e74MM\u6708dd\u65e5")
private val BusinessMonthDayFormatter = DateTimeFormatter.ofPattern("MM\u6708dd\u65e5")
private val BusinessTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private data class BusinessLoadState(
    val loading: Boolean = true,
    val message: String? = null
)

@Composable
fun OpenTabContentHost(
    modifier: Modifier = Modifier,
    tab: OpenTabItem?,
    onBackToWorkbench: (() -> Unit)? = null
) {
    if (tab == null) {
        OpenStatePage(
            modifier = modifier,
            title = "未选择业务页",
            message = "当前没有需要渲染的动态业务 Tab。"
        )
        return
    }
    if (tab.openState != OpenTabState.Openable) {
        val errorCode = tab.openState.toErrorCode()
        OpenStatePage(
            modifier = modifier,
            title = tab.displayName,
            message = if (errorCode == 0) {
                tab.openState.toDisplayMessage(tab = tab)
            } else {
                "${tab.openState.toDisplayMessage(tab = tab)}\n协议错误码：$errorCode · ${TabErrors.description(errorCode)}"
            },
            onBackToWorkbench = onBackToWorkbench
        )
        return
    }
    val registeredTab = OpenTabContainer.findByRoute(route = tab.manifest.route)
    if (registeredTab != null) {
        RegisteredTabHost(
            modifier = modifier,
            tab = tab,
            registeredTab = registeredTab,
            onBackToWorkbench = onBackToWorkbench
        )
        return
    }
    when (tab.manifest.entryType) {
        EntryType.Native -> NativeTabPage(modifier = modifier, tab = tab, onBackToWorkbench = onBackToWorkbench)
        EntryType.Web -> WebTabPlaceholderPage(modifier = modifier, tab = tab, onBackToWorkbench = onBackToWorkbench)
        EntryType.External -> ExternalTabPlaceholderPage(modifier = modifier, tab = tab, onBackToWorkbench = onBackToWorkbench)
        EntryType.Hybrid,
        EntryType.Unknown -> OpenStatePage(
            modifier = modifier,
            title = tab.displayName,
            message = "当前客户端暂不支持打开该业务应用。",
            onBackToWorkbench = onBackToWorkbench
        )
    }
}

@Composable
private fun NativeTabPage(
    modifier: Modifier,
    tab: OpenTabItem,
    onBackToWorkbench: (() -> Unit)?
) {
    when (tab.manifest.route) {
        "/company",
        "/company-intro" -> CompanyIntroPage(modifier = modifier, tab = tab, onBackToWorkbench = onBackToWorkbench)
        "/announcements" -> AnnouncementsPage(modifier = modifier, tab = tab, onBackToWorkbench = onBackToWorkbench)
        "/approval" -> ApprovalPlaceholderPage(modifier = modifier, tab = tab, onBackToWorkbench = onBackToWorkbench)
        "/calendar" -> CalendarPlaceholderPage(modifier = modifier, tab = tab, onBackToWorkbench = onBackToWorkbench)
        "/fun" -> FunPage(modifier = modifier, tab = tab, onBackToWorkbench = onBackToWorkbench)
        "/permission-admin" -> PermissionAdminPage(modifier = modifier, tab = tab, onBackToWorkbench = onBackToWorkbench)
        "/finance" -> FinancePlaceholderPage(modifier = modifier, tab = tab, onBackToWorkbench = onBackToWorkbench)
        "/ai-oncall" -> OnCallPlaceholderPage(modifier = modifier, onBackToWorkbench = onBackToWorkbench)
        else -> OpenStatePage(
            modifier = modifier,
            title = tab.displayName,
            message = "该业务页暂未接入当前客户端。",
            onBackToWorkbench = onBackToWorkbench
        )
    }
}

@Composable
private fun ApprovalPlaceholderPage(
    modifier: Modifier,
    tab: OpenTabItem,
    onBackToWorkbench: (() -> Unit)?,
    repository: OpenTeamBusinessRepository = remember { OpenTeamBusinessRepository() },
    fallbackRepository: OpenTeamBusinessMockRepository = remember { OpenTeamBusinessMockRepository() }
) {
    val coroutineScope = rememberCoroutineScope()
    var profile by remember {
        mutableStateOf(fallbackRepository.currentProfile(account = OpenSessionManager.lastAccount))
    }
    var approvalPageMode by remember { mutableStateOf(ApprovalPageMode.Home) }
    var selectedFilter by remember { mutableStateOf(ApprovalFilter.Pending) }
    var selectedItemId by remember { mutableStateOf<String?>(null) }
    var selectedApprovalDetail by remember { mutableStateOf<OpenApprovalItem?>(null) }
    var approvalType by remember { mutableStateOf(OpenApprovalTypes.Leave) }
    var approvalTitle by remember { mutableStateOf("") }
    var approvalReason by remember { mutableStateOf("") }
    var approvalExtra by remember { mutableStateOf("") }
    var approvalFormValues by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var refreshKey by remember { mutableStateOf(0) }
    var loadState by remember { mutableStateOf(BusinessLoadState()) }
    var items by remember { mutableStateOf<List<OpenApprovalItem>>(emptyList()) }

    suspend fun loadApprovalPage() {
        loadState = BusinessLoadState(loading = true)
        var nextMessage: String? = null
        val nextProfile = when (val result = repository.currentProfile()) {
            is OpenApiResult.Success -> result.data
            is OpenApiResult.Failed -> {
                nextMessage = "用户信息加载失败：${result.message}"
                fallbackRepository.currentProfile(account = OpenSessionManager.lastAccount)
            }
        }
        profile = nextProfile
        val loadedItems = mutableListOf<OpenApprovalItem>()
        val scopes = selectedFilter.requestScopes(profile = nextProfile)
        scopes.forEach { scope ->
            when (val result = repository.approvalItems(scope = scope)) {
                is OpenApiResult.Success -> loadedItems += result.data
                is OpenApiResult.Failed -> {
                    nextMessage = nextMessage ?: "审批列表加载失败：${result.message}"
                }
            }
        }
        val filteredItems = loadedItems
            .distinctBy { item -> item.id }
            .filter { item ->
                item.status.normalizedApprovalStatus() == selectedFilter.status
            }
            .sortedByDescending { item -> item.createdAt }
        items = filteredItems
        loadState = BusinessLoadState(loading = false, message = nextMessage)
    }

    fun resetApprovalForm() {
        approvalType = OpenApprovalTypes.Leave
        approvalTitle = ""
        approvalReason = ""
        approvalExtra = ""
        approvalFormValues = emptyMap()
    }

    LaunchedEffect(selectedFilter, refreshKey) {
        loadApprovalPage()
    }

    fun openApprovalDetail(item: OpenApprovalItem) {
        selectedItemId = item.id
        selectedApprovalDetail = item
        coroutineScope.launch {
            when (val result = repository.approvalDetail(approvalId = item.id)) {
                is OpenApiResult.Success -> {
                    selectedApprovalDetail = result.data
                }

                is OpenApiResult.Failed -> {
                    loadState = BusinessLoadState(loading = false, message = "审批详情加载失败：${result.message}")
                }
            }
        }
    }

    BackHandler(enabled = approvalPageMode != ApprovalPageMode.Home) {
        if (approvalPageMode == ApprovalPageMode.Create) {
            resetApprovalForm()
        }
        approvalPageMode = ApprovalPageMode.Home
    }

    OpenTabScaffold(
        modifier = modifier,
        tab = tab,
        onBackToWorkbench = if (approvalPageMode == ApprovalPageMode.Home) {
            onBackToWorkbench
        } else {
            {
                if (approvalPageMode == ApprovalPageMode.Create) {
                    resetApprovalForm()
                }
                approvalPageMode = ApprovalPageMode.Home
            }
        },
        titleOverride = when (approvalPageMode) {
            ApprovalPageMode.Home -> null
            ApprovalPageMode.Create -> "发起审批"
            ApprovalPageMode.Records -> "审批记录"
        },
        backTextOverride = "",
        centerTitle = true,
        compactTitle = true,
        iconOnlyBack = true
    ) {
        if (!loadState.message.isNullOrBlank()) {
            BusinessStatusCard(state = loadState.copy(loading = false))
        }
        when (approvalPageMode) {
            ApprovalPageMode.Home -> {
                ApprovalHomePage(
                    canCreateApproval = OpenBusinessPermissionRules.canCreateApproval(
                        profile = profile,
                        teamId = profile.currentTeamId
                    ),
                    onCreateClick = {
                        approvalPageMode = ApprovalPageMode.Create
                    },
                    onRecordsClick = {
                        approvalPageMode = ApprovalPageMode.Records
                    }
                )
            }
            ApprovalPageMode.Create -> {
                ApprovalCreatePage(
                    approvalType = approvalType,
                    approvalTitle = approvalTitle,
                    approvalReason = approvalReason,
                    approvalExtra = approvalExtra,
                    approvalFormValues = approvalFormValues,
                    onTypeChange = {
                        approvalType = it
                        approvalFormValues = emptyMap()
                    },
                    onTitleChange = { approvalTitle = it },
                    onReasonChange = { approvalReason = it },
                    onExtraChange = { approvalExtra = it },
                    onFormValueChange = { key, value ->
                        approvalFormValues = approvalFormValues + (key to value)
                    },
                    onBackClick = {
                        resetApprovalForm()
                        approvalPageMode = ApprovalPageMode.Home
                    },
                    onSubmitClick = {
                        coroutineScope.launch {
                            loadState = BusinessLoadState(loading = true)
                            val result = repository.createApproval(
                                teamId = profile.currentTeamId,
                                type = approvalType.ifBlank { OpenApprovalTypes.Leave },
                                title = approvalTitle,
                                reason = approvalReason,
                                form = approvalType.toApprovalSubmitForm(
                                    values = approvalType.withComputedApprovalValues(approvalFormValues),
                                    extra = approvalExtra
                                )
                            )
                            when (result) {
                                is OpenApiResult.Success -> {
                                    selectedFilter = ApprovalFilter.Pending
                                    selectedItemId = result.data.id
                                    selectedApprovalDetail = result.data
                                    resetApprovalForm()
                                    approvalPageMode = ApprovalPageMode.Records
                                    refreshKey += 1
                                }

                                is OpenApiResult.Failed -> {
                                    loadState = BusinessLoadState(
                                        loading = false,
                                        message = "审批提交失败：${result.message}"
                                    )
                                }
                            }
                        }
                    }
                )
            }
            ApprovalPageMode.Records -> {
                ApprovalRecordsPage(
                    profile = profile,
                    selectedFilter = selectedFilter,
                    items = items,
                    selectedItemId = selectedItemId,
                    onBackClick = {
                        approvalPageMode = ApprovalPageMode.Home
                    },
                    onFilterChange = { filter ->
                        selectedFilter = filter
                    },
                    onItemClick = ::openApprovalDetail
                )
            }
        }
        val selectedApproval = selectedApprovalDetail
            ?: selectedItemId?.let { id -> items.firstOrNull { item -> item.id == id } }
        if (selectedApproval != null) {
            ApprovalDetailDialog(
                profile = profile,
                item = selectedApproval,
                onApprove = { comment ->
                    coroutineScope.launch {
                        loadState = BusinessLoadState(loading = true)
                        when (val result = repository.approveApproval(approvalId = selectedApproval.id, comment = comment)) {
                            is OpenApiResult.Success -> {
                                selectedItemId = null
                                selectedApprovalDetail = null
                                refreshKey += 1
                            }

                            is OpenApiResult.Failed -> {
                                loadState = BusinessLoadState(loading = false, message = "审批通过失败：${result.message}")
                            }
                        }
                    }
                },
                onReject = { comment ->
                    coroutineScope.launch {
                        loadState = BusinessLoadState(loading = true)
                        when (val result = repository.rejectApproval(approvalId = selectedApproval.id, comment = comment)) {
                            is OpenApiResult.Success -> {
                                selectedItemId = null
                                selectedApprovalDetail = null
                                refreshKey += 1
                            }

                            is OpenApiResult.Failed -> {
                                loadState = BusinessLoadState(loading = false, message = "审批驳回失败：${result.message}")
                            }
                        }
                    }
                },
                onCancelApproval = { comment ->
                    coroutineScope.launch {
                        loadState = BusinessLoadState(loading = true)
                        when (val result = repository.cancelApproval(approvalId = selectedApproval.id, comment = comment)) {
                            is OpenApiResult.Success -> {
                                selectedItemId = null
                                selectedApprovalDetail = null
                                refreshKey += 1
                            }

                            is OpenApiResult.Failed -> {
                                loadState = BusinessLoadState(loading = false, message = "审批撤回失败：${result.message}")
                            }
                        }
                    }
                },
                onClose = {
                    selectedItemId = null
                    selectedApprovalDetail = null
                }
            )
        }
    }
}

@Composable
private fun CompanyIntroPage(
    modifier: Modifier,
    tab: OpenTabItem,
    onBackToWorkbench: (() -> Unit)?,
    repository: OpenTeamBusinessRepository = remember { OpenTeamBusinessRepository() },
    fallbackRepository: OpenTeamBusinessMockRepository = remember { OpenTeamBusinessMockRepository() }
) {
    val coroutineScope = rememberCoroutineScope()
    var profile by remember {
        mutableStateOf(fallbackRepository.currentProfile(account = OpenSessionManager.lastAccount))
    }
    var calendarPageMode by remember { mutableStateOf(CalendarPageMode.Home) }
    var selectedEventId by remember { mutableStateOf<String?>(null) }
    var editingEventId by remember { mutableStateOf<String?>(null) }
    var selectedCalendarDetail by remember { mutableStateOf<OpenCalendarEvent?>(null) }
    var eventTitle by remember { mutableStateOf("") }
    var eventDescription by remember { mutableStateOf("") }
    var eventStart by remember { mutableStateOf("") }
    var eventEnd by remember { mutableStateOf("") }
    var eventLocation by remember { mutableStateOf("") }
    var eventVisibility by remember { mutableStateOf(OpenCalendarVisibility.Team) }
    var eventTeamId by remember { mutableStateOf(profile.currentTeamId.orEmpty()) }
    var eventParticipantIds by remember { mutableStateOf<List<String>>(listOf(profile.userId)) }
    var refreshKey by remember { mutableStateOf(0) }
    var loadState by remember { mutableStateOf(BusinessLoadState()) }
    var teams by remember { mutableStateOf<List<OpenTeamDto>>(emptyList()) }
    var membersByTeam by remember { mutableStateOf<Map<String, List<OpenTeamMemberDto>>>(emptyMap()) }
    var events by remember { mutableStateOf<List<OpenCalendarEvent>>(emptyList()) }
    val targetTeamId = eventTeamId.ifBlank { profile.currentTeamId.orEmpty() }
    val eventMembers = remember(membersByTeam, targetTeamId) {
        membersByTeam[targetTeamId].orEmpty()
    }

    fun membersOf(teamId: String): List<OpenTeamMemberDto> {
        return membersByTeam[teamId].orEmpty()
    }

    fun defaultTeamId(): String {
        return profile.currentTeamId ?: teams.firstOrNull()?.teamId.orEmpty()
    }

    fun selectTeamForEvent(teamId: String) {
        eventVisibility = OpenCalendarVisibility.Team
        eventTeamId = teamId
        eventParticipantIds = membersOf(teamId).map { member -> member.userId }
            .ifEmpty { listOf(profile.userId) }
    }
    var announcements by remember { mutableStateOf<List<OpenAnnouncementItem>>(emptyList()) }
    var selectedAnnouncementId by remember { mutableStateOf<String?>(null) }
    var showAnnouncementForm by remember { mutableStateOf(false) }
    var editingAnnouncementId by remember { mutableStateOf<String?>(null) }
    var announcementTitle by remember { mutableStateOf("") }
    var announcementContent by remember { mutableStateOf("") }
    var announcementPinned by remember { mutableStateOf(false) }

    fun selectParticipantsScope(teamId: String = targetTeamId.ifBlank { defaultTeamId() }) {
        eventVisibility = OpenCalendarVisibility.Participants
        eventTeamId = teamId
        if (eventParticipantIds.isEmpty() || eventParticipantIds.all { userId -> membersOf(teamId).none { member -> member.userId == userId } }) {
            eventParticipantIds = listOf(profile.userId)
        }
    }

    fun submitParticipantIds(): List<String> {
        return when (eventVisibility) {
            OpenCalendarVisibility.Company -> emptyList()
            OpenCalendarVisibility.Team -> eventParticipantIds.ifEmpty { eventMembers.map { member -> member.userId } }
            else -> eventParticipantIds.ifEmpty { listOf(profile.userId) }
        }
    }

    fun resetEventForm() {
        editingEventId = null
        eventTitle = ""
        eventDescription = ""
        eventStart = ""
        eventEnd = ""
        eventLocation = ""
        eventVisibility = OpenCalendarVisibility.Team
        eventTeamId = profile.currentTeamId.orEmpty()
        eventParticipantIds = listOf(profile.userId)
    }

    fun openCreateEventForm() {
        resetEventForm()
        val teamId = defaultTeamId()
        eventTeamId = teamId
        eventParticipantIds = membersOf(teamId).map { member -> member.userId }
            .ifEmpty { listOf(profile.userId) }
        calendarPageMode = CalendarPageMode.Create
    }

    fun openEditEventForm(event: OpenCalendarEvent) {
        editingEventId = event.eventId
        eventTitle = event.title
        eventDescription = event.description
        eventStart = event.startTime
        eventEnd = event.endTime
        eventLocation = event.location.orEmpty()
        eventVisibility = event.visibility
        eventTeamId = event.teamId.orEmpty().ifBlank { defaultTeamId() }
        eventParticipantIds = event.participantIds.ifEmpty { listOf(profile.userId) }
        selectedEventId = null
        selectedCalendarDetail = null
        calendarPageMode = CalendarPageMode.Edit
    }

    suspend fun loadCalendarPage() {
        loadState = BusinessLoadState(loading = true)
        var nextMessage: String? = null
        val nextProfile = when (val result = repository.currentProfile()) {
            is OpenApiResult.Success -> result.data
            is OpenApiResult.Failed -> {
                nextMessage = "用户信息加载失败：${result.message}"
                fallbackRepository.currentProfile(account = OpenSessionManager.lastAccount)
            }
        }
        profile = nextProfile
        val nextTeams = if (OpenBusinessPermissionRules.isAdmin(nextProfile)) {
            when (val result = repository.teams()) {
                is OpenApiResult.Success -> result.data
                is OpenApiResult.Failed -> {
                    nextMessage = nextMessage ?: "团队列表加载失败：${result.message}"
                    emptyList()
                }
            }
        } else {
            nextProfile.memberships.map { membership ->
                OpenTeamDto(
                    teamId = membership.teamId,
                    teamName = membership.teamName,
                    description = "当前账号所属团队",
                    memberCount = 0,
                    managerCount = 0,
                    enabled = true,
                    createdAt = "",
                    updatedAt = ""
                )
            }
        }
        teams = nextTeams
        val nextMembers = nextTeams.associate { team ->
            val members = if (OpenBusinessPermissionRules.canReadTeamMembers(nextProfile, team.teamId)) {
                when (val result = repository.teamMembers(teamId = team.teamId)) {
                    is OpenApiResult.Success -> result.data
                    is OpenApiResult.Failed -> {
                        nextMessage = nextMessage ?: "${team.teamName} 成员加载失败：${result.message}"
                        emptyList()
                    }
                }
            } else {
                emptyList()
            }
            team.teamId to members
        }
        membersByTeam = nextMembers
        val eventScope = if (OpenBusinessPermissionRules.isAdmin(nextProfile)) "all" else "visible"
        val nextEvents = when (val result = repository.calendarEvents(scope = eventScope)) {
            is OpenApiResult.Success -> result.data
            is OpenApiResult.Failed -> {
                nextMessage = nextMessage ?: "日程列表加载失败：${result.message}"
                emptyList()
            }
        }
        events = nextEvents.filter { event ->
            OpenBusinessPermissionRules.canViewCalendarEvent(nextProfile, event)
        }.sortedBy { event -> event.startTime.toLocalDateTimeOrNull() ?: LocalDateTime.MAX }
        loadState = BusinessLoadState(loading = false, message = nextMessage)
    }

    LaunchedEffect(refreshKey) {
        loadCalendarPage()
    }

    fun openCalendarDetail(event: OpenCalendarEvent) {
        selectedEventId = event.eventId
        selectedCalendarDetail = event
        coroutineScope.launch {
            when (val result = repository.calendarDetail(eventId = event.eventId)) {
                is OpenApiResult.Success -> {
                    selectedCalendarDetail = result.data
                }

                is OpenApiResult.Failed -> {
                    loadState = BusinessLoadState(loading = false, message = "日程详情加载失败：${result.message}")
                }
            }
        }
    }

    fun handleCalendarBack() {
        when (calendarPageMode) {
            CalendarPageMode.Home -> onBackToWorkbench?.invoke()
            CalendarPageMode.Create -> {
                resetEventForm()
                calendarPageMode = CalendarPageMode.Home
            }
            CalendarPageMode.Edit -> {
                resetEventForm()
                calendarPageMode = CalendarPageMode.Home
            }
        }
    }

    BackHandler(enabled = calendarPageMode != CalendarPageMode.Home) {
        handleCalendarBack()
    }

    OpenTabScaffold(
        modifier = modifier,
        tab = tab,
        onBackToWorkbench = if (calendarPageMode == CalendarPageMode.Home) onBackToWorkbench else ::handleCalendarBack,
        titleOverride = when (calendarPageMode) {
            CalendarPageMode.Home -> null
            CalendarPageMode.Create -> "新建日程"
            CalendarPageMode.Edit -> "编辑日程"
        },
        backTextOverride = "",
        centerTitle = true,
        compactTitle = true,
        iconOnlyBack = true
    ) {
        if (!loadState.message.isNullOrBlank()) {
            BusinessStatusCard(state = loadState.copy(loading = false))
        }
        when (calendarPageMode) {
            CalendarPageMode.Home -> {
                CalendarHomePage(
                    canCreateCalendar = OpenBusinessPermissionRules.canCreateCalendarEvent(
                        profile = profile,
                        teamId = profile.currentTeamId,
                        visibility = OpenCalendarVisibility.Team
                    ),
                    onCreateClick = { openCreateEventForm() }
                )
                CalendarRecordsTimeline(
                    events = events,
                    selectedEventId = selectedEventId,
                    onItemClick = ::openCalendarDetail
                )
            }
            CalendarPageMode.Create -> {
                CalendarEventForm(
                    primaryText = "创建日程",
                    eventTitle = eventTitle,
                    onEventTitleChange = { eventTitle = it },
                    eventDescription = eventDescription,
                    onEventDescriptionChange = { eventDescription = it },
                    eventStart = eventStart,
                    onEventStartChange = { eventStart = it },
                    eventEnd = eventEnd,
                    onEventEndChange = { eventEnd = it },
                    eventLocation = eventLocation,
                    onEventLocationChange = { eventLocation = it },
                    eventVisibility = eventVisibility,
                    eventTeamId = eventTeamId,
                    teams = teams,
                    members = eventMembers,
                    participantIds = eventParticipantIds,
                    editableScope = true,
                    onCompanySelected = {
                        eventVisibility = OpenCalendarVisibility.Company
                        eventTeamId = ""
                        eventParticipantIds = emptyList()
                    },
                    onTeamSelected = { teamId -> selectTeamForEvent(teamId) },
                    onParticipantsSelected = { selectParticipantsScope() },
                    onParticipantToggle = { userId ->
                        eventParticipantIds = eventParticipantIds.toggleValue(value = userId)
                    },
                    onCancelClick = {
                        resetEventForm()
                        calendarPageMode = CalendarPageMode.Home
                    },
                    onPrimaryClick = {
                        coroutineScope.launch {
                            loadState = BusinessLoadState(loading = true)
                            when (val result = repository.createCalendarEvent(
                                teamId = eventTeamId.ifBlank { null },
                                title = eventTitle,
                                description = eventDescription,
                                startTime = eventStart,
                                endTime = eventEnd,
                                location = eventLocation,
                                visibility = eventVisibility.ifBlank { OpenCalendarVisibility.Team },
                                participantIds = submitParticipantIds()
                            )) {
                                is OpenApiResult.Success -> {
                                    selectedEventId = result.data.eventId
                                    selectedCalendarDetail = result.data
                                    resetEventForm()
                                    calendarPageMode = CalendarPageMode.Home
                                    refreshKey += 1
                                }

                                is OpenApiResult.Failed -> {
                                    loadState = BusinessLoadState(loading = false, message = "日程创建失败：${result.message}")
                                }
                            }
                        }
                    }
                )
            }
            CalendarPageMode.Edit -> {
                CalendarEventForm(
                    primaryText = "保存修改",
                    eventTitle = eventTitle,
                    onEventTitleChange = { eventTitle = it },
                    eventDescription = eventDescription,
                    onEventDescriptionChange = { eventDescription = it },
                    eventStart = eventStart,
                    onEventStartChange = { eventStart = it },
                    eventEnd = eventEnd,
                    onEventEndChange = { eventEnd = it },
                    eventLocation = eventLocation,
                    onEventLocationChange = { eventLocation = it },
                    eventVisibility = eventVisibility,
                    eventTeamId = eventTeamId,
                    teams = teams,
                    members = eventMembers,
                    participantIds = eventParticipantIds,
                    editableScope = true,
                    onCompanySelected = {
                        eventVisibility = OpenCalendarVisibility.Company
                        eventTeamId = ""
                        eventParticipantIds = emptyList()
                    },
                    onTeamSelected = { teamId -> selectTeamForEvent(teamId) },
                    onParticipantsSelected = { selectParticipantsScope() },
                    onParticipantToggle = { userId ->
                        eventParticipantIds = eventParticipantIds.toggleValue(value = userId)
                    },
                    onCancelClick = {
                        resetEventForm()
                        calendarPageMode = CalendarPageMode.Home
                    },
                    onPrimaryClick = {
                        coroutineScope.launch {
                            loadState = BusinessLoadState(loading = true)
                            when (val result = repository.updateCalendarEvent(
                                eventId = editingEventId.orEmpty(),
                                teamId = eventTeamId.ifBlank { null },
                                title = eventTitle,
                                description = eventDescription,
                                startTime = eventStart,
                                endTime = eventEnd,
                                location = eventLocation,
                                visibility = eventVisibility.ifBlank { OpenCalendarVisibility.Team },
                                participantIds = submitParticipantIds()
                            )) {
                                is OpenApiResult.Success -> {
                                    selectedEventId = result.data.eventId
                                    selectedCalendarDetail = result.data
                                    resetEventForm()
                                    calendarPageMode = CalendarPageMode.Home
                                    refreshKey += 1
                                }

                                is OpenApiResult.Failed -> {
                                    loadState = BusinessLoadState(loading = false, message = "日程保存失败：${result.message}")
                                }
                            }
                        }
                    }
                )
            }
        }
        val selectedCalendar = selectedCalendarDetail
            ?: selectedEventId?.let { id -> events.firstOrNull { event -> event.eventId == id } }
        if (selectedCalendar != null) {
            val selectedCalendarTeamId = selectedCalendar.teamId.orEmpty()
            val selectedCalendarMembers = if (selectedCalendarTeamId.isNotBlank()) {
                membersByTeam[selectedCalendarTeamId].orEmpty()
            } else {
                membersByTeam.values.flatten()
            }
            CalendarDetailDialog(
                profile = profile,
                event = selectedCalendar,
                members = selectedCalendarMembers,
                onEdit = { openEditEventForm(selectedCalendar) },
                onDelete = {
                    coroutineScope.launch {
                        loadState = BusinessLoadState(loading = true)
                        when (val result = repository.deleteCalendarEvent(eventId = selectedCalendar.eventId)) {
                            is OpenApiResult.Success -> {
                                selectedEventId = null
                                selectedCalendarDetail = null
                                refreshKey += 1
                            }

                            is OpenApiResult.Failed -> {
                                loadState = BusinessLoadState(loading = false, message = "日程删除失败：${result.message}")
                            }
                        }
                    }
                },
                onClose = {
                    selectedEventId = null
                    selectedCalendarDetail = null
                }
            )
        }
    }
}

@Composable
private fun CompanyIntroPage(
    modifier: Modifier,
    tab: OpenTabItem,
    onBackToWorkbench: (() -> Unit)?
) {
    var selectedIndex by remember { mutableStateOf(0) }
    var revealAnswer by remember { mutableStateOf(false) }
    val joke = FunJokes[selectedIndex % FunJokes.size]
    OpenTabScaffold(modifier = modifier, tab = tab, onBackToWorkbench = onBackToWorkbench) {
        CompanyDocumentHero()
        CompanyDocumentSection(
            title = "公司简介",
            accentColor = Color(color = 0xFFEAF4FF),
            rows = listOf(
                "AI-OnCall 示例企业是一家面向企业协作场景的数字化团队，核心目标是把审批、日程、公告和智能问答放进统一工作台。",
                "当前演示企业包含产品研发部和运营支持部，团队之间默认数据隔离，主管负责本团队业务协同，管理员负责全局配置。"
            )
        )
        CompanyDocumentSection(
            title = "组织架构",
            accentColor = Color(color = 0xFFEAF7EF),
            rows = listOf(
                "产品研发部：负责开放式 Tab 容器、AI 助手能力和移动端体验迭代。",
                "运营支持部：负责企业公告、日程协同、流程执行和跨团队支持。"
            )
        )
        CompanyDocumentSection(
            title = "平台能力",
            accentColor = Color(color = 0xFFFFF7ED),
            rows = listOf(
                "开放式 Tab 容器支持服务端下发业务入口，并由客户端按权限渲染。",
                "AI-OnCall 助手支持围绕接口、配置、审批和日程问题进行咨询。",
                "团队业务遵循角色和团队隔离原则，员工、主管、管理员看到的内容不同。"
            )
        )
        CompanyDocumentQuote(
            text = "让企业管理更简单，让团队协作更清楚。"
        )
    }
}

@Composable
private fun AnnouncementsPage(
    modifier: Modifier,
    tab: OpenTabItem,
    onBackToWorkbench: (() -> Unit)?,
    repository: OpenTeamBusinessRepository = remember { OpenTeamBusinessRepository() },
    fallbackRepository: OpenTeamBusinessMockRepository = remember { OpenTeamBusinessMockRepository() }
) {
    val coroutineScope = rememberCoroutineScope()
    var profile by remember {
        mutableStateOf(fallbackRepository.currentProfile(account = OpenSessionManager.lastAccount))
    }
    var announcements by remember { mutableStateOf<List<OpenAnnouncementItem>>(emptyList()) }
    var selectedAnnouncementId by remember { mutableStateOf<String?>(null) }
    var showAnnouncementForm by remember { mutableStateOf(false) }
    var editingAnnouncementId by remember { mutableStateOf<String?>(null) }
    var announcementTitle by remember { mutableStateOf("") }
    var announcementContent by remember { mutableStateOf("") }
    var announcementPinned by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableStateOf(0) }
    var loadState by remember { mutableStateOf(BusinessLoadState()) }

    fun resetAnnouncementForm() {
        editingAnnouncementId = null
        announcementTitle = ""
        announcementContent = ""
        announcementPinned = false
    }

    suspend fun loadAnnouncementsPage() {
        loadState = BusinessLoadState(loading = true)
        var nextMessage: String? = null
        val nextProfile = when (val result = repository.currentProfile()) {
            is OpenApiResult.Success -> result.data
            is OpenApiResult.Failed -> {
                nextMessage = "用户信息加载失败：${result.message}"
                fallbackRepository.currentProfile(account = OpenSessionManager.lastAccount)
            }
        }
        profile = nextProfile
        announcements = when (val result = repository.announcements(scope = "visible")) {
            is OpenApiResult.Success -> result.data.filter { item ->
                OpenBusinessPermissionRules.canViewAnnouncement(nextProfile, item)
            }
            is OpenApiResult.Failed -> {
                nextMessage = nextMessage ?: "公告列表加载失败：${result.message}"
                emptyList()
            }
        }.sortedWith(compareByDescending<OpenAnnouncementItem> { item -> item.pinned }.thenByDescending { item -> item.createdAt })
        loadState = BusinessLoadState(loading = false, message = nextMessage)
    }

    LaunchedEffect(refreshKey) {
        loadAnnouncementsPage()
    }

    val selectedAnnouncement = announcements.firstOrNull { item -> item.announcementId == selectedAnnouncementId }
    val canPublish = OpenBusinessPermissionRules.canPublishAnnouncement(
        profile = profile,
        teamId = profile.currentTeamId,
        scope = OpenAnnouncementScope.Team
    )

    OpenTabScaffold(modifier = modifier, tab = tab, onBackToWorkbench = onBackToWorkbench) {
        BusinessStatusCard(state = loadState)
        AnnouncementHeroCard(
            profile = profile,
            announcementCount = announcements.size,
            canPublish = canPublish,
            onPublishClick = {
                selectedAnnouncementId = null
                resetAnnouncementForm()
                showAnnouncementForm = true
            }
        )
        if (showAnnouncementForm && editingAnnouncementId == null) {
            AnnouncementForm(
                title = "发布公告",
                primaryText = "发布",
                announcementTitle = announcementTitle,
                onAnnouncementTitleChange = { announcementTitle = it },
                announcementContent = announcementContent,
                onAnnouncementContentChange = { announcementContent = it },
                pinned = announcementPinned,
                onPinnedChange = { announcementPinned = !announcementPinned },
                onCancelClick = {
                    resetAnnouncementForm()
                    showAnnouncementForm = false
                },
                onPrimaryClick = {
                    coroutineScope.launch {
                        val scope = if (OpenBusinessPermissionRules.isAdmin(profile) && profile.currentTeamId == null) {
                            OpenAnnouncementScope.Company
                        } else {
                            OpenAnnouncementScope.Team
                        }
                        when (val result = repository.createAnnouncement(
                            teamId = if (scope == OpenAnnouncementScope.Team) profile.currentTeamId else null,
                            scope = scope,
                            title = announcementTitle,
                            content = announcementContent,
                            pinned = announcementPinned
                        )) {
                            is OpenApiResult.Success -> {
                                selectedAnnouncementId = result.data.announcementId
                                resetAnnouncementForm()
                                showAnnouncementForm = false
                                refreshKey += 1
                            }

                            is OpenApiResult.Failed -> {
                                loadState = BusinessLoadState(loading = false, message = "公告发布失败：${result.message}")
                            }
                        }
                    }
                }
            )
        }
        if (announcements.isEmpty()) {
            SectionCard(title = "暂无公告", body = "当前账号暂时没有可见公告。")
        }
        announcements.forEach { announcement ->
            AnnouncementArticleCard(
                announcement = announcement,
                onClick = {
                    resetAnnouncementForm()
                    showAnnouncementForm = false
                    selectedAnnouncementId = announcement.announcementId
                }
            )
        }
        selectedAnnouncement?.let { announcement ->
            AnnouncementDetailDialog(
                profile = profile,
                announcement = announcement,
                onEdit = {
                    editingAnnouncementId = announcement.announcementId
                    announcementTitle = announcement.title
                    announcementContent = announcement.content
                    announcementPinned = announcement.pinned
                    showAnnouncementForm = true
                },
                onDelete = {
                    coroutineScope.launch {
                        when (val result = repository.deleteAnnouncement(announcementId = announcement.announcementId)) {
                            is OpenApiResult.Success -> {
                                selectedAnnouncementId = null
                                refreshKey += 1
                            }

                            is OpenApiResult.Failed -> {
                                loadState = BusinessLoadState(loading = false, message = "公告删除失败：${result.message}")
                            }
                        }
                    }
                },
                onClose = { selectedAnnouncementId = null }
            )
        }
        val editingAnnouncement = announcements.firstOrNull { item -> item.announcementId == editingAnnouncementId }
        if (showAnnouncementForm && editingAnnouncement != null) {
            AnnouncementEditDialog(
                title = "编辑公告",
                primaryText = "保存",
                announcementTitle = announcementTitle,
                onAnnouncementTitleChange = { announcementTitle = it },
                announcementContent = announcementContent,
                onAnnouncementContentChange = { announcementContent = it },
                pinned = announcementPinned,
                onPinnedChange = { announcementPinned = !announcementPinned },
                onCancelClick = {
                    resetAnnouncementForm()
                    showAnnouncementForm = false
                },
                onPrimaryClick = {
                    coroutineScope.launch {
                        when (val result = repository.updateAnnouncement(
                            announcementId = editingAnnouncement.announcementId,
                            title = announcementTitle,
                            content = announcementContent,
                            pinned = announcementPinned
                        )) {
                            is OpenApiResult.Success -> {
                                selectedAnnouncementId = result.data.announcementId
                                resetAnnouncementForm()
                                showAnnouncementForm = false
                                refreshKey += 1
                            }

                            is OpenApiResult.Failed -> {
                                loadState = BusinessLoadState(loading = false, message = "公告保存失败：${result.message}")
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun FunPage(
    modifier: Modifier,
    tab: OpenTabItem,
    onBackToWorkbench: (() -> Unit)?
) {
    var selectedIndex by remember { mutableStateOf(0) }
    var revealAnswer by remember { mutableStateOf(false) }
    val joke = FunJokes[selectedIndex % FunJokes.size]
    OpenTabScaffold(modifier = modifier, tab = tab, onBackToWorkbench = onBackToWorkbench) {
        FunHeroCard()
        FunJokeCard(
            joke = joke,
            revealAnswer = revealAnswer,
            onToggleReveal = { revealAnswer = !revealAnswer },
            onNext = {
                selectedIndex = (selectedIndex + 1) % FunJokes.size
                revealAnswer = false
            }
        )
        FunMoodBoard()
    }
}

@Composable
private fun PermissionAdminPage(
    modifier: Modifier,
    tab: OpenTabItem,
    onBackToWorkbench: (() -> Unit)?,
    repository: OpenTeamBusinessRepository = remember { OpenTeamBusinessRepository() },
    fallbackRepository: OpenTeamBusinessMockRepository = remember { OpenTeamBusinessMockRepository() }
) {
    val coroutineScope = rememberCoroutineScope()
    var profile by remember {
        mutableStateOf(fallbackRepository.currentProfile(account = OpenSessionManager.lastAccount))
    }
    var teams by remember { mutableStateOf<List<OpenTeamDto>>(emptyList()) }
    var membersByTeam by remember { mutableStateOf<Map<String, List<OpenTeamMemberDto>>>(emptyMap()) }
    var selectedMember by remember { mutableStateOf<OpenTeamMemberDto?>(null) }
    var expandedTeamIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showCreateTeamDialog by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableStateOf(0) }
    var loadState by remember { mutableStateOf(BusinessLoadState()) }

    suspend fun loadPermissionAdminPage() {
        loadState = BusinessLoadState(loading = true)
        var nextMessage: String? = null
        val nextProfile = when (val result = repository.currentProfile()) {
            is OpenApiResult.Success -> result.data
            is OpenApiResult.Failed -> {
                nextMessage = "用户信息加载失败：${result.message}"
                fallbackRepository.currentProfile(account = OpenSessionManager.lastAccount)
            }
        }
        profile = nextProfile
        val nextTeams = when (val result = repository.teams()) {
            is OpenApiResult.Success -> result.data
            is OpenApiResult.Failed -> {
                nextMessage = nextMessage ?: "团队列表加载失败：${result.message}"
                emptyList()
            }
        }
        teams = nextTeams
        if (expandedTeamIds.isEmpty() && nextTeams.isNotEmpty()) {
            expandedTeamIds = setOf(nextTeams.first().teamId)
        }
        membersByTeam = nextTeams.associate { team ->
            val members = when (val result = repository.teamMembers(teamId = team.teamId)) {
                is OpenApiResult.Success -> result.data
                is OpenApiResult.Failed -> {
                    nextMessage = nextMessage ?: "${team.teamName} 成员加载失败：${result.message}"
                    emptyList()
                }
            }
            team.teamId to members
        }
        loadState = BusinessLoadState(loading = false, message = nextMessage)
    }

    LaunchedEffect(refreshKey) {
        loadPermissionAdminPage()
    }

    OpenTabScaffold(modifier = modifier, tab = tab, onBackToWorkbench = onBackToWorkbench) {
        BusinessStatusCard(state = loadState)
        PermissionDirectoryHeader(
            profile = profile,
            teams = teams,
            membersByTeam = membersByTeam,
            onCreateTeamClick = { showCreateTeamDialog = true }
        )
        teams.forEach { team ->
            val members = membersByTeam[team.teamId].orEmpty()
            PermissionTeamSection(
                team = team,
                members = members,
                expanded = expandedTeamIds.contains(team.teamId),
                onToggleExpanded = {
                    expandedTeamIds = if (expandedTeamIds.contains(team.teamId)) {
                        expandedTeamIds - team.teamId
                    } else {
                        expandedTeamIds + team.teamId
                    }
                },
                canEdit = OpenBusinessPermissionRules.canManageTeams(profile),
                onMemberClick = { member -> selectedMember = member }
            )
        }
        selectedMember?.let { member ->
            MemberPermissionDialog(
                member = member,
                teams = teams,
                canEdit = OpenBusinessPermissionRules.canManageTeams(profile),
                onClose = { selectedMember = null },
                onSave = { targetTeamId, targetRole ->
                    coroutineScope.launch {
                        if (targetTeamId != member.teamId) {
                            when (val addResult = repository.addTeamMember(
                                teamId = targetTeamId,
                                userId = member.userId,
                                teamRole = targetRole
                            )) {
                                is OpenApiResult.Success -> {
                                    selectedMember = null
                                    loadState = BusinessLoadState(
                                        loading = false,
                                        message = "已加入新部门。原部门移出需要服务端成员移出状态闭环修正后再开放。"
                                    )
                                    refreshKey += 1
                                }

                                is OpenApiResult.Failed -> {
                                    loadState = BusinessLoadState(loading = false, message = "部门调整失败：${addResult.message}")
                                }
                            }
                        } else {
                            when (val result = repository.updateTeamMemberRole(
                                teamId = member.teamId,
                                userId = member.userId,
                                teamRole = targetRole
                            )) {
                                is OpenApiResult.Success -> {
                                    selectedMember = null
                                    loadState = BusinessLoadState(loading = false, message = "成员角色已更新。")
                                    refreshKey += 1
                                }

                                is OpenApiResult.Failed -> {
                                    loadState = BusinessLoadState(loading = false, message = "角色保存失败：${result.message}")
                                }
                            }
                        }
                    }
                }
            )
        }
        if (showCreateTeamDialog) {
            CreateTeamDialog(
                onClose = { showCreateTeamDialog = false },
                onSave = { teamName, description ->
                    coroutineScope.launch {
                        when (val result = repository.createTeam(teamName = teamName, description = description)) {
                            is OpenApiResult.Success -> {
                                showCreateTeamDialog = false
                                expandedTeamIds = expandedTeamIds + result.data.teamId
                                loadState = BusinessLoadState(loading = false, message = "部门已创建。")
                                refreshKey += 1
                            }

                            is OpenApiResult.Failed -> {
                                loadState = BusinessLoadState(loading = false, message = "创建部门失败：${result.message}")
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun ApprovalDetailDialog(
    profile: OpenTeamProfile,
    item: OpenApprovalItem,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit,
    onCancelApproval: (String) -> Unit,
    onClose: () -> Unit
) {
    var comment by remember(item.id) { mutableStateOf("") }
    var showRejectConfirm by remember(item.id) { mutableStateOf(false) }
    var showCancelConfirm by remember(item.id) { mutableStateOf(false) }
    Dialog(onDismissRequest = onClose) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .clip(shape = RoundedCornerShape(size = 8.dp))
                .background(color = AppTheme.colorScheme.c_FFFFFFFF_FF101010.color)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(space = 12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(weight = 1f),
                    verticalArrangement = Arrangement.spacedBy(space = 3.dp)
                ) {
                    Text(
                        text = item.displayApprovalTitle(),
                        fontSize = 18.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
                    )
                    Text(
                        text = "${item.type.toApprovalTypeText()} · ${item.status.toApprovalStatusText()}",
                        fontSize = 13.sp,
                        lineHeight = 16.sp,
                        color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
                    )
                }
                IconButton(
                    modifier = Modifier.size(size = 34.dp),
                    onClick = onClose
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "关闭详情",
                        tint = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(state = rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(space = 10.dp),
                horizontalAlignment = Alignment.Start
            ) {
                ApprovalInfoBlock(title = "基础信息") {
                    ApprovalInfoRow(label = "所属团队", value = item.teamName.toCleanBusinessText() ?: "未填写")
                    ApprovalInfoRow(label = "申请人", value = item.applicant.toCleanBusinessText() ?: item.applicantId.ifBlank { "未填写" })
                    ApprovalInfoRow(label = "审批人", value = item.approver.toCleanBusinessText() ?: item.approverId.ifBlank { "未分配" })
                    ApprovalInfoRow(label = "提交时间", value = formatBusinessTime(item.createdAt).ifBlank { "未记录" })
                    item.updatedAt?.let { updatedAt ->
                        ApprovalInfoRow(label = "更新时间", value = formatBusinessTime(updatedAt).ifBlank { "未记录" })
                    }
                }
                ApprovalInfoBlock(title = "${item.type.toApprovalTypeText()}内容") {
                    ApprovalInfoRow(label = "审批标题", value = item.displayApprovalTitle())
                    ApprovalInfoRow(label = "申请原因", value = item.reason.toCleanBusinessText() ?: "未填写")
                    val formRows = item.form.toApprovalFormRows(type = item.type)
                    if (formRows.isEmpty()) {
                        ApprovalInfoRow(label = "表单内容", value = "未填写")
                    } else {
                        formRows.forEach { row ->
                            ApprovalInfoRow(label = row.label, value = row.value)
                        }
                    }
                }
                ApprovalInfoBlock(title = "审批流转") {
                    ApprovalInfoRow(label = "当前状态", value = item.status.toApprovalStatusText())
                    ApprovalInfoRow(
                        label = "审批意见",
                        value = item.comment.toCleanBusinessText() ?: if (item.status == OpenApprovalStatus.Pending) "待审批人填写" else "未填写"
                    )
                    ApprovalInfoRow(
                        label = "操作权限",
                        value = when {
                            OpenBusinessPermissionRules.canApprove(profile, item) -> "可通过或驳回"
                            OpenBusinessPermissionRules.canCancelApproval(profile, item) -> "可撤回"
                            else -> "仅可查看"
                        }
                    )
                }
                ApprovalOperationBlock(
                    profile = profile,
                    item = item,
                    comment = comment,
                    onCommentChange = { comment = it },
                    showCancelConfirm = showCancelConfirm,
                    onApprove = { onApprove(comment) },
                    onRejectClick = { showRejectConfirm = true },
                    onCancelApprovalClick = { showCancelConfirm = true },
                    onCancelApprovalCancel = { showCancelConfirm = false },
                    onCancelApprovalConfirm = { onCancelApproval(comment) }
                )
            }
        }
    }
    if (showRejectConfirm) {
        ApprovalConfirmDialog(
            title = "确认驳回审批",
            body = "驳回后发起人会看到审批被驳回和你填写的意见。",
            confirmText = "确认驳回",
            onClose = { showRejectConfirm = false },
            onCancel = { showRejectConfirm = false },
            onConfirm = {
                showRejectConfirm = false
                onReject(comment)
            }
        )
    }
}

@Composable
private fun ApprovalInfoBlock(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(size = 8.dp))
            .background(color = AppTheme.colorScheme.c_FFEFF1F3_FF22202A.color)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = title,
            fontSize = 15.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
        )
        content()
    }
}

@Composable
private fun ApprovalInfoRow(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 3.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
        )
        Text(
            text = value,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
        )
    }
}

@Composable
private fun ApprovalOperationBlock(
    profile: OpenTeamProfile,
    item: OpenApprovalItem,
    comment: String,
    onCommentChange: (String) -> Unit,
    showCancelConfirm: Boolean,
    onApprove: () -> Unit,
    onRejectClick: () -> Unit,
    onCancelApprovalClick: () -> Unit,
    onCancelApprovalCancel: () -> Unit,
    onCancelApprovalConfirm: () -> Unit
) {
    when {
        OpenBusinessPermissionRules.canApprove(profile, item) -> {
            ApprovalInfoBlock(title = "审批处理") {
                TeamTextField(value = comment, onValueChange = onCommentChange, label = "审批意见")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(space = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BusinessActionButton(
                        modifier = Modifier.weight(weight = 1f),
                        text = "驳回",
                        primary = true,
                        danger = true,
                        onClick = onRejectClick
                    )
                    BusinessActionButton(
                        modifier = Modifier.weight(weight = 1f),
                        text = "通过",
                        primary = true,
                        onClick = onApprove
                    )
                }
            }
        }
        OpenBusinessPermissionRules.canCancelApproval(profile, item) -> {
            ApprovalInfoBlock(title = "撤回审批") {
                TeamTextField(value = comment, onValueChange = onCommentChange, label = "撤回说明")
                BusinessActionButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = "撤回审批",
                    primary = true,
                    onClick = onCancelApprovalClick
                )
                if (showCancelConfirm) {
                    ApprovalInlineConfirm(
                        title = "确认撤回审批",
                        body = "撤回后该审批将不再进入主管待我审批列表。",
                        confirmText = "确认撤回",
                        onCancel = onCancelApprovalCancel,
                        onConfirm = onCancelApprovalConfirm
                    )
                }
            }
        }
    }
}

@Composable
private fun ApprovalInlineConfirm(
    title: String,
    body: String,
    confirmText: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(size = 8.dp))
            .background(color = AppTheme.colorScheme.c_FFFFFFFF_FF101010.color)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
        )
        Text(
            text = body,
            fontSize = 13.sp,
            lineHeight = 17.sp,
            color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
        )
        FormActionRow(
            primaryText = confirmText,
            onCancelClick = onCancel,
            onPrimaryClick = onConfirm
        )
    }
}

@Composable
private fun ApprovalConfirmDialog(
    title: String,
    body: String,
    confirmText: String,
    onClose: () -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onClose) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape = RoundedCornerShape(size = 8.dp))
                .background(color = AppTheme.colorScheme.c_FFFFFFFF_FF101010.color)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(space = 12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.weight(weight = 1f),
                    text = title,
                    fontSize = 17.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
                )
                IconButton(
                    modifier = Modifier.size(size = 34.dp),
                    onClick = onClose
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "关闭确认弹窗",
                        tint = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
                    )
                }
            }
            Text(
                text = body,
                fontSize = 14.sp,
                lineHeight = 19.sp,
                color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
            )
            FormActionRow(
                primaryText = confirmText,
                onCancelClick = onCancel,
                onPrimaryClick = onConfirm
            )
        }
    }
}

@Composable
private fun CalendarDetailCard(
    profile: OpenTeamProfile,
    event: OpenCalendarEvent,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit
) {
    DetailCard(
        title = event.title,
        body = buildString {
            appendLine("\u8303\u56f4\uff1a${event.visibility.toCalendarVisibilityText()}")
            appendLine("\u5f52\u5c5e\uff1a${event.teamName ?: "\u5168\u516c\u53f8"}")
            appendLine("\u65f6\u95f4\uff1a${formatEventTimeRange(event.startTime, event.endTime)}")
            event.location?.let { appendLine("\u5730\u70b9\uff1a$it") }
            appendLine("\u521b\u5efa\u4eba\uff1a${event.creator}")
            appendLine("\u53c2\u4e0e\u4eba\uff1a${event.participants.joinToString { it.displayName }}")
            appendLine(event.description)
            if (OpenBusinessPermissionRules.canManageCalendarEvent(profile, event)) {
                append("\u53ef\u64cd\u4f5c\uff1a\u7f16\u8f91 / \u5220\u9664")
            } else {
                append("\u5f53\u524d\u8d26\u53f7\u4ec5\u53ef\u67e5\u770b")
            }
        },
        onClose = onClose
    )
    if (OpenBusinessPermissionRules.canManageCalendarEvent(profile, event)) {
        Row(horizontalArrangement = Arrangement.spacedBy(space = 8.dp)) {
            SmallIconButton(text = "\u7f16\u8f91", onClick = onEdit)
            SmallIconButton(text = "\u5220\u9664", onClick = onDelete)
        }
    }
}

@Composable
private fun AnnouncementDetailCard(
    profile: OpenTeamProfile,
    announcement: OpenAnnouncementItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit
) {
    DetailCard(
        title = announcement.title,
        body = buildString {
            appendLine("范围：${announcement.teamName ?: "全公司"}")
            appendLine("发布人：${announcement.publisher}")
            appendLine("发布时间：${formatBusinessTime(announcement.createdAt)}")
            announcement.updatedAt?.let { appendLine("更新时间：${formatBusinessTime(it)}") }
            if (announcement.pinned) {
                appendLine("状态：置顶")
            }
            appendLine(announcement.content)
            if (OpenBusinessPermissionRules.canManageAnnouncement(profile, announcement)) {
                append("可操作：编辑 / 删除")
            } else {
                append("当前账号仅可查看")
            }
        },
        onClose = onClose
    )
    if (OpenBusinessPermissionRules.canManageAnnouncement(profile, announcement)) {
        Row(horizontalArrangement = Arrangement.spacedBy(space = 8.dp)) {
            SmallIconButton(text = "编辑", onClick = onEdit)
            SmallIconButton(text = "删除", onClick = onDelete)
        }
    }
}


@Composable
private fun CalendarPlaceholderPage(
    modifier: Modifier,
    tab: OpenTabItem,
    onBackToWorkbench: (() -> Unit)?
) {
    OpenTabScaffold(modifier = modifier, tab = tab, onBackToWorkbench = onBackToWorkbench) {
        SectionCard(
            title = "日程管理",
            body = "日程管理功能仍在接入中，后续会展示团队日程、会议安排和提醒功能。"
        )
    }
}


@Composable
private fun FinancePlaceholderPage(
    modifier: Modifier,
    tab: OpenTabItem,
    onBackToWorkbench: (() -> Unit)?
) {
    OpenTabScaffold(modifier = modifier, tab = tab, onBackToWorkbench = onBackToWorkbench) {
        SectionCard(
            title = "财务看板",
            body = "财务数据看板仍在接入中，后续会展示预算、报销和付款进度。"
        )
    }
}

@Composable
private fun OnCallPlaceholderPage(
    modifier: Modifier,
    onBackToWorkbench: (() -> Unit)?
) {
    OpenOnCallPage(modifier = modifier, onBackToOnCallHome = onBackToWorkbench)
}

@Composable
private fun WebTabPlaceholderPage(
    modifier: Modifier,
    tab: OpenTabItem,
    onBackToWorkbench: (() -> Unit)?
) {
    WebTabPage(modifier = modifier, tab = tab, onBackToWorkbench = onBackToWorkbench)
}

@Composable
private fun ExternalTabPlaceholderPage(
    modifier: Modifier,
    tab: OpenTabItem,
    onBackToWorkbench: (() -> Unit)?
) {
    OpenTabScaffold(modifier = modifier, tab = tab, onBackToWorkbench = onBackToWorkbench) {
        SectionCard(
            title = "外部入口暂不可用",
            body = "该入口需要跳转到外部应用，当前客户端暂未开放直接跳转。"
        )
    }
}

@Composable
private fun RegisteredTabHost(
    modifier: Modifier,
    tab: OpenTabItem,
    registeredTab: RegisteredOpenTab,
    onBackToWorkbench: (() -> Unit)?
) {
    var lifecycleError by remember(tab.id) { mutableStateOf<String?>(null) }
    RegisteredTabLifecycleEffect(
        registeredTab = registeredTab,
        onError = { message ->
            lifecycleError = message
        }
    )
    LaunchedEffect(registeredTab.definition.route) {
        OpenTabContainer.switchTab(route = registeredTab.definition.route)
    }
    if (lifecycleError != null) {
        OpenStatePage(
            modifier = modifier,
            title = tab.displayName,
            message = lifecycleError.orEmpty(),
            onBackToWorkbench = onBackToWorkbench
        )
        return
    }
    OpenTabScaffold(
        modifier = modifier,
        tab = tab,
        registeredTab = registeredTab,
        onBackToWorkbench = onBackToWorkbench
    ) {
        registeredTab.page()
        registeredTab.definition.extension?.bottomPanel?.let { bottomPanel ->
            SectionCard(
                title = "底部扩展区",
                body = "默认高度 ${bottomPanel.defaultHeight}dp，内容由业务方通过协议提供。"
            )
            bottomPanel.content()
        }
    }
}

@Composable
private fun RegisteredTabLifecycleEffect(
    registeredTab: RegisteredOpenTab,
    onError: (String) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val configuration = LocalConfiguration.current
    DisposableEffect(registeredTab.definition.id, lifecycleOwner) {
        fun dispatch(name: String, callback: () -> Unit) {
            val startTime = SystemClock.elapsedRealtime()
            runCatching(callback).onFailure { throwable ->
                onError(
                    "协议错误码：${TabErrors.LIFECYCLE_EXCEPTION} · ${TabErrors.description(TabErrors.LIFECYCLE_EXCEPTION)}\n" +
                        "生命周期 $name 执行异常：${throwable.message ?: "未知异常"}"
                )
            }
            val cost = SystemClock.elapsedRealtime() - startTime
            if (cost > TabErrors.CALLBACK_TIMEOUT_MS) {
                onError(
                    "协议错误码：${TabErrors.LIFECYCLE_TIMEOUT} · ${TabErrors.description(TabErrors.LIFECYCLE_TIMEOUT)}\n" +
                        "生命周期 $name 耗时 ${cost}ms，超过 ${TabErrors.CALLBACK_TIMEOUT_MS}ms。"
                )
            }
        }

        dispatch(name = "onCreate") {
            registeredTab.lifecycle.onCreate(Bundle())
        }
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            dispatch(name = "onResume") {
                registeredTab.lifecycle.onResume()
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> dispatch(name = "onResume") {
                    registeredTab.lifecycle.onResume()
                }
                Lifecycle.Event.ON_PAUSE -> dispatch(name = "onPause") {
                    registeredTab.lifecycle.onPause()
                }
                Lifecycle.Event.ON_DESTROY -> dispatch(name = "onDestroy") {
                    registeredTab.lifecycle.onDestroy()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            dispatch(name = "onDestroy") {
                registeredTab.lifecycle.onDestroy()
            }
        }
    }
    LaunchedEffect(configuration, registeredTab.definition.id) {
        registeredTab.lifecycle.onConfigChange(
            Configuration(configuration).apply {
                setTo(configuration)
            }
        )
    }
}

@Composable
fun ProtocolGuideTabPage() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 12.dp),
        horizontalAlignment = Alignment.Start
    ) {
        SectionCard(
            title = "业务接入三件套",
            body = "业务方先写页面，再提供 TabDefinition、TabLifecycle 和页面函数。容器收到后负责校验、展示入口、切换路由和调生命周期。"
        )
        ProtocolStepRow(
            index = "1",
            title = "定义 TabDefinition",
            body = "填写 id、displayName、icon、route、version、minContainerVersion 和 permissions。"
        )
        ProtocolStepRow(
            index = "2",
            title = "注册到容器",
            body = "调用 registerTab(definition, lifecycle, page)，容器会检查重复 ID、容器版本和路由格式。"
        )
        ProtocolStepRow(
            index = "3",
            title = "点击后渲染",
            body = "用户点击工作台图标后，容器按 route 找到注册页面并渲染，同时触发 onCreate/onResume/onPause/onDestroy。"
        )
        SectionCard(
            title = "当前示例状态",
            body = "该页面就是通过协议注册进入工作台的示例 Tab。它不是硬编码 route 页面，而是由容器注册表动态找到并展示。"
        )
        ProtocolFieldGrid()
    }
}

@Composable
private fun ProtocolStepRow(
    index: String,
    title: String,
    body: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(size = 8.dp))
            .background(color = AppTheme.colorScheme.c_FFEFF1F3_FF22202A.color)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(size = 28.dp)
                .clip(shape = RoundedCornerShape(size = 8.dp))
                .background(color = AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = index,
                fontSize = 13.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Column(
            modifier = Modifier.weight(weight = 1f),
            verticalArrangement = Arrangement.spacedBy(space = 5.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
            )
            Text(
                text = body,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
            )
        }
    }
}

@Composable
private fun ProtocolFieldGrid() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(size = 8.dp))
            .background(color = AppTheme.colorScheme.c_FFEFF1F3_FF22202A.color)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "字段校验",
            fontSize = 16.sp,
            lineHeight = 19.sp,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
        )
        ProtocolFieldRow(name = "id", value = "唯一编号，防止两个业务入口冲突")
        ProtocolFieldRow(name = "displayName", value = "展示名称，不能为空且不超过 16 字")
        ProtocolFieldRow(name = "icon", value = "工作台入口图标，帮助用户快速识别业务")
        ProtocolFieldRow(name = "route", value = "点击后的业务地址，必须以 / 开头")
        ProtocolFieldRow(name = "version", value = "业务版本，用于后续升级和排查")
        ProtocolFieldRow(name = "minContainerVersion", value = "最低容器版本，防止老客户端硬打开新能力")
    }
}

@Composable
private fun ProtocolFieldRow(
    name: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(space = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            modifier = Modifier.weight(weight = 0.42f),
            text = name,
            fontSize = 13.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color
        )
        Text(
            modifier = Modifier.weight(weight = 0.58f),
            text = value,
            fontSize = 13.sp,
            lineHeight = 17.sp,
            color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun WebTabPage(
    modifier: Modifier,
    tab: OpenTabItem,
    onBackToWorkbench: (() -> Unit)?
) {
    val entryUri = tab.manifest.entryUri.orEmpty()
    val allowedHosts = remember(entryUri, tab.manifest.extraConfig) {
        tab.manifest.extraConfig.toAllowedWebHosts(entryUri = entryUri)
    }
    val fallbackUrl = remember(entryUri, tab.manifest.extraConfig) {
        tab.manifest.extraConfig.toFallbackAssetUrl()
    }
    var usingFallback by remember(entryUri) { mutableStateOf(false) }
    val targetUrl = if (usingFallback) fallbackUrl ?: entryUri else entryUri
    var loading by remember(entryUri) { mutableStateOf(true) }
    var errorMessage by remember(entryUri) { mutableStateOf<String?>(null) }
    var fallbackMessage by remember(entryUri) { mutableStateOf<String?>(null) }
    var webView by remember(entryUri) { mutableStateOf<WebView?>(null) }
    var canGoBack by remember(entryUri) { mutableStateOf(false) }
    var currentUrl by remember(entryUri) { mutableStateOf(entryUri) }
    fun switchToFallback(view: WebView?, message: String) {
        if (fallbackUrl != null && !usingFallback) {
            usingFallback = true
            loading = true
            errorMessage = null
            fallbackMessage = message
            view?.loadUrl(fallbackUrl)
        } else {
            loading = false
            errorMessage = message
        }
    }
    BackHandler(enabled = canGoBack) {
        webView?.goBack()
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = AppTheme.colorScheme.c_FFFFFFFF_FF101010.color)
            .statusBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(space = 10.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(space = 6.dp)
        ) {
            OpenTabHeader(tab = tab, onBackToWorkbench = onBackToWorkbench)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(space = 8.dp)
            ) {
                SmallIconButton(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    text = "网页返回",
                    enabled = canGoBack,
                    onClick = {
                        webView?.goBack()
                    }
                )
                SmallIconButton(
                    icon = Icons.Rounded.Refresh,
                    text = "重新加载",
                    onClick = {
                        usingFallback = false
                        loading = true
                        errorMessage = null
                        fallbackMessage = null
                        webView?.loadUrl(entryUri)
                    }
                )
            }
            fallbackMessage?.let { message ->
                Text(
                    text = message,
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    color = Color(color = 0xFFB45309)
                )
            }
        }
        Box(
            modifier = Modifier
                .weight(weight = 1f)
                .fillMaxWidth()
        ) {
            if (entryUri.isBlank()) {
                OpenStatePage(
                    modifier = Modifier.fillMaxSize(),
                    title = "Web Tab 配置异常",
                    message = "服务端没有为 ${tab.displayName} 下发 entryUri。",
                    onBackToWorkbench = null
                )
                return@Box
            }
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        webView = this
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        webChromeClient = WebChromeClient()
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val nextUri = request?.url ?: return false
                                if (nextUri.isAllowedWebNavigation(allowedHosts = allowedHosts)) {
                                    return false
                                }
                                val blockedTarget = nextUri.host.orEmpty().ifBlank { nextUri.scheme.orEmpty() }
                                errorMessage = "已拦截非白名单链接：$blockedTarget"
                                return true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                loading = false
                                canGoBack = view?.canGoBack() == true
                                currentUrl = url.orEmpty().ifBlank { entryUri }
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                if (request?.isForMainFrame == true) {
                                    canGoBack = view?.canGoBack() == true
                                    val message = error?.description?.toString()?.ifBlank { null } ?: "网页加载失败"
                                    if (fallbackUrl != null && !usingFallback) {
                                        switchToFallback(
                                            view = view,
                                            message = "外部页面不可用，已切换到本地短视频演示。"
                                        )
                                    } else {
                                        loading = false
                                        errorMessage = message
                                    }
                                }
                            }

                            override fun onReceivedHttpError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                errorResponse: WebResourceResponse?
                            ) {
                                val statusCode = errorResponse?.statusCode ?: return
                                if (request?.isForMainFrame == true && statusCode >= 400) {
                                    canGoBack = view?.canGoBack() == true
                                    if (fallbackUrl != null && !usingFallback) {
                                        switchToFallback(
                                            view = view,
                                            message = "外部页面返回 $statusCode，已切换到本地短视频演示。"
                                        )
                                    } else {
                                        loading = false
                                        errorMessage = "网页返回异常状态：$statusCode"
                                    }
                                }
                            }
                        }
                        isVerticalScrollBarEnabled = false
                        isHorizontalScrollBarEnabled = false
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.cacheMode = WebSettings.LOAD_DEFAULT
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        settings.loadsImagesAutomatically = true
                        settings.javaScriptCanOpenWindowsAutomatically = false
                        settings.setSupportZoom(false)
                        settings.builtInZoomControls = false
                        settings.displayZoomControls = false
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        loadUrl(targetUrl)
                    }
                },
                update = { webView ->
                    if (webView.url != targetUrl) {
                        loading = true
                        errorMessage = null
                        webView.loadUrl(targetUrl)
                    }
                }
            )
            if (loading) {
                SectionCard(
                    title = "正在加载网页",
                    body = "容器正在打开 ${tab.displayName}。"
                )
            }
            if (errorMessage != null) {
                SectionCard(
                    title = "网页加载失败",
                    body = errorMessage.orEmpty()
                )
            }
        }
    }
}

private fun Map<String, String>.toAllowedWebHosts(entryUri: String): Set<String> {
    val configuredHosts = get("allowedHosts")
        ?.split(",", ";")
        ?.map { host -> host.normalizedWebHost() }
        ?.filter { host -> host.isNotBlank() && host != "unknown" }
        .orEmpty()
    val entryHost = runCatching {
        android.net.Uri.parse(entryUri).host.orEmpty().normalizedWebHost()
    }.getOrDefault("")
    return (configuredHosts + entryHost).filter { host -> host.isNotBlank() }.toSet()
}

private fun Map<String, String>.toFallbackAssetUrl(): String? {
    val asset = get("fallbackAsset")
        ?.trim()
        ?.trimStart('/')
        ?.takeIf { it.isNotBlank() && !it.contains("..") }
        ?: return null
    return "file:///android_asset/$asset"
}

private fun android.net.Uri.isAllowedWebNavigation(allowedHosts: Set<String>): Boolean {
    val normalizedScheme = scheme.orEmpty().lowercase()
    if (normalizedScheme == "file") {
        return toString().startsWith(prefix = "file:///android_asset/")
    }
    if (normalizedScheme == "about" || normalizedScheme == "data") {
        return true
    }
    if (normalizedScheme != "http" && normalizedScheme != "https") {
        return false
    }
    val requestHost = host.orEmpty().normalizedWebHost()
    return allowedHosts.isEmpty() || allowedHosts.any { allowedHost ->
        requestHost == allowedHost || requestHost.endsWith(suffix = ".$allowedHost")
    }
}

private fun String.normalizedWebHost(): String {
    return trim()
        .lowercase()
        .removePrefix("www.")
}

@Composable
private fun OpenTabScaffold(
    modifier: Modifier,
    tab: OpenTabItem,
    onBackToWorkbench: (() -> Unit)?,
    registeredTab: RegisteredOpenTab? = null,
    scrollState: ScrollState = rememberScrollState(),
    titleOverride: String? = null,
    backTextOverride: String? = null,
    centerTitle: Boolean = true,
    compactTitle: Boolean = true,
    iconOnlyBack: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = AppTheme.colorScheme.c_FFFFFFFF_FF101010.color)
            .statusBarsPadding()
            .verticalScroll(state = scrollState)
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(space = 14.dp),
        horizontalAlignment = Alignment.Start
    ) {
        OpenTabHeader(
            tab = tab,
            onBackToWorkbench = onBackToWorkbench,
            registeredTab = registeredTab,
            titleOverride = titleOverride,
            backTextOverride = backTextOverride,
            centerTitle = centerTitle,
            compactTitle = compactTitle,
            iconOnlyBack = iconOnlyBack
        )
        content()
        registeredTab?.definition?.extension?.fab?.let { fab ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape = RoundedCornerShape(size = 8.dp))
                    .background(color = AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color)
                    .clickable(onClick = fab.onClick)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    modifier = Modifier.size(size = 18.dp),
                    imageVector = fab.icon,
                    contentDescription = null,
                    tint = Color.White
                )
                Text(
                    modifier = Modifier.padding(start = 8.dp),
                    text = fab.label,
                    fontSize = 14.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun OpenTabHeader(
    tab: OpenTabItem,
    onBackToWorkbench: (() -> Unit)?,
    registeredTab: RegisteredOpenTab? = null,
    titleOverride: String? = null,
    backTextOverride: String? = null,
    centerTitle: Boolean = true,
    compactTitle: Boolean = true,
    iconOnlyBack: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBackToWorkbench != null) {
            if (iconOnlyBack) {
                Box(
                    modifier = Modifier
                        .size(size = 36.dp)
                        .clip(shape = RoundedCornerShape(size = 999.dp))
                        .clickable(onClick = onBackToWorkbench),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier.size(size = 20.dp),
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = null,
                        tint = AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .clip(shape = RoundedCornerShape(size = 999.dp))
                        .background(color = AppTheme.colorScheme.c_FFEFF1F3_FF22202A.color)
                        .clickable(onClick = onBackToWorkbench)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(space = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        modifier = Modifier.size(size = 18.dp),
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = null,
                        tint = AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color
                    )
                    Text(
                        text = backTextOverride ?: "工作台",
                        fontSize = 13.sp,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color
                    )
                }
            }
        } else if (centerTitle) {
            Box(modifier = Modifier.size(size = 36.dp))
        }
        Column(
            modifier = Modifier.weight(weight = 1f),
            verticalArrangement = Arrangement.spacedBy(space = 2.dp),
            horizontalAlignment = if (centerTitle) Alignment.CenterHorizontally else Alignment.Start
        ) {
            Text(
                text = titleOverride ?: tab.displayName,
                fontSize = if (compactTitle) 18.sp else 22.sp,
                lineHeight = if (compactTitle) 22.sp else 26.sp,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
            )
            if (registeredTab != null) {
                Text(
                    text = "协议注册 · ${registeredTab.definition.version}",
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
                )
            }
        }
        if (registeredTab != null) {
            ProtocolHeaderActions(registeredTab = registeredTab)
        } else if (centerTitle) {
            Box(modifier = Modifier.size(size = 36.dp))
        }
    }
}

@Composable
private fun ProtocolHeaderActions(registeredTab: RegisteredOpenTab) {
    val titleBar = registeredTab.definition.extension?.titleBar
    var menuExpanded by remember { mutableStateOf(false) }
    val menuItems = titleBar?.menuItems.orEmpty()
    val rightIcon = titleBar?.rightIcon
    val rightText = titleBar?.rightText
    when {
        menuItems.isNotEmpty() -> {
            Box {
                Row(
                    modifier = Modifier
                        .clip(shape = RoundedCornerShape(size = 999.dp))
                        .background(color = AppTheme.colorScheme.c_FFEFF1F3_FF22202A.color)
                        .clickable { menuExpanded = true }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(space = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = rightText ?: "更多",
                        fontSize = 12.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color
                    )
                    Icon(
                        modifier = Modifier.size(size = 16.dp),
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        tint = AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    menuItems.forEach { item ->
                        DropdownMenuItem(
                            text = {
                                Text(text = item.label)
                            },
                            onClick = {
                                menuExpanded = false
                                item.onClick()
                            }
                        )
                    }
                }
            }
        }
        rightIcon != null -> {
            Box(
                modifier = Modifier
                    .size(size = 36.dp)
                    .clip(shape = RoundedCornerShape(size = 999.dp))
                    .background(color = AppTheme.colorScheme.c_FFEFF1F3_FF22202A.color),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier.size(size = 18.dp),
                    imageVector = rightIcon,
                    contentDescription = null,
                    tint = AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color
                )
            }
        }
        rightText?.isNotBlank() == true -> {
            Text(
                modifier = Modifier
                    .clip(shape = RoundedCornerShape(size = 999.dp))
                    .background(color = AppTheme.colorScheme.c_FFEFF1F3_FF22202A.color)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                text = rightText,
                fontSize = 12.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color
            )
        }
        else -> {
            Box(modifier = Modifier.size(size = 36.dp))
        }
    }
}

@Composable
private fun OpenStatePage(
    modifier: Modifier,
    title: String,
    message: String,
    onBackToWorkbench: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = AppTheme.colorScheme.c_FFFFFFFF_FF101010.color)
            .statusBarsPadding()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(
            space = 12.dp,
            alignment = Alignment.CenterVertically
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            modifier = Modifier,
            text = title,
            fontSize = 22.sp,
            lineHeight = 26.sp,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
        )
        Text(
            modifier = Modifier,
            text = message,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
        )
        if (onBackToWorkbench != null) {
            Row(
                modifier = Modifier
                    .clip(shape = RoundedCornerShape(size = 999.dp))
                    .background(color = AppTheme.colorScheme.c_FFEFF1F3_FF22202A.color)
                    .clickable(onClick = onBackToWorkbench)
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.spacedBy(space = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    modifier = Modifier.size(size = 18.dp),
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = null,
                    tint = AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color
                )
                Text(
                    text = "工作台",
                    fontSize = 13.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color
                )
            }
        }
    }
}

@Composable
private fun MetricRow(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(size = 8.dp))
            .background(
                color = if (selected) {
                    AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color.copy(alpha = 0.14f)
                } else {
                    AppTheme.colorScheme.c_FFEFF1F3_FF22202A.color
                }
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier,
            text = label,
            fontSize = 15.sp,
            lineHeight = 18.sp,
            color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
        )
        Text(
            modifier = Modifier,
            text = value,
            fontSize = 20.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
        )
    }
}

@Composable
private fun ClickableInfoRow(
    title: String,
    body: String,
    highlighted: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(size = 8.dp))
            .background(
                color = if (highlighted) {
                    AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color.copy(alpha = 0.14f)
                } else {
                    AppTheme.colorScheme.c_FFEFF1F3_FF22202A.color
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(space = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(weight = 1f),
            verticalArrangement = Arrangement.spacedBy(space = 5.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
            )
            Text(
                text = body,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
            )
        }
        Icon(
            modifier = Modifier.size(size = 20.dp),
            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = null,
            tint = AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color
        )
    }
}

@Composable
private fun DetailCard(
    title: String,
    body: String,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(size = 8.dp))
            .background(color = AppTheme.colorScheme.c_FFEFF1F3_FF22202A.color)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(space = 9.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(weight = 1f),
                text = title,
                fontSize = 18.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
            )
            IconButton(
                modifier = Modifier.size(size = 34.dp),
                onClick = onClose
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "关闭详情",
                    tint = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
                )
            }
        }
        Text(
            text = body,
            fontSize = 14.sp,
            lineHeight = 19.sp,
            color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
        )
    }
}

@Composable
private fun ApprovalHomePage(
    canCreateApproval: Boolean,
    onCreateClick: () -> Unit,
    onRecordsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
            .clip(shape = RoundedCornerShape(size = 8.dp))
            .background(color = AppTheme.colorScheme.c_FFFFFFFF_FF101010.color),
        verticalArrangement = Arrangement.spacedBy(space = 0.dp),
        horizontalAlignment = Alignment.Start
    ) {
        ApprovalEntryCard(
            title = "发起审批",
            body = "请假、报销、加班、采购、外出",
            icon = Icons.Rounded.EditNote,
            iconColor = Color(color = 0xFF4C8DF6),
            iconBackground = Color(color = 0xFFE8F1FF),
            enabled = canCreateApproval,
            disabledHint = "当前账号暂无发起审批权限",
            onClick = onCreateClick
        )
        ApprovalEntryDivider()
        ApprovalEntryCard(
            title = "审批记录",
            body = "按状态查看审批记录和处理结果",
            icon = Icons.AutoMirrored.Rounded.Article,
            iconColor = Color(color = 0xFF31B28B),
            iconBackground = Color(color = 0xFFE8F8F1),
            enabled = true,
            disabledHint = "",
            onClick = onRecordsClick
        )
    }
}

@Composable
private fun ApprovalEntryCard(
    title: String,
    body: String,
    icon: ImageVector,
    iconColor: Color,
    iconBackground: Color,
    enabled: Boolean,
    disabledHint: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(size = 8.dp))
            .background(color = AppTheme.colorScheme.c_FFFFFFFF_FF101010.color)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(size = 36.dp)
                .clip(shape = RoundedCornerShape(size = 6.dp))
                .background(color = if (enabled) iconBackground else AppTheme.colorScheme.c_FFEFF1F3_FF22202A.color),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier.size(size = 20.dp),
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) iconColor else AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
            )
        }
        Column(
            modifier = Modifier.weight(weight = 1f),
            verticalArrangement = Arrangement.spacedBy(space = 5.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                fontSize = 15.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
            )
            Text(
            text = if (enabled) body else disabledHint,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
            )
        }
        Icon(
            modifier = Modifier.size(size = 20.dp),
            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = null,
            tint = if (enabled) {
                AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color
            } else {
                AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
            }
        )
    }
}

@Composable
private fun ApprovalEntryDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 58.dp)
            .heightIn(min = 1.dp)
            .background(color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color.copy(alpha = 0.08f))
    )
}

@Composable
private fun AnnouncementHeroCard(
    profile: OpenTeamProfile,
    announcementCount: Int,
    canPublish: Boolean,
    onPublishClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(size = 8.dp))
            .background(color = AppTheme.colorScheme.c_FFFFFFFF_FF101010.color)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(space = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(weight = 1f),
            verticalArrangement = Arrangement.spacedBy(space = 4.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "公告",
                fontSize = 17.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
            )
            Text(
                text = "${profile.currentTeamNameOrDefault()} · $announcementCount 条可见",
                fontSize = 12.sp,
                lineHeight = 15.sp,
                color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
            )
        }
        if (canPublish) {
            BusinessActionButton(
                text = "发布公告",
                primary = true,
                onClick = onPublishClick
            )
        }
    }
}

@Composable
private fun AnnouncementArticleCard(
    announcement: OpenAnnouncementItem,
    onClick: () -> Unit
) {
    val accentColor = if (announcement.pinned) Color(color = 0xFFF59E0B) else AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(size = 8.dp))
            .background(color = AppTheme.colorScheme.c_FFFFFFFF_FF101010.color)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(space = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(size = 8.dp)
                .clip(shape = RoundedCornerShape(size = 999.dp))
                .background(color = accentColor)
        )
        Column(
            modifier = Modifier.weight(weight = 1f),
            verticalArrangement = Arrangement.spacedBy(space = 6.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.weight(weight = 1f),
                    text = announcement.title.toCleanBusinessText() ?: "未命名公告",
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
                )
                if (announcement.pinned) {
                    Text(
                        modifier = Modifier
                            .clip(shape = RoundedCornerShape(size = 999.dp))
                            .background(color = Color(color = 0xFFFFF2D6))
                            .padding(horizontal = 7.dp, vertical = 4.dp),
                        text = "置顶",
                        fontSize = 11.sp,
                        lineHeight = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(color = 0xFFF59E0B)
                    )
                }
            }
            Text(
                text = announcement.content.toCleanBusinessText()?.take(maximumLength = 54) ?: "暂无公告正文",
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
            )
            Text(
                text = "${announcement.teamName.toCleanBusinessText() ?: "全公司"} · ${announcement.publisher.toCleanBusinessText() ?: announcement.publisherId.ifBlank { "发布人" }} · ${formatBusinessTime(announcement.createdAt)}",
                fontSize = 12.sp,
                lineHeight = 15.sp,
                color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
            )
        }
        Icon(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(size = 18.dp),
            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = null,
            tint = AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color
        )
    }
}

@Composable
private fun AnnouncementDetailDialog(
    profile: OpenTeamProfile,
    announcement: OpenAnnouncementItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit
) {
    var showDeleteConfirm by remember(announcement.announcementId) { mutableStateOf(false) }
    val title = announcement.title.toCleanBusinessText() ?: "未命名公告"
    val publisher = announcement.publisher.toCleanBusinessText() ?: announcement.publisherId.ifBlank { "发布人" }
    val teamName = announcement.teamName.toCleanBusinessText() ?: "全公司"
    val paragraphs = announcement.content
        .toCleanBusinessText()
        ?.split('\n')
        ?.map { line -> line.trim() }
        ?.filter { line -> line.isNotBlank() }
        ?: emptyList()
    val summary = paragraphs.firstOrNull()?.take(maximumLength = 72) ?: "暂无公告摘要"
    Dialog(onDismissRequest = onClose) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 660.dp)
                .clip(shape = RoundedCornerShape(size = 8.dp))
                .background(color = AppTheme.colorScheme.c_FFFFFFFF_FF101010.color)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(space = 12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "公告详情",
                    fontSize = 17.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
                )
                IconButton(
                    modifier = Modifier.size(size = 34.dp),
                    onClick = onClose
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "关闭公告详情",
                        tint = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(state = rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(space = 13.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape = RoundedCornerShape(size = 8.dp))
                        .background(color = AppTheme.colorScheme.c_FFEFF1F3_FF22202A.color)
                        .padding(horizontal = 16.dp, vertical = 15.dp),
                    verticalArrangement = Arrangement.spacedBy(space = 12.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnnouncementPlainBadge(text = announcement.scope.toAnnouncementScopeText())
                        if (announcement.pinned) {
                            AnnouncementPlainBadge(
                                text = "置顶",
                                color = Color(color = 0xFFF59E0B),
                                backgroundColor = Color(color = 0xFFFFF2D6)
                            )
                        }
                    }
                    Text(
                        text = title,
                        fontSize = 20.sp,
                        lineHeight = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
                    )
                    Text(
                        text = summary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape = RoundedCornerShape(size = 8.dp))
                        .background(color = AppTheme.colorScheme.c_FFFFFFFF_FF101010.color)
                        .padding(horizontal = 2.dp, vertical = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(space = 12.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    if (paragraphs.isEmpty()) {
                        Text(
                            text = "暂无公告正文",
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
                        )
                    } else {
                        paragraphs.forEachIndexed { index, line ->
                            Text(
                                text = line,
                                fontSize = if (index == 0 && paragraphs.size > 1) 16.sp else 15.sp,
                                lineHeight = 24.sp,
                                fontWeight = if (index == 0 && paragraphs.size > 1) FontWeight.SemiBold else FontWeight.Normal,
                                color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
                            )
                        }
                    }
                }
                ApprovalInfoBlock(title = "发布信息") {
                    ApprovalInfoRow(label = "发布范围", value = teamName)
                    ApprovalInfoRow(label = "发布人", value = publisher)
                    ApprovalInfoRow(label = "发布时间", value = formatBusinessTime(announcement.createdAt).ifBlank { "未记录" })
                    announcement.updatedAt?.let { updatedAt ->
                        ApprovalInfoRow(label = "更新时间", value = formatBusinessTime(updatedAt).ifBlank { "未记录" })
                    }
                }
                if (OpenBusinessPermissionRules.canManageAnnouncement(profile, announcement)) {
                    if (showDeleteConfirm) {
                        ApprovalInlineConfirm(
                            title = "确认删除公告",
                            body = "删除后，可见范围内的成员将不再看到该公告。",
                            confirmText = "确认删除",
                            onCancel = { showDeleteConfirm = false },
                            onConfirm = onDelete
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(space = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BusinessActionButton(
                                modifier = Modifier.weight(weight = 1f),
                                text = "删除",
                                primary = false,
                                onClick = { showDeleteConfirm = true }
                            )
                            BusinessActionButton(
                                modifier = Modifier.weight(weight = 1f),
                                text = "编辑",
                                primary = true,
                                onClick = onEdit
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnnouncementPlainBadge(
    text: String,
    color: Color = AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color,
    backgroundColor: Color = AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color.copy(alpha = 0.12f)
) {
    Text(
        modifier = Modifier
            .clip(shape = RoundedCornerShape(size = 999.dp))
            .background(color = backgroundColor)
            .padding(horizontal = 9.dp, vertical = 5.dp),
        text = text,
        fontSize = 11.sp,
        lineHeight = 13.sp,
        fontWeight = FontWeight.Bold,
        color = color
    )
}

@Composable
private fun PermissionDirectoryHeader(
    profile: OpenTeamProfile,
    teams: List<OpenTeamDto>,
    membersByTeam: Map<String, List<OpenTeamMemberDto>>,
    onCreateTeamClick: () -> Unit
) {
    val memberCount = membersByTeam.values.flatten().distinctBy { member -> member.userId }.size
    val managerCount = membersByTeam.values.flatten().count { member -> member.teamRole == "manager" }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(size = 8.dp))
            .background(color = Color(color = 0xFFF7F8FA))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "组织通讯录",
                fontSize = 15.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
            )
            if (OpenBusinessPermissionRules.canManageTeams(profile)) {
                Text(
                    modifier = Modifier
                        .clip(shape = RoundedCornerShape(size = 999.dp))
                        .background(color = Color(color = 0xFFEAF4FF))
                        .clickable(onClick = onCreateTeamClick)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    text = "新建部门",
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(color = 0xFF2563EB)
                )
            }
        }
        Text(
            text = if (OpenBusinessPermissionRules.canManageTeams(profile)) {
                "${teams.size} 个部门 · $memberCount 名员工 · $managerCount 名主管"
            } else {
                "当前账号仅可查看可访问范围内的部门成员"
            },
            fontSize = 12.sp,
            lineHeight = 16.sp,
            color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
        )
    }
}

@Composable
private fun CreateTeamDialog(
    onClose: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var teamName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onClose) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape = RoundedCornerShape(size = 8.dp))
                .background(color = AppTheme.colorScheme.c_FFFFFFFF_FF101010.color)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(space = 12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "新建部门",
                    fontSize = 17.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
                )
                IconButton(
                    modifier = Modifier.size(size = 34.dp),
                    onClick = onClose
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "关闭新建部门",
                        tint = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
                    )
                }
            }
            TeamTextField(value = teamName, onValueChange = { teamName = it }, label = requiredLabel("部门名称"))
            TeamTextField(value = description, onValueChange = { description = it }, label = "部门说明")
            FormActionRow(
                primaryText = "创建",
                primaryEnabled = teamName.isNotBlank(),
                onCancelClick = onClose,
                onPrimaryClick = { onSave(teamName.trim(), description.trim()) }
            )
        }
    }
}

@Composable
private fun PermissionTeamSection(
    team: OpenTeamDto,
    members: List<OpenTeamMemberDto>,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    canEdit: Boolean,
    onMemberClick: (OpenTeamMemberDto) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(size = 8.dp))
            .background(color = AppTheme.colorScheme.c_FFFFFFFF_FF101010.color)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape = RoundedCornerShape(size = 8.dp))
                .clickable(onClick = onToggleExpanded)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(space = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (expanded) "▾" else "▸",
                fontSize = 16.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(color = 0xFF2563EB)
            )
            Column(
                modifier = Modifier.weight(weight = 1f),
                verticalArrangement = Arrangement.spacedBy(space = 3.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = team.teamName.toCleanBusinessText() ?: "未命名部门",
                    fontSize = 17.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
                )
                Text(
                    text = team.description.toCleanBusinessText() ?: "部门成员与角色信息",
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
                )
            }
            Text(
                modifier = Modifier
                    .clip(shape = RoundedCornerShape(size = 999.dp))
                    .background(color = Color(color = 0xFFEAF4FF))
                    .padding(horizontal = 9.dp, vertical = 5.dp),
                text = "${members.size} 人",
                fontSize = 12.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(color = 0xFF2563EB)
            )
        }
        if (expanded) {
            val managers = members.filter { member -> member.teamRole == "manager" }
            val employees = members.filterNot { member -> member.teamRole == "manager" }
            if (managers.isNotEmpty()) {
                PermissionMemberGroup(
                    title = "部门主管",
                    members = managers,
                    canEdit = canEdit,
                    onMemberClick = onMemberClick
                )
            }
            if (employees.isNotEmpty()) {
                PermissionMemberGroup(
                    title = "成员",
                    members = employees,
                    canEdit = canEdit,
                    onMemberClick = onMemberClick
                )
            }
            if (members.isEmpty()) {
                Text(
                    modifier = Modifier.padding(start = 20.dp),
                    text = "当前部门暂无成员。",
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
                )
            }
        }
    }
}

@Composable
private fun PermissionMemberGroup(
    title: String,
    members: List<OpenTeamMemberDto>,
    canEdit: Boolean,
    onMemberClick: (OpenTeamMemberDto) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 6.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            modifier = Modifier.padding(start = 20.dp),
            text = title,
            fontSize = 13.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
        )
        members.forEach { member ->
            PermissionMemberRow(
                member = member,
                canEdit = canEdit,
                onClick = { onMemberClick(member) }
            )
        }
    }
}

@Composable
private fun PermissionMemberRow(
    member: OpenTeamMemberDto,
    canEdit: Boolean,
    onClick: () -> Unit
) {
    val roleColor = Color(color = 0xFF2563EB)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(size = 8.dp))
            .background(color = Color(color = 0xFFF9FAFB))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(space = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .padding(start = 10.dp)
                .size(size = 36.dp)
                .clip(shape = RoundedCornerShape(size = 999.dp))
                .background(color = Color(color = 0xFFEFF4FF)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = member.displayMemberName().takeLast(1).ifBlank { "员" },
                fontSize = 16.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Bold,
                color = roleColor
            )
        }
        Column(
            modifier = Modifier.weight(weight = 1f),
            verticalArrangement = Arrangement.spacedBy(space = 4.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = member.displayMemberName(),
                fontSize = 15.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
            )
            Text(
                text = member.account.toCleanBusinessText() ?: member.userId,
                fontSize = 12.sp,
                lineHeight = 15.sp,
                color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
            )
        }
        Text(
            modifier = Modifier
                .clip(shape = RoundedCornerShape(size = 999.dp))
                .background(color = Color(color = 0xFFEFF4FF))
                .padding(horizontal = 8.dp, vertical = 5.dp),
            text = member.teamRole.toCompactTeamRoleText(),
            fontSize = 12.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Bold,
            color = roleColor
        )
        Icon(
            modifier = Modifier.size(size = 18.dp),
            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = null,
            tint = if (canEdit) {
                AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color
            } else {
                AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
            }
        )
    }
}

@Composable
private fun MemberPermissionDialog(
    member: OpenTeamMemberDto,
    teams: List<OpenTeamDto>,
    canEdit: Boolean,
    onClose: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var selectedTeamId by remember(member.userId, member.teamId) { mutableStateOf(member.teamId) }
    var selectedRole by remember(member.userId, member.teamRole) { mutableStateOf(member.teamRole.ifBlank { "employee" }) }
    Dialog(onDismissRequest = onClose) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .clip(shape = RoundedCornerShape(size = 8.dp))
                .background(color = AppTheme.colorScheme.c_FFFFFFFF_FF101010.color)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(space = 12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "员工权限",
                    fontSize = 17.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
                )
                IconButton(
                    modifier = Modifier.size(size = 34.dp),
                    onClick = onClose
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "关闭员工权限",
                        tint = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(state = rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(space = 12.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape = RoundedCornerShape(size = 8.dp))
                        .background(color = Color(color = 0xFFEAF4FF))
                        .padding(horizontal = 15.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(space = 8.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = member.displayMemberName(),
                        fontSize = 20.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
                    )
                    ApprovalInfoRow(label = "登录账号", value = member.account.toCleanBusinessText() ?: member.userId)
                    ApprovalInfoRow(label = "当前部门", value = member.teamName.toCleanBusinessText() ?: "未分配")
                    ApprovalInfoRow(label = "当前角色", value = member.teamRole.toTeamRoleText())
                }
                ApprovalFormCard(title = "分配部门", color = Color(color = 0xFFFFFAF0)) {
                    teams.chunked(size = 2).forEach { rowTeams ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            rowTeams.forEach { team ->
                                FilterChipButton(
                                    text = team.teamName.toCleanBusinessText() ?: team.teamId,
                                    selected = selectedTeamId == team.teamId,
                                    enabled = canEdit,
                                    onClick = { selectedTeamId = team.teamId }
                                )
                            }
                        }
                    }
                    if (selectedTeamId != member.teamId) {
                        Text(
                            text = "跨部门调整会先加入目标部门；原部门移出需等待服务端成员移出状态闭环修正后开放。",
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
                        )
                    }
                }
                ApprovalFormCard(title = "分配角色", color = Color(color = 0xFFF7FBF4)) {
                    BusinessOptionGroup(
                        title = "团队内角色",
                        options = TeamRoleOptions,
                        selectedValue = selectedRole,
                        enabled = canEdit,
                        onSelected = { role -> selectedRole = role }
                    )
                }
                FormActionRow(
                    primaryText = "保存",
                    primaryEnabled = canEdit &&
                        selectedTeamId.isNotBlank() &&
                        selectedRole.isNotBlank() &&
                        (selectedTeamId != member.teamId || selectedRole != member.teamRole),
                    onCancelClick = onClose,
                    onPrimaryClick = { onSave(selectedTeamId, selectedRole) }
                )
            }
        }
    }
}

@Composable
private fun CompanyDocumentHero() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(size = 8.dp))
            .background(color = Color(color = 0xFFEAF4FF))
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(space = 12.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .size(size = 58.dp)
                .clip(shape = RoundedCornerShape(size = 8.dp))
                .background(color = Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier.size(size = 34.dp),
                imageVector = Icons.Rounded.EditNote,
                contentDescription = null,
                tint = Color(color = 0xFF2563EB)
            )
        }
        Text(
            text = "AI-OnCall 示例企业介绍",
            fontSize = 22.sp,
            lineHeight = 26.sp,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
        )
        Text(
            text = "这是一份面向演示企业的移动端介绍文档，用来说明组织背景、团队结构和开放式 Tab 平台能力。",
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
        )
    }
}

@Composable
private fun CompanyDocumentSection(
    title: String,
    accentColor: Color,
    rows: List<String>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(size = 8.dp))
            .background(color = accentColor)
            .padding(horizontal = 15.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(space = 10.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = title,
            fontSize = 17.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
        )
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 7.dp)
                        .size(size = 6.dp)
                        .clip(shape = RoundedCornerShape(size = 999.dp))
                        .background(color = AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color)
                )
                Text(
                    modifier = Modifier.weight(weight = 1f),
                    text = row,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
                )
            }
        }
    }
}

@Composable
private fun CompanyDocumentQuote(text: String) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(size = 8.dp))
            .background(color = AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color.copy(alpha = 0.14f))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        text = text,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Bold,
        color = AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color
    )
}

@Composable
private fun CalendarHomePage(
    canCreateCalendar: Boolean,
    onCreateClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
            .clip(shape = RoundedCornerShape(size = 8.dp))
            .background(color = AppTheme.colorScheme.c_FFFFFFFF_FF101010.color),
        verticalArrangement = Arrangement.spacedBy(space = 0.dp),
        horizontalAlignment = Alignment.Start
    ) {
        ApprovalEntryCard(
            title = "新建日程",
            body = "创建会议、评审、活动和协作安排",
            icon = Icons.Rounded.EditNote,
            iconColor = Color(color = 0xFF4C8DF6),
            iconBackground = Color(color = 0xFFE8F1FF),
            enabled = canCreateCalendar,
            disabledHint = "当前账号暂无新建日程权限",
            onClick = onCreateClick
        )
    }
}

@Composable
private fun CalendarRecordsTimeline(
    events: List<OpenCalendarEvent>,
    selectedEventId: String?,
    onItemClick: (OpenCalendarEvent) -> Unit
) {
    if (events.isEmpty()) {
        SectionCard(title = "暂无日程", body = "当前没有可见日程。")
        return
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 14.dp),
        horizontalAlignment = Alignment.Start
    ) {
        events
            .sortedBy { event -> event.startTime.toLocalDateTimeOrNull() ?: LocalDateTime.MAX }
            .groupBy { event -> event.startTime.toLocalDateTimeOrNull()?.toLocalDate() }
            .forEach { (date, dayEvents) ->
                CalendarTimelineDateSection(
                    date = date,
                    events = dayEvents,
                    selectedEventId = selectedEventId,
                    onItemClick = onItemClick
                )
            }
    }
}

@Composable
private fun CalendarTimelineDateSection(
    date: LocalDate?,
    events: List<OpenCalendarEvent>,
    selectedEventId: String?,
    onItemClick: (OpenCalendarEvent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(size = 8.dp))
            .background(color = Color(color = 0xFFF4F9FF))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(space = 10.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatCalendarTimelineDate(date),
                fontSize = 15.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
            )
            Text(
                text = "${events.size} 条",
                fontSize = 12.sp,
                lineHeight = 15.sp,
                color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
            )
        }
        events.forEachIndexed { index, event ->
            CalendarTimelineItem(
                event = event,
                selected = selectedEventId == event.eventId,
                showTopLine = index > 0,
                showBottomLine = index < events.lastIndex,
                onClick = { onItemClick(event) }
            )
        }
    }
}

@Composable
private fun CalendarTimelineItem(
    event: OpenCalendarEvent,
    selected: Boolean,
    showTopLine: Boolean,
    showBottomLine: Boolean,
    onClick: () -> Unit
) {
    val start = event.startTime.toLocalDateTimeOrNull()
    val end = event.endTime.toLocalDateTimeOrNull()
    val accentColor = event.visibility.calendarVisibilityColor()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(space = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            modifier = Modifier
                .padding(top = 12.dp)
                .size(width = 46.dp, height = 24.dp),
            text = start?.format(BusinessTimeFormatter) ?: "--:--",
            fontSize = 12.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
        )
        Column(
            modifier = Modifier.size(width = 14.dp, height = 74.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .weight(weight = 1f)
                    .size(width = 2.dp, height = 1.dp)
                    .background(
                        color = if (showTopLine) {
                            accentColor.copy(alpha = 0.32f)
                        } else {
                            Color.Transparent
                        }
                    )
            )
            Box(
                modifier = Modifier
                    .size(size = 10.dp)
                    .clip(shape = RoundedCornerShape(size = 999.dp))
                    .background(color = accentColor)
            )
            Box(
                modifier = Modifier
                    .weight(weight = 1f)
                    .size(width = 2.dp, height = 1.dp)
                    .background(
                        color = if (showBottomLine) {
                            accentColor.copy(alpha = 0.32f)
                        } else {
                            Color.Transparent
                        }
                    )
            )
        }
        Row(
            modifier = Modifier
                .weight(weight = 1f)
                .clip(shape = RoundedCornerShape(size = 8.dp))
                .background(
                    color = if (selected) {
                        AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color.copy(alpha = 0.14f)
                    } else {
                        AppTheme.colorScheme.c_FFFFFFFF_FF101010.color
                    }
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(weight = 1f),
                verticalArrangement = Arrangement.spacedBy(space = 5.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = event.title.toCleanBusinessText() ?: "未命名日程",
                    fontSize = 15.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
                )
                Text(
                    text = buildString {
                        append(formatCalendarTimeRange(start, end))
                        event.location.toCleanBusinessText()?.let { location -> append(" · $location") }
                    },
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
                )
                Text(
                    text = "${event.teamName.toCleanBusinessText() ?: "全公司"} · ${event.visibility.toCalendarVisibilityText()}",
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    color = accentColor
                )
            }
            Icon(
                modifier = Modifier.size(size = 18.dp),
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color
            )
        }
    }
}

@Composable
private fun CalendarDetailDialog(
    profile: OpenTeamProfile,
    event: OpenCalendarEvent,
    members: List<OpenTeamMemberDto>,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit
) {
    var showDeleteConfirm by remember(event.eventId) { mutableStateOf(false) }
    Dialog(onDismissRequest = onClose) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .clip(shape = RoundedCornerShape(size = 8.dp))
                .background(color = AppTheme.colorScheme.c_FFFFFFFF_FF101010.color)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(space = 12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(weight = 1f),
                    verticalArrangement = Arrangement.spacedBy(space = 3.dp)
                ) {
                    Text(
                        text = event.title.toCleanBusinessText() ?: "日程详情",
                        fontSize = 18.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
                    )
                    Text(
                        text = "${event.teamName.toCleanBusinessText() ?: "全公司"} · ${event.visibility.toCalendarVisibilityText()}",
                        fontSize = 13.sp,
                        lineHeight = 16.sp,
                        color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
                    )
                }
                IconButton(
                    modifier = Modifier.size(size = 34.dp),
                    onClick = onClose
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "关闭详情",
                        tint = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(state = rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(space = 10.dp),
                horizontalAlignment = Alignment.Start
            ) {
                ApprovalInfoBlock(title = "基础信息") {
                    ApprovalInfoRow(label = "日程标题", value = event.title.toCleanBusinessText() ?: "未填写")
                    ApprovalInfoRow(label = "日程说明", value = event.description.toCleanBusinessText() ?: "未填写")
                    ApprovalInfoRow(label = "创建人", value = event.creator.toCleanBusinessText() ?: event.creatorId.ifBlank { "未填写" })
                    ApprovalInfoRow(label = "所属团队", value = event.teamName.toCleanBusinessText() ?: "全公司")
                }
                ApprovalInfoBlock(title = "日程安排") {
                    ApprovalInfoRow(label = "开始时间", value = formatFullBusinessTime(event.startTime).ifBlank { "未记录" })
                    ApprovalInfoRow(label = "结束时间", value = formatFullBusinessTime(event.endTime).ifBlank { "未记录" })
                    ApprovalInfoRow(label = "时间范围", value = formatEventTimeRange(event.startTime, event.endTime))
                    ApprovalInfoRow(label = "地点", value = event.location.toCleanBusinessText() ?: "未填写")
                }
                ApprovalInfoBlock(title = "参与范围") {
                    ApprovalInfoRow(label = "可见范围", value = event.visibility.toCalendarVisibilityText())
                    ApprovalInfoRow(
                        label = "参与人",
                        value = calendarParticipantNames(event = event, members = members)
                    )
                    ApprovalInfoRow(
                        label = "操作权限",
                        value = if (OpenBusinessPermissionRules.canManageCalendarEvent(profile, event)) "可编辑或删除" else "仅可查看"
                    )
                }
                if (OpenBusinessPermissionRules.canManageCalendarEvent(profile, event)) {
                    ApprovalInfoBlock(title = "操作区") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(space = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BusinessActionButton(
                                modifier = Modifier.weight(weight = 1f),
                                text = "删除",
                                primary = false,
                                onClick = { showDeleteConfirm = true }
                            )
                            BusinessActionButton(
                                modifier = Modifier.weight(weight = 1f),
                                text = "编辑",
                                primary = true,
                                onClick = onEdit
                            )
                        }
                        if (showDeleteConfirm) {
                            ApprovalInlineConfirm(
                                title = "确认删除日程",
                                body = "删除后参与人将不再看到该日程。",
                                confirmText = "确认删除",
                                onCancel = { showDeleteConfirm = false },
                                onConfirm = onDelete
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FunHeroCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(size = 8.dp))
            .background(color = Color(color = 0xFFFFF7ED))
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(size = 54.dp)
                .clip(shape = RoundedCornerShape(size = 8.dp))
                .background(color = Color.White),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "哈",
                fontSize = 23.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color(color = 0xFFF59E0B)
            )
        }
        Column(
            modifier = Modifier.weight(weight = 1f),
            verticalArrangement = Arrangement.spacedBy(space = 5.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "放松一刻",
                fontSize = 20.sp,
                lineHeight = 23.sp,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
            )
            Text(
                text = "给认真工作的你，安排一点不太正经的快乐。",
                fontSize = 13.sp,
                lineHeight = 17.sp,
                color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
            )
        }
    }
}

@Composable
private fun FunJokeCard(
    joke: FunJoke,
    revealAnswer: Boolean,
    onToggleReveal: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(size = 8.dp))
            .background(color = Color(color = 0xFFEAF7EF))
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalArrangement = Arrangement.spacedBy(space = 12.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = joke.tag,
            modifier = Modifier
                .clip(shape = RoundedCornerShape(size = 999.dp))
                .background(color = Color.White)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            fontSize = 12.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(color = 0xFF16A34A)
        )
        Text(
            text = joke.question,
            fontSize = 19.sp,
            lineHeight = 25.sp,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
        )
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape = RoundedCornerShape(size = 8.dp))
                .background(color = Color.White)
                .clickable(onClick = onToggleReveal)
                .padding(horizontal = 14.dp, vertical = 14.dp),
            text = if (revealAnswer) joke.answer else "轻点这里看答案",
            fontSize = 15.sp,
            lineHeight = 21.sp,
            color = if (revealAnswer) {
                AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
            } else {
                AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
            }
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(space = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BusinessActionButton(
                modifier = Modifier.weight(weight = 1f),
                text = if (revealAnswer) "收起答案" else "揭晓答案",
                primary = false,
                onClick = onToggleReveal
            )
            BusinessActionButton(
                modifier = Modifier.weight(weight = 1f),
                text = "换一个",
                primary = true,
                onClick = onNext
            )
        }
    }
}

@Composable
private fun FunMoodBoard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(size = 8.dp))
            .background(color = Color(color = 0xFFF4F9FF))
            .padding(horizontal = 15.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(space = 10.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "今日微休息",
            fontSize = 16.sp,
            lineHeight = 19.sp,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("喝水", "伸展", "离屏 3 分钟").forEach { item ->
                Text(
                    modifier = Modifier
                        .clip(shape = RoundedCornerShape(size = 999.dp))
                        .background(color = Color.White)
                        .padding(horizontal = 11.dp, vertical = 7.dp),
                    text = item,
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(color = 0xFF2563EB)
                )
            }
        }
    }
}

@Composable
private fun ApprovalRecordsPage(
    profile: OpenTeamProfile,
    selectedFilter: ApprovalFilter,
    items: List<OpenApprovalItem>,
    selectedItemId: String?,
    onBackClick: () -> Unit,
    onFilterChange: (ApprovalFilter) -> Unit,
    onItemClick: (OpenApprovalItem) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 12.dp),
        horizontalAlignment = Alignment.Start
    ) {
        ApprovalStatusFilterBar(
            filters = profile.approvalRecordFilters(),
            selectedFilter = selectedFilter,
            onFilterChange = onFilterChange
        )
        if (items.isEmpty()) {
            SectionCard(title = selectedFilter.emptyTitle, body = selectedFilter.emptyBody)
        } else {
            ApprovalStatusSection(
                group = selectedFilter.toStatusGroup(),
                count = items.size
            ) {
                items.forEach { item ->
                    ApprovalRecordCard(
                        item = item,
                        selected = selectedItemId == item.id,
                        statusColor = selectedFilter.toStatusGroup().color,
                        onClick = { onItemClick(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ApprovalStatusFilterBar(
    filters: List<ApprovalFilter>,
    selectedFilter: ApprovalFilter,
    onFilterChange: (ApprovalFilter) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "状态筛选",
            fontSize = 12.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
        )
        filters.chunked(size = 4).forEach { rowFilters ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                rowFilters.forEach { filter ->
                    FilterChipButton(
                        text = filter.label,
                        selected = selectedFilter == filter,
                        onClick = { onFilterChange(filter) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ApprovalGroupedList(
    items: List<OpenApprovalItem>,
    selectedItemId: String?,
    showPendingSection: Boolean,
    onItemClick: (OpenApprovalItem) -> Unit
) {
    val groups = listOf(
        ApprovalStatusGroup(
            title = "待处理",
            status = OpenApprovalStatus.Pending,
            color = Color(color = 0xFFFFF2D6),
            emptyText = "暂无待处理审批。"
        ),
        ApprovalStatusGroup(
            title = "已通过",
            status = OpenApprovalStatus.Approved,
            color = Color(color = 0xFFE5F7ED),
            emptyText = "暂无已通过审批。"
        ),
        ApprovalStatusGroup(
            title = "已驳回",
            status = OpenApprovalStatus.Rejected,
            color = Color(color = 0xFFFFECEC),
            emptyText = "暂无已驳回审批。"
        ),
        ApprovalStatusGroup(
            title = "已撤回",
            status = OpenApprovalStatus.Canceled,
            color = Color(color = 0xFFEFF1F3),
            emptyText = "暂无已撤回审批。"
        )
    )
    groups.forEach { group ->
        if (group.status == OpenApprovalStatus.Pending && !showPendingSection) {
            return@forEach
        }
        val groupItems = items.filter { item -> item.status.normalizedApprovalStatus() == group.status }
        if (groupItems.isNotEmpty()) {
            ApprovalStatusSection(
                group = group,
                count = groupItems.size
            ) {
                groupItems.forEach { item ->
                    ApprovalRecordCard(
                        item = item,
                        selected = selectedItemId == item.id,
                        statusColor = group.color,
                        onClick = { onItemClick(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ApprovalStatusSection(
    group: ApprovalStatusGroup,
    count: Int,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(size = 8.dp))
            .background(color = group.color.copy(alpha = 0.72f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(space = 10.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = group.title,
                fontSize = 15.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
            )
            Text(
                text = "$count 条",
                fontSize = 12.sp,
                lineHeight = 15.sp,
                color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
            )
        }
        content()
    }
}

@Composable
private fun ApprovalRecordCard(
    item: OpenApprovalItem,
    selected: Boolean,
    statusColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(size = 8.dp))
            .background(
                color = if (selected) {
                    AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color.copy(alpha = 0.14f)
                } else {
                    AppTheme.colorScheme.c_FFFFFFFF_FF101010.color
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(space = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 54.dp)
                .clip(shape = RoundedCornerShape(size = 999.dp))
                .background(color = statusColor)
        )
        Column(
            modifier = Modifier.weight(weight = 1f),
            verticalArrangement = Arrangement.spacedBy(space = 6.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = item.displayApprovalTitle(),
                fontSize = 15.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
            )
            Text(
                text = "${item.type.toApprovalTypeText()} · ${item.applicant.toCleanBusinessText() ?: item.applicantId.ifBlank { "申请人" }} · ${item.teamName.toCleanBusinessText() ?: "未填写团队"}",
                fontSize = 12.sp,
                lineHeight = 15.sp,
                color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
            )
            Text(
                text = formatBusinessTime(item.createdAt).ifBlank { "未记录时间" },
                fontSize = 12.sp,
                lineHeight = 15.sp,
                color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
            )
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(space = 8.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                modifier = Modifier
                    .clip(shape = RoundedCornerShape(size = 999.dp))
                    .background(color = statusColor.copy(alpha = 0.8f))
                    .padding(horizontal = 9.dp, vertical = 5.dp),
                text = item.status.toApprovalStatusText(),
                fontSize = 12.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
            )
            Icon(
                modifier = Modifier.size(size = 18.dp),
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color
            )
        }
    }
}

@Composable
private fun EditFormCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(size = 8.dp))
            .background(color = AppTheme.colorScheme.c_FFEFF1F3_FF22202A.color)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(space = 10.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = title,
            fontSize = 17.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
        )
        content()
    }
}

@Composable
private fun TeamTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(text = label)
        },
        minLines = 1,
        maxLines = 3,
        shape = RoundedCornerShape(size = 8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            cursorColor = AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color,
            focusedBorderColor = AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color,
            unfocusedBorderColor = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color.copy(alpha = 0.35f),
            focusedContainerColor = AppTheme.colorScheme.c_FFFFFFFF_FF101010.color,
            unfocusedContainerColor = AppTheme.colorScheme.c_FFFFFFFF_FF101010.color
        )
    )
}

@Composable
private fun ApprovalCreatePage(
    approvalType: String,
    approvalTitle: String,
    approvalReason: String,
    approvalExtra: String,
    approvalFormValues: Map<String, String>,
    onTypeChange: (String) -> Unit,
    onTitleChange: (String) -> Unit,
    onReasonChange: (String) -> Unit,
    onExtraChange: (String) -> Unit,
    onFormValueChange: (String, String) -> Unit,
    onBackClick: () -> Unit,
    onSubmitClick: () -> Unit
) {
    val computedValues = approvalType.withComputedApprovalValues(values = approvalFormValues)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 14.dp),
        horizontalAlignment = Alignment.Start
    ) {
        ApprovalFormCard(title = "基础信息", color = Color(color = 0xFFF4F9FF)) {
            TeamTextField(value = approvalTitle, onValueChange = onTitleChange, label = requiredLabel("审批标题"))
            BusinessOptionGroup(
                title = requiredLabel("审批类型"),
                options = ApprovalTypeOptions,
                selectedValue = approvalType,
                onSelected = onTypeChange
            )
        }
        ApprovalFormCard(title = "${approvalType.toApprovalTypeText()}内容", color = Color(color = 0xFFF7FBF4)) {
            approvalType.toApprovalFormFields().forEach { field ->
                ApprovalFieldInput(
                    approvalType = approvalType,
                    field = field,
                    value = approvalFormValues[field.key].orEmpty(),
                    values = approvalFormValues,
                    onValueChange = { value -> onFormValueChange(field.key, value) }
                )
            }
            when (approvalType) {
                OpenApprovalTypes.Leave -> ApprovalComputedField(
                    label = "请假天数",
                    value = computedValues["days"]?.let { "$it 天" } ?: "选择开始和结束时间后自动计算"
                )
                OpenApprovalTypes.Overtime -> ApprovalComputedField(
                    label = "加班时长",
                    value = computedValues["hours"]?.let { "$it 小时" } ?: "选择开始和结束时间后自动计算"
                )
            }
        }
        ApprovalFormCard(title = "申请说明", color = Color(color = 0xFFFFFAF0)) {
            TeamTextField(value = approvalReason, onValueChange = onReasonChange, label = requiredLabel("申请原因"))
            TeamTextField(value = approvalExtra, onValueChange = onExtraChange, label = "补充信息")
        }
        FormActionRow(
            primaryText = "提交审批",
            primaryEnabled = approvalTitle.isNotBlank() &&
                approvalReason.isNotBlank() &&
                approvalType.isApprovalFormReady(values = approvalFormValues),
            onCancelClick = onBackClick,
            onPrimaryClick = onSubmitClick
        )
    }
}

@Composable
private fun ApprovalFormCard(
    title: String,
    color: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(size = 8.dp))
            .background(color = color)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(space = 10.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            lineHeight = 19.sp,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
        )
        content()
    }
}

@Composable
private fun ApprovalFieldInput(
    approvalType: String,
    field: ApprovalFormField,
    value: String,
    values: Map<String, String>,
    onValueChange: (String) -> Unit
) {
    when (field.key) {
        "leaveType" -> BusinessOptionGroup(
            title = requiredLabel(field.label),
            options = LeaveTypeOptions,
            selectedValue = value,
            onSelected = onValueChange
        )
        "expenseType" -> BusinessOptionGroup(
            title = requiredLabel(field.label),
            options = ExpenseTypeOptions,
            selectedValue = value,
            onSelected = onValueChange
        )
        "startTime", "endTime" -> DateTimePickerField(
            label = requiredLabel(field.label),
            value = value,
            onValueChange = onValueChange
        )
        else -> TeamTextField(
            value = value,
            onValueChange = onValueChange,
            label = requiredLabel(field.label)
        )
    }
    if (
        (approvalType == OpenApprovalTypes.Leave ||
            approvalType == OpenApprovalTypes.Overtime ||
            approvalType == OpenApprovalTypes.Outing) &&
        field.key == "endTime" &&
        !approvalType.hasValidApprovalTimeRange(values = values + (field.key to value))
    ) {
        Text(
            text = "结束时间需要晚于开始时间",
            fontSize = 12.sp,
            lineHeight = 15.sp,
            color = AppTheme.colorScheme.c_FFFF545C_FFFA525A.color
        )
    }
}

@Composable
private fun ApprovalComputedField(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(size = 8.dp))
            .background(color = AppTheme.colorScheme.c_FFFFFFFF_FF101010.color)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(space = 4.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
        )
        Text(
            text = value,
            fontSize = 14.sp,
            lineHeight = 17.sp,
            color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
        )
    }
}

@Composable
private fun CalendarEventForm(
    primaryText: String,
    eventTitle: String,
    onEventTitleChange: (String) -> Unit,
    eventDescription: String,
    onEventDescriptionChange: (String) -> Unit,
    eventStart: String,
    onEventStartChange: (String) -> Unit,
    eventEnd: String,
    onEventEndChange: (String) -> Unit,
    eventLocation: String,
    onEventLocationChange: (String) -> Unit,
    eventVisibility: String,
    eventTeamId: String,
    teams: List<OpenTeamDto>,
    members: List<OpenTeamMemberDto>,
    participantIds: List<String>,
    editableScope: Boolean,
    onCompanySelected: () -> Unit,
    onTeamSelected: (String) -> Unit,
    onParticipantsSelected: () -> Unit,
    onParticipantToggle: (String) -> Unit,
    onCancelClick: () -> Unit,
    onPrimaryClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 14.dp),
        horizontalAlignment = Alignment.Start
    ) {
        ApprovalFormCard(title = "基础信息", color = Color(color = 0xFFF4F9FF)) {
            TeamTextField(value = eventTitle, onValueChange = onEventTitleChange, label = requiredLabel("日程标题"))
            TeamTextField(value = eventDescription, onValueChange = onEventDescriptionChange, label = "日程说明")
            TeamTextField(value = eventLocation, onValueChange = onEventLocationChange, label = "地点")
        }
        ApprovalFormCard(title = "日程时间", color = Color(color = 0xFFF7FBF4)) {
            DateTimePickerField(
                label = requiredLabel("开始时间"),
                value = eventStart,
                onValueChange = onEventStartChange
            )
            DateTimePickerField(
                label = requiredLabel("结束时间"),
                value = eventEnd,
                onValueChange = onEventEndChange
            )
            if (!hasValidCalendarTimeRange(eventStart, eventEnd)) {
                Text(
                    text = "结束时间需要晚于开始时间",
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    color = AppTheme.colorScheme.c_FFFF545C_FFFA525A.color
                )
            }
        }
        ApprovalFormCard(title = "参与范围", color = Color(color = 0xFFFFFAF0)) {
            CalendarScopeSelector(
                visibility = eventVisibility,
                selectedTeamId = eventTeamId,
                teams = teams,
                editable = editableScope,
                onCompanySelected = onCompanySelected,
                onTeamSelected = onTeamSelected,
                onParticipantsSelected = onParticipantsSelected
            )
            if (eventVisibility != OpenCalendarVisibility.Company) {
                ParticipantSelector(
                    members = members,
                    selectedUserIds = participantIds,
                    onToggle = onParticipantToggle
                )
            }
        }
        FormActionRow(
            primaryText = primaryText,
            primaryEnabled = eventTitle.isNotBlank() &&
                eventStart.toLocalDateTimeOrNull() != null &&
                eventEnd.toLocalDateTimeOrNull() != null &&
                hasValidCalendarTimeRange(eventStart, eventEnd) &&
                (eventVisibility != OpenCalendarVisibility.Participants || participantIds.isNotEmpty()),
            onCancelClick = onCancelClick,
            onPrimaryClick = onPrimaryClick
        )
    }
}

@Composable
private fun AnnouncementForm(
    title: String,
    primaryText: String,
    announcementTitle: String,
    onAnnouncementTitleChange: (String) -> Unit,
    announcementContent: String,
    onAnnouncementContentChange: (String) -> Unit,
    pinned: Boolean,
    onPinnedChange: () -> Unit,
    onCancelClick: () -> Unit,
    onPrimaryClick: () -> Unit
) {
    EditFormCard(title = title) {
        TeamTextField(value = announcementTitle, onValueChange = onAnnouncementTitleChange, label = "公告标题")
        TeamTextField(value = announcementContent, onValueChange = onAnnouncementContentChange, label = "公告内容")
        FilterChipButton(text = "置顶公告", selected = pinned, onClick = onPinnedChange)
        FormActionRow(
            primaryText = primaryText,
            primaryEnabled = announcementTitle.isNotBlank() && announcementContent.isNotBlank(),
            onCancelClick = onCancelClick,
            onPrimaryClick = onPrimaryClick
        )
    }
}

@Composable
private fun AnnouncementEditDialog(
    title: String,
    primaryText: String,
    announcementTitle: String,
    onAnnouncementTitleChange: (String) -> Unit,
    announcementContent: String,
    onAnnouncementContentChange: (String) -> Unit,
    pinned: Boolean,
    onPinnedChange: () -> Unit,
    onCancelClick: () -> Unit,
    onPrimaryClick: () -> Unit
) {
    Dialog(onDismissRequest = onCancelClick) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .clip(shape = RoundedCornerShape(size = 8.dp))
                .background(color = AppTheme.colorScheme.c_FFFFFFFF_FF101010.color)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(space = 12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.weight(weight = 1f),
                    text = title,
                    fontSize = 17.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
                )
                IconButton(
                    modifier = Modifier.size(size = 34.dp),
                    onClick = onCancelClick
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "关闭编辑公告",
                        tint = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(state = rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(space = 12.dp),
                horizontalAlignment = Alignment.Start
            ) {
                TeamTextField(value = announcementTitle, onValueChange = onAnnouncementTitleChange, label = "公告标题")
                TeamTextField(value = announcementContent, onValueChange = onAnnouncementContentChange, label = "公告内容")
                FilterChipButton(text = "置顶公告", selected = pinned, onClick = onPinnedChange)
                FormActionRow(
                    primaryText = primaryText,
                    primaryEnabled = announcementTitle.isNotBlank() && announcementContent.isNotBlank(),
                    onCancelClick = onCancelClick,
                    onPrimaryClick = onPrimaryClick
                )
            }
        }
    }
}

@Composable
private fun DateTimePickerField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    val context = LocalContext.current
    val selectedDateTime = value.toLocalDateTimeOrNull()
    val pickerDateTime = selectedDateTime ?: LocalDateTime.now(BusinessZoneId)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp)
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChipButton(
                text = selectedDateTime?.format(BusinessDateTextFormatter) ?: "请选择日期",
                selected = selectedDateTime != null,
                onClick = {
                    DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            onValueChange(pickerDateTime.withYear(year).withMonth(month + 1).withDayOfMonth(dayOfMonth).toBusinessIsoText())
                        },
                        pickerDateTime.year,
                        pickerDateTime.monthValue - 1,
                        pickerDateTime.dayOfMonth
                    ).show()
                }
            )
            FilterChipButton(
                text = selectedDateTime?.format(BusinessTimeFormatter) ?: "请选择时间",
                selected = selectedDateTime != null,
                onClick = {
                    TimePickerDialog(
                        context,
                        { _, hourOfDay, minute ->
                            onValueChange(pickerDateTime.withHour(hourOfDay).withMinute(minute).toBusinessIsoText())
                        },
                        pickerDateTime.hour,
                        pickerDateTime.minute,
                        true
                    ).show()
                }
            )
        }
    }
}

@Composable
private fun CalendarScopeSelector(
    visibility: String,
    selectedTeamId: String,
    teams: List<OpenTeamDto>,
    editable: Boolean,
    onCompanySelected: () -> Unit,
    onTeamSelected: (String) -> Unit,
    onParticipantsSelected: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp)
    ) {
        Text(
            text = "\u53ef\u89c1\u8303\u56f4",
            fontSize = 13.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChipButton(
                text = "\u5168\u516c\u53f8",
                selected = visibility == OpenCalendarVisibility.Company,
                enabled = editable,
                onClick = onCompanySelected
            )
            FilterChipButton(
                text = "\u4ec5\u53c2\u4e0e\u4eba",
                selected = visibility == OpenCalendarVisibility.Participants,
                enabled = editable,
                onClick = onParticipantsSelected
            )
        }
        Text(
            text = "\u90e8\u95e8\u53ef\u89c1",
            fontSize = 13.sp,
            lineHeight = 16.sp,
            color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
        )
        teams.chunked(size = 2).forEach { rowTeams ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                rowTeams.forEach { team ->
                    FilterChipButton(
                        text = team.teamName,
                        selected = visibility == OpenCalendarVisibility.Team && selectedTeamId == team.teamId,
                        enabled = editable,
                        onClick = { onTeamSelected(team.teamId) }
                    )
                }
            }
        }
        if (!editable) {
            Text(
                text = "\u7f16\u8f91\u65f6\u4e0d\u4fee\u6539\u65e5\u7a0b\u5f52\u5c5e\u8303\u56f4\uff0c\u53ef\u8c03\u6574\u65f6\u95f4\u3001\u5185\u5bb9\u548c\u53c2\u4e0e\u4eba\u3002",
                fontSize = 12.sp,
                lineHeight = 15.sp,
                color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
            )
        }
    }
}

@Composable
private fun ParticipantSelector(
    members: List<OpenTeamMemberDto>,
    selectedUserIds: List<String>,
    onToggle: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp)
    ) {
        Text(
            text = "\u65e5\u7a0b\u53c2\u4e0e\u4eba",
            fontSize = 13.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
        )
        if (members.isEmpty()) {
            Text(
                text = "\u5f53\u524d\u8303\u56f4\u4e0b\u6682\u65e0\u53ef\u9009\u53c2\u4e0e\u4eba\u3002",
                fontSize = 12.sp,
                lineHeight = 15.sp,
                color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
            )
        } else {
            members.chunked(size = 2).forEach { rowMembers ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                rowMembers.forEach { member ->
                    FilterChipButton(
                        text = member.displayMemberName(),
                        selected = selectedUserIds.contains(member.userId),
                        onClick = { onToggle(member.userId) }
                    )
                }
            }
            }
        }
    }
}

@Composable
private fun ConfirmActionCard(
    title: String,
    body: String,
    confirmText: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    EditFormCard(title = title) {
        Text(
            text = body,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
        )
        FormActionRow(
            primaryText = confirmText,
            onCancelClick = onCancel,
            onPrimaryClick = onConfirm
        )
    }
}

@Composable
private fun FilterSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp)
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            content()
        }
    }
}

@Composable
private fun FilterChipButton(
    text: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(shape = RoundedCornerShape(size = 8.dp))
            .background(
                color = when {
                    !enabled -> AppTheme.colorScheme.c_FFEFF1F3_FF22202A.color.copy(alpha = 0.45f)
                    selected -> AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color.copy(alpha = 0.16f)
                    else -> AppTheme.colorScheme.c_FFEFF1F3_FF22202A.color
                }
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(space = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (enabled) {
                if (selected) {
                    AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color
                } else {
                    AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
                }
            } else {
                AppTheme.colorScheme.c_FF384F60_99FFFFFF.color.copy(alpha = 0.6f)
            }
        )
    }
}

@Composable
private fun BusinessOptionGroup(
    title: String,
    options: List<BusinessOption>,
    selectedValue: String,
    enabled: Boolean = true,
    onSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp)
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
        )
        options.chunked(size = 3).forEach { rowOptions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                rowOptions.forEach { option ->
                    FilterChipButton(
                        text = option.label,
                        selected = selectedValue == option.value,
                        enabled = enabled,
                        onClick = { onSelected(option.value) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FormActionRow(
    primaryText: String,
    primaryEnabled: Boolean = true,
    onCancelClick: () -> Unit,
    onPrimaryClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(space = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BusinessActionButton(
            modifier = Modifier.weight(weight = 1f),
            text = "\u53d6\u6d88",
            primary = false,
            onClick = onCancelClick
        )
        BusinessActionButton(
            modifier = Modifier.weight(weight = 1f),
            text = primaryText,
            primary = true,
            enabled = primaryEnabled,
            onClick = onPrimaryClick
        )
    }
}

@Composable
private fun BusinessActionButton(
    modifier: Modifier = Modifier,
    text: String,
    primary: Boolean,
    danger: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val primaryColor = AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color
    val dangerColor = Color(color = 0xFFE5484D)
    val backgroundColor = when {
        !enabled -> AppTheme.colorScheme.c_FFEFF1F3_FF22202A.color.copy(alpha = 0.5f)
        danger -> dangerColor
        primary -> primaryColor
        else -> AppTheme.colorScheme.c_FFEFF1F3_FF22202A.color
    }
    val textColor = when {
        !enabled -> AppTheme.colorScheme.c_FF384F60_99FFFFFF.color.copy(alpha = 0.65f)
        danger -> Color.White
        primary -> Color.White
        else -> AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
    }
    Row(
        modifier = modifier
            .clip(shape = RoundedCornerShape(size = 8.dp))
            .background(color = backgroundColor)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
private fun SmallIconButton(
    icon: ImageVector = Icons.Rounded.Refresh,
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(shape = RoundedCornerShape(size = 8.dp))
            .background(
                color = if (enabled) {
                    AppTheme.colorScheme.c_FFEFF1F3_FF22202A.color
                } else {
                    AppTheme.colorScheme.c_FFEFF1F3_FF22202A.color.copy(alpha = 0.5f)
                }
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(space = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier.size(size = 16.dp),
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) {
                AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color
            } else {
                AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
            }
        )
        Text(
            text = text,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            color = if (enabled) {
                AppTheme.colorScheme.c_FF42A5F5_FF26A69A.color
            } else {
                AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
            }
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    body: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(size = 8.dp))
            .background(color = AppTheme.colorScheme.c_FFEFF1F3_FF22202A.color)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            modifier = Modifier,
            text = title,
            fontSize = 17.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colorScheme.c_FF001018_DEFFFFFF.color
        )
        Text(
            modifier = Modifier,
            text = body,
            fontSize = 14.sp,
            lineHeight = 19.sp,
            color = AppTheme.colorScheme.c_FF384F60_99FFFFFF.color
        )
    }
}

@Composable
private fun BusinessStatusCard(state: BusinessLoadState) {
    when {
        state.loading -> SectionCard(title = "正在同步", body = "正在从服务端加载最新业务数据。")
        !state.message.isNullOrBlank() -> SectionCard(title = "服务端提示", body = state.message)
    }
}

private fun OpenTabState.toDisplayMessage(tab: OpenTabItem): String {
    return when (this) {
        OpenTabState.Openable -> "该业务 Tab 可以打开。"
        OpenTabState.Disabled -> "该业务应用暂未启用。"
        OpenTabState.PermissionDenied -> "当前账号暂无访问权限，请联系管理员开通。"
        OpenTabState.VersionIncompatible -> "当前客户端版本过低，请升级后再打开。"
        OpenTabState.RouteUnsupported -> "该业务页暂未接入当前客户端。"
        OpenTabState.EntryUnsupported -> "当前客户端暂不支持打开该业务应用。"
        OpenTabState.InvalidConfig -> "该业务应用配置不完整，暂时无法打开。"
    }
}

private data class BusinessOption(
    val label: String,
    val value: String
)

private data class ApprovalFormField(
    val key: String,
    val label: String
)

private data class ApprovalFormDisplayRow(
    val label: String,
    val value: String
)

private data class ApprovalStatusGroup(
    val title: String,
    val status: String,
    val color: Color,
    val emptyText: String
)

private data class FunJoke(
    val tag: String,
    val question: String,
    val answer: String
)

private enum class ApprovalPageMode {
    Home,
    Create,
    Records
}

private enum class CalendarPageMode {
    Home,
    Create,
    Edit
}

private val ApprovalTypeOptions = listOf(
    BusinessOption(label = "\u8bf7\u5047", value = OpenApprovalTypes.Leave),
    BusinessOption(label = "\u62a5\u9500", value = OpenApprovalTypes.Expense),
    BusinessOption(label = "\u52a0\u73ed", value = OpenApprovalTypes.Overtime),
    BusinessOption(label = "\u91c7\u8d2d", value = OpenApprovalTypes.Purchase),
    BusinessOption(label = "\u5916\u51fa", value = OpenApprovalTypes.Outing)
)

private val LeaveTypeOptions = listOf(
    BusinessOption(label = "事假", value = "事假"),
    BusinessOption(label = "病假", value = "病假"),
    BusinessOption(label = "年假", value = "年假"),
    BusinessOption(label = "调休", value = "调休")
)

private val ExpenseTypeOptions = listOf(
    BusinessOption(label = "交通差旅", value = "交通差旅"),
    BusinessOption(label = "活动物料", value = "活动物料"),
    BusinessOption(label = "办公采购", value = "办公采购"),
    BusinessOption(label = "客户招待", value = "客户招待")
)

private val TeamRoleOptions = listOf(
    BusinessOption(label = "部门主管", value = "manager"),
    BusinessOption(label = "普通员工", value = "employee")
)

private val FunJokes = listOf(
    FunJoke(
        tag = "职场冷知识",
        question = "为什么会议室里的空调最懂管理？",
        answer = "因为它总能让大家先冷静一下。"
    ),
    FunJoke(
        tag = "程序员友好",
        question = "为什么接口文档不怕迷路？",
        answer = "因为它一出门就带着路径。"
    ),
    FunJoke(
        tag = "审批中心",
        question = "为什么请假单最有礼貌？",
        answer = "因为它每次都要先走流程。"
    ),
    FunJoke(
        tag = "团队日程",
        question = "为什么日程表很少迟到？",
        answer = "因为它一直把时间安排得明明白白。"
    )
)

private enum class ApprovalFilter(
    val label: String,
    val requestScope: String,
    val status: String,
    val emptyTitle: String,
    val emptyBody: String
) {
    Pending(
        label = "\u672a\u5904\u7406",
        requestScope = "pending",
        status = OpenApprovalStatus.Pending,
        emptyTitle = "\u6682\u65e0\u672a\u5904\u7406\u5ba1\u6279",
        emptyBody = "\u5f53\u524d\u6ca1\u6709\u672a\u5904\u7406\u7684\u5ba1\u6279\u3002"
    ),
    Approved(
        label = "\u5df2\u901a\u8fc7",
        requestScope = "visible",
        status = OpenApprovalStatus.Approved,
        emptyTitle = "\u6682\u65e0\u5df2\u901a\u8fc7\u5ba1\u6279",
        emptyBody = "\u5f53\u524d\u6ca1\u6709\u5df2\u901a\u8fc7\u7684\u5ba1\u6279\u8bb0\u5f55\u3002"
    ),
    Rejected(
        label = "\u5df2\u9a73\u56de",
        requestScope = "visible",
        status = OpenApprovalStatus.Rejected,
        emptyTitle = "\u6682\u65e0\u5df2\u9a73\u56de\u5ba1\u6279",
        emptyBody = "\u5f53\u524d\u6ca1\u6709\u5df2\u9a73\u56de\u7684\u5ba1\u6279\u8bb0\u5f55\u3002"
    ),
    Canceled(
        label = "\u5df2\u64a4\u56de",
        requestScope = "visible",
        status = OpenApprovalStatus.Canceled,
        emptyTitle = "\u6682\u65e0\u5df2\u64a4\u56de\u5ba1\u6279",
        emptyBody = "\u5f53\u524d\u6ca1\u6709\u5df2\u64a4\u56de\u7684\u5ba1\u6279\u8bb0\u5f55\u3002"
    )
}

private fun ApprovalFilter.requestScopes(profile: OpenTeamProfile): List<String> {
    return when {
        OpenBusinessPermissionRules.isAdmin(profile) -> listOf("all")
        this == ApprovalFilter.Pending && profile.canApproveInCurrentTeam() -> listOf("pending")
        else -> listOf("mine")
    }
}

private fun OpenTeamProfile.approvalRecordFilters(): List<ApprovalFilter> {
    return listOf(
        ApprovalFilter.Pending,
        ApprovalFilter.Approved,
        ApprovalFilter.Rejected,
        ApprovalFilter.Canceled
    )
}

private fun ApprovalFilter.toStatusGroup(): ApprovalStatusGroup {
    return when (this) {
        ApprovalFilter.Pending -> ApprovalStatusGroup(
            title = label,
            status = status,
            color = Color(color = 0xFFFFF2D6),
            emptyText = emptyBody
        )
        ApprovalFilter.Approved -> ApprovalStatusGroup(
            title = label,
            status = status,
            color = Color(color = 0xFFE5F7ED),
            emptyText = emptyBody
        )
        ApprovalFilter.Rejected -> ApprovalStatusGroup(
            title = label,
            status = status,
            color = Color(color = 0xFFFFECEC),
            emptyText = emptyBody
        )
        ApprovalFilter.Canceled -> ApprovalStatusGroup(
            title = label,
            status = status,
            color = Color(color = 0xFFEFF1F3),
            emptyText = emptyBody
        )
    }
}

private fun String.toApprovalStatusText(): String {
    return when (this) {
        "pending" -> "\u5f85\u5904\u7406"
        "approved" -> "\u5df2\u901a\u8fc7"
        "rejected" -> "\u5df2\u9a73\u56de"
        "canceled" -> "\u5df2\u64a4\u56de"
        "cancelled" -> "\u5df2\u64a4\u56de"
        else -> this
    }
}

private fun String.normalizedApprovalStatus(): String {
    return when (this) {
        OpenApprovalStatus.Cancelled -> OpenApprovalStatus.Canceled
        else -> this
    }
}

private fun String.toApprovalTypeText(): String {
    return when (this) {
        "leave" -> "\u8bf7\u5047"
        "expense" -> "\u62a5\u9500"
        "overtime" -> "\u52a0\u73ed"
        "purchase" -> "\u91c7\u8d2d"
        "outing" -> "\u5916\u51fa"
        else -> this
    }
}

private fun String.toCalendarVisibilityText(): String {
    return when (this) {
        "company" -> "\u5168\u516c\u53f8"
        "team" -> "\u56e2\u961f"
        "participants" -> "\u4ec5\u53c2\u4e0e\u4eba"
        else -> this
    }
}

private fun String.toAnnouncementScopeText(): String {
    return when (this) {
        OpenAnnouncementScope.Company -> "公司"
        OpenAnnouncementScope.Team -> "部门"
        else -> "公告"
    }
}

private fun String.toTeamRoleText(): String {
    return when (this) {
        "admin" -> "系统管理员"
        "manager" -> "部门主管"
        "employee" -> "普通员工"
        else -> ifBlank { "普通员工" }
    }
}

private fun String.toCompactTeamRoleText(): String {
    return when (this) {
        "admin" -> "管理员"
        "manager" -> "主管"
        "employee" -> "员工"
        else -> ifBlank { "员工" }
    }
}

private fun String.roleAccentColor(): Color {
    return when (this) {
        "manager" -> Color(color = 0xFF2563EB)
        "admin" -> Color(color = 0xFF7C3AED)
        else -> Color(color = 0xFF16A34A)
    }
}

private fun String.calendarVisibilityColor(): Color {
    return when (this) {
        OpenCalendarVisibility.Company -> Color(color = 0xFF4C8DF6)
        OpenCalendarVisibility.Participants -> Color(color = 0xFF8B5CF6)
        else -> Color(color = 0xFF31B28B)
    }
}

private fun String.toApprovalFormFields(): List<ApprovalFormField> {
    return when (this) {
        OpenApprovalTypes.Leave -> listOf(
            ApprovalFormField(key = "leaveType", label = "\u8bf7\u5047\u7c7b\u578b"),
            ApprovalFormField(key = "startTime", label = "\u5f00\u59cb\u65f6\u95f4"),
            ApprovalFormField(key = "endTime", label = "\u7ed3\u675f\u65f6\u95f4")
        )
        OpenApprovalTypes.Expense -> listOf(
            ApprovalFormField(key = "amount", label = "\u62a5\u9500\u91d1\u989d"),
            ApprovalFormField(key = "expenseType", label = "\u8d39\u7528\u7c7b\u578b"),
            ApprovalFormField(key = "invoice", label = "\u53d1\u7968\u6216\u51ed\u8bc1\u8bf4\u660e")
        )
        OpenApprovalTypes.Overtime -> listOf(
            ApprovalFormField(key = "startTime", label = "\u5f00\u59cb\u65f6\u95f4"),
            ApprovalFormField(key = "endTime", label = "\u7ed3\u675f\u65f6\u95f4"),
            ApprovalFormField(key = "workContent", label = "\u52a0\u73ed\u5185\u5bb9")
        )
        OpenApprovalTypes.Purchase -> listOf(
            ApprovalFormField(key = "item", label = "\u91c7\u8d2d\u7269\u54c1"),
            ApprovalFormField(key = "budget", label = "\u9884\u7b97\u91d1\u989d"),
            ApprovalFormField(key = "supplier", label = "\u4f9b\u5e94\u5546")
        )
        OpenApprovalTypes.Outing -> listOf(
            ApprovalFormField(key = "destination", label = "\u5916\u51fa\u5730\u70b9"),
            ApprovalFormField(key = "startTime", label = "\u5f00\u59cb\u65f6\u95f4"),
            ApprovalFormField(key = "endTime", label = "\u7ed3\u675f\u65f6\u95f4"),
            ApprovalFormField(key = "contact", label = "\u8054\u7cfb\u4eba")
        )
        else -> emptyList()
    }
}

private fun String.toApprovalSubmitForm(values: Map<String, String>, extra: String): Map<String, String> {
    return (values + mapOf("extra" to extra)).filterValues { value -> value.isNotBlank() }
}

private fun requiredLabel(label: String): String {
    return "$label *"
}

private fun OpenApprovalItem.displayApprovalTitle(): String {
    return title.toCleanBusinessText() ?: "${type.toApprovalTypeText()}\u7533\u8bf7"
}

private fun Map<String, String>.toApprovalFormDisplay(type: String): String {
    return toApprovalFormRows(type = type).joinToString(separator = "\uff1b") { row ->
        "${row.label}\uff1a${row.value}"
    }
}

private fun Map<String, String>.toApprovalFormRows(type: String): List<ApprovalFormDisplayRow> {
    val labelMap = type.toApprovalFormFields().associate { field -> field.key to field.label } +
        mapOf(
            "date" to "\u65e5\u671f",
            "extra" to "\u8865\u5145\u4fe1\u606f",
            "leaveType" to "\u8bf7\u5047\u7c7b\u578b",
            "startDate" to "\u5f00\u59cb\u65e5\u671f",
            "endDate" to "\u7ed3\u675f\u65e5\u671f",
            "startTime" to "\u5f00\u59cb\u65f6\u95f4",
            "endTime" to "\u7ed3\u675f\u65f6\u95f4",
            "days" to "\u8bf7\u5047\u5929\u6570",
            "amount" to "\u91d1\u989d",
            "category" to "\u8d39\u7528\u7c7b\u578b",
            "expenseType" to "\u8d39\u7528\u7c7b\u578b",
            "invoice" to "\u53d1\u7968\u6216\u51ed\u8bc1\u8bf4\u660e",
            "hours" to "\u52a0\u73ed\u65f6\u957f",
            "workContent" to "\u52a0\u73ed\u5185\u5bb9",
            "item" to "\u91c7\u8d2d\u7269\u54c1",
            "budget" to "\u9884\u7b97\u91d1\u989d",
            "supplier" to "\u4f9b\u5e94\u5546",
            "destination" to "\u5916\u51fa\u5730\u70b9",
            "timeRange" to "\u5916\u51fa\u65f6\u95f4",
            "contact" to "\u8054\u7cfb\u4eba"
        )
    return entries.mapNotNull { (key, value) ->
        val cleanValue = value.toCleanBusinessText() ?: return@mapNotNull null
        ApprovalFormDisplayRow(
            label = labelMap[key] ?: key,
            value = key.toApprovalFormValueText(value = cleanValue)
        )
    }
}

private fun String.toApprovalFormValueText(value: String): String {
    return when (this) {
        "startTime", "endTime" -> formatFullBusinessTime(value).ifBlank { value }
        "days" -> if (value.endsWith("\u5929")) value else "$value \u5929"
        "hours" -> if (value.endsWith("\u5c0f\u65f6")) value else "$value \u5c0f\u65f6"
        else -> value
    }
}

private fun String.isApprovalFormReady(values: Map<String, String>): Boolean {
    return toApprovalFormFields().all { field -> values[field.key].orEmpty().isNotBlank() } &&
        when (this) {
            OpenApprovalTypes.Leave,
            OpenApprovalTypes.Overtime,
            OpenApprovalTypes.Outing -> hasValidApprovalTimeRange(values = values)
            else -> true
        }
}

private fun String.hasValidApprovalTimeRange(values: Map<String, String>): Boolean {
    val start = values["startTime"].toLocalDateTimeOrNull()
    val end = values["endTime"].toLocalDateTimeOrNull()
    if (start == null || end == null) {
        return true
    }
    return end.isAfter(start)
}

private fun String.withComputedApprovalValues(values: Map<String, String>): Map<String, String> {
    return when (this) {
        OpenApprovalTypes.Leave -> values + mapOfNotNullValue("days", calculateLeaveDays(values))
        OpenApprovalTypes.Overtime -> values + mapOfNotNullValue("hours", calculateDurationHours(values))
        else -> values
    }
}

private fun mapOfNotNullValue(key: String, value: String?): Map<String, String> {
    return if (value.isNullOrBlank()) emptyMap() else mapOf(key to value)
}

private fun calculateLeaveDays(values: Map<String, String>): String? {
    val start = values["startTime"].toLocalDateTimeOrNull() ?: return null
    val end = values["endTime"].toLocalDateTimeOrNull() ?: return null
    if (!end.isAfter(start)) {
        return null
    }
    val days = (end.toLocalDate().toEpochDay() - start.toLocalDate().toEpochDay() + 1).coerceAtLeast(1)
    return days.toString()
}

private fun calculateDurationHours(values: Map<String, String>): String? {
    val start = values["startTime"].toLocalDateTimeOrNull() ?: return null
    val end = values["endTime"].toLocalDateTimeOrNull() ?: return null
    if (!end.isAfter(start)) {
        return null
    }
    val hours = Duration.between(start, end).toMinutes() / 60.0
    return if (hours % 1.0 == 0.0) {
        hours.toInt().toString()
    } else {
        String.format("%.1f", hours)
    }
}

private fun hasValidCalendarTimeRange(start: String, end: String): Boolean {
    val startDateTime = start.toLocalDateTimeOrNull()
    val endDateTime = end.toLocalDateTimeOrNull()
    if (startDateTime == null || endDateTime == null) {
        return true
    }
    return endDateTime.isAfter(startDateTime)
}

private fun calendarParticipantNames(
    event: OpenCalendarEvent,
    members: List<OpenTeamMemberDto>
): String {
    val memberNames = members.associate { member -> member.userId to member.displayName }
    val participantNames = event.participants.mapNotNull { participant ->
        participant.displayName.toCleanBusinessText()
            ?: memberNames[participant.userId].toCleanBusinessText()
    }
    return participantNames
        .ifEmpty { event.participantIds.map { userId -> memberNames[userId].toCleanBusinessText() ?: userId } }
        .joinToString(separator = "、")
        .ifBlank { "未填写" }
}

private fun String?.toCleanBusinessText(): String? {
    if (isNullOrBlank()) {
        return null
    }
    val value = trim()
    if (value.equals("null", ignoreCase = true)) {
        return null
    }
    if (value.startsWith("???") || value.startsWith("\uff1f\uff1f\uff1f")) {
        return null
    }
    val questionCount = value.count { char -> char == '?' || char == '\uff1f' }
    if (questionCount >= 3 && questionCount * 2 >= value.length) {
        return null
    }
    return value
}

private fun String.take(maximumLength: Int): String {
    return if (length <= maximumLength) this else take(n = maximumLength).trimEnd() + "..."
}

private fun OpenTeamMemberDto.displayMemberName(): String {
    val cleanName = displayName.toCleanBusinessText()
    val accountName = account.toCleanBusinessText()
    val seedRealName = accountName?.toDemoRealName()
    return when {
        seedRealName != null && cleanName.isSeedRoleName() -> seedRealName
        seedRealName != null && cleanName == null -> seedRealName
        cleanName != null -> cleanName
        accountName != null -> accountName
        else -> userId.ifBlank { "未命名成员" }
    }
}

private fun String?.isSeedRoleName(): Boolean {
    return this == null ||
        this == "产品主管" ||
        this == "产品员工" ||
        this == "运营主管" ||
        this == "运营员工" ||
        this == "部门主管" ||
        this == "普通员工" ||
        this == "演示账号" ||
        this == "系统管理员"
}

private fun String.toDemoRealName(): String? {
    return when (this) {
        "admin" -> "陈明"
        "opentab-demo" -> "林一凡"
        "opentab-guest" -> "访客用户"
        "product-manager" -> "王睿"
        "product-employee" -> "张晨"
        "operation-manager" -> "赵宁"
        "operation-employee" -> "李晓"
        else -> null
    }
}

private fun formatBusinessTime(value: String?): String {
    val dateTime = value.toLocalDateTimeOrNull() ?: return value.orEmpty()
    val today = LocalDate.now(BusinessZoneId)
    val datePrefix = when (dateTime.toLocalDate()) {
        today -> "\u4eca\u5929"
        today.minusDays(1) -> "\u6628\u5929"
        else -> dateTime.format(BusinessMonthDayFormatter)
    }
    return "$datePrefix ${dateTime.format(BusinessTimeFormatter)}"
}

private fun formatFullBusinessTime(value: String?): String {
    val dateTime = value.toLocalDateTimeOrNull() ?: return value.orEmpty()
    return "${dateTime.format(BusinessMonthDayFormatter)} ${dateTime.format(BusinessTimeFormatter)}"
}

private fun formatCalendarTimelineDate(date: LocalDate?): String {
    if (date == null) {
        return "未记录日期"
    }
    val today = LocalDate.now(BusinessZoneId)
    return when (date) {
        today -> "今天 ${date.format(BusinessMonthDayFormatter)}"
        today.plusDays(1) -> "明天 ${date.format(BusinessMonthDayFormatter)}"
        today.minusDays(1) -> "昨天 ${date.format(BusinessMonthDayFormatter)}"
        else -> date.format(BusinessMonthDayFormatter)
    }
}

private fun formatCalendarTimeRange(start: LocalDateTime?, end: LocalDateTime?): String {
    if (start == null || end == null) {
        return "未记录时间"
    }
    return if (start.toLocalDate() == end.toLocalDate()) {
        "${start.format(BusinessTimeFormatter)}-${end.format(BusinessTimeFormatter)}"
    } else {
        "${start.format(BusinessMonthDayFormatter)} ${start.format(BusinessTimeFormatter)} - ${end.format(BusinessMonthDayFormatter)} ${end.format(BusinessTimeFormatter)}"
    }
}

private fun formatEventTimeRange(start: String, end: String): String {
    val startDateTime = start.toLocalDateTimeOrNull()
    val endDateTime = end.toLocalDateTimeOrNull()
    if (startDateTime == null || endDateTime == null) {
        return "$start - $end"
    }
    return if (startDateTime.toLocalDate() == endDateTime.toLocalDate()) {
        "${startDateTime.format(BusinessMonthDayFormatter)} ${startDateTime.format(BusinessTimeFormatter)}-${endDateTime.format(BusinessTimeFormatter)}"
    } else {
        "${startDateTime.format(BusinessMonthDayFormatter)} ${startDateTime.format(BusinessTimeFormatter)} - ${endDateTime.format(BusinessMonthDayFormatter)} ${endDateTime.format(BusinessTimeFormatter)}"
    }
}

private fun String?.toLocalDateTimeOrNull(): LocalDateTime? {
    if (isNullOrBlank()) {
        return null
    }
    return runCatching {
        OffsetDateTime.parse(this).atZoneSameInstant(BusinessZoneId).toLocalDateTime()
    }.getOrElse {
        runCatching {
            LocalDateTime.parse(this)
        }.getOrNull()
    }
}

private fun LocalDateTime.toBusinessIsoText(): String {
    return atZone(BusinessZoneId).toOffsetDateTime().withOffsetSameInstant(ZoneOffset.ofHours(8)).toString()
}

private fun List<String>.toggleValue(value: String): List<String> {
    return if (contains(value)) {
        filterNot { item -> item == value }
    } else {
        this + value
    }
}

private fun OpenTeamProfile.currentTeamNameOrDefault(): String {
    return memberships.firstOrNull { membership -> membership.teamId == currentTeamId }?.teamName
        ?: if (OpenBusinessPermissionRules.isAdmin(this)) "\u5168\u90e8\u56e2\u961f" else "\u672a\u52a0\u5165\u56e2\u961f"
}

private fun OpenTeamProfile.canApproveInCurrentTeam(): Boolean {
    return permissions.contains(OpenBusinessPermissions.ApprovalApprove) &&
        memberships.any { membership ->
            membership.teamId == currentTeamId && membership.teamRole == "manager"
        }
}

private fun OpenTeamProfile.canManageCurrentTeamCalendar(): Boolean {
    return permissions.contains(OpenBusinessPermissions.CalendarManage) &&
        memberships.any { membership ->
            membership.teamId == currentTeamId && membership.teamRole == "manager"
        }
}
