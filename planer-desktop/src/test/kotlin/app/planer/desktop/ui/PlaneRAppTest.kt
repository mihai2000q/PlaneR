package app.planer.desktop.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class PlaneRAppTest {

    @Test
    fun `when the app starts, then the default color is shown as valid`() = runComposeUiTest {
        // given
        setContent { PlaneRApp() }

        // when
        waitForIdle()

        // then
        onNodeWithText("Valid color").assertIsDisplayed()
    }

    @Test
    fun `when a malformed color is typed, then the format hint is shown`() = runComposeUiTest {
        // given
        setContent { PlaneRApp() }

        // when
        onNodeWithText("Calendar color").performTextClearance()
        onNodeWithText("Calendar color").performTextInput("purple")

        // then
        onNodeWithText("Use the form #RRGGBB").assertIsDisplayed()
    }
}
