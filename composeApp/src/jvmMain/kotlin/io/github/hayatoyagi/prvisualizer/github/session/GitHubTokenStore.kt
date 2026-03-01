package io.github.hayatoyagi.prvisualizer.github.session

import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

object GitHubTokenStore {
    private const val SERVICE_NAME = "io.github.hayatoyagi.prvisualizer.github-token"
    private const val ACCOUNT_NAME = "default-user"
    private const val SECURITY_COMMAND = "security"
    private const val WINDOWS_TOKEN_ENV_PATH = "GHPV_PATH"
    private const val WINDOWS_TOKEN_ENV_VALUE = "GHPV_TOKEN"
    private const val WINDOWS_TOKEN_DIR_NAME = "PRsVisualizerForGitHub"
    private const val WINDOWS_TOKEN_FILE_NAME = "oauth_token.dpapi"
    private val COMMAND_TIMEOUT = 5.seconds

    fun loadToken(fallback: String): String {
        val loaded = when {
            isMacOs() -> loadFromMacKeychain()
            isWindows() -> loadFromWindowsDpapi()
            else -> null
        }
        return loaded?.takeIf { it.isNotBlank() } ?: fallback
    }

    fun saveToken(token: String) {
        if (token.isBlank()) return
        when {
            isMacOs() -> saveToMacKeychain(token)
            isWindows() -> saveToWindowsDpapi(token)
            else -> Unit // No secure storage implementation for this OS yet.
        }
    }

    fun clearToken() {
        when {
            isMacOs() -> clearFromMacKeychain()
            isWindows() -> clearFromWindowsDpapi()
            else -> Unit
        }
    }

    private fun loadFromMacKeychain(): String? {
        val result = runCommand(
            SECURITY_COMMAND,
            "find-generic-password",
            "-a",
            ACCOUNT_NAME,
            "-s",
            SERVICE_NAME,
            "-w",
        ) ?: return null
        if (result.exitCode != 0) return null
        return result.stdout.trim()
    }

    private fun saveToMacKeychain(token: String) {
        runCommand(
            SECURITY_COMMAND,
            "add-generic-password",
            "-U",
            "-a",
            ACCOUNT_NAME,
            "-s",
            SERVICE_NAME,
            "-w",
            token,
        )
    }

    private fun clearFromMacKeychain() {
        runCommand(
            SECURITY_COMMAND,
            "delete-generic-password",
            "-a",
            ACCOUNT_NAME,
            "-s",
            SERVICE_NAME,
        )
    }

    private fun loadFromWindowsDpapi(): String? = loadFromWindowsDpapiFile(windowsTokenFilePath())

    private fun loadFromWindowsDpapiFile(path: Path): String? {
        if (!Files.exists(path)) return null
        val script =
            """
                |if (!(Test-Path ${'$'}env:$WINDOWS_TOKEN_ENV_PATH)) { exit 1 }
                |${'$'}enc = Get-Content -Path ${'$'}env:$WINDOWS_TOKEN_ENV_PATH -Raw
                |if ([string]::IsNullOrWhiteSpace(${'$'}enc)) { exit 2 }
                |${'$'}secure = ConvertTo-SecureString ${'$'}enc
                |${'$'}bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR(${'$'}secure)
                |try {
                |  [Runtime.InteropServices.Marshal]::PtrToStringBSTR(${'$'}bstr)
                |} finally {
                |  [Runtime.InteropServices.Marshal]::ZeroFreeBSTR(${'$'}bstr)
                |}
            """.trimMargin()
        val result = runCommand(
            command = arrayOf("powershell", "-NoProfile", "-NonInteractive", "-Command", script),
            extraEnv = mapOf(WINDOWS_TOKEN_ENV_PATH to path.toString()),
        ) ?: return null
        if (result.exitCode != 0) return null
        return result.stdout.trim()
    }

    private fun saveToWindowsDpapi(token: String) {
        val path = windowsTokenFilePath()
        val script =
            """
                |if (!(Test-Path ${'$'}env:$WINDOWS_TOKEN_ENV_PATH)) { exit 1 }
                |${'$'}dir = [IO.Path]::GetDirectoryName(${'$'}env:$WINDOWS_TOKEN_ENV_PATH)
                |if (!(Test-Path ${'$'}dir)) { New-Item -Path ${'$'}dir -ItemType Directory | Out-Null }
                |${'$'}secure = ConvertTo-SecureString -String ${'$'}env:$WINDOWS_TOKEN_ENV_VALUE -AsPlainText -Force
                |${'$'}enc = ConvertFrom-SecureString ${'$'}secure
                |Set-Content -Path ${'$'}env:$WINDOWS_TOKEN_ENV_PATH -Value ${'$'}enc -NoNewline
            """.trimMargin()
        runCommand(
            command = arrayOf("powershell", "-NoProfile", "-NonInteractive", "-Command", script),
            extraEnv = mapOf(
                WINDOWS_TOKEN_ENV_PATH to path.toString(),
                WINDOWS_TOKEN_ENV_VALUE to token,
            ),
        )
    }

    private fun clearFromWindowsDpapi() {
        val path = windowsTokenFilePath()
        runCatching { Files.deleteIfExists(path) }
    }

    private fun windowsTokenFilePath(): Path =
        windowsRoamingAppDataBasePath()
            .resolve(WINDOWS_TOKEN_DIR_NAME)
            .resolve(WINDOWS_TOKEN_FILE_NAME)

    private fun windowsRoamingAppDataBasePath(): Path {
        val appData = System.getenv("APPDATA")
        return if (appData.isNullOrBlank()) {
            Path.of(System.getProperty("user.home"), "AppData", "Roaming")
        } else {
            Path.of(appData)
        }
    }

    private fun runCommand(
        command: Array<String>,
        extraEnv: Map<String, String> = emptyMap(),
    ): CommandResult? {
        return runCatching {
            val builder = ProcessBuilder(command.toList())
            if (extraEnv.isNotEmpty()) {
                builder.environment().putAll(extraEnv)
            }
            val process = builder.start()
            val finished = process.waitFor(COMMAND_TIMEOUT.inWholeMilliseconds, TimeUnit.MILLISECONDS)
            if (!finished) {
                process.destroyForcibly()
                return null
            }
            CommandResult(
                exitCode = process.exitValue(),
                stdout = process.inputStream.bufferedReader().readText(),
            )
        }.getOrNull()
    }

    private fun runCommand(vararg command: String): CommandResult? = runCommand(command = arrayOf(*command))

    private fun isMacOs(): Boolean = System.getProperty("os.name").contains("mac", ignoreCase = true)

    private fun isWindows(): Boolean = System.getProperty("os.name").contains("windows", ignoreCase = true)

    private data class CommandResult(
        val exitCode: Int,
        val stdout: String,
    )
}
