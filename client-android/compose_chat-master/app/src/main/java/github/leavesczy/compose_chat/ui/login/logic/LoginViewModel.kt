package github.leavesczy.compose_chat.ui.login.logic

import android.app.Activity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.viewModelScope
import github.leavesczy.compose_chat.open.config.OpenApiConfig
import github.leavesczy.compose_chat.open.model.LoginRequest
import github.leavesczy.compose_chat.open.model.RegisterRequest
import github.leavesczy.compose_chat.open.network.OpenApiResult
import github.leavesczy.compose_chat.open.repository.OpenAuthRepository
import github.leavesczy.compose_chat.open.session.OpenSessionManager
import github.leavesczy.compose_chat.ui.MainActivity
import github.leavesczy.compose_chat.ui.base.BaseViewModel
import kotlinx.coroutines.launch

/**
 * 训练营客户端登录 ViewModel。
 *
 * 原 compose_chat demo 使用腾讯 IM UserId 登录。当前阶段服务端已经提供 /auth/login，
 * 因此主登录链路切换为训练营服务端登录；腾讯 IM 登录逻辑保留在原 provider 中，后续如需
 * 聊天能力可再做“服务端账号 -> IM UserId”的映射，不再阻塞工作台和业务 Tab。
 */
class LoginViewModel : BaseViewModel() {

    private val authRepository = OpenAuthRepository()

    var loginPageViewState by mutableStateOf(
        value = buildLoginPageViewState()
    )
        private set

    private fun buildLoginPageViewState(): LoginPageViewState {
        val account = OpenApiConfig.DEFAULT_ACCOUNT.toTextFieldValue()
        val password = OpenApiConfig.DEFAULT_PASSWORD.toTextFieldValue()
        return LoginPageViewState(
            panelVisible = !OpenSessionManager.isLoggedIn,
            account = account,
            password = password,
            registerDisplayName = "".toTextFieldValue(),
            registerPasswordConfirm = "".toTextFieldValue(),
            registerMode = false,
            onAccountInputChanged = ::onAccountInputChanged,
            onPasswordInputChanged = ::onPasswordInputChanged,
            onRegisterDisplayNameInputChanged = ::onRegisterDisplayNameInputChanged,
            onRegisterPasswordConfirmInputChanged = ::onRegisterPasswordConfirmInputChanged,
            onToggleRegisterMode = ::onToggleRegisterMode,
            onClickLogin = ::onClickLogin,
            onClickRegister = ::onClickRegister
        )
    }

    private fun onAccountInputChanged(input: TextFieldValue) {
        loginPageViewState = loginPageViewState.copy(
            account = input.copy(text = input.text.trim())
        )
    }

    private fun onPasswordInputChanged(input: TextFieldValue) {
        loginPageViewState = loginPageViewState.copy(password = input)
    }

    private fun onRegisterDisplayNameInputChanged(input: TextFieldValue) {
        loginPageViewState = loginPageViewState.copy(
            registerDisplayName = input.copy(text = input.text.trim())
        )
    }

    private fun onRegisterPasswordConfirmInputChanged(input: TextFieldValue) {
        loginPageViewState = loginPageViewState.copy(registerPasswordConfirm = input)
    }

    private fun onToggleRegisterMode() {
        loginPageViewState = loginPageViewState.copy(registerMode = !loginPageViewState.registerMode)
    }

    fun tryAutoLogin(activity: Activity) {
        if (OpenSessionManager.isLoggedIn) {
            navToMainActivityAndFinish(activity = activity)
        }
    }

    private fun onClickLogin(activity: Activity) {
        viewModelScope.launch {
            val account = loginPageViewState.account.text.trim()
            val password = loginPageViewState.password.text
            if (account.isBlank()) {
                showToast(resId = github.leavesczy.compose_chat.base.R.string.login_account_required)
                return@launch
            }
            if (password.isBlank()) {
                showToast(resId = github.leavesczy.compose_chat.base.R.string.login_password_required)
                return@launch
            }
            showLoadingDialog()
            // 这里调用训练营服务端登录，成功后 token 会由 OpenAuthRepository 保存到 OpenSessionManager。
            when (val result = authRepository.login(LoginRequest(account = account, password = password))) {
                is OpenApiResult.Success -> {
                    navToMainActivityAndFinish(activity = activity)
                }

                is OpenApiResult.Failed -> {
                    showToast(msg = result.message)
                }
            }
            dismissLoadingDialog()
        }
    }

    private fun onClickRegister(activity: Activity) {
        viewModelScope.launch {
            val account = loginPageViewState.account.text.trim()
            val password = loginPageViewState.password.text
            val passwordConfirm = loginPageViewState.registerPasswordConfirm.text
            val displayName = loginPageViewState.registerDisplayName.text.trim()
            if (account.isBlank()) {
                showToast(resId = github.leavesczy.compose_chat.base.R.string.login_account_required)
                return@launch
            }
            if (displayName.isBlank()) {
                showToast(msg = "请输入昵称")
                return@launch
            }
            if (password.isBlank()) {
                showToast(resId = github.leavesczy.compose_chat.base.R.string.login_password_required)
                return@launch
            }
            if (password != passwordConfirm) {
                showToast(msg = "两次输入的密码不一致")
                return@launch
            }
            showLoadingDialog()
            // 注册接口目前在云端返回 404。这里先按照约定协议接入，接口上线后可直接复用登录成功流程。
            when (val result = authRepository.register(
                RegisterRequest(
                    account = account,
                    password = password,
                    displayName = displayName
                )
            )) {
                is OpenApiResult.Success -> {
                    navToMainActivityAndFinish(activity = activity)
                }

                is OpenApiResult.Failed -> {
                    showToast(
                        msg = if (result.code == "HTTP_ERROR" && result.message.contains("404")) {
                            "注册接口暂未开放，请先使用演示账号登录"
                        } else {
                            result.message
                        }
                    )
                }
            }
            dismissLoadingDialog()
        }
    }

    private fun navToMainActivityAndFinish(activity: Activity) {
        activity.startActivity<MainActivity>()
        activity.finish()
    }

    private fun String.toTextFieldValue(): TextFieldValue {
        return TextFieldValue(
            text = this,
            selection = TextRange(index = length)
        )
    }

}
