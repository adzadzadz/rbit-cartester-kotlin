package com.adriansaycon.rbit_cartester.rest

import android.content.Context
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.Spinner
import com.adriansaycon.rbit_cartester.MainActivity
import com.adriansaycon.rbit_cartester.R
import com.adriansaycon.rbit_cartester.rest.data.Class
import com.adriansaycon.rbit_cartester.rest.models.Login
import com.adriansaycon.rbit_cartester.rest.models.Required
import com.adriansaycon.rbit_cartester.ui.login.LoginViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Callback
import retrofit2.Call
import retrofit2.Response
import java.net.CookieManager
import java.net.CookieStore
import java.util.*
import kotlin.collections.ArrayList



class Client {

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

        call.enqueue(object : Callback<Login> {

            override fun onResponse(call: Call<Login>, response: Response<Login>) {
                val body = response.body()
                if (body?.status?.code == 200) {
                    android.webkit.CookieManager.getInstance().setCookie("LOGIN_INFO", body.result.token)
                    loginViewModel.login(body, restResult = true)
                } else {
                    loginViewModel.login(body, restResult = false)
                    loading.visibility = View.GONE
//                    println("Adz else : " + loginViewModel.loginFormState)
                }
            }

            override fun onFailure(call: Call<Login>, t: Throwable) {
                println("Adz onFailure : ")
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

                if (body?.status?.code == 200) {
                    val gson = Gson()
                    val result = body?.result
                    val content = gson.toJson(result)
                    val filename = "required_form_data_contents"
                    activity.writeInternalFile(filename, content, Context.MODE_PRIVATE)
//                    activity.readInternalFile(filename)

                    Snackbar.make(view.findViewById(R.id.fabStart), "Data has been stored for offline use.", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show()

                } else {
                    println("ADZ REQ FAILED")
                }
            }

            override fun onFailure(call: Call<Required>, t: Throwable) {
                println("Adz onFailure : ")
                t.printStackTrace()
            }
        })
    }
}