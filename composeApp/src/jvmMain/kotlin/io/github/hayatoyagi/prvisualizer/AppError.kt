package io.github.hayatoyagi.prvisualizer

sealed class AppError {
    abstract val message: String
    data class AuthExpired(override val message: String = "Session expired. Please login again.") : AppError()
    data class Network(override val message: String) : AppError()
    data class ApiError(val statusCode: Int, override val message: String) : AppError()
    data class OAuthFailed(override val message: String) : AppError()
    data class Unknown(override val message: String) : AppError()
}
