package app.planer.android.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.planer.shared.validation.Validation

/**
 * Placeholder root screen. The color field is checked with a rule from `planer-shared`,
 * which proves the module is wired to the shared contract; real screens replace this later.
 * (The desktop client has its own, separate implementation: the two share no UI code.)
 */
@Composable
fun PlaneRApp() {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme(),
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("PlaneR", style = MaterialTheme.typography.headlineMedium)
                Text("Android client", style = MaterialTheme.typography.bodyMedium)

                var color by remember { mutableStateOf("#7C3AED") }
                val valid = Validation.isValidHexColor(color)

                OutlinedTextField(
                    value = color,
                    onValueChange = { color = it },
                    label = { Text("Calendar color") },
                    isError = !valid,
                    singleLine = true,
                )
                Text(if (valid) "Valid color" else "Use the form #RRGGBB")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PlaneRAppPreview() {
    PlaneRApp()
}
