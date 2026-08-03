/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.settings

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kpt.core.analytics.KptAnalyticsTracker
import kpt.core.base.analytics.NoOpAnalyticsHelper
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.base.observability.ConsoleCrashReporter
import kpt.core.base.security.BiometricAuthenticator
import kpt.core.datastore.UserPreferencesRepository
import kpt.core.model.user.DarkThemeConfig
import kpt.core.model.user.LanguageConfig
import kpt.core.model.user.ThemeBrand
import kpt.core.model.user.UserData
import org.mifos.groupbanking.core.data.repository.ChangePinRepository
import org.mifos.groupbanking.core.model.ChangePinRequest
import org.mifos.groupbanking.core.model.ChangePinResult
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * See API.md#viewmodel — `SettingsViewModelTest` exercises the reactive 4-flow `combine()` fan-in
 * (language/theme/biometric-enabled/notifications-enabled), the one-shot
 * `appVersion`/`buildNumber`/`isBiometricAvailable` reads, every declared [SettingsAction]
 * (language/theme/biometric/notifications/change-PIN dialog lifecycle/logout), and the
 * [shouldPersistBiometricToggle] guard predicate, per RULE-TDD-METHODOLOGY-001 /
 * RULE-IMPL-DEAD-CLICKABLE-001.
 *
 * **`BiometricAuthenticator` "available" branch — documented, honest test-coverage gap:** every
 * platform actual of `BiometricAuthenticator` (`core-base/security`, NEVER edited here per
 * Mandatory Rule 6) currently returns `isAvailable() = false` unconditionally, so this suite cannot
 * construct a real `SettingsViewModel` whose `state.isBiometricAvailable` is `true`. The
 * `available -> persists` direction of the toggle guard is instead covered directly on the
 * extracted pure [shouldPersistBiometricToggle] predicate — see [SettingsViewModel]'s
 * `shouldPersistBiometricToggle` KDoc for the full rationale.
 */
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var userPreferencesRepository: FakeSettingsUserPreferencesRepository
    private lateinit var changePinRepository: FakeSettingsChangePinRepository
    private lateinit var biometricAuthenticator: BiometricAuthenticator
    private lateinit var appVersionInfo: AppVersionInfo

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        userPreferencesRepository = FakeSettingsUserPreferencesRepository()
        changePinRepository = FakeSettingsChangePinRepository()
        // Real (non-fakeable) class — see class KDoc "documented, honest test-coverage gap".
        biometricAuthenticator = BiometricAuthenticator()
        appVersionInfo = AppVersionInfo(appVersion = "2.3.1", buildNumber = "17")
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(): SettingsViewModel = SettingsViewModel(
        userPreferencesRepository = userPreferencesRepository,
        biometricAuthenticator = biometricAuthenticator,
        changePinRepository = changePinRepository,
        appVersionInfo = appVersionInfo,
        analytics = KptAnalyticsTracker(NoOpAnalyticsHelper()),
        crashReporter = ConsoleCrashReporter(),
    )

    // -- Initial load / reactive combine() fan-in -----------------------------------------------

    @Test
    fun `initial state loads language theme biometric and notifications prefs plus version and biometric availability`() =
        runTest(testDispatcher) {
            userPreferencesRepository.seed(
                UserData.DEFAULT.copy(
                    appLanguage = LanguageConfig.FRENCH,
                    darkThemeConfig = DarkThemeConfig.DARK,
                    isBiometricsEnabled = true,
                    enableNotifications = false,
                ),
            )
            val viewModel = buildViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            val state = viewModel.stateFlow.value
            assertEquals(LanguageConfig.FRENCH, state.selectedLanguage)
            assertEquals(DarkThemeConfig.DARK, state.selectedTheme)
            assertTrue(state.isBiometricEnabled)
            assertFalse(state.isNotificationsEnabled)
            assertEquals("2.3.1", state.appVersion)
            assertEquals("17", state.buildNumber)
            // Real BiometricAuthenticator actual — see class KDoc gap note.
            assertFalse(state.isBiometricAvailable)
            assertEquals(SettingsScreenState.Content, state.deriveScreenState())
        }

    // -- OnLanguageSelected / OnThemeSelected (persist_db) ---------------------------------------

    @Test
    fun `OnLanguageSelected persists via setLanguage and state reflects the reactive re-emission`() =
        runTest(testDispatcher) {
            val viewModel = buildViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.trySendAction(SettingsAction.OnLanguageSelected(LanguageConfig.HINDI))
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(1, userPreferencesRepository.setLanguageCallCount)
            assertEquals(LanguageConfig.HINDI, userPreferencesRepository.lastLanguage)
            assertEquals(LanguageConfig.HINDI, viewModel.stateFlow.value.selectedLanguage)
        }

    @Test
    fun `OnThemeSelected persists via setDarkThemeConfig and state reflects the reactive re-emission`() =
        runTest(testDispatcher) {
            val viewModel = buildViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.trySendAction(SettingsAction.OnThemeSelected(DarkThemeConfig.LIGHT))
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(1, userPreferencesRepository.setDarkThemeConfigCallCount)
            assertEquals(DarkThemeConfig.LIGHT, userPreferencesRepository.lastDarkThemeConfig)
            assertEquals(DarkThemeConfig.LIGHT, viewModel.stateFlow.value.selectedTheme)
        }

    // -- OnBiometricToggled guard -----------------------------------------------------------------

    @Test
    fun `OnBiometricToggled while hardware unavailable is ignored and never calls setIsBiometricsEnabled`() =
        runTest(testDispatcher) {
            val viewModel = buildViewModel()
            testDispatcher.scheduler.advanceUntilIdle()
            assertFalse(viewModel.stateFlow.value.isBiometricAvailable)

            viewModel.trySendAction(SettingsAction.OnBiometricToggled(enabled = true))
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(0, userPreferencesRepository.setIsBiometricsEnabledCallCount)
        }

    @Test
    fun `shouldPersistBiometricToggle guard predicate is true only when hardware is available`() {
        assertTrue(shouldPersistBiometricToggle(isBiometricAvailable = true))
        assertFalse(shouldPersistBiometricToggle(isBiometricAvailable = false))
    }

    // -- OnNotificationsToggled (persist_db) --------------------------------------------------------

    @Test
    fun `OnNotificationsToggled persists via setNotificationsEnabled and state reflects the reactive re-emission`() =
        runTest(testDispatcher) {
            val viewModel = buildViewModel()
            testDispatcher.scheduler.advanceUntilIdle()
            assertTrue(viewModel.stateFlow.value.isNotificationsEnabled)

            viewModel.trySendAction(SettingsAction.OnNotificationsToggled(enabled = false))
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(1, userPreferencesRepository.setNotificationsEnabledCallCount)
            assertEquals(false, userPreferencesRepository.lastNotificationsEnabled)
            assertFalse(viewModel.stateFlow.value.isNotificationsEnabled)
        }

    // -- Change-PIN dialog lifecycle -----------------------------------------------------------------

    @Test
    fun `OnChangePinTapped opens the dialog and derives ChangingPin screen state`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(SettingsAction.OnChangePinTapped)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertTrue(state.isChangingPin)
        assertEquals(SettingsScreenState.ChangingPin, state.deriveScreenState())
    }

    @Test
    fun `OnDismissPinDialog closes the dialog and clears any pending error`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.trySendAction(SettingsAction.OnChangePinTapped)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(SettingsAction.OnDismissPinDialog)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertFalse(state.isChangingPin)
        assertNull(state.pinChangeError)
        assertEquals(SettingsScreenState.Content, state.deriveScreenState())
    }

    @Test
    fun `OnSubmitPinChange with a newPin shorter than 4 digits is rejected client-side without a network call`() =
        runTest(testDispatcher) {
            val viewModel = buildViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.trySendAction(SettingsAction.OnSubmitPinChange(currentPin = "1234", newPin = "12"))
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(0, changePinRepository.callCount)
            assertEquals("error_pin_change", viewModel.stateFlow.value.pinChangeError)
        }

    @Test
    fun `OnSubmitPinChange success closes the dialog and sets pinChangeSuccess`() = runTest(testDispatcher) {
        changePinRepository.result = NetworkResult.Success(ChangePinResult(resourceId = 42L))
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(SettingsAction.OnSubmitPinChange(currentPin = "1234", newPin = "5678"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, changePinRepository.callCount)
        assertEquals(ChangePinRequest(currentPin = "1234", newPin = "5678"), changePinRepository.lastRequest)
        val state = viewModel.stateFlow.value
        assertFalse(state.isChangingPin)
        assertTrue(state.pinChangeSuccess)
        assertNull(state.pinChangeError)
    }

    @Test
    fun `OnSubmitPinChange with a 400 sets pinChangeError and keeps the dialog open`() = runTest(testDispatcher) {
        changePinRepository.result = NetworkResult.Error(NetworkError.BAD_REQUEST)
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.trySendAction(SettingsAction.OnChangePinTapped)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(SettingsAction.OnSubmitPinChange(currentPin = "0000", newPin = "5678"))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertTrue(state.isChangingPin)
        assertEquals("error_pin_change", state.pinChangeError)
        assertFalse(state.pinChangeSuccess)
    }

    @Test
    fun `OnSubmitPinChange with a 401 closes the dialog and emits NavigateToLogin`() = runTest(testDispatcher) {
        changePinRepository.result = NetworkResult.Error(NetworkError.UNAUTHORIZED)
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.trySendAction(SettingsAction.OnChangePinTapped)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.eventFlow.test {
            viewModel.trySendAction(SettingsAction.OnSubmitPinChange(currentPin = "0000", newPin = "5678"))
            assertEquals(SettingsEvent.NavigateToLogin, awaitItem())
        }
        val state = viewModel.stateFlow.value
        assertFalse(state.isChangingPin)
        assertNull(state.pinChangeError)
    }

    // -- OnLogoutTapped (emit_event — settings-logout-dialog owns the confirm) ------------------------

    @Test
    fun `OnLogoutTapped emits ShowLogoutDialog`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.eventFlow.test {
            viewModel.trySendAction(SettingsAction.OnLogoutTapped)
            assertEquals(SettingsEvent.ShowLogoutDialog, awaitItem())
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Test fixtures — uniquely named to avoid K2 same-package Fake collisions with sibling features.
// ---------------------------------------------------------------------------------------------

private class FakeSettingsUserPreferencesRepository(
    initial: UserData = UserData.DEFAULT,
) : UserPreferencesRepository {

    private val _userData = MutableStateFlow(initial)

    override val userData: StateFlow<UserData> = _userData.asStateFlow()
    override val authToken: String? = null
    override val passcode: String get() = _userData.value.passcode
    override val observeLanguage: Flow<LanguageConfig> = _userData.map { it.appLanguage }
    override val observeDarkThemeConfig: Flow<DarkThemeConfig> = _userData.map { it.darkThemeConfig }
    override val observeDynamicColorPreference: Flow<Boolean> = _userData.map { it.useDynamicColor }
    override val observeScreenCapturePreference: Flow<Boolean> = _userData.map { it.enableScreenCapture }
    override val observeNotificationsEnabled: Flow<Boolean> = _userData.map { it.enableNotifications }

    var setLanguageCallCount = 0
        private set
    var lastLanguage: LanguageConfig? = null
        private set
    var setDarkThemeConfigCallCount = 0
        private set
    var lastDarkThemeConfig: DarkThemeConfig? = null
        private set
    var setIsBiometricsEnabledCallCount = 0
        private set
    var lastIsBiometricsEnabled: Boolean? = null
        private set
    var setNotificationsEnabledCallCount = 0
        private set
    var lastNotificationsEnabled: Boolean? = null
        private set

    fun seed(userData: UserData) {
        _userData.value = userData
    }

    override suspend fun setLanguage(language: LanguageConfig) {
        setLanguageCallCount++
        lastLanguage = language
        _userData.value = _userData.value.copy(appLanguage = language)
    }

    override suspend fun setThemeBrand(themeBrand: ThemeBrand) {
        _userData.value = _userData.value.copy(themeBrand = themeBrand)
    }

    override suspend fun setDarkThemeConfig(darkThemeConfig: DarkThemeConfig) {
        setDarkThemeConfigCallCount++
        lastDarkThemeConfig = darkThemeConfig
        _userData.value = _userData.value.copy(darkThemeConfig = darkThemeConfig)
    }

    override suspend fun setDynamicColorPreference(useDynamicColor: Boolean) {
        _userData.value = _userData.value.copy(useDynamicColor = useDynamicColor)
    }

    override suspend fun setIsAuthenticated(isAuthenticated: Boolean) {
        _userData.value = _userData.value.copy(isAuthenticated = isAuthenticated)
    }

    override suspend fun setIsUnlocked(isUnlocked: Boolean) {
        _userData.value = _userData.value.copy(isUnlocked = isUnlocked)
    }

    override suspend fun setIsPasscodeEnabled(isPasscodeEnabled: Boolean) {
        _userData.value = _userData.value.copy(isPasscodeEnabled = isPasscodeEnabled)
    }

    override suspend fun setIsBiometricsEnabled(isBiometricsEnabled: Boolean) {
        setIsBiometricsEnabledCallCount++
        lastIsBiometricsEnabled = isBiometricsEnabled
        _userData.value = _userData.value.copy(isBiometricsEnabled = isBiometricsEnabled)
    }

    override suspend fun setShowOnboarding(showOnboarding: Boolean) {
        _userData.value = _userData.value.copy(showOnboarding = showOnboarding)
    }

    override suspend fun setFirstTimeState(firstTimeState: Boolean) {
        _userData.value = _userData.value.copy(firstTimeUser = firstTimeState)
    }

    override suspend fun setPasscode(passcode: String) {
        _userData.value = _userData.value.copy(passcode = passcode)
    }

    override suspend fun setScreenCapturePreference(isScreenCaptureEnabled: Boolean) {
        _userData.value = _userData.value.copy(enableScreenCapture = isScreenCaptureEnabled)
    }

    override suspend fun setNotificationsEnabled(isEnabled: Boolean) {
        setNotificationsEnabledCallCount++
        lastNotificationsEnabled = isEnabled
        _userData.value = _userData.value.copy(enableNotifications = isEnabled)
    }

    override suspend fun clearUserData() {
        _userData.value = UserData.DEFAULT
    }
}

private class FakeSettingsChangePinRepository : ChangePinRepository {
    var result: NetworkResult<ChangePinResult, NetworkError> = NetworkResult.Success(ChangePinResult(resourceId = 1L))
    var callCount: Int = 0
        private set
    var lastRequest: ChangePinRequest? = null
        private set

    override suspend fun changePin(request: ChangePinRequest): NetworkResult<ChangePinResult, NetworkError> {
        callCount++
        lastRequest = request
        return result
    }
}
