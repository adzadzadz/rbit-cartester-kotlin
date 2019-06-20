package com.adriansaycon.rbit_cartester.rest.models

import com.adriansaycon.rbit_cartester.rest.data.Required
import com.adriansaycon.rbit_cartester.rest.data.RestStatus

data class Required(
    val author : Any,
    val website : Any,
    val timestamp : Any,
    val result : Required,
    val status : RestStatus
)