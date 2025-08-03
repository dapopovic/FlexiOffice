package com.example.flexioffice.util

import android.util.Log
import com.example.flexioffice.BuildConfig

/**
 * Custom logger that provides Timber-like functionality
 * with production-safe logging practices.
 */
object Logger {
    /**
     * Log a debug message. Only logs in debug builds.
     */
    fun d(
        tag: String,
        message: String,
    ) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, message)
        }
    }

    /**
     * Log a verbose message. Only logs in debug builds.
     */
    fun v(
        tag: String,
        message: String,
    ) {
        if (BuildConfig.DEBUG) {
            Log.v(tag, message)
        }
    }

    /**
     * Log an info message. Only logs in debug builds.
     */
    fun i(
        tag: String,
        message: String,
    ) {
        if (BuildConfig.DEBUG) {
            Log.i(tag, message)
        }
    }

    /**
     * Log a warning message. Always logs regardless of build type.
     */
    fun w(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    ) {
        if (throwable != null) {
            Log.w(tag, message, throwable)
        } else {
            Log.w(tag, message)
        }
    }

    /**
     * Log an error message. Always logs regardless of build type.
     */
    fun e(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    ) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }

    /**
     * Log with specific priority level.
     */
    fun log(
        priority: Int,
        tag: String,
        message: String,
        throwable: Throwable? = null,
    ) {
        when (priority) {
            Log.VERBOSE -> v(tag, message)
            Log.DEBUG -> d(tag, message)
            Log.INFO -> i(tag, message)
            Log.WARN -> w(tag, message, throwable)
            Log.ERROR -> e(tag, message, throwable)
        }
    }
}
