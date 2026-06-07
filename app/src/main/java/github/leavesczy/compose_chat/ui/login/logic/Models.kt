package github.leavesczy.compose_chat.ui.login.logic

import android.app.Activity
import androidx.compose.runtime.Stable
import androidx.compose.ui.text.input.TextFieldValue

/**
 * @Author: leavesCZY
 * @Date: 2026/5/20 17:18
 * @Desc:
 */
@Stable
data class LoginPageViewState(
    val account: TextFieldValue,
    val password: TextFieldValue,
    val registerDisplayName: TextFieldValue,
    val registerPasswordConfirm: TextFieldValue,
    val registerMode: Boolean,
    val panelVisible: Boolean,
    val onAccountInputChanged: (account: TextFieldValue) -> Unit,
    val onPasswordInputChanged: (password: TextFieldValue) -> Unit,
    val onRegisterDisplayNameInputChanged: (displayName: TextFieldValue) -> Unit,
    val onRegisterPasswordConfirmInputChanged: (passwordConfirm: TextFieldValue) -> Unit,
    val onToggleRegisterMode: () -> Unit,
    val onClickLogin: (activity: Activity) -> Unit,
    val onClickRegister: (activity: Activity) -> Unit
)
