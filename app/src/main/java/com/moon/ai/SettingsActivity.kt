package com.moon.ai

import android.os.Bundle
import android.text.InputType
import android.view.MenuItem
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText

class SettingsActivity : AppCompatActivity() {

    private lateinit var tokenInput: TextInputEditText
    private lateinit var modelStatusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        tokenInput = findViewById(R.id.tokenInput)
        modelStatusText = findViewById(R.id.modelStatusText)
        val showTokenBox: CheckBox = findViewById(R.id.showTokenBox)
        val saveTokenButton: Button = findViewById(R.id.saveTokenButton)
        val deleteModelButton: Button = findViewById(R.id.deleteModelButton)

        tokenInput.setText(Prefs.getHfToken(this) ?: "")

        showTokenBox.setOnCheckedChangeListener { _, checked ->
            tokenInput.inputType = if (checked) {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            tokenInput.setSelection(tokenInput.text?.length ?: 0)
        }

        saveTokenButton.setOnClickListener {
            val token = tokenInput.text?.toString().orEmpty()
            if (token.isBlank()) {
                Toast.makeText(this, "Paste a token first.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Prefs.saveHfToken(this, token)
            Toast.makeText(this, R.string.token_saved, Toast.LENGTH_SHORT).show()
        }

        deleteModelButton.setOnClickListener {
            val file = ModelManager.modelFile(this)
            if (!file.exists()) {
                Toast.makeText(this, "No model downloaded.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_MoonAI_Dialog)
                .setMessage(R.string.delete_model_confirm)
                .setPositiveButton(R.string.delete) { _, _ ->
                    file.delete()
                    Toast.makeText(this, R.string.model_deleted, Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }

        refreshStatus()
    }

    private fun refreshStatus() {
        modelStatusText.text = when {
            ModelManager.isModelReady(this) -> "Downloaded and ready"
            ModelManager.downloadedBytes(this) > 0 -> {
                val pct =
                    ((ModelManager.downloadedBytes(this) * 100) / ModelManager.EXPECTED_SIZE_BYTES).toInt()
                "Partly downloaded: $pct%"
            }
            else -> "Not downloaded"
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
