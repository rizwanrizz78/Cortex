package io.cortex.terminal.data.model

data class CommandRequest(
    val prompt: String,
    val context: String? = null
)

data class CommandResponse(
    val command: String,
    val explanation: String? = null
)
