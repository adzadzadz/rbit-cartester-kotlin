package com.adriansaycon.rbit_cartester.data

import com.adriansaycon.rbit_cartester.data.model.LoggedInUser
import com.adriansaycon.rbit_cartester.rest.Client
import com.adriansaycon.rbit_cartester.rest.Model
import retrofit2.http.Body
import java.io.IOException

/**
 * Class that handles authentication w/ login credentials and retrieves user information.
 */
class LoginDataSource {

    fun login(body: Model?): Result<LoggedInUser> {
        try {
            // TODO: handle loggedInUser authentication
//            val user = LoggedInUser("","","")
            val user = LoggedInUser(body?.result?.token.toString(), body?.result?.userId.toString(), body?.result?.displayName.toString())
            return Result.Success(user)
        } catch (e: Throwable) {
            return Result.Error(IOException("Error logging in", e))
        }
    }

    fun logout() {
        // TODO: revoke authentication
    }
}

