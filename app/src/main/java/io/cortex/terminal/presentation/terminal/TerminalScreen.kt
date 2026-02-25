package io.cortex.terminal.presentation.terminal

import android.view.KeyEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import io.cortex.terminal.presentation.ai.AIUiState
import io.cortex.terminal.presentation.ai.AIViewModel
import kotlinx.coroutines.flow.collect

@Composable
fun TerminalScreen(
    terminalViewModel: TerminalViewModel = hiltViewModel(),
    aiViewModel: AIViewModel = hiltViewModel()
) {
    val session = terminalViewModel.session
    val updateTrigger by session.screenUpdate.collectAsState()

    // AI State
    val aiState by aiViewModel.uiState.collectAsState()
    var showAiInput by remember { mutableStateOf(false) }
    var aiPrompt by remember { mutableStateOf("") }

    // Config
    val textMeasurer = rememberTextMeasurer()
    var fontSize by remember { mutableStateOf(14.sp) }

    // Focus & Input
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var inputState by remember { mutableStateOf(TextFieldValue("")) }

    // Glassmorphism background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F0F1A).copy(alpha = 0.60f), // More transparency for glass effect
                        Color(0xFF1A1A2E).copy(alpha = 0.80f)
                    )
                )
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                         keyboardController?.show()
                         focusRequester.requestFocus()
                    }
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                     if (zoom != 1f) {
                         val newSize = fontSize.value * zoom
                         fontSize = newSize.coerceIn(8f, 32f).sp
                     }
                }
            }
    ) {

        // Hidden TextField for Soft Keyboard Input
        BasicTextField(
            value = inputState,
            onValueChange = { newValue ->
                val newText = newValue.text
                val oldText = inputState.text

                if (newText.length > oldText.length) {
                    if (newText.startsWith(oldText)) {
                         val diff = newText.substring(oldText.length)
                         session.write(diff)
                    } else {
                         session.write(newText.last().toString())
                    }
                } else if (newText.length < oldText.length) {
                     val count = oldText.length - newText.length
                     repeat(count) { session.write("\u007f") }
                }

                inputState = newValue
            },
            modifier = Modifier
                .focusRequester(focusRequester)
                .alpha(0f)
                .size(1.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.None)
        )

        // Terminal Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .clickable {
                    focusRequester.requestFocus()
                    keyboardController?.show()
                }
        ) {
            // Re-draw when updateTrigger changes
            updateTrigger.let { _ ->
                val style = TextStyle(
                    color = Color(0xFF00FFCC), // Cyberpunk Cyan
                    fontSize = fontSize,
                    fontFamily = FontFamily.Monospace
                )

                val measurement = textMeasurer.measure("A", style)
                val charWidth = measurement.size.width
                val lineHeight = measurement.size.height

                if (charWidth > 0 && lineHeight > 0) {
                    val cols = (size.width / charWidth).toInt()
                    val rows = (size.height / lineHeight).toInt()

                    if (cols != session.initialCols || rows != session.initialRows) {
                        session.resize(rows, cols)
                    }

                    // Render visible rows
                    for (i in 0 until rows) {
                        val line = session.getLine(i)
                        if (line.isNotEmpty()) {
                            drawText(
                                textMeasurer = textMeasurer,
                                text = line,
                                topLeft = Offset(0f, i * lineHeight.toFloat()),
                                style = style
                            )
                        }
                    }
                }
            }
        }

        // Floating AI Button
        FloatingActionButton(
            onClick = { showAiInput = !showAiInput },
            modifier = Modifier
                .align(androidx.compose.ui.alignment.Alignment.BottomEnd)
                .padding(16.dp)
                .padding(bottom = 32.dp),
            containerColor = Color(0xFF00FFCC)
        ) {
            Text("AI", color = Color.Black)
        }

        // AI Overlay
        if (showAiInput) {
            Surface(
                modifier = Modifier
                    .align(androidx.compose.ui.alignment.Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                color = Color(0xFF1E1E1E).copy(alpha = 0.95f),
                shape = MaterialTheme.shapes.medium,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00FFCC))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = aiPrompt,
                        onValueChange = { aiPrompt = it },
                        label = { Text("Ask Cortex...", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(color = Color.White),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00FFCC),
                            unfocusedBorderColor = Color.Gray,
                            cursorColor = Color(0xFF00FFCC),
                            focusedLabelColor = Color(0xFF00FFCC),
                            unfocusedLabelColor = Color.Gray
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            aiViewModel.suggestCommand(aiPrompt)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Translate to Shell", color = Color.Black)
                    }

                    if (aiState is AIUiState.Success) {
                        val cmd = (aiState as AIUiState.Success).command
                        Text("Suggestion: $cmd", color = Color.Green, modifier = Modifier.padding(top = 8.dp))
                        Button(
                            onClick = {
                                session.write(cmd)
                                showAiInput = false
                                aiViewModel.clearState()
                                aiPrompt = ""
                                focusRequester.requestFocus()
                            },
                            modifier = Modifier.fillMaxWidth().padding(top=8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                        ) {
                            Text("Insert Command", color = Color.White)
                        }
                    }

                    if (aiState is AIUiState.Error) {
                         Text("Error: ${(aiState as AIUiState.Error).message}", color = Color.Red, modifier = Modifier.padding(top=8.dp))
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}
