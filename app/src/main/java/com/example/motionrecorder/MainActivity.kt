package com.example.motionrecorder

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var statusText: TextView
    private var cameraManager: CameraManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.previewView)
        statusText = findViewById(R.id.statusText)

        if (checkPermissions()) {
            initCamera()
        } else {
            requestPermissions()
        }
    }

    private fun checkPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO),
            REQUEST_CODE_PERMISSIONS
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                initCamera()
            } else {
                Toast.makeText(this, "Разрешения необходимы", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun initCamera() {
        cameraManager = CameraManager(this, previewView, object : CameraManager.StatusCallback {
            override fun onStatusChanged(status: String) {
                runOnUiThread {
                    statusText.text = status
                }
            }
        })
        cameraManager?.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraManager?.stop()
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
    }
}


