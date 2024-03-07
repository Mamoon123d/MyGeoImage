package com.mygeoimage.app.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale




class GeoTagHelper {
    companion object {


    /** that is  for Map SDK Integration */
    fun isGoogleMapsLinked(context: Context): Boolean {
        val packageManager = context.packageManager
        var apiKey: String? = null
        try {
            val applicationInfo =
                packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
            val metaData = applicationInfo.metaData
            if (metaData != null && metaData.containsKey("com.google.android.geo.API_KEY")) {
                apiKey = metaData.getString("com.google.android.geo.API_KEY")
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return apiKey != null
    }

    /** Check for google map api key (if Map SDK is integrated) */
    fun getMapKey(context: Context): String? {
        val packageManager = context.packageManager
        var apiKey: String? = null
        try {
            val applicationInfo =
                packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
            val metaData = applicationInfo.metaData
            if (metaData != null && metaData.containsKey("com.google.android.geo.API_KEY")) {
                apiKey = metaData.getString("com.google.android.geo.API_KEY")
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return apiKey
    }

    /** To get application name */
    fun getApplicationName(context: Context): String {
        val packageManager = context.packageManager
        val applicationInfo: ApplicationInfo?
        applicationInfo = try {
            packageManager.getApplicationInfo(context.applicationInfo.packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
        return (if (applicationInfo != null) packageManager.getApplicationLabel(applicationInfo) else "Unknown") as String
    }

    /** Save original image in camera directory  */
    fun generateOriginalFile(mContext: FragmentActivity, IMAGE_EXTENSION: String): File? {
        var file: File? = null
        try {
            val mediaStorageDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                "Camera"
            )
            if (!mediaStorageDir.exists()) {
                if (!mediaStorageDir.mkdirs()) {
                    return null
                }
            }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
            file =
                File(mediaStorageDir.path + File.separator + "IMG_" + timeStamp + IMAGE_EXTENSION)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (file != null) {
            scanMediaFIle(mContext, file)
        }
        return file
    }

    private fun scanMediaFIle(mContext: FragmentActivity, file: File) {
        MediaScannerConnection.scanFile(
            mContext, arrayOf(file.absolutePath),
            null
        ) { path: String?, uri: Uri? -> }
    }

    /** Optimize bitmap to prevent OutOfMemory Exception */
    fun optimizeBitmap(filePath: String?): Bitmap {
        val options = BitmapFactory.Options()
        options.inSampleSize = 4
        return BitmapFactory.decodeFile(filePath, options)
    }

    /** get File Uri from application provider */
    fun getFileUri(context: Context, file: File?): Uri {
        return FileProvider.getUriForFile(
            context, context.packageName + ".provider",
            file!!
        )
    }
}
}
