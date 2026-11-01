package app.planer.shared.dto

import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlin.test.Test

class HealthResponseTest {

    @Test
    fun `when serialized, then it matches the documented body`() {
        // given
        val response = HealthResponse(HealthResponse.HEALTHY)

        // when
        val json = Json.encodeToString(HealthResponse.serializer(), response)

        // then
        json shouldBe """{"status":"healthy"}"""
    }

    @Test
    fun `when deserialized, then it round-trips`() {
        // given
        val json = """{"status":"unhealthy"}"""

        // when
        val response = Json.decodeFromString(HealthResponse.serializer(), json)

        // then
        response shouldBe HealthResponse(HealthResponse.UNHEALTHY)
    }
}
