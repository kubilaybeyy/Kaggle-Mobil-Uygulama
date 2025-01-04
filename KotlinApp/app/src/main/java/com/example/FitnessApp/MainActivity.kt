package com.example.FitnessApp

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File

class MainActivity : ComponentActivity() {

    private lateinit var model: TensorFlowModel
    private val CAMERA_PERMISSION_REQUEST_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TensorFlow modelini yükle
        model = TensorFlowModel(this)

        var selectedImage by mutableStateOf<Bitmap?>(null)
        var predictionResult by mutableStateOf("No prediction yet")

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        }

        // Resim seçme işlemi
        val selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                try {
                    val inputStream = contentResolver.openInputStream(it)
                    val bitmap = BitmapFactory.decodeStream(inputStream)

                    // Tahmin yap
                    val result = model.predict(bitmap)
                    selectedImage = bitmap
                    predictionResult = result
                } catch (e: Exception) {
                    Log.e("Prediction", "Tahmin işlemi sırasında hata oluştu: ${e.message}")
                }
            }
        }

        setContent {
            AppContent(
                predictionText = predictionResult,
                selectedImage = selectedImage,
                onPickImageClick = {
                    selectImageLauncher.launch("image/*")
                },
                onTakePhotoClick = { bitmap ->
                    // Tahmin yap
                    selectedImage = bitmap
                    predictionResult = model.predict(bitmap)
                }
            )
        }
    }


    private fun prepareBitmapForTensor(bitmap: Bitmap): Bitmap {
        val width = 224
        val height = 224
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }


    override fun onDestroy() {
        super.onDestroy()
        model.close()
    }
}

@Composable
fun CameraPreview(
    onImageCaptured: (Bitmap) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var imageCapture: ImageCapture? = remember { null }

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { context ->
            val previewView = PreviewView(context)
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                Log.e("CameraX", "Camera binding failed", e)
            }

            previewView
        })

        // Fotoğraf çekme düğmesi
        Button(
            onClick = {
                val file = File(context.cacheDir, "temp_image.jpg")
                val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
                imageCapture?.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onError(exception: ImageCaptureException) {
                            Log.e("CameraX", "Photo capture failed", exception)
                        }

                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                            onImageCaptured(bitmap)
                        }
                    }
                )
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Text("Fotoğraf Çek ve Kaydet")
        }

        // Kapatma düğmesi
        Button(
            onClick = { onClose() },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Text("Kapat")
        }
    }
}

@Composable
fun AppContent(
    predictionText: String,
    selectedImage: Bitmap?,
    onPickImageClick: () -> Unit,
    onTakePhotoClick: (Bitmap) -> Unit
) {
    // Kamera önizleme ekranını kontrol eden durum
    var showCamera by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = predictionText,
            modifier = Modifier.padding(16.dp)
        )

        selectedImage?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "Selected Image",
                modifier = Modifier
                    .padding(16.dp)
                    .size(200.dp)
            )
        }

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(onClick = onPickImageClick) {
                Text(text = "Galeriden Fotoğraf Seç")
            }
            Button(onClick = { showCamera = true }) {  // Kamera önizlemesini aç
                Text(text = "Fotoğraf Çek")
            }
        }

        // Kamera önizleme ekranını göster
        if (showCamera) {
            CameraPreview(
                onImageCaptured = { bitmap ->
                    onTakePhotoClick(bitmap)
                    showCamera = false  // Kamera ekranını kapat
                },
                onClose = { showCamera = false }  // Kullanıcı kapatmak isterse
            )
        }
    }
}
