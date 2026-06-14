package com.techazsure.leanflow

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.BufferedReader
import java.io.InputStreamReader

object ContextExtractor {

    // Reads raw text from standard .txt and basic document files
    fun extractTextFromUri(context: Context, uri: Uri): String {
        val stringBuilder = java.lang.StringBuilder()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String? = reader.readLine()
                    while (line != null) {
                        stringBuilder.append(line).append("\n")
                        line = reader.readLine()
                    }
                }
            }
        } catch (e: Exception) {
            return "Error reading file content."
        }
        return stringBuilder.toString()
    }

    // Runs offline Optical Character Recognition (OCR) on an image
    fun extractTextWithMLKit(bitmap: Bitmap, onResult: (String) -> Unit) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                if (visionText.text.isBlank()) {
                    onResult("No readable text found in the image.")
                } else {
                    onResult(visionText.text)
                }
            }
            .addOnFailureListener { e ->
                onResult("OCR Extraction failed: ${e.message}")
            }
    }
}
