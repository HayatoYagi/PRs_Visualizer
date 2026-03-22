package io.github.hayatoyagi.prvisualizer.github.session

import io.github.hayatoyagi.prvisualizer.AppError
import io.github.hayatoyagi.prvisualizer.FileNode
import io.github.hayatoyagi.prvisualizer.github.GitHubAuthExpiredException
import io.github.hayatoyagi.prvisualizer.github.GitHubOAuthDesktopAuthenticator
import io.github.hayatoyagi.prvisualizer.github.GitHubSnapshot
import io.github.hayatoyagi.prvisualizer.repository.RepoState
import io.github.hayatoyagi.prvisualizer.state.AuthState
import io.github.hayatoyagi.prvisualizer.state.RepoSelectionState
import io.github.hayatoyagi.prvisualizer.state.SnapshotFetchState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GitHubSessionManagerTest {
    @Test
    fun `initializeSession should restore token resolve repo and fetch snapshot`() =
        runTest(UnconfinedTestDispatcher()) {
            var authState: AuthState = AuthState.Unauthenticated
            var snapshotFetchState: SnapshotFetchState = SnapshotFetchState.Idle
            var repoState: RepoState = RepoState.Unselected
            var repoSelectionState: RepoSelectionState = RepoSelectionState.Idle
            var snapshotLoaded = false

            val authService = FakeAuthService(
                restoredToken = "restored-token",
                loginResult = Result.success("unused"),
            )
            val repoService = FakeRepoSelectionService(Result.success(listOf("owner/repo")))
            val snapshotService = FakeSnapshotFetchService(
                result = Result.success(snapshot()),
            )

            val manager = GitHubSessionManager(
                scope = CoroutineScope(coroutineContext),
                getAuthState = { authState },
                setAuthState = { authState = it },
                getSnapshotFetchState = { snapshotFetchState },
                setSnapshotFetchState = { snapshotFetchState = it },
                getRepoState = { repoState },
                getRepoSelectionState = { repoSelectionState },
                setRepoSelectionState = { repoSelectionState = it },
                onSnapshotLoaded = { snapshotLoaded = true },
                selectRepo = { fullName ->
                    repoState = RepoState.Selected(
                        owner = fullName.substringBefore('/'),
                        repo = fullName.substringAfter('/'),
                    )
                },
                unselectRepo = {},
                authService = authService,
                repoSelectionService = repoService,
                snapshotFetchService = snapshotService,
            )

            manager.initializeSession()

            val authenticated = assertIs<AuthState.Authenticated>(authState)
            assertEquals("restored-token", authenticated.oauthToken)
            assertIs<RepoState.Selected>(repoState)
            assertTrue(snapshotLoaded)
            assertNotFetching(snapshotFetchState)
            assertTrue(snapshotService.fetchCalled)
        }

    @Test
    fun `loginAndConnect should set OAuthFailed when login fails`() =
        runTest(UnconfinedTestDispatcher()) {
            var authState: AuthState = AuthState.Unauthenticated
            var snapshotFetchState: SnapshotFetchState = SnapshotFetchState.Idle
            var repoState: RepoState = RepoState.Selected("owner", "repo")
            var repoSelectionState: RepoSelectionState = RepoSelectionState.Idle

            val manager = GitHubSessionManager(
                scope = CoroutineScope(coroutineContext),
                getAuthState = { authState },
                setAuthState = { authState = it },
                getSnapshotFetchState = { snapshotFetchState },
                setSnapshotFetchState = { snapshotFetchState = it },
                getRepoState = { repoState },
                getRepoSelectionState = { repoSelectionState },
                setRepoSelectionState = { repoSelectionState = it },
                onSnapshotLoaded = {},
                selectRepo = {},
                unselectRepo = {},
                authService = FakeAuthService(
                    restoredToken = "",
                    loginResult = Result.failure(IllegalStateException("boom")),
                ),
                repoSelectionService = FakeRepoSelectionService(Result.success(emptyList())),
                snapshotFetchService = FakeSnapshotFetchService(Result.success(snapshot())),
            )

            manager.loginAndConnect()

            val failed = assertIs<AuthState.Failed>(authState)
            val error = assertIs<AppError.OAuthFailed>(failed.error)
            assertFalse(authState is AuthState.Authorizing)
            assertTrue(error.message.contains("boom"))
        }

    @Test
    fun `loginAndConnect should keep manual device flow prompt reachable when browser auto-open fails`() =
        runTest(UnconfinedTestDispatcher()) {
            var authState: AuthState = AuthState.Unauthenticated
            var snapshotFetchState: SnapshotFetchState = SnapshotFetchState.Idle
            var repoState: RepoState = RepoState.Selected("owner", "repo")
            var repoSelectionState: RepoSelectionState = RepoSelectionState.Idle
            val authStates = mutableListOf<AuthState>()

            val manager = GitHubSessionManager(
                scope = CoroutineScope(coroutineContext),
                getAuthState = { authState },
                setAuthState = {
                    authState = it
                    authStates += it
                },
                getSnapshotFetchState = { snapshotFetchState },
                setSnapshotFetchState = { snapshotFetchState = it },
                getRepoState = { repoState },
                getRepoSelectionState = { repoSelectionState },
                setRepoSelectionState = { repoSelectionState = it },
                onSnapshotLoaded = {},
                selectRepo = {},
                unselectRepo = {},
                authService = object : AuthService {
                    override suspend fun restoreToken(fallbackToken: String): String = ""

                    override suspend fun login(
                        clientId: String,
                        onDeviceFlowStart: (GitHubOAuthDesktopAuthenticator.DeviceFlowPrompt) -> Unit,
                    ): Result<String> {
                        onDeviceFlowStart(
                            GitHubOAuthDesktopAuthenticator.DeviceFlowPrompt(
                                userCode = "ABCD-EFGH",
                                verificationUri = "https://github.com/login/device",
                                verificationUriComplete = null,
                            ),
                        )
                        return Result.failure(IllegalStateException("stop after prompt"))
                    }

                    override suspend fun clearToken() = Unit
                },
                repoSelectionService = FakeRepoSelectionService(Result.success(emptyList())),
                snapshotFetchService = FakeSnapshotFetchService(Result.success(snapshot())),
            )

            manager.loginAndConnect()

            val promptState = authStates
                .filterIsInstance<AuthState.Authorizing>()
                .lastOrNull { it.deviceUserCode != null }
            assertNotNull(promptState)
            assertEquals("ABCD-EFGH", promptState.deviceUserCode)
            assertEquals("https://github.com/login/device", promptState.deviceVerificationUrl)
        }

    @Test
    fun `refresh should not fetch when repository remains unselected`() =
        runTest(UnconfinedTestDispatcher()) {
            var authState: AuthState = AuthState.Authenticated("token")
            var snapshotFetchState: SnapshotFetchState = SnapshotFetchState.Idle
            var repoState: RepoState = RepoState.Unselected
            var repoSelectionState: RepoSelectionState = RepoSelectionState.Idle

            val snapshotService = FakeSnapshotFetchService(Result.success(snapshot()))
            val manager = GitHubSessionManager(
                scope = CoroutineScope(coroutineContext),
                getAuthState = { authState },
                setAuthState = { authState = it },
                getSnapshotFetchState = { snapshotFetchState },
                setSnapshotFetchState = { snapshotFetchState = it },
                getRepoState = { repoState },
                getRepoSelectionState = { repoSelectionState },
                setRepoSelectionState = { repoSelectionState = it },
                onSnapshotLoaded = {},
                selectRepo = {},
                unselectRepo = {},
                authService = FakeAuthService(
                    restoredToken = "",
                    loginResult = Result.success("token"),
                ),
                repoSelectionService = FakeRepoSelectionService(Result.success(emptyList())),
                snapshotFetchService = snapshotService,
            )

            manager.refresh()

            assertFalse(snapshotService.fetchCalled)
            assertIs<SnapshotFetchState.Idle>(snapshotFetchState)
        }

    @Test
    fun `cancelAuthorization should reset authorizing state to unauthenticated`() =
        runTest(UnconfinedTestDispatcher()) {
            var authState: AuthState = AuthState.Unauthenticated
            var snapshotFetchState: SnapshotFetchState = SnapshotFetchState.Idle
            var repoState: RepoState = RepoState.Selected("owner", "repo")
            var repoSelectionState: RepoSelectionState = RepoSelectionState.Idle

            val manager = GitHubSessionManager(
                scope = CoroutineScope(coroutineContext),
                getAuthState = { authState },
                setAuthState = { authState = it },
                getSnapshotFetchState = { snapshotFetchState },
                setSnapshotFetchState = { snapshotFetchState = it },
                getRepoState = { repoState },
                getRepoSelectionState = { repoSelectionState },
                setRepoSelectionState = { repoSelectionState = it },
                onSnapshotLoaded = {},
                selectRepo = {},
                unselectRepo = {},
                authService = object : AuthService {
                    override suspend fun restoreToken(fallbackToken: String): String = ""

                    override suspend fun login(
                        clientId: String,
                        onDeviceFlowStart: (GitHubOAuthDesktopAuthenticator.DeviceFlowPrompt) -> Unit,
                    ): Result<String> {
                        onDeviceFlowStart(
                            GitHubOAuthDesktopAuthenticator.DeviceFlowPrompt(
                                userCode = "ABCD-EFGH",
                                verificationUri = "https://github.com/login/device",
                                verificationUriComplete = null,
                            ),
                        )
                        awaitCancellation()
                    }

                    override suspend fun clearToken() = Unit
                },
                repoSelectionService = FakeRepoSelectionService(Result.success(emptyList())),
                snapshotFetchService = FakeSnapshotFetchService(Result.success(snapshot())),
            )

            manager.loginAndConnect()
            assertIs<AuthState.Authorizing>(authState)

            manager.cancelAuthorization()

            assertEquals(AuthState.Unauthenticated, authState)
        }

    @Test
    fun `refresh should clear auth and snapshot state when token is expired`() =
        runTest(UnconfinedTestDispatcher()) {
            var authState: AuthState = AuthState.Authenticated("token")
            var snapshotFetchState: SnapshotFetchState = SnapshotFetchState.Ready(snapshot())
            var repoState: RepoState = RepoState.Selected("owner", "repo")
            var repoSelectionState: RepoSelectionState = RepoSelectionState.Loading

            val authService = FakeAuthService(
                restoredToken = "",
                loginResult = Result.success("token"),
            )
            val manager = GitHubSessionManager(
                scope = CoroutineScope(coroutineContext),
                getAuthState = { authState },
                setAuthState = { authState = it },
                getSnapshotFetchState = { snapshotFetchState },
                setSnapshotFetchState = { snapshotFetchState = it },
                getRepoState = { repoState },
                getRepoSelectionState = { repoSelectionState },
                setRepoSelectionState = { repoSelectionState = it },
                onSnapshotLoaded = {},
                selectRepo = {},
                unselectRepo = {},
                authService = authService,
                repoSelectionService = FakeRepoSelectionService(Result.success(emptyList())),
                snapshotFetchService = FakeSnapshotFetchService(
                    result = Result.failure(GitHubAuthExpiredException("expired")),
                ),
            )

            manager.refresh()

            val authError = assertIs<AuthState.Failed>(authState)
            assertIs<AppError.AuthExpired>(authError.error)
            assertIs<SnapshotFetchState.Idle>(snapshotFetchState)
            assertTrue(authService.clearTokenCalled)
            assertEquals(RepoSelectionState.Idle, repoSelectionState)
        }

    @Test
    fun `logout should clear token and reset all states`() =
        runTest(UnconfinedTestDispatcher()) {
            var authState: AuthState = AuthState.Authenticated("token")
            var snapshotFetchState: SnapshotFetchState = SnapshotFetchState.Ready(snapshot())
            var repoState: RepoState = RepoState.Selected("owner", "repo")
            var repoSelectionState: RepoSelectionState = RepoSelectionState.Ready(listOf("owner/repo"))
            var unselectRepoCalled = false

            val authService = FakeAuthService(
                restoredToken = "",
                loginResult = Result.success("token"),
            )
            val manager = GitHubSessionManager(
                scope = CoroutineScope(coroutineContext),
                getAuthState = { authState },
                setAuthState = { authState = it },
                getSnapshotFetchState = { snapshotFetchState },
                setSnapshotFetchState = { snapshotFetchState = it },
                getRepoState = { repoState },
                getRepoSelectionState = { repoSelectionState },
                setRepoSelectionState = { repoSelectionState = it },
                onSnapshotLoaded = {},
                selectRepo = {},
                unselectRepo = { unselectRepoCalled = true },
                authService = authService,
                repoSelectionService = FakeRepoSelectionService(Result.success(emptyList())),
                snapshotFetchService = FakeSnapshotFetchService(Result.success(snapshot())),
            )

            manager.logout()

            assertEquals(AuthState.Unauthenticated, authState)
            assertIs<SnapshotFetchState.Idle>(snapshotFetchState)
            assertEquals(RepoSelectionState.Idle, repoSelectionState)
            assertTrue(authService.clearTokenCalled)
            assertTrue(unselectRepoCalled)
        }

    private fun assertNotFetching(snapshotFetchState: SnapshotFetchState) {
        assertFalse(snapshotFetchState is SnapshotFetchState.Fetching)
        assertFalse(snapshotFetchState is SnapshotFetchState.Failed)
    }

    private fun snapshot(): GitHubSnapshot = GitHubSnapshot(
        rootNode = FileNode.Directory(path = "", name = "repo", children = emptyList(), weight = 1.0),
        pullRequests = emptyList(),
        viewerLogin = "viewer",
        defaultBranch = "main",
    )

    private class FakeAuthService(
        private val restoredToken: String,
        private val loginResult: Result<String>,
    ) : AuthService {
        var clearTokenCalled: Boolean = false

        override suspend fun restoreToken(fallbackToken: String): String = restoredToken

        override suspend fun login(
            clientId: String,
            onDeviceFlowStart: (GitHubOAuthDesktopAuthenticator.DeviceFlowPrompt) -> Unit,
        ): Result<String> = loginResult

        override suspend fun clearToken() {
            clearTokenCalled = true
        }
    }

    private class FakeRepoSelectionService(
        private val optionsResult: Result<List<String>>,
    ) : RepoSelectionService {
        override suspend fun fetchRepositoryOptions(token: String): Result<List<String>> = optionsResult
    }

    private class FakeSnapshotFetchService(
        private val result: Result<GitHubSnapshot>,
    ) : SnapshotFetchService {
        var fetchCalled: Boolean = false

        override suspend fun fetchSnapshot(
            token: String,
            owner: String,
            repo: String,
        ): Result<GitHubSnapshot> {
            fetchCalled = true
            return result
        }
    }
}
