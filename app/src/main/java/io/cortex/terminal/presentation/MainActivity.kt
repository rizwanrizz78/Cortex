package io.cortex.terminal.presentation

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint
import io.cortex.terminal.presentation.terminal.TerminalScreen
import io.cortex.terminal.ui.theme.CortexTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
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

        setContent {
            // Minimal theme wrapper
            CortexTheme {
                TerminalScreen()
            }
        }
    }
}
