package io.cortex.terminal.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint
import io.cortex.terminal.presentation.terminal.TerminalScreen
import io.cortex.terminal.ui.theme.CortexTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            // Permission result handled
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge to edge
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Blur/Transparency
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.attributes.blurBehindRadius = 20
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
        }

        checkNotificationPermission()

        setContent {
            // Minimal theme wrapper
            CortexTheme {
                TerminalScreen()
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
