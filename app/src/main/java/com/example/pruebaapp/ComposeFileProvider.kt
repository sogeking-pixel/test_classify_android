package com.example.pruebaapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

class ComposeFileProvider : FileProvider(R.xml.file_paths) {

    companion object {
        /**
         * Return the file's uri from the temporary file
         * using our file provider
         * */
        suspend fun generateImageUri(
            context: Context,
        ): Uri = withContext(Dispatchers.IO) {
            // Create the cache directory (cache/images)
            // just if does not exist (See 'file_paths.xml' file)
            val directory = File(
                context.cacheDir,
                "images",
            )
            directory.mkdirs()

            // Create a temporary file
            val tmpFile = File.createTempFile(
                "${ System.currentTimeMillis() }",
                ".jpg",
                directory,
            )

            // Define the authority, declared on the manifest under the
            // section 'provider -> android:authorities' on the manifest
            val authority = "${ context.packageName }.fileprovider"

            // Get the file's uri
            getUriForFile(context, authority, tmpFile)
        }


        /**
         * Delete the file from the filesystem (E.g: cache/images/<file>)
         * */
        suspend fun deleteFileFromCacheDirImages(
            context: Context,
            uri: Uri,
        ): Boolean = withContext(Dispatchers.IO) {
            // Creating the file object from the uri
            val file = File(
                context.cacheDir,
                "images/${ uri.lastPathSegment }",
            )
            // Delete file from the filesystem if exist
            file.delete()
        }

        /**
         * Clear the cache/images directory (E.g: cache/images)
         * */
        suspend fun deleteAllFilesFromCacheDirImages(
            context: Context,
        ) = withContext(Dispatchers.IO) {
            // Creating the file object from the uri
            val dir = File(
                context.cacheDir,
                "images",
            )
            // Delete directory if exist
            //dir.deleteRecursively()

            // Delete all the files of the directory
            dir.listFiles()?.let {  files ->
                files.forEach { file ->
                    file.delete()
                }
            }
        }

        /**
         * Compress and replace an image using its URI
         * */
        suspend fun compressImageFile(
            context: Context,
            imageUri: Uri,
        ): Uri = withContext(Dispatchers.IO) {
            try {
                // Load image from the URI
                val imageInputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
                val imageBitmap: Bitmap = BitmapFactory.decodeStream(imageInputStream)

                // Create a file to save the compressed image
                val newImageFile = File(context.cacheDir, "images/comp_${ imageUri.lastPathSegment }")

                // Compress the image and save it in the new file
                val fos = FileOutputStream(newImageFile)
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 60, fos)
                fos.flush()
                fos.close()

                // Returning the new image's URI
                Uri.fromFile(newImageFile)
            } catch (ex: IOException) {
                // If exception, returning the original image's URI
                imageUri
            }
        }
    }
}