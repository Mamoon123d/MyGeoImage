package com.mygeoimage.app.util

interface PermissionCallback {
    fun onPermissionGranted()

    fun onPermissionDenied()
}