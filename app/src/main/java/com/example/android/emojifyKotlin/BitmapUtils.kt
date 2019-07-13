/*
* Copyright (C) 2017 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*  	http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.example.android.emojifyKotlin

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.support.v4.content.FileProvider
import android.util.DisplayMetrics
import android.view.WindowManager
import android.widget.Toast
import com.example.android.emojify.R

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal object BitmapUtils {

    private val FILE_PROVIDER_AUTHORITY = "com.example.android.fileprovider"


    /**
     * Resamples the captured photo to fit the screen for better memory usage.
     *
     * @param context   The application context.
     * @param imagePath The path of the photo to be resampled.
     * @return The resampled bitmap
     */
    fun resamplePic(context: Context, imagePath: String): Bitmap {

        // Get device screen size information
        val metrics = DisplayMetrics()
        val manager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        manager.defaultDisplay.getMetrics(metrics)

        val targetH = metrics.heightPixels
        val targetW = metrics.widthPixels

        // Get the dimensions of the original bitmap
        val bmOptions = BitmapFactory.Options()
        bmOptions.inJustDecodeBounds = true
        BitmapFactory.decodeFile(imagePath, bmOptions)
        val photoW = bmOptions.outWidth - 70
        val photoH = bmOptions.outHeight - 70

        // Determine how much to scale down the image
        val scaleFactor = Math.min(photoW / targetW, photoH / targetH)

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false
        bmOptions.inSampleSize = scaleFactor

        return BitmapFactory.decodeFile(imagePath)
    }

    /**
     * Creates the temporary image file in the cache directory.
     *
     * @return The temporary image file.
     * @throws IOException Thrown if there is an error creating the file
     */
    @Throws(IOException::class)
    fun createTempImageFile(context: Context): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss",
                Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = context.externalCacheDir

        return File.createTempFile(
                imageFileName, /* prefix */
                ".jpg", /* suffix */
                storageDir      /* directory */
        )
    }

    /**
     * Deletes image file for a given path.
     *
     * @param context   The application context.
     * @param imagePath The path of the photo to be deleted.
     */
    fun deleteImageFile(context: Context, imagePath: String): Boolean {
        // Get the file
        val imageFile = File(imagePath)

        // Delete the image
        val deleted = imageFile.delete()

        // If there is an error deleting the file, show a Toast
        if (!deleted) {
            val errorMessage = context.getString(R.string.error)
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
        }

        return deleted
    }

    /**
     * Helper method for adding the photo to the system photo gallery so it can be accessed
     * from other apps.
     *
     * @param imagePath The path of the saved image
     */
    private fun galleryAddPic(context: Context, imagePath: String) {
        val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        val f = File(imagePath)
        val contentUri = Uri.fromFile(f)
        mediaScanIntent.data = contentUri
        context.sendBroadcast(mediaScanIntent)
    }


    /**
     * Helper method for saving the image.
     *
     * @param context The application context.
     * @param image   The image to be saved.
     * @return The path of the saved image.
     */
    fun saveImage(context: Context, image: Bitmap): String? {

        var savedImagePath: String? = null

        // Create the new file in the external storage
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss",
                Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_$timeStamp.jpg"
        val storageDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + "/Emojify")
        var success = true
        if (!storageDir.exists()) {
            success = storageDir.mkdirs()
        }

        // Save the new Bitmap
        if (success) {
            val imageFile = File(storageDir, imageFileName)
            savedImagePath = imageFile.absolutePath
            try {
                val fOut = FileOutputStream(imageFile)
                image.compress(Bitmap.CompressFormat.JPEG, 100, fOut)
                fOut.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Add the image to the system gallery
            galleryAddPic(context, savedImagePath)

            // Show a Toast with the save location
            val savedMessage = context.getString(R.string.saved_message, savedImagePath)
            Toast.makeText(context, savedMessage, Toast.LENGTH_SHORT).show()
        }

        return savedImagePath
    }

    /**
     * Helper method for sharing an image.
     *
     * @param context   The image context.
     * @param imagePath The path of the image to be shared.
     */
    fun shareImage(context: Context, imagePath: String) {
        // Create the share intent and start the share activity
        val imageFile = File(imagePath)
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "image/*"
        val photoURI = FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, imageFile)
        shareIntent.putExtra(Intent.EXTRA_STREAM, photoURI)
        context.startActivity(shareIntent)
    }
}
