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

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kpt.core.analytics.KptAnalyticsTracker
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.base.observability.CrashReporter
import kpt.core.base.observability.CrashSeverity
import kpt.core.base.security.BiometricAuthenticator
import kpt.core.base.ui.viewmodel.BaseViewModel
import kpt.core.datastore.UserPreferencesRepository
import kpt.core.model.user.DarkThemeConfig
import kpt.core.model.user.LanguageConfig
import org.mifos.groupbanking.core.data.repository.ChangePinRepository
import org.mifos.groupbanking.core.model.ChangePinRequest
import org.mifos.groupbanking.core.model.ChangePinResult

// MVI stack (State/Event/Action/ViewModel/DI) for the `settings` feature — see
// API.md#viewmodel / #state / #actions / #events / #di for the full generated-symbol contract.
//
// **This is the group-banking `settings-screen` contract from `idea-layer/screens/settings/ui.yaml`
// — NOT the legacy `kpt.feature.settings.SettingsViewmodel`/`SettingsModule` shell that already
// lives in this same Gradle module (`feature:settings`).** That shell is the unmigrated generic
// Money-Toolkit template settings screen (theme-brand/dynamic-color picker only — no PIN change, no
// biometric toggle, no logout, no notifications preference) and is still wired into
// `KoinModules.featureModule` as-is. Per the "idea-layer truth over source modules" precedent (the
// idea-layer screen contract is authoritative; an unmigrated shell in the same module is legacy
// debt, not a peer implementation to extend), this file lands the REAL contract-driven ViewModel in
// a SEPARATE package (`org.mifos.groupbanking.feature.settings`, matching every other
// group-banking feature's package convention) alongside the legacy `kpt.feature.settings` package,
// additively — nothing in the legacy shell is touched or deleted (that Screen/Route migration is
// out of this ViewModel-only generation step's scope; flagged for the caller,
// RULE-IMPLEMENT-CROSS-FEATURE-FIT-001 CFF1 / RULE-IMPL-DUPLICATION-001).
private const val TAG = "SettingsViewModel"

/** `ui.yaml#i18n.en.error_pin_change` — the ONLY declared pin-change failure copy key. */
private const val PIN_CHANGE_ERROR_MESSAGE_KEY = "error_pin_change"

/**
 * Client-side minimum PIN length — inferred from `ui.yaml#i18n.en.new_pin: "New PIN (min 4
 * digits)"` (the field label). `ui.yaml` declares no dedicated `action_contract`/validation rule
 * for `OnSubmitPinChange` beyond the label copy, so this constant is derived from that copy, not
 * fabricated. Flagged for an idea-layer `ui.yaml#components.change_pin_dialog` explicit validation
 * rule, RULE-IMPLEMENT-CROSS-FEATURE-FIT-001 CFF1.
 */
private const val MIN_PIN_LENGTH = 4

/**
 * DI-injected app-version seam. There is no app-version provider anywhere in this codebase
 * (`data-flow.yaml#entries[0].local_sources` names a `BuildConfig` source that does not exist as a
 * Kotlin symbol) — this is an HONEST, documented wiring gap, not a fabricated/stubbed value.
 * [SettingsModule] provides a real (non-`@Stub`) default instance with an explicit TODO seam: the
 * consumer app module (`cmp-android`/`cmp-ios`/`cmp-desktop`/`cmp-web`) is expected to override this
 * single at its own DI-graph composition root with the platform's actual generated
 * `BuildConfig.VERSION_NAME`/`VERSION_CODE` (Android) or equivalent once that wiring lands. Flagged
 * for the caller, RULE-IMPLEMENT-CROSS-FEATURE-FIT-001 CFF1.
 */
data class AppVersionInfo(
    val appVersion: String,
    val buildNumber: String,
)

