package com.adriansaycon.rbit_cartester.rest

import com.adriansaycon.rbit_cartester.data.model.LoggedInUser


data class Model(
    val author : Any,
    val website : Any,
    val timestamp : Any,
    val result : LoggedInUser,
    val status : StatusModel
)