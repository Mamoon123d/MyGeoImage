package com.mygeoimage.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import com.mygeoimage.app.R
import com.mygeoimage.app.util.ImageQuality.Companion.HIGH
import com.mygeoimage.app.util.ImageQuality.Companion.LOW
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Arrays
import java.util.Date
import java.util.Locale
import java.util.Objects
import java.util.concurrent.Executor
import java.util.concurrent.Executors


class GeoTagImage(context: Context?, callback: PermissionCallback?) {
    private var place = ""
    private var road = ""
    private var latlng = ""
    private var date = ""
    private var originalImageHeight = 0
    private var originalImageWidth = 0
    private val context: Context
    private var returnFile: File? = null
    private var bitmap: Bitmap? = null
    private var mapBitmap: Bitmap? = null
    private var addresses: List<Address>? = null
    private var IMAGE_EXTENSION = ".png"
    private var fileUri: Uri? = null
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private var geocoder: Geocoder? = null
    private var latitude = 0.0
    private var longitude = 0.0
    private var textSize = 0f
    private var textTopMargin = 0f
    private var typeface: Typeface? = null
    private var radius = 0f
    private var backgroundColor = 0
    private var textColor = 0
    private var backgroundHeight = 0f
    private var backgroundLeft = 0f
    private var authorName: String? = null
    private var showAuthorName = false
    private var showAppName = false
    private var showLatLng = false
    private var showDate = false
    private var showGoogleMap = false
    private val elementsList = ArrayList<String>()
    private var mapHeight = 0
    private var mapWidth = 0
    private var bitmapWidth = 0
    private var bitmapHeight = 0
    private var apiKey: String? = null
    private var center: String? = null
    private var imageUrl: String? = null
    private var dimension: String? = null
    private var markerUrl: String? = null
    private var imageQuality: String? = null
    private val permissionCallback: PermissionCallback
    private val executor: Executor = Executors.newSingleThreadExecutor()
    private val executorService = Executors.newSingleThreadExecutor()

    init {
        if (context == null) {
            throw GeoTagException("Context is not initialized or assigned to null value, use : context = getActivity();")
        }
        if (callback == null) {
            throw GeoTagException("Permission callback is not initialized or assigned to null value, use : permissionCallback = this;")
        }
        this.context = context
        permissionCallback = callback
    }

    @Throws(GeoTagException::class)
    fun createImage(fileUri: Uri?) {
        if (fileUri == null) {
            throw GeoTagException("Uri cannot be null")
        }
        this.fileUri = fileUri

        // set default values here.
        textSize = 25f
        typeface = Typeface.DEFAULT
        radius = dpToPx(6f)
        backgroundColor = Color.parseColor("#66000000")
        textColor = context.getColor(R.color.white)
        backgroundHeight = 150f
        authorName = ""
        showAuthorName = false
        showAppName = false
        showGoogleMap = true
        showLatLng = true
        showDate = true
        mapHeight = backgroundHeight.toInt()
        mapWidth = 120
        imageQuality = null
        initialization()
    }

