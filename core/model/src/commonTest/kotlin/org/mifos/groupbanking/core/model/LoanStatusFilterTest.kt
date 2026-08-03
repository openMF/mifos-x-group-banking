/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * TDD RED-first coverage for [LoanStatusFilter.matches] — the client-side filter predicate reused
 * by both loan-list and personal-loans (`ui.yaml#components.filter_chips_row` /
 * `filter_chips_row`). See API.md#models — LoanStatusFilter.
 */
class LoanStatusFilterTest {

    @Test
    fun all_matchesEveryStatus() {
        LoanAccountStatus.entries.forEach { status ->
            assertTrue(LoanStatusFilter.ALL.matches(loan(status)), "ALL must match $status")
        }
    }

    @Test
    fun active_matchesOnlyActiveStatus() {
        assertTrue(LoanStatusFilter.ACTIVE.matches(loan(LoanAccountStatus.ACTIVE)))
        assertFalse(LoanStatusFilter.ACTIVE.matches(loan(LoanAccountStatus.CLOSED)))
        assertFalse(LoanStatusFilter.ACTIVE.matches(loan(LoanAccountStatus.OVERDUE)))
        assertFalse(LoanStatusFilter.ACTIVE.matches(loan(LoanAccountStatus.PENDING)))
    }

    @Test
    fun overdue_matchesOnlyOverdueStatus() {
        assertTrue(LoanStatusFilter.OVERDUE.matches(loan(LoanAccountStatus.OVERDUE)))
        assertFalse(LoanStatusFilter.OVERDUE.matches(loan(LoanAccountStatus.ACTIVE)))
        assertFalse(LoanStatusFilter.OVERDUE.matches(loan(LoanAccountStatus.CLOSED)))
    }

    @Test
    fun closed_matchesOnlyClosedStatus() {
        assertTrue(LoanStatusFilter.CLOSED.matches(loan(LoanAccountStatus.CLOSED)))
        assertFalse(LoanStatusFilter.CLOSED.matches(loan(LoanAccountStatus.ACTIVE)))
        assertFalse(LoanStatusFilter.CLOSED.matches(loan(LoanAccountStatus.REJECTED)))
    }

    private fun loan(status: LoanAccountStatus): LoanSummary = LoanSummary(
        id = 1L,
        memberId = 1L,
        memberName = "Test Member",
        memberPhotoUrl = null,
        loanProductName = "Personal Loan",
        principalAmount = 1000.0,
        outstandingBalance = 500.0,
        overdueAmount = 0.0,
        status = status,
        nextRepaymentDate = null,
        isOverdue = status == LoanAccountStatus.OVERDUE,
        fineractLoanId = 1L,
    )
}
