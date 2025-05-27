package com.example.healthmonitor

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class HealthPredictor(context: Context) {
    private var interpreter: Interpreter? = null
    private val modelPath = "health_model.tflite"
    private val inputSize = 3 // pulse, temp, SpO2
    private val outputSize = 3 // Bình thường, Bất thường, Nguy hiểm

    // Scaler values (chuẩn hóa)
    private val mean = floatArrayOf(
        86.91826f,    // pulse
        37.78914f,    // temperature
        94.09883f     // SpO2
    )

    private val scale = floatArrayOf(
        24.40187f,    // pulse
        1.38078f,     // temperature
        3.65911f      // SpO2
    )

    init {
        try {
            interpreter = Interpreter(loadModelFile(context, modelPath))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadModelFile(context: Context, modelPath: String): ByteBuffer {
        val assetFileDescriptor = context.assets.openFd(modelPath)
        val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = fileInputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun predict(heartRate: Int, spO2: Int, temperature: Float): String {
        if (interpreter == null) return "Lỗi: Không thể tải mô hình"

        val inputBuffer = ByteBuffer.allocateDirect(3 * 4).apply {
            order(ByteOrder.nativeOrder())

            // Đảm bảo đúng thứ tự dữ liệu: pulse (heartRate), temperature, SpO2
            putFloat((heartRate.toFloat() - mean[0]) / scale[0])
            putFloat((temperature - mean[1]) / scale[1])
            putFloat((spO2.toFloat() - mean[2]) / scale[2])
            rewind()
        }

        val outputBuffer = ByteBuffer.allocateDirect(3 * 4).apply {
            order(ByteOrder.nativeOrder())
        }

        interpreter?.run(inputBuffer, outputBuffer)

        outputBuffer.rewind()
        val results = FloatArray(3)
        outputBuffer.asFloatBuffer().get(results)

        val maxIndex = results.indices.maxByOrNull { results[it] } ?: 0
        val confidence = results[maxIndex]

        return when (maxIndex) {
            0 -> "Bình thường (${String.format("%.1f", confidence * 100)}%)"
            1 -> "Bất thường (${String.format("%.1f", confidence * 100)}%)"
            2 -> "Nguy hiểm (${String.format("%.1f", confidence * 100)}%)"
            else -> "Không xác định"
        }
    }

    fun close() {
        interpreter?.close()
    }
}
