package com.adriansaycon.rbit_cartester.rest

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

    val api : Api by lazy {
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

        println("ADZ REQ PREP : ${call.request().headers()}")
        call.enqueue(object : Callback<Required> {

            override fun onResponse(call: Call<Required>, response: Response<Required>) {
                val body = response.body()
                if (body?.status?.code == 200) {

                    // Class preparation
                    val classList = arrayListOf<String>()
                    body.result.classes.forEach {
                        classList.add(it.name)
                        activity.classVals.add(it.id)
                    }

                    val spinnerClass: Spinner = view.findViewById(R.id.spinnerClass)
                    val aaClass = ArrayAdapter(activity, android.R.layout.simple_spinner_item, classList)
                    aaClass.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

                    spinnerClass.prompt = "Select Class"
                    spinnerClass.adapter = aaClass
                    // Class preparation - END

                    // Car preparation
                    val carList = arrayListOf<String>()
                    body.result.cars.forEach {
                        carList.add(it.name)
                        activity.carVals.add(it.id)
                    }

                    val spinnerCar: Spinner = view.findViewById(R.id.spinnerCar)
                    val aaCar = ArrayAdapter(activity, android.R.layout.simple_spinner_item, carList)
                    aaCar.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

                    spinnerCar.prompt = "Select Class"
                    spinnerCar.adapter = aaCar
                    // Car preparation - END

                    // Student preparation
                    val studentList = arrayListOf<String>()
                    body.result.users.forEach {
                        studentList.add(it.name)
                        activity.studentVals.add(it.userId)
                    }

                    val spinnerStudent: Spinner = view.findViewById(R.id.spinnerStudent)
                    val aaStudent = ArrayAdapter(activity, android.R.layout.simple_spinner_item, studentList)
                    aaCar.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

                    spinnerStudent.prompt = "Select Class"
                    spinnerStudent.adapter = aaStudent
                    // Student preparation - END

                    println("ADZ - SELECTED : CLASS[${spinnerClass.selectedItem}]")
                    println("ADZ - SELECTED : CLASS[${spinnerCar.selectedItem}]")
                    println("ADZ - SELECTED : CLASS[${spinnerStudent.selectedItem}]")

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