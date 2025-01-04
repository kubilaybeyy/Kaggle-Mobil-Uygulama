package com.example.FitnessApp

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class TensorFlowModel(context: Context) {
    private var interpreter: Interpreter? = null
    private val classNames = listOf(
        "Elma", "Muz", "Karambola", "Guava", "Kivi", "Portakal", "Şeftali", "Armut", "Hurma", "Pitaya", "Erik", "Nar", "Domates", "Kavun" )

    init {
        val modelPath = "model.tflite" // Yeni model dosyasının adı
        val assetFileDescriptor = context.assets.openFd(modelPath)
        val fileInputStream = assetFileDescriptor.createInputStream()
        val fileChannel = fileInputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        val modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        interpreter = Interpreter(modelBuffer)
    }

    fun predict(input: Bitmap): String {
        return try {
            // Giriş tensörünü hazırla
            val tensorInput = prepareTensorFromBitmap(input)

            // Çıkış tensörünü oluştur
            val outputBuffer = TensorBuffer.createFixedSize(
                intArrayOf(1, 26),
                org.tensorflow.lite.DataType.FLOAT32
            )

            // Modeli çalıştır
            interpreter?.run(tensorInput, outputBuffer.buffer)

            // Çıkış tensöründeki verileri işle
            val outputArray = outputBuffer.floatArray
            val top3Indices = outputArray
                .mapIndexed { index, confidence -> index to confidence }
                .sortedByDescending { it.second }
                .take(3)

            // En iyi 3 tahmini string olarak döndür
            top3Indices.joinToString("\n") { (index, confidence) ->
                "${classNames[index]}: %.2f%%".format(confidence * 100)
            }
        } catch (e: Exception) {
            val inputTensorShape = interpreter?.getInputTensor(0)?.shape() // Giriş tensör şekli
            val outputTensorShape = interpreter?.getOutputTensor(0)?.shape() // Çıkış tensör şekli
            println("${inputTensorShape?.contentToString()}\n${outputTensorShape?.contentToString()}")
            println(e.message)
            "Prediction Error: ${e.message}"
        }
    }


    private fun prepareTensorFromBitmap(bitmap: Bitmap): ByteBuffer {
        val width = 224
        val height = 224

        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)

        // TensorFlow Lite için ByteBuffer oluştur
        val byteBuffer = ByteBuffer.allocateDirect(4 * width * height * 3) // 4 byte (float32), 3 kanal (RGB)
        byteBuffer.order(ByteOrder.nativeOrder())

        // Bitmap'i ByteBuffer'a dönüştür
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = resizedBitmap.getPixel(x, y)
                byteBuffer.putFloat((pixel shr 16 and 0xFF) / 255.0f) // R
                byteBuffer.putFloat((pixel shr 8 and 0xFF) / 255.0f)  // G
                byteBuffer.putFloat((pixel and 0xFF) / 255.0f)        // B
            }
        }

        return byteBuffer
    }



    fun close() {
        interpreter?.close()

    }
}
