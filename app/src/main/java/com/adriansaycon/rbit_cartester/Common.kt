package com.adriansaycon.rbit_cartester

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader


fun Context.writeInternalFile(filename: String?, content : String, mode : Int) {
    this.openFileOutput(filename, mode).use {
        it.write(content.toByteArray())
    }
}

fun Context.readInternalFile(filename : String?): StringBuilder {
    val fis = this.openFileInput(filename)
    val isr = InputStreamReader(fis)
    val bufferedReader = BufferedReader(isr)
    val sb = StringBuilder()
    var line : String?

    do {
        var isNotNull = false
        line = bufferedReader.readLine()
        if (line !== null) {
            isNotNull = true
            sb.append(line);
        }

    } while (isNotNull)

    return sb
}
