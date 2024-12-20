package com.example.FitnessApp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.FitnessApp.ui.theme.FitnessAppTheme

class MainActivity : ComponentActivity() {

    private lateinit var model: TensorFlowModel

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        println("bitmap")
        model = TensorFlowModel(this)

        val selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                val inputStream = contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)

                val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 100, 100, true)
                println(resizedBitmap)


                val result = model.predict(resizedBitmap)
                println(result)
            }
        }

        val predictionResult = mutableStateOf("No prediction yet")

        setContent {
            AppContent(
                predictionText = predictionResult.value,
                onPickImageClick = {
                    selectImageLauncher.launch("image/*")
                }
            )
        }

    }


    private fun loadImageFromUri(uri: android.net.Uri): Bitmap {
        val inputStream = contentResolver.openInputStream(uri)
        return android.graphics.BitmapFactory.decodeStream(inputStream)
    }

    override fun onDestroy() {
        super.onDestroy()
        model.close()
    }
}

@Composable
fun AppContent(predictionText: String, onPickImageClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = predictionText, modifier = Modifier.padding(16.dp))

        Button(onClick = onPickImageClick) {
            Text(text = "Pick an Image")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AppContentPreview() {
    FitnessAppTheme {
        AppContent(predictionText = "No prediction yet") {}
    }
}


@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    FitnessAppTheme {
        Greeting("Android")
    }
}
