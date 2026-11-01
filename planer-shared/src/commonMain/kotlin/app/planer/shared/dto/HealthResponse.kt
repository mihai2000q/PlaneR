package app.planer.shared.dto

import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(val status: String) {

    companion object {
        const val HEALTHY = "healthy"
        const val UNHEALTHY = "unhealthy"
    }
}
