package com.example.pruebaapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
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
    _uiState.value = UiState.Loading

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
          _uiState.value = UiState.Success(outputContent)
        }
      } catch (e: Exception) {
        _uiState.value = UiState.Error(e.localizedMessage ?: "")
      }
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