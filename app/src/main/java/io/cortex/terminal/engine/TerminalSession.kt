package io.cortex.terminal.engine

import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets

class TerminalSession(
    val command: String = "/system/bin/sh",
    var initialRows: Int = 24,
    var initialCols: Int = 80
) {

    private var handle: Long = 0
    private var ptyFd: Int = -1
    private var pfd: ParcelFileDescriptor? = null
    private var fileDescriptor: FileDescriptor? = null
    private var outputStream: FileOutputStream? = null
    private var inputStream: FileInputStream? = null

    private val scope = CoroutineScope(Dispatchers.IO)
    private var readJob: Job? = null
    private var isRunning = false

    // A flow to notify UI of screen updates.
    private val _screenUpdate = MutableStateFlow(0L)
    val screenUpdate = _screenUpdate.asStateFlow()

    companion object {
        var isNativeLoaded = false
        init {
            try {
                System.loadLibrary("terminal-jni")
                isNativeLoaded = true
            } catch (e: UnsatisfiedLinkError) {
                Log.e("TerminalSession", "Failed to load terminal-jni library", e)
            } catch (e: Exception) {
                Log.e("TerminalSession", "Failed to load terminal-jni library", e)
            }
        }
    }

    fun initialize() {
        if (!isNativeLoaded) {
            Log.e("TerminalSession", "Native library not loaded, skipping initialization")
            return
        }
        if (handle != 0L) return

        try {
            handle = createSession(command, initialRows, initialCols)
        } catch (e: UnsatisfiedLinkError) {
            Log.e("TerminalSession", "Failed to link native createSession", e)
            return
        }

        if (handle == 0L) {
            Log.e("TerminalSession", "Failed to create native session")
            return
        }

        ptyFd = getPtyFd(handle)
        if (ptyFd < 0) {
            Log.e("TerminalSession", "Invalid PTY fd")
            return
        }

        // Create FileDescriptor via ParcelFileDescriptor for safety
        try {
            pfd = ParcelFileDescriptor.adoptFd(ptyFd)
            fileDescriptor = pfd?.fileDescriptor

            outputStream = FileOutputStream(fileDescriptor)
            inputStream = FileInputStream(fileDescriptor)
        } catch (e: Exception) {
            Log.e("TerminalSession", "Failed to create FileDescriptor", e)
            return
        }

        isRunning = true
        startReading()
    }

    private fun startReading() {
        readJob = scope.launch {
            val buffer = ByteArray(4096)
            while (isRunning) {
                try {
                    val read = inputStream?.read(buffer) ?: -1
                    if (read > 0) {
                        pushBytes(handle, buffer, read)
                        _screenUpdate.emit(System.currentTimeMillis())
                    } else if (read < 0) {
                        // EOF
                        Log.d("TerminalSession", "EOF reached")
                        break
                    }
                } catch (e: Exception) {
                    if (isRunning) {
                        Log.e("TerminalSession", "Read error: ${e.message}")
                    }
                    break
                }
            }
        }
    }

    fun write(data: String) {
        if (!isRunning || outputStream == null) return
        scope.launch {
            try {
                outputStream?.write(data.toByteArray(StandardCharsets.UTF_8))
            } catch (e: Exception) {
                Log.e("TerminalSession", "Write error", e)
            }
        }
    }

    fun resize(newRows: Int, newCols: Int) {
        if (handle == 0L) return
        initialRows = newRows
        initialCols = newCols
        resize(handle, newRows, newCols)
    }

    fun getLine(row: Int): String {
        if (handle == 0L) return ""
        return getLineInternal(handle, row)
    }

    fun close() {
        isRunning = false
        try {
            // Close streams and FD safely
            inputStream?.close()
            outputStream?.close()
            pfd?.close()
        } catch (e: Exception) {
            // Ignore
        }
        if (handle != 0L) {
            closeSession(handle)
            handle = 0L
        }
    }

    // Native Interface
    private external fun createSession(cmd: String, rows: Int, cols: Int): Long
    private external fun getPtyFd(handle: Long): Int
    private external fun resize(handle: Long, rows: Int, cols: Int)
    private external fun pushBytes(handle: Long, data: ByteArray, length: Int)
    private external fun closeSession(handle: Long)
    private external fun getLineInternal(handle: Long, row: Int): String
}
