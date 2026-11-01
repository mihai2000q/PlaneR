package app.planer.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import app.planer.desktop.ui.PlaneRApp

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "PlaneR") {
        PlaneRApp()
    }
}
