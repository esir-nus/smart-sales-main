package android.util

object Log {

    const val VERBOSE = 2

    const val DEBUG = 3

    const val INFO = 4

    const val WARN = 5

    const val ERROR = 6

    const val ASSERT = 7



    @JvmStatic

    fun v(tag: String?, msg: String?): Int = println("V/$tag: $msg").let { 0 }



    @JvmStatic

    fun v(tag: String?, msg: String?, tr: Throwable?): Int =

        println("V/$tag: $msg\n${tr?.stackTraceToString() ?: ""}").let { 0 }



    @JvmStatic

    fun d(tag: String?, msg: String?): Int = println("D/$tag: $msg").let { 0 }



    @JvmStatic

    fun d(tag: String?, msg: String?, tr: Throwable?): Int =

        println("D/$tag: $msg\n${tr?.stackTraceToString() ?: ""}").let { 0 }



    @JvmStatic

    fun i(tag: String?, msg: String?): Int = println("I/$tag: $msg").let { 0 }



    @JvmStatic

    fun i(tag: String?, msg: String?, tr: Throwable?): Int =

        println("I/$tag: $msg\n${tr?.stackTraceToString() ?: ""}").let { 0 }



    @JvmStatic

    fun w(tag: String?, msg: String?): Int = println("W/$tag: $msg").let { 0 }



    @JvmStatic

    fun w(tag: String?, msg: String?, tr: Throwable?): Int =

        println("W/$tag: $msg\n${tr?.stackTraceToString() ?: ""}").let { 0 }



    @JvmStatic

    fun e(tag: String?, msg: String?): Int = println("E/$tag: $msg").let { 0 }



    @JvmStatic

    fun e(tag: String?, msg: String?, tr: Throwable?): Int =

        println("E/$tag: $msg\n${tr?.stackTraceToString() ?: ""}").let { 0 }

}

