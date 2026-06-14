package com.techazsure.leanflow

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

object ContextExtractor {

    /**
     * Extracts raw text from a URI. 
     * In a real app, you would handle PDFs, Docx, or simple text files.
     */
    fun extractTextFromUri(context: Context, uri: Uri): String {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader().use { it.readText() }
            } ?: "Empty file content"
        } catch (e: Exception) {
            "Error extracting text: ${e.message}"
        }
    }

    /**
     * Uses ML Kit Text Recognition to extract text from a Bitmap.
     */
    fun extractTextWithMLKit(bitmap: Bitmap, onResult: (String) -> Unit) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                onResult(visionText.text.ifBlank { "No text detected in image." })
            }
            .addOnFailureListener { e ->
                onResult("OCR Failure: ${e.message}")
            }
    }
}
