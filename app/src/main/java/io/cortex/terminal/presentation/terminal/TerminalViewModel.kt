package io.cortex.terminal.presentation.terminal

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.cortex.terminal.engine.TerminalSession
import javax.inject.Inject

@HiltViewModel
class TerminalViewModel @Inject constructor() : ViewModel() {

    // For simplicity, manage one session directly.
    val session = TerminalSession()

    init {
        session.initialize()
    }

    override fun onCleared() {
        super.onCleared()
        session.close()
    }
}
