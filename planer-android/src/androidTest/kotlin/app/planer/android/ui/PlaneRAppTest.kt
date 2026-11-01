package app.planer.android.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test

// Instrumented tests cannot use backtick names with spaces while minSdk < 30,
// so the when/then sentence is written as camelCase clauses (see .docs/testing/Testing.md).
class PlaneRAppTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun whenTheAppStarts_thenTheDefaultColorIsShownAsValid() {
        // given
        rule.setContent { PlaneRApp() }

        // when
        rule.waitForIdle()

        // then
        rule.onNodeWithText("Valid color").assertIsDisplayed()
    }

    @Test
    fun whenAMalformedColorIsTyped_thenTheFormatHintIsShown() {
        // given
        rule.setContent { PlaneRApp() }

        // when
        rule.onNodeWithText("Calendar color").performTextClearance()
        rule.onNodeWithText("Calendar color").performTextInput("purple")

        // then
        rule.onNodeWithText("Use the form #RRGGBB").assertIsDisplayed()
    }
}
