package com.example.pruebaapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.classifier.ImageClassifier
import java.io.IOException

class BakingViewModel : ViewModel() {
  private val _uiState: MutableStateFlow<UiState> =
    MutableStateFlow(UiState.Initial)
  val uiState: StateFlow<UiState> =
    _uiState.asStateFlow()
  private val _imageState: MutableStateFlow<ImageState> = MutableStateFlow(ImageState())
  val imageState: StateFlow<ImageState> = _imageState.asStateFlow()

  private val generativeModel = GenerativeModel(
    modelName = "gemini-2.0-flash",
    apiKey = BuildConfig.apiKey
  )

  fun sendPrompt(
    bitmap: Bitmap,
    prompt: String
  ) {
    _uiState.value = UiState.LoadingGeminis

    if(prompt.isEmpty()) return

    viewModelScope.launch(Dispatchers.IO) {
      try {
        val response = generativeModel.generateContent(
          content {
            image(bitmap)
            text(prompt)
          }
        )
        response.text?.let { outputContent ->
          _uiState.value = UiState.SuccessGeminis(outputContent)
        }
      } catch (e: Exception) {
        _uiState.value = UiState.Error(e.localizedMessage ?: "")
      }
    }
  }

  fun analizeImage(bitmap: Bitmap, context: Context) {
    _uiState.value = UiState.LoadingTensorFlow
    Log.d("ModalCustom", "uistate is loaging in view model")

    return try {
      // Load classifier from assets/model.tflite
      val classifier = ImageClassifier.createFromFile( context,"NSFW.tflite")

      // Wrap bitmap into TensorImage
      val image = TensorImage.fromBitmap(bitmap)

      // Run inference
      var results = classifier.classify(image)

      var list_out: String = ""
      var score : Float = 0f

      results.firstOrNull()?.categories?.sortedByDescending { it.score }?.take(2)?.forEach {
        list_out += "${it.label}: ${"%.2f".format(it.score * 100)}%"
        println("${it.label}: ${"%.2f".format(it.score * 100)}%")
        score = if (it.score >= score) it.score else score
      }
      Log.d("ModalCustom", "uistate is success in view model")
      _uiState.value = UiState.SuccessTensorFlow(list_out, score)

    } catch (e: IOException) {
      _uiState.value = UiState.Error(e.localizedMessage ?: "")
    }

  }





  fun takePhoto(photoUri: Uri) {
    _imageState.update { currentState -> currentState.copy(photoUris = photoUri) }
  }


  fun uriToBitmap(context: Context, imageUri: Uri?): Bitmap? {
    if (imageUri == null) return null
    return try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val source = ImageDecoder.createSource(context.contentResolver, imageUri)
        ImageDecoder.decodeBitmap(source)
      } else {
        context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
          BitmapFactory.decodeStream(inputStream)
        }
      }
    } catch (e: IOException) {
      e.printStackTrace()
      null
    }
  }



}