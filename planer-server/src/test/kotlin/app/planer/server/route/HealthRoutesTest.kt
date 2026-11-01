package app.planer.server.route

import app.planer.server.module
import app.planer.shared.dto.HealthResponse
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlin.test.Test

class HealthRoutesTest {

    @Test
    fun `when health is requested, then it responds 200 with a healthy body`() = testApplication {
        // given
        environment { config = MapApplicationConfig() }
        application { module() }

        // when
        val response = client.get("/health")

        // then
        response.status shouldBe HttpStatusCode.OK
        response.contentType()?.withoutParameters() shouldBe ContentType.Application.Json
        Json.decodeFromString<HealthResponse>(response.bodyAsText()) shouldBe
            HealthResponse(HealthResponse.HEALTHY)
    }

    @Test
    fun `when health is requested without credentials, then it is still allowed`() = testApplication {
        // given
        environment { config = MapApplicationConfig() }
        application { module() }

        // when
        val response = client.get("/health")

        // then
        response.status shouldBe HttpStatusCode.OK
    }
}
