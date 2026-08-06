/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.data.user.UserDataRepository
import kpt.core.model.user.DarkThemeConfig
import kpt.core.model.user.LanguageConfig
import kpt.core.model.user.ThemeBrand
import kpt.core.model.user.UserData
import org.mifos.groupbanking.core.datastore.session.CompanionSessionStore
import org.mifos.groupbanking.core.model.AuthSession
import org.mifos.groupbanking.core.model.LoginCredentials
import org.mifos.groupbanking.core.model.SelfRegistration
import org.mifos.groupbanking.core.network.model.AuthResponseDto
import org.mifos.groupbanking.core.network.model.LoginRequestDto
import org.mifos.groupbanking.core.network.model.SelfRegisterRequestDto
import org.mifos.groupbanking.core.network.model.UserProfileDto
import org.mifos.groupbanking.core.network.service.loginsignup.CompanionAuthApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private class FakeCompanionAuthApi(
    private val selfRegisterResult: NetworkResult<AuthResponseDto, NetworkError>? = null,
    private val loginResult: NetworkResult<AuthResponseDto, NetworkError>? = null,
    private val meResult: NetworkResult<UserProfileDto, NetworkError>? = null,
) : CompanionAuthApi {
    var lastSelfRegisterRequest: SelfRegisterRequestDto? = null
    var lastLoginRequest: LoginRequestDto? = null
    var lastMeToken: String? = null

    override suspend fun selfRegister(request: SelfRegisterRequestDto): NetworkResult<AuthResponseDto, NetworkError> {
        lastSelfRegisterRequest = request
        return selfRegisterResult ?: error("selfRegisterResult not stubbed")
    }

    override suspend fun login(request: LoginRequestDto): NetworkResult<AuthResponseDto, NetworkError> {
        lastLoginRequest = request
        return loginResult ?: error("loginResult not stubbed")
    }

    override suspend fun me(sessionToken: String): NetworkResult<UserProfileDto, NetworkError> {
        lastMeToken = sessionToken
        return meResult ?: error("meResult not stubbed")
    }
}

private class FakeCompanionSessionStore : CompanionSessionStore {
    private val _session = MutableStateFlow<AuthSession?>(null)
    override val session: Flow<AuthSession?> = _session

    var saveCallCount = 0
    var clearCallCount = 0
    var savedToken: String? = null

    override suspend fun save(userId: String, sessionToken: String, tokenExpiresAt: Instant) {
        saveCallCount++
        savedToken = sessionToken
        _session.value = AuthSession(userId, sessionToken, tokenExpiresAt, emptyList())
    }

    override suspend fun clear() {
        clearCallCount++
        _session.value = null
    }
}

private class FakeLocalCacheCleaner : LocalCacheCleaner {
    var clearAllCallCount = 0
    override suspend fun clearAll() {
        clearAllCallCount++
    }
}

private class FakeUserDataRepository : UserDataRepository {
    private val _userData = MutableStateFlow(UserData.DEFAULT.copy(isAuthenticated = false, isUnlocked = false))
    override val userData: StateFlow<UserData> = _userData
    override val authToken: String? = null
    override val passcode: String get() = _userData.value.passcode
    override val observeLanguage: Flow<LanguageConfig> = flowOf(UserData.DEFAULT.appLanguage)
    override val observeDarkThemeConfig: Flow<DarkThemeConfig> = flowOf(UserData.DEFAULT.darkThemeConfig)
    override val observeDynamicColorPreference: Flow<Boolean> = flowOf(false)
    override val observeScreenCapturePreference: Flow<Boolean> = flowOf(false)

    override suspend fun setLanguage(language: LanguageConfig) {}
    override suspend fun setThemeBrand(themeBrand: ThemeBrand) {}
    override suspend fun setDarkThemeConfig(darkThemeConfig: DarkThemeConfig) {}
    override suspend fun setDynamicColorPreference(useDynamicColor: Boolean) {}
    override suspend fun setIsAuthenticated(isAuthenticated: Boolean) {
        _userData.value = _userData.value.copy(isAuthenticated = isAuthenticated)
    }
    override suspend fun setIsUnlocked(isUnlocked: Boolean) {
        _userData.value = _userData.value.copy(isUnlocked = isUnlocked)
    }
    override suspend fun setIsPasscodeEnabled(isPasscodeEnabled: Boolean) {}
    override suspend fun setIsBiometricsEnabled(isBiometricsEnabled: Boolean) {}
    override suspend fun setShowOnboarding(showOnboarding: Boolean) {}
    override suspend fun setFirstTimeState(firstTimeState: Boolean) {}
    override suspend fun setPasscode(passcode: String) {}
    override suspend fun clearUserData() {}
}

/**
 * TDD RED-first coverage for [AuthRepository] / [AuthRepositoryImpl]. No try-catch anywhere in
 * the repository under test (Mandatory Rule 4) — every branch below is a plain `when` over the
 * fake service's [NetworkResult].
 */
