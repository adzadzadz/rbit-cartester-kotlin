package com.adriansaycon.rbit_cartester.rest

import android.content.Context
import android.view.View
import android.widget.ProgressBar
import com.adriansaycon.rbit_cartester.MainActivity
import com.adriansaycon.rbit_cartester.R
import com.adriansaycon.rbit_cartester.rest.models.Generic
import com.adriansaycon.rbit_cartester.rest.models.Login
import com.adriansaycon.rbit_cartester.rest.models.Required
import com.adriansaycon.rbit_cartester.ui.login.LoginViewModel
import com.adriansaycon.rbit_cartester.writeInternalFile
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Callback
import retrofit2.Call
import retrofit2.Response
import java.net.InetAddress


class Client {

//    private val baseUrl = "https://cartester.rbit.makersph.com/rest/v1/app/"
     private val baseUrl = "http://10.0.2.2:8080/rest/v1/app/"

    private val api : Api by lazy {
        val retrofit = Retrofit.Builder()
            .client(OkHttpClient().newBuilder().build())
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return@lazy retrofit.create(Api::class.java)
    }

    /**
     * Login method
     * Requests user token for future REST api usage
     * Stores token in a cookie as LOGIN_INFO
     * @void
     */
    fun login(username : String, password : String, loginViewModel: LoginViewModel, loading: ProgressBar) {
        println("Starting Adz $api")
        val call = api.home(username, password)

        println("ADZ: WAITING RESPONSE : START")
        call.enqueue(object : Callback<Login> {

            override fun onResponse(call: Call<Login>, response: Response<Login>) {
                println("LOGIN_INFO : RESPONSE : START")
                val body = response.body()
                println("LOGIN_INFO : BODY READ")
                if (body?.status?.code == 200) {
                    println("LOGIN_INFO : STATUS CODE 200")
                    android.webkit.CookieManager.getInstance().setCookie("LOGIN_INFO : RESPONSE : ", body.result.token)
                    loginViewModel.login(body, restResult = true)
                } else {
                    println("LOGIN_INFO : STATUS FAILED")
                    loginViewModel.login(body, restResult = false)
                    loading.visibility = View.GONE
//                    println("Adz else : " + loginViewModel.loginFormState)
                }
                println("LOGIN_INFO : RESPONSE : END")
            }

            override fun onFailure(call: Call<Login>, t: Throwable) {
                println("LOGIN_INFO : RESPONSE : FAILED REQUEST")
                t.printStackTrace()
            }
        })
    }

    /**
     * uploads user locations data
     * @void
     */
    fun uploadData(activity: MainActivity, view : View, name : String, data : String) {
        val loginInfo = android.webkit.CookieManager.getInstance().getCookie("LOGIN_INFO")
        val call = api.uploadData("Bearer $loginInfo", name, data)

        println("ADZ DATAU : $data")

        call.enqueue(object : Callback<Generic> {

            override fun onResponse(call: Call<Generic>, response: Response<Generic>) {
                val body = response.body()
                println("ADZ DATAM : $body")
                if (body?.status?.code == 200) {
                    println("ADZ REQ SUCCESS")
                    if (body.result != null) {
                        activity.deleteFile(body.result.toString())
                    }

                } else {
                    println("ADZ REQ FAILED")

                }

                Snackbar.make(view, "Everything has been synced.", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
            }

            override fun onFailure(call: Call<Generic>, t: Throwable) {
                println("ADZ REQ FAILED ACCESS")
                t.printStackTrace()
            }
        })
    }

    /**
     * Pulls the required data to start tracking locations
     * @void
     */
    fun getRequiredData(activity : MainActivity, view : View) {
        val loginInfo = android.webkit.CookieManager.getInstance().getCookie("LOGIN_INFO")
        val call = api.requiredData("Bearer $loginInfo")

        call.enqueue(object : Callback<Required> {

            override fun onResponse(call: Call<Required>, response: Response<Required>) {
                val body = response.body()
                println("ADZ : GET REQUIRED DATA : RESPONSE : $body")
                if (body?.status?.code == 200) {
                    val gson = Gson()
                    val result = body.result
                    val content = gson.toJson(result)
                    val filename = activity.savedRequiredDataFilename
                    activity.writeInternalFile(filename, content, Context.MODE_PRIVATE)
                    activity.readyForm()
//                    activity.readInternalFile(filename)

                    Snackbar.make(view.findViewById(R.id.fabStart), "Data has been stored for offline use.", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show()

                } else {
                    println("ADZ REQ FAILED")
                    Snackbar.make(view.findViewById(R.id.fabStart), "Failed. Please contact Administrator.", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show()
                }
            }

            override fun onFailure(call: Call<Required>, t: Throwable) {
                println("Adz onFailure : ")
                Snackbar.make(view.findViewById(R.id.fabStart), "Failed. Please check your internet.", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()

                t.printStackTrace()
            }
        })
    }
}