package com.example.FitnessApp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.channels.FileChannel

class TensorFlowModel(context: Context) {
    private var interpreter: Interpreter? = null

    init {
        val modelPath = "model.tflite" // assets/model.tflite
        val assetFileDescriptor = context.assets.openFd(modelPath)
        val fileInputStream = assetFileDescriptor.createInputStream()
        val fileChannel = fileInputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        val modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        interpreter = Interpreter(modelBuffer)
    }

    fun predict(bitmap: Bitmap): String {

        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 200, 200, true)

        val normalizedBitmap = normalizeBitmap(resizedBitmap)
        val tensorImage = TensorImage.fromBitmap(normalizedBitmap)

        val inputBuffer = tensorImage.tensorBuffer
        val outputBuffer = TensorBuffer.createFixedSize(intArrayOf(1, 4), org.tensorflow.lite.DataType.FLOAT32)

        interpreter?.run(inputBuffer.buffer, outputBuffer.buffer)

        val outputArray = outputBuffer.floatArray
        val maxIndex = outputArray.indices.maxByOrNull { outputArray[it] } ?: -1
        return "Predicted Class: $maxIndex with Confidence: ${outputArray[maxIndex]}"
    }

    private fun normalizeBitmap(bitmap: Bitmap): Bitmap {
        val normalizedBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel) / 255.0f
                val g = Color.green(pixel) / 255.0f
                val b = Color.blue(pixel) / 255.0f
                val normalizedColor = Color.rgb((r * 255).toInt(), (g * 255).toInt(), (b * 255).toInt())
                normalizedBitmap.setPixel(x, y, normalizedColor)
            }
        }
        return normalizedBitmap
    }

    fun close() {
        interpreter?.close()
    }
}
