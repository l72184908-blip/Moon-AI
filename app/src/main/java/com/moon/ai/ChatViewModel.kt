package com.moon.ai

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    enum class ModelStatus { NOT_DOWNLOADED, DOWNLOADING, LOADING, READY, ERROR }

    data class UiState(
        val status: ModelStatus = ModelStatus.NOT_DOWNLOADED,
        val downloadProgress: Int = 0, // 0..100
        val errorMessage: String? = null,
        val isGenerating: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(ChatMessage(Role.MODEL, application.getString(R.string.welcome)))
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val engine = LlmEngine(application)
    private val downloader = ModelDownloader()
    private var downloadJob: Job? = null

    init {
        refreshModelState()
    }

    /** Re-checks the model file (called on start and when returning from Settings). */
    fun refreshModelState() {
        val app = getApplication<Application>()
        if (ModelManager.isModelReady(app)) {
            if (_uiState.value.status != ModelStatus.READY &&
                _uiState.value.status != ModelStatus.LOADING
            ) {
                loadEngine()
            }
        } else {
            if (engine.isReady) engine.close()
            if (_uiState.value.status != ModelStatus.DOWNLOADING) {
                _uiState.update {
                    it.copy(
                        status = ModelStatus.NOT_DOWNLOADED,
                        downloadProgress = partialPercent(app),
                        errorMessage = null
                    )
                }
            }
        }
    }

    private fun partialPercent(app: Application): Int {
        val done = ModelManager.downloadedBytes(app)
        return if (done > 0) ((done * 100) / ModelManager.EXPECTED_SIZE_BYTES).toInt() else 0
    }

    private fun loadEngine() {
        val app = getApplication<Application>()
        _uiState.update { it.copy(status = ModelStatus.LOADING, errorMessage = null) }
        viewModelScope.launch {
            try {
                engine.initialize(ModelManager.modelFile(app).absolutePath)
                _uiState.update { it.copy(status = ModelStatus.READY) }
            } catch (e: Exception) {
                _uiState.update { it.copy(status = ModelStatus.ERROR, errorMessage = e.message) }
            }
        }
    }

    fun startDownload() {
        val app = getApplication<Application>()
        if (_uiState.value.status == ModelStatus.DOWNLOADING) return

        val dir = ModelManager.modelFile(app).parentFile
        val free = dir?.freeSpace ?: 0L
        if (free < ModelManager.EXPECTED_SIZE_BYTES + 512L * 1024 * 1024) {
            _uiState.update {
                it.copy(
                    status = ModelStatus.ERROR,
                    errorMessage = app.getString(R.string.not_enough_space)
                )
            }
            return
        }

        val token = Prefs.getHfToken(app)
        _uiState.update {
            it.copy(
                status = ModelStatus.DOWNLOADING,
                downloadProgress = partialPercent(app),
                errorMessage = null
            )
        }
        downloadJob = viewModelScope.launch {
            try {
                downloader.download(app, token) { downloaded, total ->
                    val pct = if (total > 0) ((downloaded * 100) / total).toInt() else 0
                    _uiState.update { it.copy(downloadProgress = pct) }
                }
                loadEngine()
            } catch (e: ModelDownloader.AuthException) {
                _uiState.update {
                    it.copy(
                        status = ModelStatus.ERROR,
                        errorMessage = app.getString(R.string.download_failed_auth)
                    )
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update { it.copy(status = ModelStatus.ERROR, errorMessage = e.message) }
            }
        }
    }

    fun send(prompt: String) {
        val text = prompt.trim()
        if (text.isEmpty()) return
        if (_uiState.value.status != ModelStatus.READY || _uiState.value.isGenerating) return

        _messages.update { it + ChatMessage(Role.USER, text) + ChatMessage(Role.MODEL, "", streaming = true) }
        _uiState.update { it.copy(isGenerating = true) }

        viewModelScope.launch {
            val reply = StringBuilder()
            try {
                engine.sendStreaming(text).collect { chunk ->
                    reply.append(chunk)
                    val full = reply.toString()
                    _messages.update { list ->
                        list.dropLast(1) + list.last().copy(text = full)
                    }
                }
                _messages.update { list ->
                    list.dropLast(1) + list.last().copy(streaming = false)
                }
            } catch (e: Exception) {
                _messages.update { list ->
                    val fallback = if (reply.isEmpty()) {
                        "Sorry, something went wrong: ${e.message}"
                    } else {
                        reply.toString()
                    }
                    list.dropLast(1) + list.last().copy(text = fallback, streaming = false)
                }
            } finally {
                _uiState.update { it.copy(isGenerating = false) }
            }
        }
    }

    fun clearChat() {
        if (_uiState.value.isGenerating) return
        engine.newChat()
        _messages.value = listOf(
            ChatMessage(Role.MODEL, getApplication<Application>().getString(R.string.welcome))
        )
    }

    override fun onCleared() {
        downloadJob?.cancel()
        engine.close()
        super.onCleared()
    }
}
