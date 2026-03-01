package io.github.hayatoyagi.prvisualizer.github

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class GitHubDeviceCodeResponse(
    @SerialName("device_code")
    val deviceCode: String = "",
    @SerialName("user_code")
    val userCode: String = "",
    @SerialName("verification_uri")
    val verificationUri: String = "",
    @SerialName("verification_uri_complete")
    val verificationUriComplete: String? = null,
    @SerialName("expires_in")
    val expiresIn: Int = 0,
    @SerialName("interval")
    val interval: Int = 0,
)

@Serializable
internal data class GitHubTokenResponse(
    @SerialName("access_token")
    val accessToken: String = "",
    @SerialName("error")
    val error: String = "",
    @SerialName("error_description")
    val errorDescription: String = "",
)