class AuthRepositoryTest {

    private val successDto = AuthResponseDto(
        userId = "u-1",
        sessionToken = "tok-abc",
        tokenExpiresAt = "2026-08-01T00:00:00Z",
        groupMemberships = emptyList(),
    )

    // ---------- selfRegister ----------

    @Test
    fun selfRegister_success_persistsSessionAndReturnsMappedDomain() = runTest {
        val api = FakeCompanionAuthApi(selfRegisterResult = NetworkResult.Success(successDto))
        val sessionStore = FakeCompanionSessionStore()
        val repo = AuthRepositoryImpl(api, sessionStore, FakeLocalCacheCleaner(), FakeUserDataRepository())

        val result = repo.selfRegister(SelfRegistration("Amina", "amina@example.com", "hunter22"))

        check(result is NetworkResult.Success)
        assertEquals("u-1", result.data.userId)
        assertEquals(1, sessionStore.saveCallCount)
        assertEquals("tok-abc", sessionStore.savedToken)
        assertEquals(SelfRegisterRequestDto("Amina", "amina@example.com", "hunter22"), api.lastSelfRegisterRequest)
    }

    @Test
    fun selfRegister_conflictError_doesNotPersistSessionAndReturnsError() = runTest {
        val api = FakeCompanionAuthApi(selfRegisterResult = NetworkResult.Error(NetworkError.UNKNOWN))
        val sessionStore = FakeCompanionSessionStore()
        val repo = AuthRepositoryImpl(api, sessionStore, FakeLocalCacheCleaner(), FakeUserDataRepository())

        val result = repo.selfRegister(SelfRegistration("Amina", "amina@example.com", "hunter22"))

        assertEquals(NetworkResult.Error(NetworkError.UNKNOWN), result)
        assertEquals(0, sessionStore.saveCallCount)
    }

    @Test
    fun selfRegister_validationError400_doesNotPersistSession() = runTest {
        val api = FakeCompanionAuthApi(selfRegisterResult = NetworkResult.Error(NetworkError.BAD_REQUEST))
        val sessionStore = FakeCompanionSessionStore()
        val repo = AuthRepositoryImpl(api, sessionStore, FakeLocalCacheCleaner(), FakeUserDataRepository())

        val result = repo.selfRegister(SelfRegistration("", "", ""))

        assertEquals(NetworkResult.Error(NetworkError.BAD_REQUEST), result)
        assertEquals(0, sessionStore.saveCallCount)
    }

    // ---------- login ----------

    @Test
    fun login_success_persistsSessionAndReturnsMappedDomain() = runTest {
        val api = FakeCompanionAuthApi(loginResult = NetworkResult.Success(successDto))
        val sessionStore = FakeCompanionSessionStore()
        val repo = AuthRepositoryImpl(api, sessionStore, FakeLocalCacheCleaner(), FakeUserDataRepository())

        val result = repo.login(LoginCredentials("amina@example.com", "hunter22"))

        check(result is NetworkResult.Success)
        assertEquals("tok-abc", result.data.sessionToken)
        assertEquals(1, sessionStore.saveCallCount)
        assertEquals(LoginRequestDto("amina@example.com", "hunter22"), api.lastLoginRequest)
    }

    @Test
    fun login_unauthorized_doesNotPersistSessionAndReturnsError() = runTest {
        val api = FakeCompanionAuthApi(loginResult = NetworkResult.Error(NetworkError.UNAUTHORIZED))
        val sessionStore = FakeCompanionSessionStore()
        val repo = AuthRepositoryImpl(api, sessionStore, FakeLocalCacheCleaner(), FakeUserDataRepository())

        val result = repo.login(LoginCredentials("amina@example.com", "wrong"))

        assertEquals(NetworkResult.Error(NetworkError.UNAUTHORIZED), result)
        assertEquals(0, sessionStore.saveCallCount)
    }

    @Test
    fun login_rateLimited_doesNotPersistSession() = runTest {
        val api = FakeCompanionAuthApi(loginResult = NetworkResult.Error(NetworkError.TOO_MANY_REQUESTS))
        val sessionStore = FakeCompanionSessionStore()
        val repo = AuthRepositoryImpl(api, sessionStore, FakeLocalCacheCleaner(), FakeUserDataRepository())

        val result = repo.login(LoginCredentials("amina@example.com", "hunter22"))

        assertEquals(NetworkResult.Error(NetworkError.TOO_MANY_REQUESTS), result)
        assertEquals(0, sessionStore.saveCallCount)
    }

    // ---------- refreshSession (companion_me) ----------

    @Test
    fun refreshSession_success_returnsMappedProfileAndSendsToken() = runTest {
        val profileDto = UserProfileDto("u-1", "Amina", "amina@example.com", emptyList())
        val api = FakeCompanionAuthApi(meResult = NetworkResult.Success(profileDto))
        val sessionStore = FakeCompanionSessionStore()
        val repo = AuthRepositoryImpl(api, sessionStore, FakeLocalCacheCleaner(), FakeUserDataRepository())

        val result = repo.refreshSession("tok-abc")

        check(result is NetworkResult.Success)
        assertEquals("Amina", result.data.name)
        assertEquals("tok-abc", api.lastMeToken)
    }

