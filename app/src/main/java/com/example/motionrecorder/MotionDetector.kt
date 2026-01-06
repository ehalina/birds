package com.example.motionrecorder

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs

class MotionDetector(private val threshold: Float = 0.1f) {
    private var previousFrame: Bitmap? = null
    private var frameCount = 0

    fun detectMotion(currentFrame: Bitmap): Boolean {
        val prev = previousFrame
        if (prev == null || prev.width != currentFrame.width || prev.height != currentFrame.height) {
            previousFrame = currentFrame.copy(currentFrame.config, false)
            return false
        }

        frameCount++
        if (frameCount < 5) {
            previousFrame = currentFrame.copy(currentFrame.config, false)
            return false
        }

        val diff = calculateDifference(prev, currentFrame)
        previousFrame = currentFrame.copy(currentFrame.config, false)

        return diff > threshold
    }

    private fun calculateDifference(frame1: Bitmap, frame2: Bitmap): Float {
        var totalDiff = 0L
        val pixels1 = IntArray(frame1.width * frame1.height)
        val pixels2 = IntArray(frame2.width * frame2.height)

        frame1.getPixels(pixels1, 0, frame1.width, 0, 0, frame1.width, frame1.height)
        frame2.getPixels(pixels2, 0, frame2.width, 0, 0, frame2.width, frame2.height)

        for (i in pixels1.indices) {
            val r1 = Color.red(pixels1[i])
            val g1 = Color.green(pixels1[i])
            val b1 = Color.blue(pixels1[i])

            val r2 = Color.red(pixels2[i])
            val g2 = Color.green(pixels2[i])
            val b2 = Color.blue(pixels2[i])

            totalDiff += abs(r1 - r2) + abs(g1 - g2) + abs(b1 - b2)
        }

        val pixelCount = frame1.width * frame1.height
        return (totalDiff / (pixelCount * 765.0)).toFloat()
    }

    fun reset() {
        previousFrame = null
        frameCount = 0
    }
}


