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

import app.cash.turbine.test
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkChangeEvent
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkInfo
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkMonitor
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kpt.core.analytics.KptAnalyticsTracker
import kpt.core.base.analytics.NoOpAnalyticsHelper
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.base.observability.ConsoleCrashReporter
import org.mifos.groupbanking.core.data.repository.LoanApplyRepository
import org.mifos.groupbanking.core.model.ApplyLoanRequest
import org.mifos.groupbanking.core.model.GroupMember
import org.mifos.groupbanking.core.model.LoanApplicationResult
import org.mifos.groupbanking.core.model.LoanApplyTemplate
import org.mifos.groupbanking.core.model.LoanProduct
import org.mifos.groupbanking.core.model.LoanPurpose
import org.mifos.groupbanking.core.model.MemberStatus
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * See API.md#viewmodel — `LoanApplyViewModelTest` exercises every declared [LoanApplyAction] path
 * (on-mount member load happy/error, member/product selection driving the [LoanApplyRepository.loadTemplate]
 * eligibility recompute, amount/duration/purpose field transitions, the `eligibleAmount`/`corpusWarning`
 * derivations, submit happy/incomplete/offline/transport-error, and back navigation), per
 * RULE-TDD-METHODOLOGY-001 / RULE-IMPL-DEAD-CLICKABLE-001.
 */
class LoanApplyViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: FakeLoanApplyRepository
    private lateinit var networkMonitor: FakeNetworkMonitor

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeLoanApplyRepository()
        networkMonitor = FakeNetworkMonitor(initiallyOnline = true)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(groupId: Long = 42L): LoanApplyViewModel = LoanApplyViewModel(
        repository = repository,
        networkMonitor = networkMonitor,
        analytics = KptAnalyticsTracker(NoOpAnalyticsHelper()),
        crashReporter = ConsoleCrashReporter(),
        groupId = groupId,
    )

    // -- On-mount member load ---------------------------------------------------------------------

    @Test
    fun `init loads group members and resolves screen state to Content`() = runTest(testDispatcher) {
        repository.getGroupMembersResult = NetworkResult.Success(listOf(sampleMember()))
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals(listOf(sampleMember()), state.members)
        assertFalse(state.isLoadingTemplate)
        assertEquals(LoanApplyScreenState.Content, state.deriveScreenState())
        assertEquals(1, repository.getGroupMembersCallCount)
    }

    @Test
    fun `init member-load failure surfaces a LoanApplyError and Error screen state`() = runTest(testDispatcher) {
        repository.getGroupMembersResult = NetworkResult.Error(NetworkError.SERVER)
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals(LoanApplyError.Server, state.error)
        assertEquals(LoanApplyScreenState.Error, state.deriveScreenState())
    }

    // -- Member / product selection drive the eligibility template ---------------------------------

    @Test
    fun `OnMemberSelected sets selectedMember and loads eligibility fields from the template`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(LoanApplyAction.OnMemberSelected(sampleMember()))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals(sampleMember(), state.selectedMember)
        assertEquals(5000.0, state.memberSavingsBalance)
        assertEquals(3.0, state.loanMultiplier)
        assertEquals(24000.0, state.corpusBalance)
        assertEquals(15000.0, state.eligibleAmount)
        assertEquals(listOf(sampleProduct()), state.loanProducts)
        assertEquals(1, repository.loadTemplateCallCount)
        assertEquals(501L, repository.lastLoadTemplateClientId)
        assertEquals(0L, repository.lastLoadTemplateProductId) // no product chosen yet -> UNSELECTED_PRODUCT_ID sentinel
    }

    @Test
    fun `OnMemberSelected template-load failure surfaces an error`() = runTest(testDispatcher) {
        repository.loadTemplateResult = NetworkResult.Error(NetworkError.SERVER)
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(LoanApplyAction.OnMemberSelected(sampleMember()))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals(sampleMember(), state.selectedMember)
        assertEquals(LoanApplyError.Server, state.error)
        assertEquals(0.0, state.eligibleAmount)
    }

    @Test
    fun `OnProductSelected updates selectedProduct and re-loads the template with the real productId`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.trySendAction(LoanApplyAction.OnMemberSelected(sampleMember()))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(LoanApplyAction.OnProductSelected(sampleProduct()))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals(sampleProduct(), state.selectedProduct)
        assertEquals(2, repository.loadTemplateCallCount)
        assertEquals(7L, repository.lastLoadTemplateProductId)
    }

    // -- Amount / duration / purpose field transitions -----------------------------------------------

    @Test
    fun `OnAmountChanged within the eligible ceiling clears amountError and corpusWarning`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.trySendAction(LoanApplyAction.OnMemberSelected(sampleMember()))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(LoanApplyAction.OnAmountChanged("1500"))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals("1500", state.requestedAmount)
        assertNull(state.amountError)
        assertFalse(state.corpusWarning)
    }

    @Test
    fun `OnAmountChanged above eligibleAmount sets amountError`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.trySendAction(LoanApplyAction.OnMemberSelected(sampleMember()))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(LoanApplyAction.OnAmountChanged("20000")) // eligibleAmount is 15000
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("error_amount_exceeds", viewModel.stateFlow.value.amountError)
    }

    @Test
    fun `OnAmountChanged above the corpus balance sets corpusWarning without blocking submit`() = runTest(testDispatcher) {
        repository.loadTemplateResult = NetworkResult.Success(sampleTemplate(groupCorpusBalance = 1000.0))
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.trySendAction(LoanApplyAction.OnMemberSelected(sampleMember()))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(LoanApplyAction.OnAmountChanged("1500")) // > corpus(1000), still <= eligible(15000)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertTrue(state.corpusWarning)
        assertNull(state.amountError)
    }

    @Test
    fun `OnDurationChanged updates durationWeeks`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(LoanApplyAction.OnDurationChanged(24))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(24, viewModel.stateFlow.value.durationWeeks)
    }

    @Test
    fun `OnPurposeChanged updates purpose`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(LoanApplyAction.OnPurposeChanged(LoanPurpose.EDUCATION))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(LoanPurpose.EDUCATION, viewModel.stateFlow.value.purpose)
    }

    // -- Back navigation -----------------------------------------------------------------------------

    @Test
    fun `OnBack always emits NavigateBack`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.eventFlow.test {
            viewModel.trySendAction(LoanApplyAction.OnBack)
            assertEquals(LoanApplyEvent.NavigateBack, awaitItem())
        }
    }

    // -- Submit ----------------------------------------------------------------------------------------

    @Test
    fun `OnSubmit with an incomplete form does not call applyLoan`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        // no member/product selected, requestedAmount blank

        viewModel.trySendAction(LoanApplyAction.OnSubmit)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, repository.applyLoanCallCount)
        assertFalse(viewModel.stateFlow.value.isSubmitting)
    }

    @Test
    fun `OnSubmit success posts the ApplyLoanRequest, sets submitSuccess and emits NavigateToMeetingConduct`() =
        runTest(testDispatcher) {
            repository.applyLoanResult = NetworkResult.Success(
                LoanApplicationResult(officeId = 1L, clientId = 501L, loanId = 9007L, resourceId = 9007L),
            )
            val viewModel = buildViewModel()
            testDispatcher.scheduler.advanceUntilIdle()
            fillValidForm(viewModel)

            viewModel.eventFlow.test {
                viewModel.trySendAction(LoanApplyAction.OnSubmit)
                assertEquals(LoanApplyEvent.NavigateToMeetingConduct(loanId = 9007L), awaitItem())
            }

            val state = viewModel.stateFlow.value
            assertFalse(state.isSubmitting)
            assertTrue(state.submitSuccess)
            assertNull(state.error)
            assertEquals(1, repository.applyLoanCallCount)
            assertEquals(501L, repository.lastApplyLoanRequest?.memberId)
            assertEquals(7L, repository.lastApplyLoanRequest?.productId)
            assertEquals(1500.0, repository.lastApplyLoanRequest?.amount)
            assertEquals(42L, repository.lastApplyLoanRequest?.groupId)
        }

    @Test
    fun `OnSubmit while offline sets Network error and emits ShowSnackbar without calling applyLoan`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        fillValidForm(viewModel)
        networkMonitor.setOnline(false)

        viewModel.eventFlow.test {
            viewModel.trySendAction(LoanApplyAction.OnSubmit)
            assertEquals(LoanApplyEvent.ShowSnackbar(message = "error_network"), awaitItem())
        }

        val state = viewModel.stateFlow.value
        assertEquals(LoanApplyError.Network, state.error)
        assertEquals(0, repository.applyLoanCallCount)
        assertFalse(state.isSubmitting)
    }

    @Test
    fun `OnSubmit BAD_REQUEST error maps to AmountExceedsEligibility`() = runTest(testDispatcher) {
        repository.applyLoanResult = NetworkResult.Error(NetworkError.BAD_REQUEST)
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        fillValidForm(viewModel)

        viewModel.trySendAction(LoanApplyAction.OnSubmit)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals(LoanApplyError.AmountExceedsEligibility, state.error)
        assertFalse(state.isSubmitting)
        assertFalse(state.submitSuccess)
    }

    @Test
    fun `OnSubmit UNAUTHORIZED error sets Auth error and emits ShowSnackbar`() = runTest(testDispatcher) {
        repository.applyLoanResult = NetworkResult.Error(NetworkError.UNAUTHORIZED)
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        fillValidForm(viewModel)

        viewModel.eventFlow.test {
            viewModel.trySendAction(LoanApplyAction.OnSubmit)
            assertEquals(LoanApplyEvent.ShowSnackbar(message = "error_auth"), awaitItem())
        }

        assertEquals(LoanApplyError.Auth, viewModel.stateFlow.value.error)
    }

    // -- Test fixtures / helpers ------------------------------------------------------------------

    private fun fillValidForm(viewModel: LoanApplyViewModel) {
        viewModel.trySendAction(LoanApplyAction.OnMemberSelected(sampleMember()))
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.trySendAction(LoanApplyAction.OnProductSelected(sampleProduct()))
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.trySendAction(LoanApplyAction.OnAmountChanged("1500"))
        testDispatcher.scheduler.advanceUntilIdle()
    }
}

