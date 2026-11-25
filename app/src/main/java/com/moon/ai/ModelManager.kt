package com.moon.ai

import android.content.Context
import java.io.File

/**
 * Everything about the model file.
 *
 * TEST MODEL (2026-11-25): Gemma 3 1B IT, int4, LiteRT-LM format —
 * the small model the user asked for to test with before the big
 * one returns. Repo: litert-community/Gemma3-1B-IT (Hugging Face),
 * the community conversion of google/gemma-3-1b-it — the same repo
 * Google's own AI Edge Gallery app ships for this model. It is NOT
 * gated, so no token or license acceptance is needed to download.
 * Size verified 2026-11-25 via the Hugging Face API tree listing
 * (584,417,280 bytes) and Google's Edge Gallery allowlist; the
 * SHA-256 below is as listed for this file (the app checks size,
 * not SHA). The big model (Gemma 3n E2B, google/gemma-3n-E2B-it-
 * litert-lm, 3,655,827,456 bytes, SHA-256 2ed7bc3a0026c93d5b8a4544b3
 * 52d9d00cd66ff0bac3ef6a20ac3d2cba4010d6) comes back later — it IS
 * gated and needs the Gemma license accepted + a token.
 */
object ModelManager {
    const val MODEL_URL =
        "https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/gemma3-1b-it-int4.litertlm"
    const val MODEL_FILE_NAME = "gemma3-1b-it-int4.litertlm"
    const val MODEL_DISPLAY_NAME = "Gemma 3 1B"
    const val EXPECTED_SIZE_BYTES = 584_417_280L // about 557 MB
    const val EXPECTED_SHA256 =
        "1325ae366d31950f137c9c357b9fa89448b176d76998180c08ceaca78bba98be"

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
