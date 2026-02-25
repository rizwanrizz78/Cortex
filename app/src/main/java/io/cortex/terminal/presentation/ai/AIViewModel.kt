package io.cortex.terminal.presentation.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.cortex.terminal.domain.usecase.GetCommandSuggestionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AIViewModel @Inject constructor(
    private val getCommandSuggestionUseCase: GetCommandSuggestionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<AIUiState>(AIUiState.Idle)
    val uiState = _uiState.asStateFlow()

    fun suggestCommand(prompt: String) {
        viewModelScope.launch {
            _uiState.value = AIUiState.Loading
            getCommandSuggestionUseCase(prompt).collect { result ->
                result.onSuccess {
                    _uiState.value = AIUiState.Success(it.command)
                }.onFailure {
                    _uiState.value = AIUiState.Error(it.message ?: "Unknown error")
                }
            }
        }
    }

    fun clearState() {
        _uiState.value = AIUiState.Idle
    }
}

sealed class AIUiState {
    object Idle : AIUiState()
    object Loading : AIUiState()
    data class Success(val command: String) : AIUiState()
    data class Error(val message: String) : AIUiState()
}
