package io.cortex.terminal.data.repository

import io.cortex.terminal.data.api.AIService
import io.cortex.terminal.data.model.CommandRequest
import io.cortex.terminal.data.model.CommandResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class AIRepository @Inject constructor(
    private val aiService: AIService
) {

    fun getCommandSuggestion(prompt: String): Flow<Result<CommandResponse>> = flow {
        try {
            val response = aiService.suggestCommand(CommandRequest(prompt))
            if (response.isSuccessful && response.body() != null) {
                emit(Result.success(response.body()!!))
            } else {
                emit(Result.failure(Exception("AI Service Error: ${response.code()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}
