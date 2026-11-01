package app.planer.shared.validation

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class ValidationTest {

    @Test
    fun `when color is a six-digit hex, then it is valid`() {
        // given
        val color = "#7C3AED"

        // when
        val result = Validation.isValidHexColor(color)

        // then
        result shouldBe true
    }

    @Test
    fun `when color is malformed, then it is invalid`() {
        // given
        val colors = listOf("7C3AED", "#7C3AE", "#7C3AEDD", "#GGGGGG", "")

        // when
        val results = colors.map(Validation::isValidHexColor)

        // then
        results shouldBe List(colors.size) { false }
    }

    @Test
    fun `when title is blank, then it is invalid`() {
        // given
        val title = "   "

        // when
        val result = Validation.isValidTitle(title)

        // then
        result shouldBe false
    }

    @Test
    fun `when title is at the maximum length, then it is valid`() {
        // given
        val title = "a".repeat(Validation.TITLE_MAX_LENGTH)

        // when
        val result = Validation.isValidTitle(title)

        // then
        result shouldBe true
    }

    @Test
    fun `when title is longer than the maximum, then it is invalid`() {
        // given
        val title = "a".repeat(Validation.TITLE_MAX_LENGTH + 1)

        // when
        val result = Validation.isValidTitle(title)

        // then
        result shouldBe false
    }
}
