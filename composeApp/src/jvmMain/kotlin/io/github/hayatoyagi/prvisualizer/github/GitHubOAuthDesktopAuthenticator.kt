package io.github.hayatoyagi.prvisualizer.github

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.awt.Desktop
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

class GitHubOAuthDesktopAuthenticator {
    private val client = HttpClient.newHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun authenticate(
        clientId: String,
        scope: String = "repo",
        onDeviceFlowStart: ((DeviceFlowPrompt) -> Unit)? = null,
    ): String = withContext(Dispatchers.IO) {
        require(clientId.isNotBlank()) { "client_id is required" }
        authenticateWithDeviceFlow(clientId = clientId, scope = scope, onDeviceFlowStart = onDeviceFlowStart)
    }

    private suspend fun authenticateWithDeviceFlow(
        clientId: String,
        scope: String,
        onDeviceFlowStart: ((DeviceFlowPrompt) -> Unit)?,
    ): String {
        val start = requestDeviceCode(clientId = clientId, scope = scope)
        val autoVerificationUrl = start.verificationUriComplete
            ?: "${start.verificationUri}?user_code=${enc(start.userCode)}"
        onDeviceFlowStart?.invoke(
            DeviceFlowPrompt(
                userCode = start.userCode,
                verificationUri = start.verificationUri,
                verificationUriComplete = start.verificationUriComplete,
                openedUrl = autoVerificationUrl,
            ),
        )
        openVerificationPage(autoVerificationUrl)

        var pollInterval = start.intervalSeconds.seconds.coerceAtLeast(MIN_POLL_INTERVAL)
        val deadline = TimeSource.Monotonic.markNow() + start.expiresInSeconds.seconds

        while (deadline.hasNotPassedNow()) {
            delay(pollInterval)
            val body = exchangeDeviceCode(clientId = clientId, deviceCode = start.deviceCode)
            val response = json.decodeFromString<GitHubTokenResponse>(body)
            val token = response.accessToken
            if (token.isNotBlank()) return token

            val errorType = response.error
            when (errorType) {
                "authorization_pending" -> {}
                "slow_down" -> {
                    pollInterval += MIN_POLL_INTERVAL
                }
                "expired_token" -> {
                    error("Device code expired. Please click Login with GitHub again.")
                }
                "access_denied" -> {
                    error("Authorization canceled by user.")
                }
                "device_flow_disabled" -> {
                    error("Device Flow is disabled for this GitHub App. Enable 'Device Flow' in app settings.")
                }
                "incorrect_client_credentials" -> {
                    error("Incorrect GitHub client_id. Check GITHUB_CLIENT_ID.")
                }
                "" -> {
                    error("OAuth response missing access_token: $body")
                }
                else -> {
                    val desc = response.errorDescription
                    if (desc.isNotBlank()) error("$errorType: $desc") else error("$errorType: $body")
                }
            }
        }

        error("Timed out waiting for GitHub authorization.")
    }

    private fun requestDeviceCode(
        clientId: String,
        scope: String,
    ): DeviceFlowStart {
        val body = listOf(
            "client_id" to clientId,
            "scope" to scope,
        ).joinToString("&") { (k, v) -> "${enc(k)}=${enc(v)}" }

        val request = HttpRequest
            .newBuilder(URI("https://github.com/login/device/code"))
            .header("Accept", "application/json")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in HttpURLConnection.HTTP_OK until HttpURLConnection.HTTP_MULT_CHOICE) {
            error("Failed to start Device Flow: ${response.statusCode()} ${response.body()}")
        }
        val deviceCodeResponse = json.decodeFromString<GitHubDeviceCodeResponse>(response.body())
        return DeviceFlowStart(
            deviceCode = deviceCodeResponse.deviceCode.ifBlank {
                error("Device Flow response missing device_code: ${response.body()}")
            },
            userCode = deviceCodeResponse.userCode.ifBlank {
                error("Device Flow response missing user_code: ${response.body()}")
            },
            verificationUri = deviceCodeResponse.verificationUri.ifBlank {
                error("Device Flow response missing verification_uri: ${response.body()}")
            },
            verificationUriComplete = deviceCodeResponse.verificationUriComplete?.ifBlank { null },
            expiresInSeconds = if (deviceCodeResponse.expiresIn > 0) {
                deviceCodeResponse.expiresIn
            } else {
                DEFAULT_EXPIRES_IN.inWholeSeconds.toInt()
            },
            intervalSeconds = if (deviceCodeResponse.interval > 0) {
                deviceCodeResponse.interval
            } else {
                MIN_POLL_INTERVAL.inWholeSeconds.toInt()
            },
        )
    }

    private fun exchangeDeviceCode(
        clientId: String,
        deviceCode: String,
    ): String {
        val body = listOf(
            "client_id" to clientId,
            "device_code" to deviceCode,
            "grant_type" to "urn:ietf:params:oauth:grant-type:device_code",
        ).joinToString("&") { (k, v) -> "${enc(k)}=${enc(v)}" }

        val request = HttpRequest
            .newBuilder(URI("https://github.com/login/oauth/access_token"))
            .header("Accept", "application/json")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in HttpURLConnection.HTTP_OK until HttpURLConnection.HTTP_MULT_CHOICE) {
            error("OAuth device token poll failed: ${response.statusCode()} ${response.body()}")
        }
        return response.body()
    }

    private fun enc(raw: String): String = URLEncoder.encode(raw, StandardCharsets.UTF_8)

    private fun openVerificationPage(url: String) {
        if (!Desktop.isDesktopSupported()) {
            error("Desktop browser is not supported in this environment. Open this URL manually: $url")
        }
        Desktop.getDesktop().browse(URI(url))
    }

    data class DeviceFlowPrompt(
        val userCode: String,
        val verificationUri: String,
        val verificationUriComplete: String?,
        val openedUrl: String,
    )

    private data class DeviceFlowStart(
        val deviceCode: String,
        val userCode: String,
        val verificationUri: String,
        val verificationUriComplete: String?,
        val expiresInSeconds: Int,
        val intervalSeconds: Int,
    )

    private companion object {
        private val MIN_POLL_INTERVAL = 5.seconds
        private val DEFAULT_EXPIRES_IN = 15.minutes
    }
}
