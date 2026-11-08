package app.planer.desktop.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.planer.shared.validation.ValidationRules

@Composable
fun PlaneRApp() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("PlaneR", style = MaterialTheme.typography.headlineMedium)
                Text("Desktop client", style = MaterialTheme.typography.bodyMedium)

                var color by remember { mutableStateOf("#7C3AED") }
                val valid = ValidationRules.isValidHexColor(color)

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
