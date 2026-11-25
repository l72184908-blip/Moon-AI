package com.moon.ai

import java.util.UUID

enum class Role { USER, MODEL }

data class ChatMessage(
    val role: Role,
    val text: String,
    val streaming: Boolean = false,
    val id: String = UUID.randomUUID().toString()
)
