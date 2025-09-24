@file:Suppress("unused")

package io.github.canvas.data

import android.util.Log

/** A low-overhead logging class that stores the tag */
@JvmInline
value class Logger(val tag: String) {
    fun v(msg: String) {
        Log.v(tag, msg)
    }

    fun d(msg: String) {
        Log.d(tag, msg)
    }

    fun i(msg: String) {
        Log.i(tag, msg)
    }

    fun w(msg: String) {
        Log.w(tag, msg)
    }

    fun w(msg: String, exc: Exception) {
        Log.w(tag, msg, exc)
    }

    fun e(msg: String) {
        Log.e(tag, msg)
    }

    fun e(msg: String, exc: Exception) {
        Log.e(tag, msg, exc)
    }

    fun v(msg: Any?): Unit = v(msg.toString())
    fun d(msg: Any?): Unit = d(msg.toString())
    fun i(msg: Any?): Unit = i(msg.toString())
    fun w(msg: Any?): Unit = w(msg.toString())
    fun e(msg: Any?): Unit = e(msg.toString())
}

/** Same as e() but indicates that the log call is only temporary during development. A custom Android Studio inspection can warn you about calls to this method in commits */
fun Logger.tmp(msg: String): Unit = e(msg)

/** Same as e() but indicates that the log call is only temporary during development. A custom Android Studio inspection can warn you about calls to this method in commits */
fun Logger.tmp(msg: Any?): Unit = e(msg)
