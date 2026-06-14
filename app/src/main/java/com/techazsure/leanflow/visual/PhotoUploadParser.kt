package com.techazsure.leanflow.visual

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.techazsure.leanflow.BrainEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await

class PhotoUploadParser(
    private val context: Context,
    private val brainEngine: BrainEngine
) {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Takes a Uri from the Android Gallery/File picker, extracts the on-device
     * text text schema via ML Kit, and passes it directly to the local Gemma model.
     */
    suspend fun parseUploadedPhoto(imageUri: Uri, userPrompt: String): String {
        return try {
            // Convert gallery Uri to ML Kit InputImage matrix natively
            val inputImage = InputImage.fromFilePath(context, imageUri)

            // Execute offline high-speed OCR text extraction
            val visionText = recognizer.process(inputImage).await()
            val extractedText = visionText.text

            if (extractedText.trim().isEmpty()) {
                return "The uploaded image does not contain any clearly recognizable textual data for analysis."
            }

            // Frame a clean multi-modal context block for Gemma
            val agenticPrompt = """
                You are analyzing an uploaded document/photo asset.
                Extracted Text Material:
                ---
                $extractedText
                ---
                User Command: $userPrompt
                
                Please evaluate the text above against the user command and output a clear, structured response.
            """.trimIndent()

            // Pass directly to the local LLM
            brainEngine.generateMentorResponse(agenticPrompt)
        } catch (e: Exception) {
            "Upload Processing Failure: ${e.localizedMessage ?: "Could not decode file stream data."}"
        }
    }
}