    private fun initialization() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
        if (imageQuality == null) {
            bitmapWidth = 960 * 2
            bitmapHeight = 1280 * 2
            backgroundHeight = (backgroundHeight * 2)
            mapWidth = 120 * 2
            mapHeight = backgroundHeight.toInt()
            textSize = textSize * 2
            textTopMargin = (50 * 2).toFloat()
            radius = radius * 2
        }
        deviceLocation
        //        getDimensions();
    }

    private val deviceLocation: Unit
        private get() {
            if (GeoTagPermission.checkCameraLocationPermission(context)) {
                val task: Task<Location> = fusedLocationProviderClient!!.lastLocation
                task.addOnSuccessListener { location ->
                    if (location != null) {
                        latitude = location.getLatitude()
                        longitude = location.getLongitude()
                        geocoder = Geocoder(context, Locale.getDefault())
                        try {
                            addresses = geocoder!!.getFromLocation(latitude, longitude, 1)
                            if (GeoTagHelper.isGoogleMapsLinked(context)) {
                                if (GeoTagHelper.getMapKey(context) == null) {
                                    val bitmap = createBitmap()
                                    storeBitmapInternally(bitmap)
                                    throw GeoTagException("API key not found for this project")
                                } else {
                                    apiKey = GeoTagHelper.getMapKey(context)
                                    center = "$latitude,$longitude"
                                    dimension = mapWidth.toString() + "x" + mapHeight
                                    markerUrl = String.format(
                                        Locale.getDefault(),
                                        "%s%s%s",
                                        "markers=color:red%7C",
                                        center,
                                        "&"
                                    )
                                    imageUrl = String.format(
                                        Locale.getDefault(),
                                        "https://maps.googleapis.com/maps/api/staticmap?center=%s&zoom=%d&size=%s&%s&maptype=%s&key=%s",
                                        center,
                                        15,
                                        dimension,
                                        markerUrl,
                                        "satellite",
                                        apiKey
                                    )
                                    executorService.submit(LoadImageTask(imageUrl!!))
                                }
                            } else if (!GeoTagHelper.isGoogleMapsLinked(context)) {
                                val bitmap = createBitmap()
                                storeBitmapInternally(bitmap)
                                throw GeoTagException("Project is not linked with google map sdk")
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Logger.e(
                                "error ", Objects.requireNonNull(e.message).toString()
                            )
                        }
                    }
                }
            }
        }

    private inner class LoadImageTask(private val imageUrl: String) : Runnable {
        override fun run() {
            try {
                val bitmap = loadImageFromUrl(imageUrl)
                if (bitmap != null) {
                    mapBitmap = bitmap
                    val newBitmap = createBitmap()
                    storeBitmapInternally(newBitmap)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadImageFromUrl(imageUrl: String): Bitmap? {
        try {
            val inputStream = URL(imageUrl).openStream()
            return BitmapFactory.decodeStream(inputStream)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    fun shutdown() {
        executorService.shutdown()
    }

    @get:Throws(GeoTagException::class)
    val dimensions: Unit
        get() {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            try {
                BitmapFactory.decodeStream(
                    context.contentResolver.openInputStream(fileUri!!),
                    null,
                    options
                )
                originalImageHeight = options.outHeight
                originalImageWidth = options.outWidth
                Logger.d("GeoTag", "$originalImageHeight & $originalImageWidth")
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
                throw GeoTagException("File Not Found : " + e.message)
            }
        }

    private fun createBitmap(): Bitmap {
        val b = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(b)
        canvas.drawARGB(0, 255, 255, 255)
        canvas.drawRGB(255, 255, 255)
        copyTheImage(canvas)
        return b
    }

    private fun copyTheImage(canvas: Canvas) {
        try {
            val inputStream = context.contentResolver.openInputStream(fileUri!!)
            bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        val design = Paint()
        val scaledbmp = Bitmap.createScaledBitmap(bitmap!!, bitmapWidth, bitmapHeight, false)
        canvas.drawBitmap(scaledbmp, 0f, 0f, design)
        val rectPaint = Paint()
        rectPaint.color = backgroundColor
        rectPaint.style = Paint.Style.FILL
        if (showAuthorName) {
            backgroundHeight = backgroundHeight + textTopMargin
        }
        if (showDate) {
            backgroundHeight = backgroundHeight + textTopMargin
        }
        if (showLatLng) {
            backgroundHeight = backgroundHeight + textTopMargin
        }
        if (GeoTagHelper.isGoogleMapsLinked(context)) {
            if (mapBitmap != null) {
                if (showGoogleMap) {
                    val mapLeft = 10f
                    backgroundLeft = (mapBitmap!!.width + 20).toFloat()
                    canvas.drawRoundRect(
                        backgroundLeft,
                        canvas.height - backgroundHeight,
                        (canvas.width - 10).toFloat(),
                        (canvas.height - 10).toFloat(),
                        dpToPx(radius),
                        dpToPx(radius),
                        rectPaint
                    )
                    val scaledbmp2 = Bitmap.createScaledBitmap(
                        mapBitmap!!, mapWidth, mapHeight, false
                    )
                    canvas.drawBitmap(
                        scaledbmp2,
                        mapLeft,
                        canvas.height - backgroundHeight + (backgroundHeight - mapBitmap!!.height) / 2,
                        design
                    )
                    val textX = backgroundLeft + 10
                    val textY = canvas.height - (backgroundHeight - textTopMargin)
                    drawText(textX, textY, canvas)
                } else {
                    backgroundLeft = 10f
                    canvas.drawRoundRect(
                        backgroundLeft,
                        canvas.height - backgroundHeight,
                        (canvas.width - 60).toFloat(),
                        (canvas.height - 60).toFloat(),
                        dpToPx(radius),
                        dpToPx(radius),
                        rectPaint
                    )
                    val textX = backgroundLeft + 20
                    val textY = canvas.height - (backgroundHeight - textTopMargin)
                    drawText(textX, textY, canvas)
                }
            }
        } else {
            backgroundLeft = 10f
            canvas.drawRoundRect(
                backgroundLeft,
                canvas.height - backgroundHeight,
                (canvas.width - 10).toFloat(),
                (canvas.height - 10).toFloat(),
                dpToPx(radius),
                dpToPx(radius),
                rectPaint
            )
            val textX = backgroundLeft + 10
            val textY = canvas.height - (backgroundHeight - textTopMargin)
            drawText(textX, textY, canvas)
        }
    }

    private fun drawText(textX: Float, textY: Float, canvas: Canvas) {
        var textY = textY
        if (imageQuality == null) {
            textSize = textSize * 2
            textTopMargin = (50 * 2).toFloat()
        }
        elementsList.clear()
        val textPaint = Paint()
        textPaint.color = textColor
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.setTypeface(typeface)
        textPaint.textSize = textSize
        if (addresses != null) {
            place =
                addresses!![0].locality + ", " + addresses!![0].adminArea + ", " + addresses!![0].countryName
            road = addresses!![0].getAddressLine(0)
            elementsList.add(place)
            elementsList.add(road)
            if (showLatLng) {

                latlng = "Lat:$latitude,Lng:$longitude"
                elementsList.add(latlng)
            }
        }
        if (showDate) {
            // set date
            date = SimpleDateFormat("dd/MM/yyyy hh:mm a z", Locale.getDefault()).format(Date())
            elementsList.add(date)
        }
        if (showAuthorName) {
           // authorName = "Clicked by : $authorName"
           // elementsList.add(authorName!!)
        }
        for (item in elementsList) {
            canvas.drawText(item, textX, textY, textPaint)
            textY += textTopMargin
        }
        if (showAppName) {
            val appName = GeoTagHelper.getApplicationName(context)
            if (imageQuality != null) {
                when (imageQuality) {
                    LOW -> {
                        textTopMargin = 50f
                        textPaint.textSize = textSize / 2
                        textY = (canvas.height - 20).toFloat()
                        canvas.drawText(
                            appName,
                            canvas.width - 10 - 10 - textPaint.measureText(appName),
                            textY,
                            textPaint
                        )
                    }

                    HIGH -> {
                        textSize = textSize / 2
                        textTopMargin = (50 * 3.6).toFloat()
                        textPaint.textSize = textSize
                        textY = (canvas.height - 40).toFloat()
                        canvas.drawText(
                            appName,
                            canvas.width - 10 - 20 - textPaint.measureText(appName),
                            textY,
                            textPaint
                        )
                    }
                }
            } else {
                textSize = textSize / 2
                textTopMargin = (50 * 2).toFloat()
                textY = (canvas.height - 20).toFloat()
                textPaint.textSize = textSize
                canvas.drawText(
                    appName,
                    canvas.width - 10 - 10 - textPaint.measureText(appName),
                    textY,
                    textPaint
                )
            }
        }
    }

    private fun dpToPx(dp: Float): Float {
        return dp * context.resources.displayMetrics.density
    }

    private fun storeBitmapInternally(b: Bitmap) {
        val pictureFile = outputMediaFile
        returnFile = pictureFile
        if (pictureFile == null) {
            Logger.e("GeoTag", "Error creating media file, check storage permissions: ")
            return
        }
        try {
            val outputStream = ByteArrayOutputStream()
            b.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            val compressedImageData = outputStream.toByteArray()
            val fileOutputStream = FileOutputStream(pictureFile)
            fileOutputStream.write(compressedImageData)
            fileOutputStream.close()
            Logger.d("GeoTag", "file compressed " + Arrays.toString(compressedImageData))
        } catch (e: IOException) {
            Logger.e("GeoTag", Objects.requireNonNull(e.message).toString())
            e.printStackTrace()
        }
    }

    private val outputMediaFile: File?
        private get() {
            val mediaStorageDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "/"
            )
            if (!mediaStorageDir.exists()) {
                if (!mediaStorageDir.mkdirs()) {
                    return null
                }
            }
            val timeStamp: String =
                SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
            val mImageName = "IMG_$timeStamp$IMAGE_EXTENSION"
            val imagePath = mediaStorageDir.path + File.separator + mImageName
            val mediaFile = File(imagePath)
            MediaScannerConnection.scanFile(
                context, arrayOf(imagePath), null
            ) { path, uri -> }
            return mediaFile
        }

    fun setTextSize(textSize: Float) {
        this.textSize = textSize
    }

    fun setCustomFont(typeface: Typeface?) {
        this.typeface = typeface
    }

    fun setBackgroundRadius(radius: Float) {
        this.radius = radius
    }

    fun setBackgroundColor(backgroundColor: Int) {
        this.backgroundColor = backgroundColor
    }

    fun setTextColor(textColor: Int) {
        this.textColor = textColor
    }

    fun showAuthorName(showAuthorName: Boolean) {
        this.showAuthorName = showAuthorName
    }

    fun showAppName(showAppName: Boolean) {
        this.showAppName = showAppName
    }

    fun showLatLng(showLatLng: Boolean) {
        this.showLatLng = showLatLng
    }

    fun showDate(showDate: Boolean) {
        this.showDate = showDate
    }

    fun showGoogleMap(showGoogleMap: Boolean) {
        this.showGoogleMap = showGoogleMap
    }

    fun setAuthorName(authorName: String?) {
        this.authorName = authorName
    }

    fun setImageQuality(imageQuality: String?) {
        this.imageQuality = imageQuality
        when (imageQuality) {
            LOW -> {
                bitmapWidth = 960
                bitmapHeight = 1280
                textTopMargin = 50f
                backgroundHeight = 150f
                mapWidth = 120
                mapHeight = backgroundHeight.toInt()
            }

            HIGH -> {
                bitmapWidth = (960 * 3.6).toInt()
                bitmapHeight = (1280 * 3.6).toInt()
                backgroundHeight = (backgroundHeight * 1.5).toFloat()
                textSize = (textSize * 3.6).toFloat()
                textTopMargin = (50 * 3.6).toFloat()
                radius = (radius * 3.6).toFloat()
                mapWidth = (mapWidth * 2)
                mapHeight = (backgroundHeight.toInt() * 1.5).toInt()
            }
        }
    }

    fun setImageExtension(imgExtension: String) {
        IMAGE_EXTENSION = imgExtension
        when (imgExtension) {
            JPG -> IMAGE_EXTENSION = ".jpg"
            PNG -> IMAGE_EXTENSION = ".png"
            JPEG -> IMAGE_EXTENSION = ".jpeg"
        }
    }

    val imagePath: String?
        get() {
            val mediaStorageDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "/"
            )
            if (!mediaStorageDir.exists()) {
                if (!mediaStorageDir.mkdirs()) {
                    return null
                }
            }
            val timeStamp: String =
                SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
            val mImageName = "IMG_$timeStamp$IMAGE_EXTENSION"
            val ImagePath = mediaStorageDir.path + File.separator + mImageName
            val media = File(ImagePath)
            MediaScannerConnection.scanFile(
                context, arrayOf(media.absolutePath), null
            ) { path, uri -> }
            return ImagePath
        }
    val imageUri: Uri?
        get() {
            val mediaStorageDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "/"
            )
            if (!mediaStorageDir.exists()) {
                if (!mediaStorageDir.mkdirs()) {
                    return null
                }
            }
            val timeStamp: String =
                SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
            val mImageName = "IMG_$timeStamp$IMAGE_EXTENSION"
            val imagePath = mediaStorageDir.path + File.separator + mImageName
            val media = File(imagePath)
            return Uri.fromFile(media)
        }

    fun handlePermissionGrantResult() {
        permissionCallback.onPermissionGranted()
    }

    companion object {
        const val PNG = ".png"
        const val JPG = ".jpg"
        const val JPEG = ".jpeg"
    }
}