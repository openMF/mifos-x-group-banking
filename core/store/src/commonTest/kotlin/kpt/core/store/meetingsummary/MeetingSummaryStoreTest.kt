/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.store.meetingsummary

import kpt.core.store.meetingsummary.impl.emptyNotConductedSummary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Coverage for the not-conducted meeting snapshot the store's fetcher returns on a `404` from the
 * meeting-record read (a scheduled/past slot with no `dt_meeting_record`) — the tolerant-read fix
 * that makes the previous-meeting-review render a "not conducted yet" state instead of a hard
 * "Server error". A blank [actualDate] is the signal the review screen branches on.
 */
class MeetingSummaryStoreTest {

    @Test
    fun emptyNotConductedSummary_hasBlankDate_zeroTotals_andEmptyBreakdowns() {
        val summary = emptyNotConductedSummary(groupId = 24, meetingNumber = 1)

        // The blank actualDate is the "not conducted" signal the previous-review screen keys on.
        assertTrue(summary.actualDate.isBlank(), "actualDate must be blank for a not-conducted meeting")
        assertEquals(1, summary.meetingNumber)
        assertEquals("24-1", summary.meetingId)
        // Every monetary/count total is zero — nothing happened at a meeting that was never held.
        assertEquals(0, summary.attendanceCount)
        assertEquals(0, summary.totalMemberCount)
        assertEquals(0L, summary.totalSavingsCollected)
        assertEquals(0L, summary.groupSavingsCollected)
        assertEquals(0L, summary.individualSavingsCollected)
        assertEquals(0L, summary.loansDisbursed)
        assertEquals(0L, summary.loansRepaid)
        assertEquals(0L, summary.finesCollected)
        assertEquals(0L, summary.openingCorpus)
        assertEquals(0L, summary.closingCorpus)
        assertTrue(summary.savingsBreakdown.isEmpty())
        assertTrue(summary.loanItems.isEmpty())
    }

    @Test
    fun emptyNotConductedSummary_carriesMeetingIdentityForTheReviewHeader() {
        val summary = emptyNotConductedSummary(groupId = 100, meetingNumber = 7)
        assertEquals("100-7", summary.meetingId)
        assertEquals(7, summary.meetingNumber)
    }
}
