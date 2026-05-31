package github.leavesczy.compose_chat.ui.login

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import github.leavesczy.compose_chat.R
import github.leavesczy.compose_chat.provider.ToastProvider
import github.leavesczy.compose_chat.ui.login.logic.LoginPageViewState
import github.leavesczy.compose_chat.ui.theme.AppTheme

/**
 * 登录页已经从原 demo 的简易表单调整为训练营产品入口。
 * 这里仍复用原 ViewState，避免改动登录业务逻辑，只把视觉和键盘体验升级。
 */
@Composable
internal fun LoginPage(viewState: LoginPageViewState) {
    val localActivity = LocalActivity.current
    val localSoftwareKeyboardController = LocalSoftwareKeyboardController.current
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = AppTheme.colorScheme.c_FFFFFFFF_FF101010.color,
        contentWindowInsets = WindowInsets()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(paddingValues = innerPadding)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            if (viewState.panelVisible) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(state = rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(space = 18.dp)
                ) {
                    BrandHeader()
                    LoginFormCard(
                        viewState = viewState,
                        onSubmit = {
                            val account = viewState.account.text
                            val password = viewState.password.text
                            val passwordConfirm = viewState.registerPasswordConfirm.text
                            val displayName = viewState.registerDisplayName.text
                            if (viewState.registerMode) {
                                if (account.isBlank()) {
                                    ToastProvider.showToast(resId = github.leavesczy.compose_chat.base.R.string.login_account_required)
                                } else if (displayName.isBlank()) {
                                    ToastProvider.showToast(msg = "请输入昵称")
                                } else if (password.isBlank()) {
                                    ToastProvider.showToast(resId = github.leavesczy.compose_chat.base.R.string.login_password_required)
                                } else if (password != passwordConfirm) {
                                    ToastProvider.showToast(msg = "两次输入的密码不一致")
                                } else {
                                    localSoftwareKeyboardController?.hide()
                                    viewState.onClickRegister(localActivity!!)
                                }
                            } else {
                                if (account.isBlank()) {
                                    ToastProvider.showToast(resId = github.leavesczy.compose_chat.base.R.string.login_account_required)
                                } else if (password.isBlank()) {
                                    ToastProvider.showToast(resId = github.leavesczy.compose_chat.base.R.string.login_password_required)
                                } else {
                                    localSoftwareKeyboardController?.hide()
                                    viewState.onClickLogin(localActivity!!)
                                }
                            }
                        }
                    )
                    Text(
                        text = "登录后获取团队权限与业务 Tab 配置",
                        fontSize = 13.sp,
                        lineHeight = 16.sp,
                        color = Color(color = 0xFF6B7280)
                    )
                }
            }
        }
    }
}

@Composable
private fun BrandHeader() {
    Column(
        modifier = Modifier.padding(top = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(shape = RoundedCornerShape(size = 20.dp))
                .background(color = Color(color = 0xFF2563EB))
                .padding(all = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.FolderOpen,
                contentDescription = null,
                tint = Color.White
            )
        }
        Text(
            text = stringResource(id = R.string.app_name),
            style = TextStyle(
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = Color(color = 0xFF111827)
            )
        )
        Text(
            text = "开发者接入与 AI oncall 助理",
            fontSize = 16.sp,
            lineHeight = 20.sp,
            color = Color(color = 0xFF6B7280)
        )
    }
}

@Composable
private fun LoginFormCard(
    viewState: LoginPageViewState,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .widthIn(max = 420.dp)
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(size = 18.dp))
            .background(color = Color.White)
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(space = 14.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = if (viewState.registerMode) "创建训练营账号" else "登录工作区",
            fontSize = 20.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(color = 0xFF111827)
        )
        Text(
            text = "使用服务端账号获取团队权限、TabManifest 和 AI 助手能力。",
            fontSize = 13.sp,
            lineHeight = 17.sp,
            color = Color(color = 0xFF6B7280)
        )
        LoginTextField(
            modifier = Modifier.fillMaxWidth(),
            content = viewState.account,
            label = stringResource(id = R.string.login_account),
            icon = Icons.Rounded.AccountCircle,
            onContentChange = viewState.onAccountInputChanged
        )
        LoginTextField(
            modifier = Modifier.fillMaxWidth(),
            content = viewState.password,
            label = stringResource(id = R.string.login_password),
            icon = Icons.Rounded.Lock,
            visualTransformation = PasswordVisualTransformation(),
            onContentChange = viewState.onPasswordInputChanged
        )
        if (viewState.registerMode) {
            LoginTextField(
                modifier = Modifier.fillMaxWidth(),
                content = viewState.registerPasswordConfirm,
                label = "确认密码",
                icon = Icons.Rounded.Lock,
                visualTransformation = PasswordVisualTransformation(),
                onContentChange = viewState.onRegisterPasswordConfirmInputChanged
            )
            LoginTextField(
                modifier = Modifier.fillMaxWidth(),
                content = viewState.registerDisplayName,
                label = "昵称",
                icon = Icons.Rounded.Badge,
                onContentChange = viewState.onRegisterDisplayNameInputChanged
            )
        }
        LoginButton(
            modifier = Modifier,
            text = if (viewState.registerMode) "提交注册" else stringResource(id = R.string.login),
            onClick = onSubmit
        )
        Text(
            modifier = Modifier
                .align(alignment = Alignment.CenterHorizontally)
                .clickable(onClick = viewState.onToggleRegisterMode),
            text = if (viewState.registerMode) "已有账号，返回登录" else "注册账号",
            fontSize = 14.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color(color = 0xFF2563EB)
        )
    }
}

@Composable
private fun LoginTextField(
    modifier: Modifier,
    content: TextFieldValue,
    label: String,
    icon: ImageVector,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onContentChange: (content: TextFieldValue) -> Unit
) {
    OutlinedTextField(
        modifier = modifier,
        value = content,
        onValueChange = onContentChange,
        maxLines = 1,
        singleLine = true,
        visualTransformation = visualTransformation,
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(color = 0xFF6B7280)
            )
        },
        label = {
            Text(
                text = label,
                fontSize = 14.sp,
                lineHeight = 16.sp,
                color = Color(color = 0xFF6B7280)
            )
        },
        textStyle = TextStyle(
            fontSize = 16.sp,
            color = Color(color = 0xFF111827)
        ),
        shape = RoundedCornerShape(size = 12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            cursorColor = Color(color = 0xFF2563EB),
            focusedBorderColor = Color(color = 0xFF2563EB),
            unfocusedBorderColor = Color(color = 0xFFD1D5DB),
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
        )
    )
}

@Composable
private fun LoginButton(
    modifier: Modifier,
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(size = 14.dp))
            .background(color = Color(color = 0xFF2563EB))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            modifier = Modifier.padding(vertical = 12.dp),
            text = text,
            fontSize = 16.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}
