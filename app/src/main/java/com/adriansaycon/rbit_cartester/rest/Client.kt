package com.adriansaycon.rbit_cartester.rest

import android.view.View
import android.widget.ProgressBar
import com.adriansaycon.rbit_cartester.ui.login.LoginViewModel
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Callback
import retrofit2.Call
import retrofit2.Response

class Client {

    private val baseUrl = "http://10.0.2.2:8080/rest/v1/app/"

    val api : Api by lazy {
        val retrofit = Retrofit.Builder()
            .client(OkHttpClient().newBuilder().build())
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return@lazy retrofit.create(Api::class.java)
    }

    fun login(username : String, password : String, loginViewModel: LoginViewModel, loading: ProgressBar) {
        println("Starting Adz $api")

        val call = api.home(username, password)

        println("Adz : Made it here")

        val test = "Adrian"

        call.enqueue(object : Callback<Model> {

            override fun onResponse(call: Call<Model>, response: Response<Model>) {
                var body = response.body()
                if (body?.status?.code == 200) {
                    loginViewModel.login(body, restResult = true)
                } else {
                    loginViewModel.login(body, restResult = false)
                    loading.visibility = View.GONE
//                    println("Adz else : " + loginViewModel.loginFormState)
                }
            }

            override fun onFailure(call: Call<Model>, t: Throwable) {
                println("Adz onFailure : ")
                t.printStackTrace()
            }
        })

        println("Adz : after execute :")

        var response = Response.success("")
        println("Adz Response Raw : ${response.raw()}")
    }
}