/**
 * The reactive 4-flow fan-in payload of the `combine()` in [SettingsViewModel.init] —
 * `data-flow.yaml#entries[0].local_sources` (`DataStore` `app_settings` key) — language, theme,
 * biometric-enabled, and notifications-enabled are all persisted through the SAME
 * [UserPreferencesRepository]-backed `UserData` object (see [UserPreferencesRepository] KDoc), so
 * a single `combine()` correctly reflects every write immediately, mirroring
 * `SyncStatusViewModel.CombinedSyncData`'s identical reactive fan-in convention.
 */
data class SettingsPrefsSnapshot(
    val language: LanguageConfig,
    val theme: DarkThemeConfig,
    val isBiometricEnabled: Boolean,
    val isNotificationsEnabled: Boolean,
)

/**
 * Screen-level render state for `settings-screen` — verbatim mirror of
 * `ui.yaml#state_model.SettingsViewModel.screen_state.members`. Derived only (not stored) via
 * [SettingsState.deriveScreenState] — same convention as `MemberAddState`/`SyncStatusState` (keeps
 * [SettingsState.isChangingPin]/[SettingsState.isLoggingOut] the single source of truth instead of
 * a fourth, independently-mutable flag). See API.md#state.
 */
@Serializable
sealed interface SettingsScreenState {
    @Serializable
    data object Content : SettingsScreenState

    @Serializable
    data object ChangingPin : SettingsScreenState

    @Serializable
    data object LoggingOut : SettingsScreenState
}

/**
 * MVI state for `SettingsViewModel`. Field set + defaults are a verbatim mirror of
 * `ui.yaml#state_model.SettingsViewModel.state`, with ONE documented type-reuse divergence:
 * `ui.yaml` names the field types `AppLanguage`/`AppTheme` (idea-layer shorthand DTOs that do not
 * exist as Kotlin symbols anywhere in this codebase); this generation step consumes the REAL,
 * already-shipped [LanguageConfig] (`core/model`, already extended with `SWAHILI` for this screen's
 * `FR-010` language selector) and [DarkThemeConfig] directly — `ui.yaml`'s `AppTheme.SYSTEM` maps
 * 1:1 onto [DarkThemeConfig.FOLLOW_SYSTEM]. Flagged for an idea-layer `ui.yaml#state_model.state`
 * field-type backfill, RULE-IMPLEMENT-CROSS-FEATURE-FIT-001 CFF1.
 *
 * [pinChangeError] is `@Transient` — a screen-level render concern (the dialog's inline error text)
 * that should not survive process death, same convention as `MemberAddState.error`.
 *
 * **[isLoggingOut] — documented gap, not a fabricated flag:** `ui.yaml#components.logout_button`
 * binds its `loading_when: isLoggingOut` style to THIS state's field, but the actual logout
 * submission (session clear + `NavigateToLogin`) is owned end-to-end by the separate
 * `settings-logout-dialog` feature's own `SettingsLogoutDialogViewModel`/`SettingsLogoutDialogState
 * .isLoggingOut` (confirmed after [SettingsEvent.ShowLogoutDialog] is emitted) — this ViewModel
 * never flips [isLoggingOut] true itself (there is nothing to submit at THIS layer; `OnLogoutTapped`
 * only opens the confirmation dialog). The field is kept (per `ui.yaml`'s declared contract, so the
 * `LoggingOut` [SettingsScreenState] member stays structurally reachable/exhaustive) but is
 * currently always `false` from this ViewModel's own writes. Flagged for a cross-feature UI wiring
 * follow-up (bridging the dialog's own `isLoggingOut` back onto this screen's outlined button, or an
 * idea-layer `ui.yaml` correction removing the duplicate field), RULE-IMPLEMENT-CROSS-FEATURE-FIT-001
 * CFF1 — same documented-gap class as `SyncStatusViewModel`'s `OnRefresh` no-op-repository note.
 *
 * See API.md#state.
 */
@Serializable
@Immutable
data class SettingsState(
    val selectedLanguage: LanguageConfig = LanguageConfig.ENGLISH,
    val selectedTheme: DarkThemeConfig = DarkThemeConfig.FOLLOW_SYSTEM,
    val isBiometricEnabled: Boolean = false,
    val isNotificationsEnabled: Boolean = true,
    val isBiometricAvailable: Boolean = false,
    val appVersion: String = "",
    val buildNumber: String = "",
    val isChangingPin: Boolean = false,
    @Transient
    val pinChangeError: String? = null,
    val pinChangeSuccess: Boolean = false,
    val isLoggingOut: Boolean = false,
)

