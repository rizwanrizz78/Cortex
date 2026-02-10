package io.cortex.terminal.presentation.terminal

import android.app.Application
import android.content.Intent
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.cortex.terminal.engine.TerminalSession
import io.cortex.terminal.engine.TerminalService
import javax.inject.Inject

@HiltViewModel
class TerminalViewModel @Inject constructor(
    private val application: Application
) : ViewModel() {

    // For simplicity, manage one session directly.
    val session = TerminalSession()

    init {
        session.initialize()
        startService()
    }

    private fun startService() {
        val intent = Intent(application, TerminalService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            application.startForegroundService(intent)
        } else {
            application.startService(intent)
        }
    }

    private fun stopService() {
        val intent = Intent(application, TerminalService::class.java)
        application.stopService(intent)
    }

    override fun onCleared() {
        super.onCleared()
        session.close()
        stopService()
    }
}
