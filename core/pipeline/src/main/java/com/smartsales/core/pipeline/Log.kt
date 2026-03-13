package com.smartsales.core.pipeline

/**
 * Pure Kotlin Logger stub for OS Layer 3 (Pipeline).
 * Replaces android.util.Log to maintain Domain/Pipeline independence from the Android Framework.
 */
object Log {
    fun v(tag: String, msg: String) { println("V/[$tag] $msg") }
    fun d(tag: String, msg: String) { println("D/[$tag] $msg") }
    fun i(tag: String, msg: String) { println("I/[$tag] $msg") }
    fun w(tag: String, msg: String, t: Throwable? = null) { 
        println("W/[$tag] $msg")
        t?.printStackTrace()
    }
    fun e(tag: String, msg: String, t: Throwable? = null) { 
        println("E/[$tag] $msg")
        t?.printStackTrace()
    }
}
