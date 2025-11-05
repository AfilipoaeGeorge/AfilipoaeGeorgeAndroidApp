package com.example.mindfocus.core.face

import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import java.lang.Math.toDegrees
import kotlin.math.atan2
import kotlin.math.sqrt

object FaceMetricsCalculator {

    fun calculateFromResult(result: FaceLandmarkerResult?): Triple<Double, Double, Double> {
        if (result == null || result.faceLandmarks().isEmpty()) {
            return Triple(0.0, 0.0, 0.0)
        }

        val landmarks = result.faceLandmarks()[0]

        val points = landmarks.map { lm ->
            Triple(lm.x().toDouble(), lm.y().toDouble(), lm.z().toDouble())
        }

        val ear = calculateEAR(points)
        val mar = calculateMAR(points)
        val head = calculateHeadPose(points)

        return Triple(ear, mar, head)
    }

    private fun calculateEAR(landmarks: List<Triple<Double, Double, Double>>): Double {
        val leftEye = listOf(33, 160, 158, 133, 153, 144)
        val rightEye = listOf(362, 385, 387, 263, 373, 380)

        val leftEAR = (
                distance(landmarks[leftEye[1]], landmarks[leftEye[5]]) +
                        distance(landmarks[leftEye[2]], landmarks[leftEye[4]])
                ) / (2.0 * distance(landmarks[leftEye[0]], landmarks[leftEye[3]]))

        val rightEAR = (
                distance(landmarks[rightEye[1]], landmarks[rightEye[5]]) +
                        distance(landmarks[rightEye[2]], landmarks[rightEye[4]])
                ) / (2.0 * distance(landmarks[rightEye[0]], landmarks[rightEye[3]]))

        return (leftEAR + rightEAR) / 2.0
    }


    private fun calculateMAR(landmarks: List<Triple<Double, Double, Double>>): Double {
        val mouth = listOf(13, 14, 78, 308)
        val vertical = distance(landmarks[mouth[0]], landmarks[mouth[1]])
        val horizontal = distance(landmarks[mouth[2]], landmarks[mouth[3]])
        val mar = if (horizontal > 0) vertical / horizontal else 0.0
        return mar.coerceIn(0.0, 0.5)
    }

    private fun calculateHeadPose(landmarks: List<Triple<Double, Double, Double>>): Double {
        val nose = landmarks[1]
        val chin = landmarks[152]
        val dx = chin.first - nose.first
        val dy = chin.second - nose.second
        val angle = toDegrees(atan2(dy, dx))

        return angle - 90.0
    }


    private fun distance(a: Triple<Double, Double, Double>, b: Triple<Double, Double, Double>): Double {
        val dx = a.first - b.first
        val dy = a.second - b.second
        return sqrt(dx * dx + dy * dy)
    }
}
