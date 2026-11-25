package com.moon.ai

import android.content.Context

/** Tiny settings store. The Hugging Face token lives here, in the app's
 * private preferences — it is never sent anywhere except to
 * huggingface.co when downloading the model. */
object Prefs {
    private const val NAME = "moon_ai"
    private const val KEY_TOKEN = "hf_token"

    fun getHfToken(context: Context): String? =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .getString(KEY_TOKEN, null)

    fun saveHfToken(context: Context, token: String) {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_TOKEN, token.trim())
            .apply()
    }
}
