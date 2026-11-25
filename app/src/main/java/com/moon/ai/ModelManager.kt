package com.moon.ai

import android.content.Context
import java.io.File

/**
 * Everything about the model file.
 *
 * Model: Google Gemma 3n E2B, instruction-tuned, int4, LiteRT-LM format.
 * Repo: google/gemma-3n-E2B-it-litert-lm (Hugging Face, gated — the user
 * must accept the Gemma license there and use a free read token).
 * File verified 2026-11-25 via the Hugging Face API + the file's
 * published SHA-256 (see BUILD_NOTES.md).
 */
object ModelManager {
    const val MODEL_URL =
        "https://huggingface.co/google/gemma-3n-E2B-it-litert-lm/resolve/main/gemma-3n-E2B-it-int4.litertlm"
    const val MODEL_FILE_NAME = "gemma-3n-E2B-it-int4.litertlm"
    const val MODEL_DISPLAY_NAME = "Gemma 3n E2B"
    const val EXPECTED_SIZE_BYTES = 3_655_827_456L // about 3.4 GB
    const val EXPECTED_SHA256 =
        "2ed7bc3a0026c93d5b8a4544b352d9d00cd66ff0bac3ef6a20ac3d2cba4010d6"

    fun modelFile(context: Context): File =
        File(File(context.filesDir, "models"), MODEL_FILE_NAME)

    fun isModelReady(context: Context): Boolean {
        val f = modelFile(context)
        return f.exists() && f.length() == EXPECTED_SIZE_BYTES
    }

    fun downloadedBytes(context: Context): Long {
        val f = modelFile(context)
        return if (f.exists()) f.length() else 0L
    }
}
