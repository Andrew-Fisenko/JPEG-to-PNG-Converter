package com.example.jpeg_to_png_converter

import android.graphics.Bitmap
import android.net.Uri

interface ConverterView {

    fun setFirstImage(imagePickedUri: Uri)

    fun setError(error: Throwable)

    fun setSuccess(error: Pair<String, Bitmap>)

    fun showResultImage(second: Bitmap)

    fun restoreImage(path: String)
}
