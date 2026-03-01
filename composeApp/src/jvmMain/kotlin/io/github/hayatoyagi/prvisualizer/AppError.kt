package io.github.hayatoyagi.prvisualizer

sealed class AppError {
    abstract val message: String

    companion object {
        /**
         * Creates an AppError from a Throwable.
         *
         * @param error The throwable to convert
         * @return An appropriate AppError subclass
         */
        fun from(error: Throwable): AppError = when (error) {
            is java.net.ConnectException, is java.net.UnknownHostException ->
                Network(error.message ?: "Network error")
            is io.github.hayatoyagi.prvisualizer.github.GitHubApiException ->
                ApiError(error.statusCode, error.message ?: "API error")
            else -> Unknown(error.message ?: "Unknown error")
        }
    }

    data class AuthExpired(
        override val message: String = "Session expired. Please login again.",
    ) : AppError()

    data class Network(
        override val message: String,
    ) : AppError()

    data class ApiError(
        val statusCode: Int,
        override val message: String,
    ) : AppError()

    data class OAuthFailed(
        override val message: String,
    ) : AppError()

    data class Unknown(
        override val message: String,
    ) : AppError()
}
