package github.leavesczy.compose_chat.protocol

import android.content.res.Configuration
import android.os.Bundle

interface TabLifecycle {

    fun onCreate(bundle: Bundle?)

    fun onResume()

    fun onPause()

    fun onDestroy()

    fun onConfigChange(config: Configuration) {}

}

