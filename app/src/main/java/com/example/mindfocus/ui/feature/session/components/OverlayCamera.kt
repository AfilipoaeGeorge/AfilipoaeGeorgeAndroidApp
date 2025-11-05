package com.example.mindfocus.ui.feature.session.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult

@Composable
fun OverlayCamera(
    faceResult: FaceLandmarkerResult?,
    imageWidth: Int,
    imageHeight: Int,
    scaleFactor: Float,
    modifier: Modifier = Modifier
) {
    if (faceResult == null || faceResult.faceLandmarks()?.isEmpty() == true) return
    if (imageWidth == 0 || imageHeight == 0) return

    Canvas(modifier) {
        val landmarks = faceResult.faceLandmarks()[0]
        
        val scaledImageWidth = imageWidth * scaleFactor
        val scaledImageHeight = imageHeight * scaleFactor
        
        val offsetX = (size.width - scaledImageWidth) / 2f
        val offsetY = (size.height - scaledImageHeight) / 2f

        FaceLandmarker.FACE_LANDMARKS_CONNECTORS.forEach {
            val start = landmarks[it.start()]
            val end = landmarks[it.end()]
            val startX = start.x() * imageWidth * scaleFactor + offsetX
            val startY = start.y() * imageHeight * scaleFactor + offsetY
            val endX = end.x() * imageWidth * scaleFactor + offsetX
            val endY = end.y() * imageHeight * scaleFactor + offsetY
            drawLine(Color.Red, Offset(startX, startY), Offset(endX, endY), strokeWidth = 2f)
        }

        landmarks.forEach { landmark ->
            val x = landmark.x() * imageWidth * scaleFactor + offsetX
            val y = landmark.y() * imageHeight * scaleFactor + offsetY
            drawCircle(Color.Yellow, radius = 3f, center = Offset(x, y))
        }
    }
}

