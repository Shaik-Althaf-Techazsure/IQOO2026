package com.techazsure.leanflow.visual

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.techazsure.leanflow.BrainEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import java.io.File

class PhotoParser(
    private val context: Context,
    private val brainEngine: BrainEngine
) {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Extracts raw text from a cached bitmap image file and passes the context
     * directly into your stable Gemma local BrainEngine runner.
     */
    suspend fun parseImageAndSummarize(imageFile: File, userCustomQuery: String): String {
        return try {
            val inputImage = InputImage.fromFilePath(context, Uri.fromFile(imageFile))

            // Native on-device OCR text extraction execution loop
            val visionText = recognizer.process(inputImage).await()
            val extractedText = visionText.text

            if (extractedText.trim().isEmpty()) {
                return "The system could not detect any readable text elements in this image."
            }

            // Construct the clean socratic context prompt for Gemma
            val unifiedPrompt = """
                Context material extracted from image:
                ---
                $extractedText
                ---
                User Specific Request: $userCustomQuery
                
                Please generate a clear response in natural paragraphs. 
                Do NOT use bullet points or markdown bolding (**).
            """.trimIndent()

            // Pass the text to your local model
            brainEngine.generateMentorResponse(unifiedPrompt)
        } catch (e: Exception) {
            "Visual Processing Error: ${e.localizedMessage ?: "Failed to extract image matrix."}"
        }
    }
}