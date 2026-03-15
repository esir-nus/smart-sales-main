package com.smartsales.core.test.fakes

object Log {
    fun d(tag: String, msg: String) {
        println("[$tag] $msg")
    }
    fun i(tag: String, msg: String) {
        println("[$tag] $msg")
    }
    fun e(tag: String, msg: String, t: Throwable? = null) {
        println("[$tag] ERROR: $msg")
        t?.printStackTrace()
    }
}