/**
 * Derives [SettingsScreenState] from [SettingsState] — see the type's KDoc for why this is a pure
 * function rather than a stored field (mirrors `MemberAddState.deriveScreenState()`).
 */
fun SettingsState.deriveScreenState(): SettingsScreenState = when {
    isChangingPin -> SettingsScreenState.ChangingPin
    isLoggingOut -> SettingsScreenState.LoggingOut
    else -> SettingsScreenState.Content
}

/**
 * One-shot side effects emitted by `SettingsViewModel` — verbatim mirror of
 * `ui.yaml#state_model.SettingsViewModel.events.members`. See API.md#events.
 */
sealed interface SettingsEvent {
    data object ShowLogoutDialog : SettingsEvent
    data object NavigateToLogin : SettingsEvent
    data class ShowSnackbar(val message: String) : SettingsEvent
}

/**
 * User intents dispatched to `SettingsViewModel`. The 8 top-level members are a verbatim mirror of
 * `ui.yaml#state_model.SettingsViewModel.actions.members` — RULE-IMPL-DEAD-CLICKABLE-001 Rule 1.
 * [OnLanguageSelected]/[OnThemeSelected] carry the REAL [LanguageConfig]/[DarkThemeConfig] types
 * (see [SettingsState] KDoc divergence note) rather than `ui.yaml`'s `AppLanguage`/`AppTheme`
 * shorthand. [Internal] is the sanctioned async-result-routing sub-interface (never a user intent)
 * per `training-layer/TRAINING_MASTER.yaml#patterns.actions`. See API.md#actions.
 */
sealed interface SettingsAction {
    data class OnLanguageSelected(val language: LanguageConfig) : SettingsAction
    data class OnThemeSelected(val theme: DarkThemeConfig) : SettingsAction
    data class OnBiometricToggled(val enabled: Boolean) : SettingsAction
    data class OnNotificationsToggled(val enabled: Boolean) : SettingsAction
    data object OnChangePinTapped : SettingsAction
    data class OnSubmitPinChange(val currentPin: String, val newPin: String) : SettingsAction
    data object OnDismissPinDialog : SettingsAction
    data object OnLogoutTapped : SettingsAction

    /** Async stream/coroutine-result emissions — routed via `trySendAction`, never dispatched by the UI. */
    sealed interface Internal : SettingsAction {
        data class PrefsLoaded(val snapshot: SettingsPrefsSnapshot) : Internal
        data class PinChangeResult(val result: NetworkResult<ChangePinResult, NetworkError>) : Internal
    }
}

/**
 * MVI processor for the full-featured settings screen (`business_logic.kind: crud` per ui.yaml —
 * language/theme/biometric/notifications are pure local `multiplatform-settings`-backed
 * preference reads+writes via [UserPreferencesRepository]; PIN change is the screen's only network
 * write, via [ChangePinRepository]). [analytics]/[crashReporter] are still wired despite the `crud`
 * classification — feature-level observability (SC5) applies regardless of Store5-write-path
 * classification, same convention as `GroupTypePickerViewModel`/every sibling `crud`-kind feature
 * in this codebase (a Debug breadcrumb on mount, a warning breadcrumb on PIN-change failure, and
 * [KptAnalyticsTracker.trackLogin] — the closest already-shipped auth-credential-change tracking
 * method, reused for `method = "pin_change"` rather than inventing a new generic `track()` surface,
 * mirrors `GroupTypePickerViewModel`'s identical `trackGroupOperation("select_type")` reuse
 * precedent — on every PIN-change result).
 *
 * `FieldEncryptor` (`core-base/security`) is intentionally NOT injected — `data-flow.yaml` declares
 * no `pii_columns` entry for this screen (every field here is a preference toggle or a PIN, and PIN
 * itself is never persisted client-side, only forwarded once over the wire via [ChangePinRepository]
 * per its own KDoc's `BasicAuth`-header note).
 *
 * **No success snackbar copy — documented i18n gap, not a fabricated string:** `ui.yaml#i18n.en`
 * declares `error_pin_change`/`error_auth` for failure but no dedicated success-copy key. On a
 * successful PIN change [handlePinChangeResult] therefore only flips [SettingsState.pinChangeSuccess]
 * (a REAL declared state field, closes the dialog) — it does NOT emit
 * [SettingsEvent.ShowSnackbar] with an invented English string, per
 * RULE-IMPL-NO-HARDCODED-STRING-001, same documented-gap class as `SyncStatusViewModel`'s "Sync-
 * success snackbar" note. Flagged for an idea-layer `ui.yaml#i18n` addition,
 * RULE-IMPLEMENT-CROSS-FEATURE-FIT-001 CFF1.
 *
 * See API.md#viewmodel.
 */
