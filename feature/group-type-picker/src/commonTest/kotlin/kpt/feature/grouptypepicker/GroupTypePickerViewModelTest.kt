/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.grouptypepicker

import app.cash.turbine.test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kpt.core.analytics.KptAnalyticsTracker
import kpt.core.base.analytics.NoOpAnalyticsHelper
import kpt.core.base.observability.ConsoleCrashReporter
import kpt.core.base.store.screen.ExperimentalScreenDataStreamTestingApi
import kpt.core.base.store.screen.FetchPolicy
import kpt.core.base.store.screen.ScreenDataStream
import kpt.core.base.store.screen.ScreenState
import kpt.core.base.store.screen.screenDataStreamForTesting
import kpt.core.data.repository.GroupTypeConfigRepository
import kpt.core.model.ContributionMode
import kpt.core.model.GroupTypeConfig
import kpt.core.model.GroupTypeSlug
import kpt.core.model.SavingsMechanism
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * See API.md#viewmodel — GroupTypePickerViewModelTest exercises every declared
 * [GroupTypePickerAction] path plus the [ScreenState]→[GroupTypePickerState] mapping,
 * per RULE-TDD-METHODOLOGY-001 / RULE-IMPL-DEAD-CLICKABLE-001.
 */
@OptIn(ExperimentalScreenDataStreamTestingApi::class)
class GroupTypePickerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: FakeGroupTypeConfigRepository
    private lateinit var viewModel: GroupTypePickerViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeGroupTypeConfigRepository()
        viewModel = GroupTypePickerViewModel(
            repository = repository,
            analytics = KptAnalyticsTracker(NoOpAnalyticsHelper()),
            crashReporter = ConsoleCrashReporter(),
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is loading with no configs and no error`() = runTest(testDispatcher) {
        // stream is still ScreenState.Loading (fake default) before the collector settles
        val state = viewModel.stateFlow.value
        assertTrue(state.isLoading)
        assertNull(state.error)
        assertTrue(state.typeConfigs.isEmpty())
    }

    @Test
    fun `stream Content maps to typeConfigs and clears loading`() = runTest(testDispatcher) {
        repository.emit(ScreenState.Content(data = listOf(vsla(), rosca())))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals(false, state.isLoading)
        assertNull(state.error)
        assertEquals(2, state.typeConfigs.size)
    }

    @Test
    fun `stream NoNetwork maps to Network error`() = runTest(testDispatcher) {
        repository.emit(ScreenState.NoNetwork())
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals(GroupTypePickerError.Network, state.error)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun `stream Unauthenticated maps to Auth error which is non-retryable`() = runTest(testDispatcher) {
        repository.emit(ScreenState.Unauthenticated)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertEquals(GroupTypePickerError.Auth, state.error)
        assertEquals(false, state.error?.retry)
    }

    @Test
    fun `stream Error with isNetworkError false maps to Server error`() = runTest(testDispatcher) {
        repository.emit(ScreenState.Error(error = IllegalStateException("boom"), isNetworkError = false))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(GroupTypePickerError.Server, viewModel.stateFlow.value.error)
    }

    @Test
    fun `OnTypeCardTap with known slug sets selectedType and emits NavigateToGroupCreate`() = runTest(testDispatcher) {
        repository.emit(ScreenState.Content(data = listOf(vsla(), rosca())))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.eventFlow.test {
            viewModel.trySendAction(GroupTypePickerAction.OnTypeCardTap("VSLA"))
            val event = awaitItem()
            assertTrue(event is GroupTypePickerEvent.NavigateToGroupCreate)
            assertEquals(GroupTypeSlug.VSLA, (event as GroupTypePickerEvent.NavigateToGroupCreate).typeConfig.typeSlug)
        }
        assertEquals(GroupTypeSlug.VSLA, viewModel.stateFlow.value.selectedType?.typeSlug)
    }

    @Test
    fun `OnTypeCardTap with unresolved slug does not navigate or crash`() = runTest(testDispatcher) {
        repository.emit(ScreenState.Content(data = listOf(vsla())))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(GroupTypePickerAction.OnTypeCardTap("NOT_A_REAL_SLUG"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.stateFlow.value.selectedType)
    }

    @Test
    fun `OnBack emits NavigateBack`() = runTest(testDispatcher) {
        viewModel.eventFlow.test {
            viewModel.trySendAction(GroupTypePickerAction.OnBack)
            assertEquals(GroupTypePickerEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `OnRetry re-dispatches the underlying stream fetch`() = runTest(testDispatcher) {
        repository.emit(ScreenState.NoNetwork())
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.trySendAction(GroupTypePickerAction.OnRetry)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.retryCount)
    }
}

private fun vsla(): GroupTypeConfig = GroupTypeConfig(
    typeSlug = GroupTypeSlug.VSLA,
    displayName = "Village Savings & Loan Association",
    tagline = "Save in shares each meeting; borrow up to 3x your shares; share-out at year end",
    savingsMechanism = SavingsMechanism.ACCUMULATING,
    contributionMode = ContributionMode.SHARE_BASED_VARIABLE,
    lendingEnabled = true,
    hasSocialFund = true,
    hasBankLinkage = false,
    welfareOnlyMode = false,
    formallyRegistered = false,
    defaultLoanMultiplier = 3.0,
    defaultInterestRatePct = 10.0,
    defaultCycleLengthMonths = 12,
    maxMembers = 30,
    minMembers = 5,
)

private fun rosca(): GroupTypeConfig = GroupTypeConfig(
    typeSlug = GroupTypeSlug.ROSCA,
    displayName = "Rotating Savings & Credit Association",
    tagline = "Fixed pot rotates to one member each period",
    savingsMechanism = SavingsMechanism.ROTATING_PAYOUT,
    contributionMode = ContributionMode.FIXED,
    lendingEnabled = false,
    hasSocialFund = false,
    hasBankLinkage = false,
    welfareOnlyMode = false,
    formallyRegistered = false,
    defaultLoanMultiplier = 0.0,
    defaultInterestRatePct = 0.0,
    defaultCycleLengthMonths = 12,
    maxMembers = 20,
    minMembers = 5,
)

/**
 * In-memory [GroupTypeConfigRepository] fake — wraps [screenDataStreamForTesting] so the
 * ViewModel's real `.state.collect { ... }` wiring is exercised, and counts [retryCount] by
 * collecting the same [refreshTrigger] the production `ScreenDataStream.retry()` emits into.
 */
private class FakeGroupTypeConfigRepository : GroupTypeConfigRepository {

    private val stateFlow = MutableStateFlow<ScreenState<List<GroupTypeConfig>>>(ScreenState.Loading)
    private val refreshTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    var retryCount: Int = 0
        private set

    fun emit(screenState: ScreenState<List<GroupTypeConfig>>) {
        stateFlow.value = screenState
    }

    override fun groupTypeConfigsStream(
        scope: CoroutineScope,
        fetchPolicy: FetchPolicy,
    ): ScreenDataStream<List<GroupTypeConfig>> {
        scope.launch { refreshTrigger.collect { retryCount++ } }
        return screenDataStreamForTesting(
            state = stateFlow,
            refreshTrigger = refreshTrigger,
        )
    }
}
