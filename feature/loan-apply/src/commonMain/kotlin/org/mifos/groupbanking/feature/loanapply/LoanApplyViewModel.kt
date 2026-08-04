/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.loanapply

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkMonitor
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kpt.core.analytics.KptAnalyticsTracker
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.base.observability.CrashReporter
import kpt.core.base.observability.CrashSeverity
import kpt.core.base.ui.viewmodel.BaseViewModel
import org.mifos.groupbanking.core.data.repository.LoanApplyRepository
import org.mifos.groupbanking.core.model.ApplyLoanRequest
import org.mifos.groupbanking.core.model.GroupMember
import org.mifos.groupbanking.core.model.LoanApplicationResult
import org.mifos.groupbanking.core.model.LoanApplyTemplate
import org.mifos.groupbanking.core.model.LoanProduct
import org.mifos.groupbanking.core.model.LoanPurpose

// MVI stack (State/Event/Action/ViewModel/DI) for the `loan-apply` feature — see
// API.md#viewmodel / #state / #actions / #events / #di for the full generated-symbol contract.
private const val TAG = "LoanApplyViewModel"

/**
 * Sentinel `productId` passed to [LoanApplyRepository.loadTemplate] when a group member has been
 * selected but no loan product has been chosen yet. See [LoanApplyViewModel] class KDoc
 * "template-bootstrap gap" for why this sentinel exists and its known reachability limitation.
 */
private const val UNSELECTED_PRODUCT_ID = 0L

/**
 * Screen-level render state for `loan-apply-screen` — verbatim mirror of
 * `ui.yaml#state_model.LoanApplyViewModel.screen_state.members`. Derived only (not stored) via
 * [LoanApplyState.deriveScreenState] — same convention as `MemberAddState`/`LoanDetailState` (keeps
 * `isLoadingTemplate`/`isSubmitting`/`submitSuccess`/`error` the single source of truth instead of
 * a fifth, independently-mutable flag). See API.md#state.
 */
@Serializable
sealed interface LoanApplyScreenState {
    @Serializable
    data object Loading : LoanApplyScreenState

    @Serializable
    data object Content : LoanApplyScreenState

    @Serializable
    data object Submitting : LoanApplyScreenState

    @Serializable
    data object Error : LoanApplyScreenState

    @Serializable
    data object Success : LoanApplyScreenState
}

/**
 * Error taxonomy for the loan-apply form — verbatim mirror of
 * `ui.yaml#state_model.LoanApplyViewModel.errors.types`. [messageKey] is a composeResources
 * string-resource id (never a raw hardcoded English string, per RULE-IMPL-NO-HARDCODED-STRING-001)
 * resolved by the Screen layer.
 *
 * **[CorpusInsufficient] reachability gap (flagged, not fabricated around) — same documented class
 * as `MemberAddError.PhoneAlreadyExists`:** `data-flow.yaml#entries[on_submit].error_paths` maps a
 * `422` response to `error_corpus` (this taxonomy's [CorpusInsufficient]), but [NetworkError] (the
 * transport-level enum every repository in this codebase surfaces) has NO `422`/`UNPROCESSABLE_ENTITY`
 * member — only `BAD_REQUEST`/`NOT_FOUND`/`UNAUTHORIZED`/`REQUEST_TIMEOUT`/`TOO_MANY_REQUESTS`/
 * `SERVER`/`SERIALIZATION`/`UNKNOWN`. [toLoanApplyError] therefore maps `BAD_REQUEST` to
 * [AmountExceedsEligibility] (the more common submit-time validation failure); [CorpusInsufficient]
 * is exhaustively covered in every `when` (RULE-IMPL-DEAD-CLICKABLE-001 Rule 2) but is UNREACHABLE
 * from a live backend today. Flagged for the cross-feature repair station
 * (RULE-IMPLEMENT-CROSS-FEATURE-FIT-001 CFF1) — the fix is a `NetworkError.UNPROCESSABLE_ENTITY`
 * member threaded through `core-base/network`.
 *
 * See API.md#state.
 */
