package com.hk416.vrpdetector

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Size
import android.view.WindowManager
import android.widget.TextView
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var cameraView: PreviewView
    private lateinit var detectTextView: TextView

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        cameraView = findViewById(R.id.cameraView)
        detectTextView = findViewById(R.id.detectTextView)

        // (한국어) 화면이 계속 켜져 있도록 설정합니다.
        // (English Translation) Set the screen to stay on.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // (한국어) 카메라 권한을 요청합니다.
        // (English Translation) Requesting camera permission.
        if (allPermissionsGranted()) {
            setup()
            run()
        }
        else {
            ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }
    }

    private fun setup() {
        // pass
    }

    private fun run() {
        val processCameraProvider = ProcessCameraProvider.getInstance(this).get()
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()
        val preview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(cameraView.surfaceProvider)
            }
        val analysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(640, 640))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                    imageProcess(imageProxy)
                    imageProxy.close()
                }
            }

        processCameraProvider.bindToLifecycle(
            this,
            cameraSelector,
            preview,
            analysis
        )
    }

    private fun imageProcess(imageProxy: ImageProxy) {
        // pass
    }

    private fun allPermissionsGranted() = Companion.REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }
}