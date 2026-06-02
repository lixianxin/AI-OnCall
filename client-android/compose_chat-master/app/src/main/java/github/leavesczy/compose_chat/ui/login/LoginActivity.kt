package github.leavesczy.compose_chat.ui.login

import android.os.Bundle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.activity.viewModels
import github.leavesczy.compose_chat.ui.base.BaseActivity
import github.leavesczy.compose_chat.ui.login.logic.LoginViewModel
import github.leavesczy.compose_chat.ui.widgets.LoadingDialog
import kotlinx.coroutines.delay

/**
 * @Author: leavesCZY
 * @Date: 2026/5/20 17:18
 * @Desc:
 */
class LoginActivity : BaseActivity() {

    private val loginViewModel by viewModels<LoginViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var showLaunchPage by remember { mutableStateOf(value = true) }
            LaunchedEffect(key1 = Unit) {
                delay(timeMillis = 900)
                showLaunchPage = false
            }
            if (showLaunchPage) {
                LoginLaunchPage()
            } else {
                LoginPage(viewState = loginViewModel.loginPageViewState)
            }
            LoadingDialog(viewState = loginViewModel.loadingDialogViewState)
        }
    }

}
