package com.example.mindfocus.core.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import androidx.camera.core.ImageProxy
import androidx.core.graphics.createBitmap
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult

class FaceLandmarkerHelper(
    context: Context,
    private val faceLandmarkResultListener: (result: FaceLandmarkerResult, mpImage: MPImage) -> Unit,
    private val faceLandmarkErrorListener: (error: RuntimeException) -> Unit
) {
    private var faceLandmarker: FaceLandmarker? = null

    init {
        setupFaceLandmarker(context)
    }

    private fun setupFaceLandmarker(context: Context) {
        val baseOptionsBuilder = BaseOptions.builder()
            .setDelegate(Delegate.CPU)
            .setModelAssetPath("face_landmarker.task")

        try {
            val baseOptions = baseOptionsBuilder.build()
            val options = FaceLandmarker.FaceLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setNumFaces(1)
                .setMinFaceDetectionConfidence(0.5F)
                .setMinTrackingConfidence(0.5F)
                .setMinFacePresenceConfidence(0.5F)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setErrorListener(faceLandmarkErrorListener)
                .setResultListener(faceLandmarkResultListener)
                .build()

            faceLandmarker = FaceLandmarker.createFromOptions(context, options)
        } catch (e: IllegalStateException) {
            faceLandmarkErrorListener(RuntimeException(e))
        } catch (e: RuntimeException) {
            faceLandmarkErrorListener(e)
        } catch (e: Exception) {
            faceLandmarkErrorListener(RuntimeException(e))
        }
    }

    fun detect(imageProxy: ImageProxy, isFrontCamera: Boolean) {
        if (faceLandmarker == null) {
            return
        }

        val frameTime = SystemClock.uptimeMillis()

        val width = imageProxy.width
        val height = imageProxy.height
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        
        val bitmapBuffer = createBitmap(width, height, config = Bitmap.Config.ARGB_8888)
        imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer) }
        imageProxy.close()

        val matrix = Matrix().apply {
            postRotate(rotationDegrees.toFloat())
            if (isFrontCamera) {
                postScale(-1f, 1f, width.toFloat(), height.toFloat())
            }
        }

        val rotatedBitmap = Bitmap.createBitmap(bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height, matrix, true)
        val mpImage = BitmapImageBuilder(rotatedBitmap).build()

        try {
            faceLandmarker?.detectAsync(mpImage, frameTime)
        } catch (e: Exception) {
            android.util.Log.e("FaceLandmarker", "Error in detectAsync: ${e.message}", e)
        }
    }

    fun clearFaceLandmarker() {
        faceLandmarker?.close()
        faceLandmarker = null
    }
}

