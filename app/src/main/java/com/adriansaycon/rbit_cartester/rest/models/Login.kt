package com.adriansaycon.rbit_cartester.rest.models

import com.adriansaycon.rbit_cartester.data.model.LoggedInUser
import com.adriansaycon.rbit_cartester.rest.data.RestStatus


data class Login(
    val author : Any,
    val website : Any,
    val timestamp : Any,
    val result : LoggedInUser,
    val status : RestStatus
)