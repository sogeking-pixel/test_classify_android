package com.example.pruebaapp

import android.net.Uri

data class ImageState(
    val isLoading: Boolean = false,
    val photoUris: Uri? = null,
)