@Serializable
sealed interface LoanApplyError {
    val retry: Boolean
    val messageKey: String

    @Serializable
    data object Network : LoanApplyError {
        override val retry: Boolean = true
        override val messageKey: String = "error_network"
    }

    @Serializable
    data object Server : LoanApplyError {
        override val retry: Boolean = true
        override val messageKey: String = "error_server"
    }

    @Serializable
    data object AmountExceedsEligibility : LoanApplyError {
        override val retry: Boolean = false
        override val messageKey: String = "error_amount_exceeds"
    }

    @Serializable
    data object CorpusInsufficient : LoanApplyError {
        override val retry: Boolean = false
        override val messageKey: String = "error_corpus"
    }

    @Serializable
    data object Auth : LoanApplyError {
        override val retry: Boolean = false
        override val messageKey: String = "error_auth"
    }
}

/**
 * MVI state for `LoanApplyViewModel`. Field set + defaults are a verbatim mirror of
 * `ui.yaml#state_model.LoanApplyViewModel.state.fields` (17 fields, declaration order preserved).
 * [selectedMember], [selectedProduct], [members], [loanProducts], and [error] are `@Transient` —
 * non-serializable domain-model / render-only payloads that should not survive process death and
 * are always re-derived from [LoanApplyRepository] on (re)mount, mirroring `LoanDetailState`'s /
 * `MemberAddState.error`'s identical `@Transient` convention. `groupId` is intentionally NOT a
 * state field — `ui.yaml#state_model.state.fields` does not declare it; it is forwarded as a
 * ViewModel constructor nav-arg only, mirroring `LoanDetailViewModel.loanId`'s identical precedent.
 *
 * [eligibleAmount] / [corpusWarning] are CLIENT-SIDE DERIVED fields (recomputed by
 * [LoanApplyViewModel], never sent by the server) — see class KDoc "Real-time derivation" for the
 * exact formulas.
 *
 * See API.md#state.
 */
@Serializable
@Immutable
data class LoanApplyState(
    @Transient val selectedMember: GroupMember? = null,
    val requestedAmount: String = "",
    val durationWeeks: Int = 12,
    val purpose: LoanPurpose = LoanPurpose.BUSINESS,
    @Transient val selectedProduct: LoanProduct? = null,
    @Transient val members: List<GroupMember> = emptyList(),
    @Transient val loanProducts: List<LoanProduct> = emptyList(),
    val memberSavingsBalance: Double = 0.0,
    val loanMultiplier: Double = 3.0,
    val eligibleAmount: Double = 0.0,
    val corpusBalance: Double = 0.0,
    val corpusWarning: Boolean = false,
    val isSubmitting: Boolean = false,
    val isLoadingTemplate: Boolean = false,
    val amountError: String? = null,
    @Transient val error: LoanApplyError? = null,
    val submitSuccess: Boolean = false,
)

/**
 * Derives [LoanApplyScreenState] from [LoanApplyState] — see the type's KDoc for why this is a
 * pure function rather than a stored field (mirrors `MemberAddState.deriveScreenState()`).
 */
fun LoanApplyState.deriveScreenState(): LoanApplyScreenState = when {
    error != null -> LoanApplyScreenState.Error
    submitSuccess -> LoanApplyScreenState.Success
    isSubmitting -> LoanApplyScreenState.Submitting
    isLoadingTemplate -> LoanApplyScreenState.Loading
    else -> LoanApplyScreenState.Content
}

/**
 * One-shot side effects emitted by `LoanApplyViewModel` — verbatim mirror of
 * `ui.yaml#state_model.LoanApplyViewModel.events.members`. See API.md#events.
 */
