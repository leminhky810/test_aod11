package com.blossom.myapplication

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.jvm.Throws

const val CAMERA_PROVIDER = "com.housereapairing.fileprovider"

class MainActivity : AppCompatActivity() {
    private var currentPhotoPath = ""

    private var mCameraStartForResult = mCameraStartForResult { onCameraResult(it) }

    private val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { onActivityResult(it) }

    private var mCameraPermission = mCameraPermission { launchCamera() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mCameraPermission.launch(Manifest.permission.CAMERA)
    }

    private fun onCameraResult(result: ActivityResult) {

        when (result.resultCode) {
            RESULT_OK -> {
                val bitmap = BitmapFactory.decodeFile(currentPhotoPath, BitmapFactory.Options())
                bitmap?.let { imageView.setImageBitmap(bitmap) }

                saveImageToStorage(bitmap)
            }
            RESULT_CANCELED -> {

            }
        }
    }

    private fun requestPermission() {
        try {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            intent.addCategory("android.intent.category.DEFAULT")
            intent.data = Uri.parse(String.format("package:%s", applicationContext.packageName))
            startForResult.launch(intent)

        } catch (e: Exception) {
            val intent = Intent()
            intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
            startForResult.launch(intent)
        }
    }

    private fun onActivityResult( result: ActivityResult) {
        mCameraPermission.launch(Manifest.permission.CAMERA)
    }

    private fun launchCamera() {
        try {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            takePictureIntent.also { intent ->

                val file = launchCameraUtil()
                currentPhotoPath = file?.absolutePath ?: ""
                file?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                            this,
                           CAMERA_PROVIDER,
                            it
                    )

                    intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    mCameraStartForResult.launch(intent)
                }
            }



        } catch (e: Exception) {
            Log.d("eviewActivity.TAG", "dispatchTakePictureIntent: ")
        }
    }


    fun saveImageToStorage(bitmap: Bitmap) {

        //Generating a file name
        val filename = "${System.currentTimeMillis()}.jpg"

        //Output stream
        var fos: OutputStream? = null

        val any = try {


            val imagesDir =
                    Environment.getExternalStorageDirectory().path + "/" + "ABC"


            if (!File(imagesDir).exists()) {
                File(imagesDir).mkdir()
            }
            val image = File("$imagesDir/$filename")
            image.setReadable(true)
            image.setWritable(true)
            fos = FileOutputStream(image)

            fos?.use {
                //Finally writing the bitmap to the output stream that we opened
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
            }
            // flushing output
            fos.flush();
            // closing streams
            fos.close();
            val FILE = "file://"
            sendBroadcast(
                    Intent(
                            Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                            Uri.parse(
                                    FILE
                                            + Environment.getExternalStorageDirectory().path + "/ABC/" + filename
                            )
                    )
            )

        } catch (e: java.lang.Exception) {
            e.message?.let { Log.e("Error: ", it) }
        }
    }


    fun checkWriteExternalPermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            val write = ContextCompat.checkSelfPermission(context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            write == PackageManager.PERMISSION_GRANTED
        } else {
            return true
        }
    }
    fun mCameraPermission(cb: (Boolean) -> Unit): ActivityResultLauncher<String> {
        return registerForActivityResult(
                ActivityResultContracts.RequestPermission()) { cb(it) }

    }
    fun mCameraStartForResult(cb : (ActivityResult) -> Unit) : ActivityResultLauncher<Intent>{

        return registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()) { cb(it) }
    }
    fun launchCameraUtil(): File? {
        return try {
            createImageFile()
        } catch (ex: IOException) {
            null
        }
    }

    @Throws(IOException::class)
    fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir: File? = this.getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        return File.createTempFile(
                "JPEG_${timeStamp}_", /* prefix */
                ".jpg", /* suffix */
                storageDir /* directory */
        )
    }


}