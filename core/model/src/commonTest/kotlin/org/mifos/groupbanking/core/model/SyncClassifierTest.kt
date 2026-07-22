/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * TDD RED-first coverage for the sync-status classifier (`SyncClassifier.kt`) — the resolution
 * of the SHIPPED [SyncQueueItem.operationType] `String` discriminator into the api.yaml-declared
 * [EntityType] / [SyncOperation] enum pair. See API.md#models — SyncClassifier.
 */
class SyncClassifierTest {

    // ---------- operationTypeToEntityType / operationTypeToSyncOperation — known mapping table ----------

    @Test
    fun loanRequest_classifiesAsLoanCreate() {
        assertEquals(EntityType.LOAN, operationTypeToEntityType("LOAN_REQUEST"))
        assertEquals(SyncOperation.CREATE, operationTypeToSyncOperation("LOAN_REQUEST"))
    }

    @Test
    fun createMember_classifiesAsMemberCreate() {
        assertEquals(EntityType.MEMBER, operationTypeToEntityType("CREATE_MEMBER"))
        assertEquals(SyncOperation.CREATE, operationTypeToSyncOperation("CREATE_MEMBER"))
    }

    @Test
    fun assignMemberRole_classifiesAsMemberUpdate() {
        assertEquals(EntityType.MEMBER, operationTypeToEntityType("ASSIGN_MEMBER_ROLE"))
        assertEquals(SyncOperation.UPDATE, operationTypeToSyncOperation("ASSIGN_MEMBER_ROLE"))
    }

    @Test
    fun uploadMemberPhoto_classifiesAsMemberUpdate() {
        assertEquals(EntityType.MEMBER, operationTypeToEntityType("UPLOAD_MEMBER_PHOTO"))
        assertEquals(SyncOperation.UPDATE, operationTypeToSyncOperation("UPLOAD_MEMBER_PHOTO"))
    }

    // ---------- prefix-based fallback for future/unlisted operationTypes ----------

    @Test
    fun unlistedMeetingOperation_classifiesByPrefix() {
        assertEquals(EntityType.MEETING, operationTypeToEntityType("RECORD_MEETING_ATTENDANCE_SUMMARY"))
    }

    @Test
    fun unlistedSavingsOperation_classifiesByPrefix() {
        assertEquals(EntityType.SAVINGS, operationTypeToEntityType("DEPOSIT_SAVINGS"))
    }

    @Test
    fun unlistedAttendanceOperation_classifiesByPrefix() {
        assertEquals(EntityType.ATTENDANCE, operationTypeToEntityType("MARK_ATTENDANCE"))
    }

    @Test
    fun unlistedShareOutOperation_classifiesByPrefix() {
        assertEquals(EntityType.SHARE_OUT, operationTypeToEntityType("TRIGGER_SHARE_OUT"))
    }

    @Test
    fun unlistedLoanOperation_classifiesByPrefix() {
        assertEquals(EntityType.LOAN, operationTypeToEntityType("APPROVE_LOAN"))
    }

    @Test
    fun completelyUnknownOperation_fallsBackToMemberUpdate() {
        // Documented fallback per SyncClassifier.kt kdoc — an operationType this classifier has
        // never seen must never crash the sync-status feature; it defaults to (MEMBER, UPDATE).
        assertEquals(EntityType.MEMBER, operationTypeToEntityType("SOME_FUTURE_OPERATION"))
        assertEquals(SyncOperation.UPDATE, operationTypeToSyncOperation("SOME_FUTURE_OPERATION"))
    }

    // ---------- pendingByType ----------

    private fun item(id: Long, operationType: String, status: SyncStatus) = SyncQueueItem(
        id = id,
        operationType = operationType,
        payloadJson = "{}",
        targetTable = "dt_ignored",
        status = status,
        createdAtEpochMs = 0L,
        lastAttemptEpochMs = null,
        attemptCount = 0,
        lastError = null,
    )

    @Test
    fun pendingByType_groupsOnlyPendingItemsByClassifiedEntityType() {
        val items = listOf(
            item(1L, "LOAN_REQUEST", SyncStatus.PENDING),
            item(2L, "LOAN_REQUEST", SyncStatus.PENDING),
            item(3L, "CREATE_MEMBER", SyncStatus.PENDING),
            item(4L, "CREATE_MEMBER", SyncStatus.SYNCED), // not pending — excluded
            item(5L, "ASSIGN_MEMBER_ROLE", SyncStatus.FAILED), // not pending — excluded
        )
        val result = pendingByType(items)
        assertEquals(2, result[EntityType.LOAN])
        assertEquals(1, result[EntityType.MEMBER])
    }

    @Test
    fun pendingByType_emptyInputProducesEmptyMap() {
        assertEquals(emptyMap(), pendingByType(emptyList()))
    }

    @Test
    fun pendingByType_noPendingItemsProducesEmptyMap() {
        val items = listOf(item(1L, "LOAN_REQUEST", SyncStatus.SYNCED))
        assertEquals(emptyMap(), pendingByType(items))
    }
}
