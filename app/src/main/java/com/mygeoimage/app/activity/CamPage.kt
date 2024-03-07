package com.mygeoimage.app.activity

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.base.baselibrary.base.BaseActivity
import com.mygeoimage.app.R
import com.mygeoimage.app.databinding.CamPageBinding
import com.mygeoimage.app.util.Constant
import com.mygeoimage.app.util.Extension.Companion.gone
import com.mygeoimage.app.util.Extension.Companion.visible
import com.mygeoimage.app.util.GeoTagException
import com.mygeoimage.app.util.GeoTagHelper
import com.mygeoimage.app.util.GeoTagImage
import com.mygeoimage.app.util.GeoTagImage.Companion.PNG
import com.mygeoimage.app.util.GeoTagPermission
import com.mygeoimage.app.util.ImageQuality
import com.mygeoimage.app.util.PermissionCallback
import java.io.File


class CamPage : BaseActivity<CamPageBinding>(), PermissionCallback {
    //private lateinit var ivCamera: ImageView? = null
   // private var ivImage:android.widget.ImageView? = null
   // private var ivClose:android.widget.ImageView? = null
    //private val tvOriginal: TextView? = null
  //  private var tvGtiImg:android.widget.TextView? = null
    private var originalImgStoragePath: String? = null
    private var gtiImageStoragePath:String? = null
    private var fileUri: Uri? = null

    private val PERMISSION_REQUEST_CODE = 100

    private var geoTagImage: GeoTagImage? = null
    private val progressBar: ProgressBar? = null

    override fun setLayoutId(): Int {
        return R.layout.cam_page

    }

    override fun initM() {


    setTb()
        // initialize the context

        // initialize the permission callback listener
       val permissionCallback: PermissionCallback  = this

        // initialize the GeoTagImage class object with context and callback
        // use try/catch block to handle exceptions.
        try {
            geoTagImage = GeoTagImage(mActivity, permissionCallback);
        } catch ( e: GeoTagException) {
            throw RuntimeException(e);
        }
        if (GeoTagPermission.checkCameraLocationPermission(mActivity)) {

            // if permissions are granted, than open camera.
            openCamera()

        } else {
            // otherwise request for the permissions by using GTIPermission class.
            GeoTagPermission.requestCameraLocationPermission(mActivity, PERMISSION_REQUEST_CODE);
        }

    }

    private fun setTb() {
        setSupportActionBar(binding.tb)
        supportActionBar?.run {
            setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return super.onSupportNavigateUp()
    }
    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        // create a file object

        // create a file object
        val file: File?


        file = GeoTagHelper.generateOriginalFile(this@CamPage, GeoTagImage.PNG)
        if (file != null) {
            // if file has been created, then will catch its path for future reference.
            originalImgStoragePath = file.path
        }

          fileUri = GeoTagHelper.getFileUri(mActivity, file)

        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri)

          activityResultLauncher.launch(intent)

    }

    var activityResultLauncher = registerForActivityResult<Intent, ActivityResult>(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK) {
            // Handle the result here
            try {
                binding.progressBar.visible()

                // TODO : START THE MAIN FUNCTIONALITY

                // now call the function createImage() and pass the uri object (line no. 100-110)
                geoTagImage!!.createImage(fileUri)

                // set all the customizations for geotagging as per your requirements.
                geoTagImage.apply {
                    geoTagImage!!.setTextSize(30f)
                    geoTagImage!!.setBackgroundRadius(5f)
                    geoTagImage!!.setBackgroundColor(Color.parseColor("#66000000"))
                    geoTagImage!!.setTextColor(Color.WHITE)
                    geoTagImage!!.setAuthorName(Constant.Tag.AUTH_TITLE)
                    geoTagImage!!.showAuthorName(true)
                    geoTagImage!!.showAppName(true)
                    geoTagImage!!.setImageQuality(ImageQuality.HIGH)
                    geoTagImage!!.setImageExtension(PNG)

                }

                // after geotagged photo is created, get the new image path by using getImagePath() method
                gtiImageStoragePath = geoTagImage!!.imagePath

                /* The time it takes for a Canvas to draw items on a blank Bitmap can vary depending on several factors,
                   * such as the complexity of the items being drawn, the size of the Bitmap, and the processing power of the device.*/
                Handler(Looper.myLooper()!!).postDelayed(
                    this::previewCapturedImage,
                    3000
                )
            } catch (e: GeoTagException) {
                e.printStackTrace()
            }
        }
    }


    // preview of the original image
    private fun previewCapturedImage() {
        try {
            val bitmap = GeoTagHelper.optimizeBitmap(gtiImageStoragePath)
            val capImg=binding.captureImg
            capImg.setImageBitmap(bitmap)
            if (capImg.drawable != null) {
                binding.captureImg.visible()
                binding.progressBar.gone()
            }


        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                geoTagImage!!.handlePermissionGrantResult()
                Toast.makeText(mActivity, "Permission Granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(mActivity, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onPermissionGranted() {
        openCamera()
    }

    override fun onPermissionDenied() {
        GeoTagPermission.requestCameraLocationPermission(mActivity, PERMISSION_REQUEST_CODE)
    }

}