sealed interface LoanApplyEvent {
    data class NavigateToMeetingConduct(val loanId: Long) : LoanApplyEvent
    data object NavigateBack : LoanApplyEvent
    data class ShowSnackbar(val message: String) : LoanApplyEvent
}

/**
 * User intents dispatched to `LoanApplyViewModel`. The 7 top-level members are a verbatim mirror of
 * `ui.yaml#state_model.LoanApplyViewModel.actions.members` — RULE-IMPL-DEAD-CLICKABLE-001 Rule 1.
 * [Internal] is the sanctioned async-result-routing sub-interface (never a user intent) per
 * `training-layer/TRAINING_MASTER.yaml#patterns.actions` — mirrors `MemberAddAction.Internal` /
 * `LoanDetailAction.Internal`. See API.md#actions.
 */
sealed interface LoanApplyAction {
    data class OnMemberSelected(val member: GroupMember) : LoanApplyAction
    data class OnAmountChanged(val amount: String) : LoanApplyAction
    data class OnDurationChanged(val weeks: Int) : LoanApplyAction
    data class OnPurposeChanged(val purpose: LoanPurpose) : LoanApplyAction
    data class OnProductSelected(val product: LoanProduct) : LoanApplyAction
    data object OnSubmit : LoanApplyAction
    data object OnBack : LoanApplyAction

    /** Async coroutine results — routed via `trySendAction`, never dispatched by the UI. */
    sealed interface Internal : LoanApplyAction {
        data class MembersLoaded(val result: NetworkResult<List<GroupMember>, NetworkError>) : Internal
        data class TemplateLoaded(val result: NetworkResult<LoanApplyTemplate, NetworkError>) : Internal
        data class SubmitResult(val result: NetworkResult<LoanApplicationResult, NetworkError>) : Internal
    }
}

/**
 * MVI processor for the loan-apply form (`business_logic.kind: composite` per ui.yaml — a
 * client-validated multi-field form whose submit posts through
 * [LoanApplyRepository.applyLoan]; SP-04 AC-7 analytics/crashReporter injection pair applies since
 * `composite` is not `crud`/`nav_only`, per RULE-IDEA-IMPL-INTELLIGENCE-001 AC-03i).
 * [LoanApplyRepository] is consumed directly via [NetworkResult] rather than a Store5
 * `.asScreenStream()` — see [LoanApplyRepository] KDoc's own "Store5 branch" note, same branch as
 * [org.mifos.groupbanking.feature.memberadd.MemberAddViewModel]. `FieldEncryptor`
 * (`core-base/security`) is intentionally NOT injected — `data-flow.yaml` declares no
 * `pii_columns` entry for this screen; none of [GroupMember]/[LoanProduct]/[ApplyLoanRequest]
 * carries a `@PII`-marked field per the SP-02 idea-layer schema. `SessionManager`
 * (`ui.yaml#state_model.di`) is also NOT injected — this composite/form screen follows
 * `MemberAddViewModel`'s precedent (not `LoanDetailViewModel`'s composite-READ precedent): a 401 on
 * [handleSubmitResult] surfaces [LoanApplyError.Auth] + [LoanApplyEvent.ShowSnackbar], same
 * documented "no dedicated NavigateToLogin event" gap as `MemberAddError.Auth`.
 *
 * **`di` drift (flagged, not re-created) — `ui.yaml#state_model.di` declares `LoanRepository`,
 * `MemberRepository`, `GroupRepository` (three separate repositories); the shipped data layer
 * instead exposes ONE purpose-built [LoanApplyRepository] combining all five backing reads
 * (`get_loan_products`, `get_loan_template`, `get_member_savings`, `get_group_corpus`,
 * `get_group_config`) behind [LoanApplyRepository.loadTemplate] — same class of drift as
 * `LoanDetailViewModel`'s documented `LoanRepository` note. This generation step consumes the REAL
 * shipped contract per its brief, not the idea-layer's originally-declared repository split.
 *
 * **Template-bootstrap gap (confirmed, load-bearing — flagged for the cross-feature repair
 * station, RULE-IMPLEMENT-CROSS-FEATURE-FIT-001 CFF1):** `data-flow.yaml#entries[0]` declares FOUR
 * independent on-mount reads (`get_group_members`, `get_loan_products`, `get_group_corpus`,
 * `get_group_config` — none requiring a selected member/product) so [loanProducts][LoanApplyState.loanProducts]
 * should populate on mount alongside [members][LoanApplyState.members]. The shipped
 * [LoanApplyRepository.loadTemplate] instead bundles `get_loan_products` together with the
 * `clientId`+`productId`-scoped `get_loan_template` read inside ONE 5-way combine (first-failure-wins,
 * declaration order products→template→savings→corpus→config — see `LoanApplyRepositoryImpl` KDoc)
 * — so [loanProducts] cannot be populated before a member is selected. [handleMemberSelected] calls
 * [LoanApplyRepository.loadTemplate] with the real `clientId` and the [UNSELECTED_PRODUCT_ID]
 * sentinel `productId` to bootstrap [loanProducts]/[corpusBalance][LoanApplyState.corpusBalance]/
 * [loanMultiplier][LoanApplyState.loanMultiplier]/[memberSavingsBalance][LoanApplyState.memberSavingsBalance]
 * together for that member; if a real backend's `get_loan_template` sub-read rejects the sentinel
 * `productId`, the whole 5-way combine returns [NetworkResult.Error] and the screen surfaces
 * [LoanApplyError.Server] (user re-selects the member to retry) rather than silently leaving the
 * product dropdown empty. The durable fix is a standalone
 * `LoanApplyRepository.getFormLookups(groupId): NetworkResult<LoanApplyLookups, NetworkError>`
 * matching `data-flow.yaml#entries[0]`'s real on-mount contract, decoupled from the per-product
 * template read — out of this generation step's repository-immutability scope (Mandatory Rule 6).
 *
 * See API.md#viewmodel.
 */
