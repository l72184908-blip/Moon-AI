package com.moon.ai

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val viewModel: ChatViewModel by viewModels()
    private lateinit var adapter: ChatAdapter
    private lateinit var chatList: RecyclerView
    private lateinit var inputText: EditText
    private lateinit var sendButton: Button
    private lateinit var statusText: TextView
    private lateinit var downloadButton: Button
    private lateinit var downloadNote: TextView
    private lateinit var downloadProgress: ProgressBar

    private val settingsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) viewModel.refreshModelState()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        chatList = findViewById(R.id.chatList)
        inputText = findViewById(R.id.inputText)
        sendButton = findViewById(R.id.sendButton)
        statusText = findViewById(R.id.statusText)
        downloadButton = findViewById(R.id.downloadButton)
        downloadNote = findViewById(R.id.downloadNote)
        downloadProgress = findViewById(R.id.downloadProgress)

        adapter = ChatAdapter()
        chatList.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = false }
        chatList.adapter = adapter

        sendButton.setOnClickListener { trySend() }
        inputText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                trySend()
                true
            } else {
                false
            }
        }
        downloadButton.setOnClickListener { onDownloadClicked() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.messages.collect { messages ->
                        val atBottom = isAtBottom()
                        adapter.submitList(messages.toList()) {
                            if (atBottom || messages.isNotEmpty()) {
                                chatList.scrollToPosition(messages.size - 1)
                            }
                        }
                    }
                }
                launch {
                    viewModel.uiState.collect { state -> renderState(state) }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshModelState()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_clear -> {
                viewModel.clearChat()
                true
            }
            R.id.action_settings -> {
                settingsLauncher.launch(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun isAtBottom(): Boolean {
        val manager = chatList.layoutManager as? LinearLayoutManager ?: return true
        val lastVisible = manager.findLastCompletelyVisibleItemPosition()
        return lastVisible == adapter.itemCount - 1 || adapter.itemCount == 0
    }

    private fun trySend() {
        val text = inputText.text.toString()
        if (text.isBlank()) return
        if (viewModel.uiState.value.status != ChatViewModel.ModelStatus.READY) {
            Toast.makeText(this, R.string.model_missing, Toast.LENGTH_SHORT).show()
            return
        }
        inputText.text.clear()
        viewModel.send(text)
    }

    private fun onDownloadClicked() {
        if (Prefs.getHfToken(this).isNullOrBlank()) {
            Toast.makeText(this, R.string.need_token, Toast.LENGTH_LONG).show()
            settingsLauncher.launch(Intent(this, SettingsActivity::class.java))
            return
        }
        if (!isOnWifi()) {
            MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_MoonAI_Dialog)
                .setMessage(R.string.wifi_warning)
                .setPositiveButton(R.string.continue_anyway) { _, _ ->
                    viewModel.startDownload()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
            return
        }
        viewModel.startDownload()
    }

    private fun isOnWifi(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private fun renderState(state: ChatViewModel.UiState) {
        val ready = state.status == ChatViewModel.ModelStatus.READY
        inputText.isEnabled = ready && !state.isGenerating
        sendButton.isEnabled = ready && !state.isGenerating

        when (state.status) {
            ChatViewModel.ModelStatus.NOT_DOWNLOADED -> {
                val partial = ModelManager.downloadedBytes(this)
                statusText.text = if (partial > 0) {
                    val pct = ((partial * 100) / ModelManager.EXPECTED_SIZE_BYTES).toInt()
                    "Download paused at $pct% — tap Resume"
                } else {
                    getString(R.string.status_not_downloaded)
                }
                downloadButton.text =
                    if (partial > 0) getString(R.string.resume) else getString(R.string.download)
                downloadButton.visibility = View.VISIBLE
                downloadNote.visibility = View.VISIBLE
                downloadProgress.visibility = View.GONE
            }
            ChatViewModel.ModelStatus.DOWNLOADING -> {
                statusText.text = getString(R.string.status_downloading, state.downloadProgress)
                downloadButton.visibility = View.GONE
                downloadNote.visibility = View.GONE
                downloadProgress.visibility = View.VISIBLE
                downloadProgress.progress = state.downloadProgress
            }
            ChatViewModel.ModelStatus.LOADING -> {
                statusText.text = getString(R.string.status_loading)
                downloadButton.visibility = View.GONE
                downloadNote.visibility = View.GONE
                downloadProgress.visibility = View.GONE
            }
            ChatViewModel.ModelStatus.READY -> {
                statusText.text = getString(R.string.status_ready)
                downloadButton.visibility = View.GONE
                downloadNote.visibility = View.GONE
                downloadProgress.visibility = View.GONE
            }
            ChatViewModel.ModelStatus.ERROR -> {
                statusText.text = state.errorMessage
                    ?: getString(R.string.status_error, "unknown error")
                downloadButton.text = getString(R.string.download)
                downloadButton.visibility = View.VISIBLE
                downloadNote.visibility = View.VISIBLE
                downloadProgress.visibility = View.GONE
            }
        }
    }
}
