package com.adriansaycon.rbit_cartester.rest.models

import com.adriansaycon.rbit_cartester.rest.data.RestStatus

data class Generic(
    val author : Any,
    val website : Any,
    val timestamp : Any,
    val result : Any?,
    val status : RestStatus
)