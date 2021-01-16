package com.example.pikkc

import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.core.ImageCapture.OutputFileOptions
import androidx.camera.core.ImageCapture.OutputFileResults
import androidx.camera.extensions.HdrImageCaptureExtender
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private val executor: Executor = Executors.newSingleThreadExecutor()
    private val REQUEST_CODE_PERMISSIONS = 1001
    private val REQUIRED_PERMISSIONS = arrayOf("android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE")
    var mPreviewView: PreviewView? = null
    var captureImage: ImageView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mPreviewView = findViewById(R.id.mPreviewView)
        captureImage = findViewById(R.id.captureImg)
        if (allPermissionsGranted()) {
            startCamera() //start camera if permission has been granted by user
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    private fun allPermissionsGranted(): Boolean {
        for (permission in REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {
            try {
                val cameraProvider = cameraProviderFuture.get()
                bindPreview(cameraProvider)
            } catch (e: ExecutionException) {
                // No errors need to be handled for this Future.
                // This should never be reached.
            } catch (e: InterruptedException) {
            }
        }, ContextCompat.getMainExecutor(this))
    }

    fun bindPreview(cameraProvider: ProcessCameraProvider) {
        val preview = Preview.Builder()
                .build()
        val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()
        val imageAnalysis = ImageAnalysis.Builder()
                .build()
        val builder = ImageCapture.Builder()

        //Vendor-Extensions (The CameraX extensions dependency in build.gradle)
        val hdrImageCaptureExtender = HdrImageCaptureExtender.create(builder)

        // Query if extension is available (optional).
        if (hdrImageCaptureExtender.isExtensionAvailable(cameraSelector)) {
            // Enable the extension if available.
            hdrImageCaptureExtender.enableExtension(cameraSelector)
        }
        val imageCapture = builder
                .setTargetRotation(this.windowManager.defaultDisplay.rotation)
                .build()
        preview.setSurfaceProvider(mPreviewView!!.createSurfaceProvider())
        val camera = cameraProvider.bindToLifecycle((this as LifecycleOwner), cameraSelector, preview, imageAnalysis, imageCapture)
        captureImage!!.setOnClickListener {
            val mDateFormat = SimpleDateFormat("yyyyMMddHHmmss", Locale.US)
            val file = File(batchDirectoryName, mDateFormat.format(Date()) + ".jpg")
            val outputFileOptions = OutputFileOptions.Builder(file).build()
            imageCapture.takePicture(outputFileOptions, executor, object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: OutputFileResults) {
                    Handler().post { Toast.makeText(this@MainActivity, "Image Saved successfully", Toast.LENGTH_SHORT).show() }
                }

                override fun onError(error: ImageCaptureException) {
                    error.printStackTrace()
                }
            })
        }
    }

    private val batchDirectoryName: String
        get() {
            var app_folder_path = ""
            app_folder_path = Environment.getExternalStorageDirectory().toString() + "/images"
            val dir = File(app_folder_path)
            if (!dir.exists() && !dir.mkdirs()) {
            }
            return app_folder_path
        }
}