    @Test
    fun refreshSession_expiredToken401_returnsError() = runTest {
        val api = FakeCompanionAuthApi(meResult = NetworkResult.Error(NetworkError.UNAUTHORIZED))
        val sessionStore = FakeCompanionSessionStore()
        val repo = AuthRepositoryImpl(api, sessionStore, FakeLocalCacheCleaner(), FakeUserDataRepository())

        val result = repo.refreshSession("expired-token")

        assertEquals(NetworkResult.Error(NetworkError.UNAUTHORIZED), result)
    }

    @Test
    fun refreshSession_serverError_returnsError() = runTest {
        val api = FakeCompanionAuthApi(meResult = NetworkResult.Error(NetworkError.SERVER))
        val sessionStore = FakeCompanionSessionStore()
        val repo = AuthRepositoryImpl(api, sessionStore, FakeLocalCacheCleaner(), FakeUserDataRepository())

        val result = repo.refreshSession("tok")

        assertEquals(NetworkResult.Error(NetworkError.SERVER), result)
    }

    // ---------- currentSession / clearSession ----------

    @Test
    fun currentSession_startsNullWhenNoSessionPersisted() = runTest {
        val repo = AuthRepositoryImpl(FakeCompanionAuthApi(), FakeCompanionSessionStore(), FakeLocalCacheCleaner(), FakeUserDataRepository())

        assertNull(repo.currentSession.first())
    }

    @Test
    fun currentSession_emitsSavedSessionAfterSuccessfulLogin() = runTest {
        val api = FakeCompanionAuthApi(loginResult = NetworkResult.Success(successDto))
        val sessionStore = FakeCompanionSessionStore()
        val repo = AuthRepositoryImpl(api, sessionStore, FakeLocalCacheCleaner(), FakeUserDataRepository())

        repo.login(LoginCredentials("amina@example.com", "hunter22"))

        assertEquals("u-1", repo.currentSession.first()?.userId)
    }

    @Test
    fun clearSession_delegatesToSessionStoreAndClearsCurrentSession() = runTest {
        val api = FakeCompanionAuthApi(loginResult = NetworkResult.Success(successDto))
        val sessionStore = FakeCompanionSessionStore()
        val repo = AuthRepositoryImpl(api, sessionStore, FakeLocalCacheCleaner(), FakeUserDataRepository())
        repo.login(LoginCredentials("amina@example.com", "hunter22"))

        repo.clearSession()

        assertEquals(1, sessionStore.clearCallCount)
        assertNull(repo.currentSession.first())
    }

    @Test
    fun clearSession_wipesAllLocalCaches_soTheNextUserSeesNoStaleData() = runTest {
        val api = FakeCompanionAuthApi(loginResult = NetworkResult.Success(successDto))
        val sessionStore = FakeCompanionSessionStore()
        val cacheCleaner = FakeLocalCacheCleaner()
        val repo = AuthRepositoryImpl(api, sessionStore, cacheCleaner, FakeUserDataRepository())
        repo.login(LoginCredentials("amina@example.com", "hunter22"))

        repo.clearSession()

        // Logout MUST wipe the user-scoped Room caches (dashboards/loans/savings are keyed by
        // domain id, not user) so the next sign-in on this device never reads the prior user's rows.
        assertEquals(1, cacheCleaner.clearAllCallCount)
    }

    // ---------- auth-status persistence (RootNavViewModel resolves startup from these flags) ----------

    @Test
    fun login_success_marksUserAuthenticatedAndUnlocked_soNextAppOpenResolvesToDashboard() = runTest {
        val api = FakeCompanionAuthApi(loginResult = NetworkResult.Success(successDto))
        val userData = FakeUserDataRepository()
        val repo = AuthRepositoryImpl(api, FakeCompanionSessionStore(), FakeLocalCacheCleaner(), userData)

        repo.login(LoginCredentials("amina@example.com", "hunter22"))

        assertEquals(true, userData.userData.first().isAuthenticated)
        assertEquals(true, userData.userData.first().isUnlocked)
    }

    @Test
    fun clearSession_clearsAuthenticatedFlag_soNextAppOpenResolvesToLogin() = runTest {
        val api = FakeCompanionAuthApi(loginResult = NetworkResult.Success(successDto))
        val userData = FakeUserDataRepository()
        val repo = AuthRepositoryImpl(api, FakeCompanionSessionStore(), FakeLocalCacheCleaner(), userData)
        repo.login(LoginCredentials("amina@example.com", "hunter22"))

        repo.clearSession()

        assertEquals(false, userData.userData.first().isAuthenticated)
    }
}
