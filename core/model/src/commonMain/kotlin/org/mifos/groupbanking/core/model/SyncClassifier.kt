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

/**
 * Bridges the SHIPPED [SyncQueueItem.operationType] generic `String` discriminator to the
 * api.yaml-declared [EntityType] / [SyncOperation] enum pair (`api.yaml#dtos.SyncQueueItem`
 * models these as first-class columns; the shipped queue schema deliberately keeps a single
 * generic column instead — see [EntityType] kdoc, `BatchSync.kt`, for the full registry-divergence
 * rationale). Pure functions, no I/O, no wire dependency — safe to call from both `core/data`
 * repositories and `feature/sync-status` ViewModels.
 *
 * **Known mapping table** (the enqueue call-sites this project ships today):
 *
 * | `operationType` | [EntityType] | [SyncOperation] | enqueued by |
 * |---|---|---|---|
 * | `"LOAN_REQUEST"` | [EntityType.LOAN] | [SyncOperation.CREATE] | loan-request's offline enqueue |
 * | `"CREATE_MEMBER"` | [EntityType.MEMBER] | [SyncOperation.CREATE] | member-add's create-chain |
 * | `"ASSIGN_MEMBER_ROLE"` | [EntityType.MEMBER] | [SyncOperation.UPDATE] | member-add's create-chain |
 * | `"UPLOAD_MEMBER_PHOTO"` | [EntityType.MEMBER] | [SyncOperation.UPDATE] | member-add's create-chain |
 *
 * **Fallback for any operationType outside the table above** (forward-compatible with future
 * mutation features that enqueue without updating this classifier): [operationTypeToEntityType]
 * falls back to a case-insensitive PREFIX match against the operationType string —
 * `contains("LOAN")` -> [EntityType.LOAN], `contains("MEETING")` -> [EntityType.MEETING],
 * `contains("SAVINGS")` -> [EntityType.SAVINGS], `contains("ATTENDANCE")` -> [EntityType.ATTENDANCE],
 * `contains("SHARE_OUT")` -> [EntityType.SHARE_OUT], `contains("MEMBER")` -> [EntityType.MEMBER];
 * if NONE of those match, the documented default is [EntityType.MEMBER] (confirmed gap — no
 * `api.yaml` value covers a genuinely-unclassifiable operationType, MEMBER chosen as the least
 * surprising bucket since every shipped enqueue call-site today is member-adjacent).
 * [operationTypeToSyncOperation] falls back to a `"CREATE_"`/`"UPDATE_"`/`"ASSIGN_"`/`"UPLOAD_"`/
 * `"DELETE_"`/`"REMOVE_"` prefix match; if none match, the documented default is
 * [SyncOperation.UPDATE] (same "least surprising bucket" rationale — an unrecognized mutation is
 * more likely a follow-up update than a destructive delete).
 *
 * See API.md#models — SyncClassifier.
 */
fun operationTypeToEntityType(operationType: String): EntityType = when {
    operationType == "LOAN_REQUEST" -> EntityType.LOAN
    operationType == "CREATE_MEMBER" -> EntityType.MEMBER
    operationType == "ASSIGN_MEMBER_ROLE" -> EntityType.MEMBER
    operationType == "UPLOAD_MEMBER_PHOTO" -> EntityType.MEMBER
    operationType.contains("LOAN") -> EntityType.LOAN
    operationType.contains("MEETING") -> EntityType.MEETING
    operationType.contains("SAVINGS") -> EntityType.SAVINGS
    operationType.contains("ATTENDANCE") -> EntityType.ATTENDANCE
    operationType.contains("SHARE_OUT") -> EntityType.SHARE_OUT
    operationType.contains("MEMBER") -> EntityType.MEMBER
    else -> EntityType.MEMBER // documented fallback — see kdoc above
}

/** See [operationTypeToEntityType] kdoc for the full mapping table + fallback rationale. */
fun operationTypeToSyncOperation(operationType: String): SyncOperation = when {
    operationType == "LOAN_REQUEST" -> SyncOperation.CREATE
    operationType == "CREATE_MEMBER" -> SyncOperation.CREATE
    operationType == "ASSIGN_MEMBER_ROLE" -> SyncOperation.UPDATE
    operationType == "UPLOAD_MEMBER_PHOTO" -> SyncOperation.UPDATE
    operationType.startsWith("CREATE_") -> SyncOperation.CREATE
    operationType.startsWith("DELETE_") || operationType.startsWith("REMOVE_") -> SyncOperation.DELETE
    operationType.startsWith("UPDATE_") ||
        operationType.startsWith("ASSIGN_") ||
        operationType.startsWith("UPLOAD_") -> SyncOperation.UPDATE
    else -> SyncOperation.UPDATE // documented fallback — see operationTypeToEntityType kdoc
}

/**
 * Groups every PENDING [SyncQueueItem] by its classified [EntityType] — backs the sync-status
 * screen's per-entity pending-count badges (`api.yaml#dependencies.repositories.SyncQueueRepository.getPendingByType`).
 * Non-pending rows (SYNCING/FAILED/SYNCED) are excluded; an [EntityType] with zero pending rows is
 * simply absent from the returned map (never a `0`-valued entry).
 *
 * See API.md#models — SyncClassifier.
 */
fun pendingByType(items: List<SyncQueueItem>): Map<EntityType, Int> = items
    .asSequence()
    .filter { it.status == SyncStatus.PENDING }
    .groupingBy { operationTypeToEntityType(it.operationType) }
    .eachCount()