private fun sampleMember(): GroupMember = GroupMember(
    id = 1L,
    displayName = "Peter Otieno",
    imagePresent = false,
    fineractClientId = 501L,
)

private fun sampleProduct(): LoanProduct = LoanProduct(
    id = 7L,
    name = "Group Loan",
    shortName = "GRP",
    principal = 10000.0,
    minPrincipal = 1000.0,
    maxPrincipal = 50000.0,
    numberOfRepayments = 12,
    interestRatePerPeriod = 2.5,
)

private fun sampleTemplate(
    groupCorpusBalance: Double = 24000.0,
    maxLoanAmount: Double = 50000.0,
    memberSavingsBalance: Double = 5000.0,
    loanMultiplier: Double = 3.0,
): LoanApplyTemplate = LoanApplyTemplate(
    products = listOf(sampleProduct()),
    principal = 10000.0,
    numberOfRepayments = 12,
    interestRatePerPeriod = 2.5,
    interestType = MemberStatus(id = 0, value = "Declining Balance"),
    amortizationType = MemberStatus(id = 1, value = "Equal installments"),
    repaymentEvery = 1,
    memberSavingsBalance = memberSavingsBalance,
    groupCorpusBalance = groupCorpusBalance,
    loanMultiplier = loanMultiplier,
    maxLoanAmount = maxLoanAmount,
)

private class FakeLoanApplyRepository : LoanApplyRepository {
    var getGroupMembersResult: NetworkResult<List<GroupMember>, NetworkError> = NetworkResult.Success(listOf(sampleMember()))
    var loadTemplateResult: NetworkResult<LoanApplyTemplate, NetworkError> = NetworkResult.Success(sampleTemplate())
    var applyLoanResult: NetworkResult<LoanApplicationResult, NetworkError> = NetworkResult.Success(
        LoanApplicationResult(officeId = 1L, clientId = 501L, loanId = 9001L, resourceId = 9001L),
    )

    var getGroupMembersCallCount: Int = 0
        private set
    var loadTemplateCallCount: Int = 0
        private set
    var lastLoadTemplateClientId: Long? = null
        private set
    var lastLoadTemplateProductId: Long? = null
        private set
    var applyLoanCallCount: Int = 0
        private set
    var lastApplyLoanRequest: ApplyLoanRequest? = null
        private set
    var lastApplyLoanProduct: LoanProduct? = null
        private set

    override suspend fun getGroupMembers(groupId: Long): NetworkResult<List<GroupMember>, NetworkError> {
        getGroupMembersCallCount++
        return getGroupMembersResult
    }

    override suspend fun loadTemplate(
        groupId: Long,
        clientId: Long,
        productId: Long,
    ): NetworkResult<LoanApplyTemplate, NetworkError> {
        loadTemplateCallCount++
        lastLoadTemplateClientId = clientId
        lastLoadTemplateProductId = productId
        return loadTemplateResult
    }

    override suspend fun applyLoan(
        request: ApplyLoanRequest,
        product: LoanProduct,
    ): NetworkResult<LoanApplicationResult, NetworkError> {
        applyLoanCallCount++
        lastApplyLoanRequest = request
        lastApplyLoanProduct = product
        return applyLoanResult
    }
}

private class FakeNetworkMonitor(initiallyOnline: Boolean) : NetworkMonitor {
    private val _isOnline = MutableStateFlow(initiallyOnline)
    override val isOnline: StateFlow<Boolean> = _isOnline
    override val networkStatus: StateFlow<NetworkStatus> = MutableStateFlow(
        if (initiallyOnline) NetworkStatus.Available(NetworkInfo()) else NetworkStatus.Unavailable,
    )
    override val networkChanges: SharedFlow<NetworkChangeEvent> = MutableSharedFlow<NetworkChangeEvent>().asSharedFlow()

    fun setOnline(online: Boolean) {
        _isOnline.value = online
    }

    override fun close() = Unit
}
