package com.smartsales.core.context

object Log {
    fun d(tag: String, message: String) {
        println("[$tag] DEBUG: $message")
    }

    fun i(tag: String, message: String) {
        println("[$tag] INFO: $message")
    }

    fun w(tag: String, message: String) {
        println("[$tag] WARN: $message")
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        println("[$tag] ERROR: $message")
        throwable?.printStackTrace()
    }
}
