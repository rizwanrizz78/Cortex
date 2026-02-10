package io.cortex.terminal.domain.usecase

import io.cortex.terminal.data.model.CommandResponse
import io.cortex.terminal.data.repository.AIRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCommandSuggestionUseCase @Inject constructor(
    private val repository: AIRepository
) {
    operator fun invoke(prompt: String): Flow<Result<CommandResponse>> {
        return repository.getCommandSuggestion(prompt)
    }
}