internal class LoanApplyViewModel(
    private val repository: LoanApplyRepository,
    private val networkMonitor: NetworkMonitor,
    private val analytics: KptAnalyticsTracker,
    private val crashReporter: CrashReporter,
    private val groupId: Long,
) : BaseViewModel<LoanApplyState, LoanApplyEvent, LoanApplyAction>(
    initialState = LoanApplyState(),
) {

    private var templateJob: Job? = null
    private var submitJob: Job? = null

    init {
        crashReporter.recordMessage(
            message = "feature=loan-apply screen=loan-apply-screen groupId=$groupId",
            level = CrashSeverity.Debug,
        )
        analytics.trackLoanOperation(operation = "apply_view", loanId = null)
        loadMembers()
    }

    override fun handleAction(action: LoanApplyAction) {
        when (action) {
            is LoanApplyAction.OnMemberSelected -> handleMemberSelected(action.member)
            is LoanApplyAction.OnAmountChanged -> handleAmountChanged(action.amount)
            is LoanApplyAction.OnDurationChanged -> handleDurationChanged(action.weeks)
            is LoanApplyAction.OnPurposeChanged -> handlePurposeChanged(action.purpose)
            is LoanApplyAction.OnProductSelected -> handleProductSelected(action.product)
            LoanApplyAction.OnSubmit -> handleSubmit()
            LoanApplyAction.OnBack -> sendEvent(LoanApplyEvent.NavigateBack)
            is LoanApplyAction.Internal.MembersLoaded -> handleMembersLoaded(action.result)
            is LoanApplyAction.Internal.TemplateLoaded -> handleTemplateLoaded(action.result)
            is LoanApplyAction.Internal.SubmitResult -> handleSubmitResult(action.result)
        }
    }

    // -- On-mount member load (data-flow.yaml entries[0], get_group_members slice) -----------------

    private fun loadMembers() {
        viewModelScope.launch {
            updateState { copy(isLoadingTemplate = true, error = null) }
            val result = repository.getGroupMembers(groupId)
            trySendAction(LoanApplyAction.Internal.MembersLoaded(result))
        }
    }

    private fun handleMembersLoaded(result: NetworkResult<List<GroupMember>, NetworkError>) {
        when (result) {
            is NetworkResult.Success -> {
                Logger.i(TAG) { "getGroupMembers succeeded groupId=$groupId count=${result.data.size}" }
                updateState { copy(members = result.data, isLoadingTemplate = false, error = null) }
            }
            is NetworkResult.Error -> {
                crashReporter.recordMessage(
                    message = "loan-apply: getGroupMembers failed groupId=$groupId networkError=${result.error}",
                    level = CrashSeverity.Warning,
                )
                updateState { copy(isLoadingTemplate = false, error = result.error.toLoanApplyError()) }
            }
        }
    }

    // -- Member / product selection (ui.yaml effect: call_api, library_refs: [cmp-network-monitor]) --

    private fun handleMemberSelected(member: GroupMember) {
        Logger.i(TAG) { "member selected id=${member.id} groupId=$groupId" }
        updateState { copy(selectedMember = member) }
        refreshTemplate()
    }

    private fun handleProductSelected(product: LoanProduct) {
        Logger.i(TAG) { "product selected id=${product.id} groupId=$groupId" }
        updateState { copy(selectedProduct = product) }
        refreshTemplate()
    }

    /**
     * Reloads [LoanApplyRepository.loadTemplate] for the current [LoanApplyState.selectedMember] +
     * [LoanApplyState.selectedProduct] (or [UNSELECTED_PRODUCT_ID] when no product is chosen yet —
     * see class KDoc "Template-bootstrap gap"). No-op while no member is selected (a product cannot
     * be picked before [loanProducts][LoanApplyState.loanProducts] has data, so this guard is
     * defensive only).
     */
    private fun refreshTemplate() {
        val member = state.selectedMember ?: return
        val productId = state.selectedProduct?.id ?: UNSELECTED_PRODUCT_ID
        templateJob?.cancel()
        templateJob = viewModelScope.launch {
            updateState { copy(isLoadingTemplate = true, error = null) }
            val result = repository.loadTemplate(groupId = groupId, clientId = member.fineractClientId, productId = productId)
            trySendAction(LoanApplyAction.Internal.TemplateLoaded(result))
        }
    }

    private fun handleTemplateLoaded(result: NetworkResult<LoanApplyTemplate, NetworkError>) {
        when (result) {
            is NetworkResult.Success -> {
                val template = result.data
                Logger.i(TAG) {
                    "loadTemplate succeeded groupId=$groupId maxEligibleAmount=${template.maxEligibleAmount}"
                }
                updateState {
                    copy(
                        loanProducts = template.products,
                        memberSavingsBalance = template.memberSavingsBalance,
                        loanMultiplier = template.loanMultiplier,
                        corpusBalance = template.groupCorpusBalance,
                        eligibleAmount = template.maxEligibleAmount,
                        isLoadingTemplate = false,
                        error = null,
                        amountError = computeAmountError(requestedAmount, template.maxEligibleAmount),
                        corpusWarning = computeCorpusWarning(requestedAmount, template.groupCorpusBalance),
                    )
                }
            }
            is NetworkResult.Error -> {
                crashReporter.recordMessage(
                    message = "loan-apply: loadTemplate failed groupId=$groupId networkError=${result.error}",
                    level = CrashSeverity.Warning,
                )
                updateState { copy(isLoadingTemplate = false, error = result.error.toLoanApplyError()) }
            }
        }
    }

    // -- Amount / duration / purpose (ui.yaml effect: transform_state — pure, no I/O) -----------------
    // Real-time derivation: eligibleAmount comes from LoanApplyTemplate.maxEligibleAmount
    // (min(memberSavingsBalance * loanMultiplier, groupMaxLoanAmount), recomputed server-side inputs
    // via handleTemplateLoaded above); amountError / corpusWarning recompute here on every keystroke
    // against the CURRENT eligibleAmount / corpusBalance already resident in state.

    private fun handleAmountChanged(amount: String) {
        updateState {
            copy(
                requestedAmount = amount,
                amountError = computeAmountError(amount, eligibleAmount),
                corpusWarning = computeCorpusWarning(amount, corpusBalance),
            )
        }
    }

    private fun handleDurationChanged(weeks: Int) {
        Logger.d(TAG) { "duration changed to weeks=$weeks groupId=$groupId" }
        updateState { copy(durationWeeks = weeks) }
    }

    private fun handlePurposeChanged(purpose: LoanPurpose) {
        updateState { copy(purpose = purpose) }
    }

    // -- Submit (ui.yaml effect: call_api, library_refs: [cmp-network-monitor]) -----------------------
    // corpusWarning is DISPLAY + SOFT-WARN ONLY (ui.yaml#components.submit_button.enabled_when omits
    // it entirely) — a corpus-below-buffer disbursement is still submittable; only amountError
    // (requestedAmount exceeding the savings x loanMultiplier ceiling) blocks submission.

    private fun handleSubmit() {
        val member = state.selectedMember
        val product = state.selectedProduct
        val amount = state.requestedAmount.toDoubleOrNull()
        if (member == null || product == null || amount == null) {
            Logger.w(TAG) { "OnSubmit blocked — form incomplete groupId=$groupId" }
            return
        }
        if (state.amountError != null) {
            Logger.w(TAG) { "OnSubmit blocked — requested amount invalid groupId=$groupId" }
            return
        }

        submitJob?.cancel()
        submitJob = viewModelScope.launch {
            updateState { copy(isSubmitting = true, error = null) }
            analytics.trackLoanOperation(operation = "apply_submit_start", loanId = null, amount = state.requestedAmount)

            if (!networkMonitor.isOnline.value) {
                Logger.w(TAG) { "loan submit attempted while offline groupId=$groupId" }
                crashReporter.recordMessage(
                    message = "loan-apply: submit attempted while offline groupId=$groupId",
                    level = CrashSeverity.Info,
                )
                updateState { copy(isSubmitting = false, error = LoanApplyError.Network) }
                sendEvent(LoanApplyEvent.ShowSnackbar(message = LoanApplyError.Network.messageKey))
                return@launch
            }

            val request = ApplyLoanRequest(
                memberId = member.fineractClientId,
                productId = product.id,
                amount = amount,
                durationWeeks = state.durationWeeks,
                purpose = state.purpose,
                groupId = groupId,
            )
            val result = repository.applyLoan(request = request, product = product)
            trySendAction(LoanApplyAction.Internal.SubmitResult(result))
        }
    }

    private fun handleSubmitResult(result: NetworkResult<LoanApplicationResult, NetworkError>) {
        when (result) {
            is NetworkResult.Success -> {
                val application = result.data
                analytics.trackLoanOperation(
                    operation = "apply_submit_success",
                    loanId = application.loanId.toString(),
                    success = true,
                )
                Logger.i(TAG) { "loan submitted loanId=${application.loanId} groupId=$groupId" }
                updateState { copy(isSubmitting = false, submitSuccess = true, error = null) }
                sendEvent(LoanApplyEvent.NavigateToMeetingConduct(loanId = application.loanId))
            }
            is NetworkResult.Error -> {
                analytics.trackLoanOperation(operation = "apply_submit_error", success = false)
                crashReporter.recordMessage(
                    message = "loan-apply: applyLoan failed groupId=$groupId networkError=${result.error}",
                    level = CrashSeverity.Warning,
                )
                val mapped = result.error.toLoanApplyError()
                updateState { copy(isSubmitting = false, error = mapped) }
                when (mapped) {
                    LoanApplyError.Network, LoanApplyError.Auth -> sendEvent(LoanApplyEvent.ShowSnackbar(message = mapped.messageKey))
                    LoanApplyError.Server, LoanApplyError.AmountExceedsEligibility, LoanApplyError.CorpusInsufficient ->
                        Unit // inline error_state (ui.yaml) suffices — same convention as MemberAddError's Validation/Server branch
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Top-level helpers (pure — independently unit-testable)
// ---------------------------------------------------------------------------------------------

/**
 * `ui.yaml#components.amount_input.on_change.action_contract`: "validates requestedAmount <=
 * eligibleAmount (savings x loanMultiplier)". Returns `null` for a blank/non-numeric [amount] (the
 * `input_type: number` / `keyboard: decimal` field already constrains real user input; a
 * non-numeric string is defensively treated as "not yet exceeding" rather than surfacing a
 * validation message the idea-layer's `i18n.en` block does not declare — see class KDoc "no
 * dedicated non-numeric-format i18n key" gap, flagged for the idea-layer to add
 * `error_amount_invalid`).
 */
private fun computeAmountError(amount: String, eligibleAmount: Double): String? {
    val parsed = amount.toDoubleOrNull() ?: return null
    return if (eligibleAmount > 0.0 && parsed > eligibleAmount) LoanApplyError.AmountExceedsEligibility.messageKey else null
}

/**
 * `ui.yaml#components.amount_input.on_change.action_contract`: "runs the corpus 10% buffer check,
 * updating ... corpusWarning". This generation implements the literal, unambiguous predicate
 * `requestedAmount > corpusBalance` per the generation brief — `ui.yaml`'s own prose ("reduce the
 * group corpus below 10% buffer") does not declare the precise buffer formula (no numeric
 * threshold field anywhere in `ui.yaml#state_model`/`api.yaml`), so a literal 10%-remaining
 * computation would be an invented value, not a declared one. Flagged for the idea-layer to
 * precisely specify the buffer formula (RULE-IMPLEMENT-CROSS-FEATURE-FIT-001 CFF1) — swapping the
 * predicate here is then a one-line change. [corpusWarning][LoanApplyState.corpusWarning] is
 * DISPLAY-ONLY — see [LoanApplyViewModel.handleSubmit] KDoc — it never blocks [LoanApplyAction.OnSubmit].
 */
private fun computeCorpusWarning(amount: String, corpusBalance: Double): Boolean {
    val parsed = amount.toDoubleOrNull() ?: return false
    return parsed > corpusBalance
}

/**
 * Disambiguates the transport-level [NetworkError] onto [LoanApplyState]'s declared [LoanApplyError]
 * taxonomy — `data-flow.yaml#error_paths`: `network.offline -> error_network`, `400 -> error_validation`
 * (folded onto [LoanApplyError.AmountExceedsEligibility], the more common submit-time validation
 * failure), `401 -> navigate login` (folded onto [LoanApplyError.Auth], see [LoanApplyError] class
 * KDoc), `404`/`500 -> error_server`. See [LoanApplyError.CorpusInsufficient] KDoc for why `422`
 * (`error_corpus`) cannot be distinguished here — [NetworkError] has no `422` member.
 */
private fun NetworkError.toLoanApplyError(): LoanApplyError = when (this) {
    NetworkError.REQUEST_TIMEOUT -> LoanApplyError.Network
    NetworkError.BAD_REQUEST -> LoanApplyError.AmountExceedsEligibility
    NetworkError.UNAUTHORIZED, NetworkError.TOO_MANY_REQUESTS -> LoanApplyError.Auth
    NetworkError.NOT_FOUND, NetworkError.SERIALIZATION, NetworkError.SERVER, NetworkError.UNKNOWN ->
        LoanApplyError.Server
}
