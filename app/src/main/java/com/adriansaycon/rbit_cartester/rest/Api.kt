package com.adriansaycon.rbit_cartester.rest

import com.adriansaycon.rbit_cartester.rest.models.Generic
import com.adriansaycon.rbit_cartester.rest.models.Login
import com.adriansaycon.rbit_cartester.rest.models.Required
import retrofit2.Call
import retrofit2.http.*

interface Api {

    @FormUrlEncoded
    @POST("login")
    fun home(
        @Field("username") username : String,
        @Field("password") password : String
    ) : Call<Login>

    @GET("required-data")
    fun requiredData(
        @Header("Authorization") token : String
    ) : Call<Required>

    @FormUrlEncoded
    @POST("upload-data")
    fun uploadData(
        @Header("Authorization") token : String,
        @Field("name") name : String,
        @Field("data") data : String
    ) : Call<Generic>
}