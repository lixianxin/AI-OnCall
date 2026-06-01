package github.leavesczy.compose_chat.open.repository

import android.content.Context
import android.net.Uri
import java.io.File

/**
 * 头像当前先保存在客户端本地。后续服务端 /me 返回 avatarUrl 后，UI 层可以直接将服务端头像放到更高优先级。
 *
 * 这里不直接保存相册返回的 content:// Uri，而是复制到 App 私有目录，避免 App 重启后 Uri 授权失效。
 */
class OpenAvatarRepository(
    private val context: Context
) {

    private val appContext = context.applicationContext

    private val preferences = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun getLocalAvatarPath(): String? {
        return preferences.getString(KEY_LOCAL_AVATAR_PATH, null)
            ?.takeIf { path -> File(path).exists() }
    }

    fun saveAvatarFromUri(uri: Uri): String? {
        val avatarDir = File(appContext.filesDir, AVATAR_DIR).apply {
            if (!exists()) {
                mkdirs()
            }
        }
        val avatarFile = File(avatarDir, AVATAR_FILE_NAME)
        return runCatching {
            appContext.contentResolver.openInputStream(uri)?.use { input ->
                avatarFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            preferences.edit()
                .putString(KEY_LOCAL_AVATAR_PATH, avatarFile.absolutePath)
                .apply()
            avatarFile.absolutePath
        }.getOrNull()
    }

    private companion object {

        const val PREF_NAME = "open_profile_avatar"
        const val KEY_LOCAL_AVATAR_PATH = "local_avatar_path"
        const val AVATAR_DIR = "open_avatar"
        const val AVATAR_FILE_NAME = "avatar"

    }

}
