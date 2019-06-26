package com.adriansaycon.rbit_cartester.data

import com.adriansaycon.rbit_cartester.data.model.LoggedInUser
import com.adriansaycon.rbit_cartester.rest.models.Login
import java.io.IOException

/**
 * Class that handles authentication w/ login credentials and retrieves user information.
 */
class LoginDataSource {

    fun login(body: Login?): Result<LoggedInUser> {
        return try {
            val user = LoggedInUser(body?.result?.token.toString(), body?.result?.userId.toString(), body?.result?.displayName.toString())
            Result.Success(user)
        } catch (e: Throwable) {
            Result.Error(IOException("Error logging in", e))
        }
    }

    fun logout() {
        // Logout by removing stored login_info
        android.webkit.CookieManager.getInstance().setCookie("LOGIN_INFO", "")
        val info = android.webkit.CookieManager.getInstance().getCookie("LOGIN_INFO")
        println("LOGIN_INFO : ${info != null}")
    }
}

