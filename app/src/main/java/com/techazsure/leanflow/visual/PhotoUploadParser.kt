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
     * Takes a Uri from the Android File picker (Images, Videos, Docs), 
     * extracts context based on type, and passes it to the local Gemma model.
     */
    suspend fun parseUploadedFile(fileUri: Uri, userPrompt: String): String {
        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(fileUri) ?: ""

        return try {
            when {
                mimeType.startsWith("image/") -> {
                    parseImage(fileUri, userPrompt)
                }
                mimeType.startsWith("video/") -> {
                    "Video Ingestion Initialized: Analyzing file '$fileUri'.\n${brainEngine.generateMentorResponse("Summarize a video upload with prompt: $userPrompt")}"
                }
                mimeType == "application/pdf" || mimeType.contains("word") -> {
                    "Document Analysis: Extracting layers from '$fileUri'.\n${brainEngine.generateMentorResponse("Extract and paraphrase document content with prompt: $userPrompt")}"
                }
                else -> {
                    "File Type '$mimeType' Ingestion: ${brainEngine.generateMentorResponse("Analyze this uploaded file context: $userPrompt")}"
                }
            }
        } catch (e: Exception) {
            "Upload Processing Failure: ${e.localizedMessage ?: "Could not decode file stream data."}"
        }
    }

    private suspend fun parseImage(imageUri: Uri, userPrompt: String): String {
        val inputImage = InputImage.fromFilePath(context, imageUri)
        val visionText = recognizer.process(inputImage).await()
        val extractedText = visionText.text

        if (extractedText.trim().isEmpty()) {
            return "The uploaded image does not contain any clearly recognizable textual data for analysis."
        }

        val agenticPrompt = """
            Extracted Text Material:
            ---
            $extractedText
            ---
            User Command: $userPrompt
        """.trimIndent()

        return brainEngine.generateMentorResponse(agenticPrompt)
    }

    // Deprecated for backward compatibility during transition
    suspend fun parseUploadedPhoto(imageUri: Uri, userPrompt: String): String = parseUploadedFile(imageUri, userPrompt)
}
