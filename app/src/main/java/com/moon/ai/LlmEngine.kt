package com.moon.ai

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Thin wrapper around the LiteRT-LM Kotlin API (verified against the
 * official guide at ai.google.dev/edge/litert-lm/android and the
 * Conversation.kt source on github.com/google-ai-edge/litert-lm).
 *
 * Backend is CPU on purpose: it works on every phone, including the
 * user's MediaTek device. GPU is faster on some phones but fails on
 * others; CPU + XNNPACK is the safe default for v1.
 */
class LlmEngine(private val context: Context) {

    private var engine: Engine? = null
    private var conversation: Conversation? = null

    val isReady: Boolean get() = engine != null

    private fun buildConversationConfig() = ConversationConfig(
        systemInstruction = Contents.of(
            "You are Moon AI, a helpful offline assistant. " +
                "Answer clearly and concisely."
        ),
        samplerConfig = SamplerConfig(topK = 40, topP = 0.95, temperature = 0.7),
        maxOutputToken = 1024
    )

    /** Loads the model. Slow (seconds) — always call off the main thread. */
    suspend fun initialize(modelPath: String) = withContext(Dispatchers.Default) {
        close()
        val config = EngineConfig(
            modelPath = modelPath,
            backend = Backend.CPU(),
            cacheDir = context.cacheDir.path
        )
        val newEngine = Engine(config)
        newEngine.initialize()
        engine = newEngine
        conversation = newEngine.createConversation(buildConversationConfig())
    }

    /** Streams the reply as text chunks (one Flow emission per chunk). */
    fun sendStreaming(prompt: String): Flow<String> = flow {
        val conv = conversation ?: throw IllegalStateException("Engine is not initialized")
        conv.sendMessageAsync(prompt).collect { message ->
            val text = message.text
            if (text.isNotEmpty()) emit(text)
        }
    }.flowOn(Dispatchers.Default)

    /** Starts a fresh conversation (clears the model's memory of the chat). */
    fun newChat() {
        try {
            conversation?.close()
        } catch (_: Exception) {
        }
        conversation = engine?.createConversation(buildConversationConfig())
    }

    fun close() {
        try {
            conversation?.close()
        } catch (_: Exception) {
        }
        try {
            engine?.close()
        } catch (_: Exception) {
        }
        conversation = null
        engine = null
    }
}