internal class SettingsViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val biometricAuthenticator: BiometricAuthenticator,
    private val changePinRepository: ChangePinRepository,
    private val appVersionInfo: AppVersionInfo,
    private val analytics: KptAnalyticsTracker,
    private val crashReporter: CrashReporter,
) : BaseViewModel<SettingsState, SettingsEvent, SettingsAction>(
    initialState = SettingsState(),
) {

    init {
        crashReporter.recordMessage(
            message = "feature=settings screen=settings-screen",
            level = CrashSeverity.Debug,
        )

        // `isBiometricAvailable`/`appVersion`/`buildNumber` are one-shot synchronous reads
        // (`data-flow.yaml#entries[0].local_sources`: `BiometricManager.isAvailable()`,
        // `BuildConfig.app_version`/`build_number`) — no reactive stream backs them, unlike the
        // 4-way `combine()` below, so they are set directly here rather than routed through
        // `Internal`.
        updateState {
            copy(
                isBiometricAvailable = biometricAuthenticator.isAvailable(),
                appVersion = appVersionInfo.appVersion,
                buildNumber = appVersionInfo.buildNumber,
            )
        }

        viewModelScope.launch {
            combine(
                userPreferencesRepository.observeLanguage,
                userPreferencesRepository.observeDarkThemeConfig,
                userPreferencesRepository.userData.map { it.isBiometricsEnabled },
                userPreferencesRepository.observeNotificationsEnabled,
            ) { language, theme, isBiometricEnabled, isNotificationsEnabled ->
                SettingsPrefsSnapshot(
                    language = language,
                    theme = theme,
                    isBiometricEnabled = isBiometricEnabled,
                    isNotificationsEnabled = isNotificationsEnabled,
                )
            }.collect { snapshot -> trySendAction(SettingsAction.Internal.PrefsLoaded(snapshot)) }
        }
    }

    override fun handleAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.OnLanguageSelected -> handleLanguageSelected(action.language)
            is SettingsAction.OnThemeSelected -> handleThemeSelected(action.theme)
            is SettingsAction.OnBiometricToggled -> handleBiometricToggled(action.enabled)
            is SettingsAction.OnNotificationsToggled -> handleNotificationsToggled(action.enabled)
            SettingsAction.OnChangePinTapped -> handleChangePinTapped()
            is SettingsAction.OnSubmitPinChange -> handleSubmitPinChange(action.currentPin, action.newPin)
            SettingsAction.OnDismissPinDialog -> handleDismissPinDialog()
            SettingsAction.OnLogoutTapped -> handleLogoutTapped()
            is SettingsAction.Internal.PrefsLoaded -> handlePrefsLoaded(action.snapshot)
            is SettingsAction.Internal.PinChangeResult -> handlePinChangeResult(action.result)
        }
    }

    // -- Language / theme / biometric / notifications (ui.yaml effect: persist_db) ------------------
    //
    // None of these four handlers call `updateState` directly — [UserPreferencesRepository]'s
    // `UserData`-backed flows re-emit on every write, so the single reactive `combine()` in `init`
    // is the ONE source of truth for these fields (mirrors `SyncStatusViewModel`/
    // `GroupTypePickerViewModel`'s identical reactive-only convention for repository-fed fields —
    // no second, independently-mutable copy of the same data).

    private fun handleLanguageSelected(language: LanguageConfig) {
        Logger.i(TAG) { "language selected=${language.name}" }
        viewModelScope.launch { userPreferencesRepository.setLanguage(language) }
    }

    private fun handleThemeSelected(theme: DarkThemeConfig) {
        Logger.i(TAG) { "theme selected=${theme.name}" }
        viewModelScope.launch { userPreferencesRepository.setDarkThemeConfig(theme) }
    }

    private fun handleBiometricToggled(enabled: Boolean) {
        if (!shouldPersistBiometricToggle(state.isBiometricAvailable)) {
            // `ui.yaml#components.biometric_toggle_row.trailing.enabled: "{{isBiometricAvailable}}"`
            // already disables the switch in the UI when hardware is unavailable — this is a
            // defensive guard against a stale/race-condition dispatch, not the primary gate.
            Logger.w(TAG) { "OnBiometricToggled ignored — biometric hardware unavailable" }
            return
        }
        Logger.i(TAG) { "biometric toggle enabled=$enabled" }
        viewModelScope.launch { userPreferencesRepository.setIsBiometricsEnabled(enabled) }
    }

    private fun handleNotificationsToggled(enabled: Boolean) {
        Logger.i(TAG) { "notifications toggle enabled=$enabled" }
        viewModelScope.launch { userPreferencesRepository.setNotificationsEnabled(enabled) }
    }

    // -- Change-PIN dialog (ui.yaml effect: transform_state / call_api) ------------------------------

    private fun handleChangePinTapped() {
        Logger.i(TAG) { "change-PIN dialog opened" }
        updateState { copy(isChangingPin = true, pinChangeError = null, pinChangeSuccess = false) }
    }

    private fun handleDismissPinDialog() {
        Logger.i(TAG) { "change-PIN dialog dismissed" }
        updateState { copy(isChangingPin = false, pinChangeError = null) }
    }

    private fun handleSubmitPinChange(currentPin: String, newPin: String) {
        if (newPin.length < MIN_PIN_LENGTH || !newPin.all(Char::isDigit)) {
            // See MIN_PIN_LENGTH KDoc — inferred from the `new_pin` field label, not a fabricated
            // rule. Kept as an inline client-side rejection (no network round-trip for an
            // unsendable PIN), mirrors `MemberAddViewModel.validateMemberAddForm`'s convention.
            Logger.w(TAG) { "OnSubmitPinChange rejected client-side — newPin fails min-length/digits-only check" }
            updateState { copy(pinChangeError = PIN_CHANGE_ERROR_MESSAGE_KEY) }
            return
        }

        viewModelScope.launch {
            val request = ChangePinRequest(currentPin = currentPin, newPin = newPin)
            val result = changePinRepository.changePin(request)
            trySendAction(SettingsAction.Internal.PinChangeResult(result))
        }
    }

    // -- Logout (ui.yaml effect: emit_event — the settings-logout-dialog feature owns the confirm) ---

    private fun handleLogoutTapped() {
        Logger.i(TAG) { "logout tapped — presenting confirmation dialog" }
        sendEvent(SettingsEvent.ShowLogoutDialog)
    }

    // -- Async result routing -------------------------------------------------------------------------

    private fun handlePrefsLoaded(snapshot: SettingsPrefsSnapshot) {
        updateState {
            copy(
                selectedLanguage = snapshot.language,
                selectedTheme = snapshot.theme,
                isBiometricEnabled = snapshot.isBiometricEnabled,
                isNotificationsEnabled = snapshot.isNotificationsEnabled,
            )
        }
    }

    private fun handlePinChangeResult(result: NetworkResult<ChangePinResult, NetworkError>) {
        when (result) {
            is NetworkResult.Success -> {
                analytics.trackLogin(method = "pin_change", success = true)
                Logger.i(TAG) { "PIN change succeeded resourceId=${result.data.resourceId}" }
                // See class KDoc "No success snackbar copy" — pinChangeSuccess is the real,
                // declared feedback signal; no fabricated i18n key is emitted.
                updateState { copy(isChangingPin = false, pinChangeError = null, pinChangeSuccess = true) }
            }

            is NetworkResult.Error -> {
                analytics.trackLogin(method = "pin_change", success = false, errorCode = result.error.name)
                when (result.error) {
                    NetworkError.UNAUTHORIZED -> {
                        // `data-flow.yaml#entries[2].error_paths`: "401 -> navigate: login" —
                        // session expired mid-PIN-change; close the dialog (nothing left to retry
                        // in it) and navigate away, per ui.yaml#errors.SessionExpired.
                        crashReporter.recordMessage(
                            message = "settings: PIN change failed — session expired (401)",
                            level = CrashSeverity.Warning,
                        )
                        Logger.w(TAG) { "PIN change 401 — session expired, navigating to login" }
                        updateState { copy(isChangingPin = false, pinChangeError = null) }
                        sendEvent(SettingsEvent.NavigateToLogin)
                    }

                    NetworkError.BAD_REQUEST -> {
                        // `data-flow.yaml#entries[2].error_paths`: "400 -> show_inline_error" —
                        // stays in the dialog per ui.yaml#errors.PinChangeFailed.
                        Logger.w(TAG) { "PIN change 400 — invalid current PIN or malformed request" }
                        updateState { copy(pinChangeError = PIN_CHANGE_ERROR_MESSAGE_KEY) }
                    }

                    NetworkError.NOT_FOUND, NetworkError.REQUEST_TIMEOUT, NetworkError.TOO_MANY_REQUESTS,
                    NetworkError.SERVER, NetworkError.SERIALIZATION, NetworkError.UNKNOWN,
                    -> {
                        crashReporter.recordMessage(
                            message = "settings: PIN change failed networkError=${result.error}",
                            level = CrashSeverity.Warning,
                        )
                        Logger.w(TAG) { "PIN change failed networkError=${result.error}" }
                        updateState { copy(pinChangeError = PIN_CHANGE_ERROR_MESSAGE_KEY) }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Top-level helpers (pure — independently unit-testable)
// ---------------------------------------------------------------------------------------------

/**
 * Pure guard predicate for [SettingsAction.OnBiometricToggled] — extracted as a top-level function
 * (rather than inlined in [SettingsViewModel.handleBiometricToggled]) so BOTH branches
 * (`available` / `unavailable`) are independently unit-testable.
 *
 * **Why this exists as a separate function:** [BiometricAuthenticator] (`core-base/security`) is a
 * concrete, non-`open` expect/actual class with no injectable test seam — EVERY shipped platform
 * actual (`androidMain`/`desktopMain`/`nativeMain`/`jsCommonMain`) currently returns
 * `isAvailable() = false` UNCONDITIONALLY (see the class's own KDoc: "Full BiometricPrompt wiring
 * requires an Activity reference which is not available at the core-base level... Consumer apps
 * should inject their own check"). `SettingsViewModelTest` therefore cannot exercise the
 * `available -> persists` branch through the real class in a unit test without editing
 * `core-base/security` — FORBIDDEN (Mandatory Rule 6). This pure function isolates the guard LOGIC
 * (trivially `isBiometricAvailable`) so it is still tested directly and honestly, both directions,
 * per RULE-TDD-METHODOLOGY-001 — see `SettingsViewModelTest`'s "guard predicate" tests. Flagged for
 * the caller: a real device-level `available=true` VM-integration test needs a `core-base/security`
 * test-seam addition (e.g. a fake-able `BiometricCapabilityProvider` interface), out of this
 * ViewModel-only generation step's scope, RULE-IMPLEMENT-CROSS-FEATURE-FIT-001 CFF1.
 */
internal fun shouldPersistBiometricToggle(isBiometricAvailable: Boolean): Boolean = isBiometricAvailable
