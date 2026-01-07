package com.example.motionrecorder

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraManager(
    private val context: Context,
    private val previewView: PreviewView,
    private val statusCallback: StatusCallback
) {
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val motionDetector = MotionDetector(threshold = 0.15f)
    private val handler = Handler(Looper.getMainLooper())
    
    private var isRecording = false
    private var activeRecording: Recording? = null
    private var motionDetectedTime = 0L
    private var noMotionTimer: Runnable? = null
    private val NO_MOTION_TIMEOUT = 30000L // 30 секунд

    interface StatusCallback {
        fun onStatusChanged(status: String)
    }

    fun start() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases()
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка инициализации камеры", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: return

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
            .build()
        videoCapture = VideoCapture.withOutput(recorder)

        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(android.util.Size(320, 240))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor) { imageProxy ->
                    analyzeImage(imageProxy)
                }
            }

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(
                context as LifecycleOwner,
                cameraSelector,
                preview,
                videoCapture,
                imageAnalysis
            )
            statusCallback.onStatusChanged(context.getString(R.string.status_idle))
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка привязки камеры", e)
        }
    }

    private fun analyzeImage(imageProxy: ImageProxy) {
        val bitmap = imageProxy.toBitmap()
        val hasMotion = motionDetector.detectMotion(bitmap)
        
        handler.post {
            if (hasMotion) {
                motionDetectedTime = System.currentTimeMillis()
                if (!isRecording) {
                    startRecording()
                } else {
                    cancelNoMotionTimer()
                }
            } else {
                if (isRecording) {
                    scheduleStopRecording()
                }
            }
        }
        
        imageProxy.close()
    }

    private fun startRecording() {
        if (isRecording) return
        
        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/MotionRecorder")
            }
        }

        val mediaStoreOutputOptions = MediaStoreOutputOptions.Builder(
            context.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        )
            .setContentValues(contentValues)
            .build()

        val recording = videoCapture?.output
            ?.prepareRecording(context, mediaStoreOutputOptions)
            ?.withAudioEnabled()
            ?.start(ContextCompat.getMainExecutor(context)) { event ->
                when (event) {
                    is VideoRecordEvent.Start -> {
                        isRecording = true
                        statusCallback.onStatusChanged(context.getString(R.string.status_recording))
                    }
                    is VideoRecordEvent.Finalize -> {
                        isRecording = false
                        if (event.hasError()) {
                            Log.e(TAG, "Ошибка записи: ${event.cause}")
                            statusCallback.onStatusChanged("Ошибка записи")
                        } else {
                            statusCallback.onStatusChanged(context.getString(R.string.status_idle))
                        }
                    }
                }
            }

        activeRecording = recording
        motionDetectedTime = System.currentTimeMillis()
    }

    private fun scheduleStopRecording() {
        cancelNoMotionTimer()
        noMotionTimer = Runnable {
            if (isRecording && (System.currentTimeMillis() - motionDetectedTime) >= NO_MOTION_TIMEOUT) {
                stopRecording()
            }
        }
        handler.postDelayed(noMotionTimer!!, NO_MOTION_TIMEOUT)
    }

    private fun cancelNoMotionTimer() {
        noMotionTimer?.let {
            handler.removeCallbacks(it)
            noMotionTimer = null
        }
    }

    private fun stopRecording() {
        activeRecording?.stop()
        activeRecording = null
        isRecording = false
        statusCallback.onStatusChanged(context.getString(R.string.status_idle))
    }

    fun stop() {
        cancelNoMotionTimer()
        stopRecording()
        cameraExecutor.shutdown()
        cameraProvider?.unbindAll()
    }

    companion object {
        private const val TAG = "CameraManager"
    }
}

private fun ImageProxy.toBitmap(): Bitmap {
    if (format != ImageFormat.YUV_420_888) {
        Log.w("ImageProxy", "Неожиданный формат: $format")
    }

    val yBuffer = planes[0].buffer
    val uBuffer = planes[1].buffer
    val vBuffer = planes[2].buffer

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)

    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)

    val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, width, height), 50, out)
    val imageBytes = out.toByteArray()
    
    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size) 
        ?: return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    
    val rotation = imageInfo.rotationDegrees
    return if (rotation != 0) {
        val matrix = Matrix()
        matrix.postRotate(rotation.toFloat())
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    } else {
        bitmap
    }
}

