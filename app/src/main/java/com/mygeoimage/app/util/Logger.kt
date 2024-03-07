package com.mygeoimage.app.util


import android.util.Log
import com.base.baselibrary.BuildConfig

class Logger {

    companion object {
        const val EXCEPTION: String = "exception"
        val debugEnable: Boolean
            get() = isDebugEnable()
        /*set(value) {
            isDebugEnable = value
        }*/

        private fun isDebugEnable(): Boolean {
            return BuildConfig.DEBUG
        }

        fun d(tag: String, data: String) {
            if (debugEnable) {
                Log.d(tag, data)
            }
        }

        fun i(tag: String, data: String) {
            if (debugEnable) {
                Log.i(tag, data)
            }
        }

        fun e(tag: String, data: String) {
            if (debugEnable) {
                Log.e(tag, data)
            }
        }

        fun e(tag: String, data: String, throwable: Throwable) {
            if (debugEnable) {
                Log.e(tag, data, throwable)
            }
        }

        fun w(tag: String, data: String) {
            if (debugEnable) {
                Log.w(tag, data)
            }
        }

        fun wtf(tag: String, data: String) {
            if (debugEnable) {
                Log.wtf(tag, data)
            }
        }


    }

    open class Tag {
        companion object {
            const val API: String = "api :"
            const val API_ERROR: String = "api error :"
        }
    }
}