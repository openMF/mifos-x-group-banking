/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.store.meetingsummary.impl

import kotlinx.coroutines.flow.map
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.base.store.infra.DefaultValidator
import kpt.core.base.store.infra.StoreFactory
import kpt.core.store.AppStoreRegistry
import org.mifos.groupbanking.core.database.meetingsummary.dao.MeetingRecordDao
import org.mifos.groupbanking.core.database.meetingsummary.entity.CachedLoanSummaryItem
import org.mifos.groupbanking.core.database.meetingsummary.entity.CachedSavingsBreakdownItem
import org.mifos.groupbanking.core.database.meetingsummary.entity.MeetingRecordCacheCodec
import org.mifos.groupbanking.core.database.meetingsummary.entity.MeetingRecordCacheEntity
import org.mifos.groupbanking.core.model.LoanSummaryItem
import org.mifos.groupbanking.core.model.MeetingSummaryData
import org.mifos.groupbanking.core.model.SavingsBreakdownItem
import org.mifos.groupbanking.core.network.mapper.toDomainModel
import org.mifos.groupbanking.core.network.service.meetingsummary.MeetingRecordApi
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.Store
import kotlin.time.Clock

/**
 * Builds the **single-key** read-only NETWORK_WITH_CACHE [Store] for the meeting-summary screen
 * (`GET /datatables/dt_meeting_record/{centerId}?meetingNumber=N`) that backs the read-only
 * post-meeting summary.
 *
 * The store key is the composite `"$centerId:$meetingNumber"` String and the value is one
 * [MeetingSummaryData] snapshot (persisted totals + per-member savings breakdown + per-member loan
 * activity) resolved by the single endpoint in one round trip; Store5 caches each meeting
 * independently, so re-opening a completed meeting re-uses that meeting's per-key cache or re-fetches
 * if stale. The read side is exposed to the UI exclusively through
 * `MeetingSummaryRepository.meetingSummaryStream(...)` → `.asScreenStream(...)` — there is no
 * DAO-bypass read path (RULE-IMPLEMENT-STORE5-001 S5-2), and the meeting-summary screen is read-only
 * (`data-flow.yaml` declares no write) so there is no write path (S5-1).
 *
 * - **Fetcher** — [MeetingRecordApi.getMeetingRecord] with `centerId` + `meetingNumber` decoded from
 *   the composite store key. The service returns a sealed [NetworkResult]; on
 *   [NetworkResult.Success] the DTO is mapped to domain via [toDomainModel], on [NetworkResult.Error]
 *   the fetcher throws so Store5 routes it to an error response (no try-catch, no `Result` envelope).
 * - **SourceOfTruth** — a Room table ([MeetingRecordDao]) so each meeting's summary survives process
 *   death and a cold start with no network still renders the last-seen totals
 *   (`data-flow.yaml#error_paths[network.offline]: fallback_cache`, SC2 — never memory-only). The
 *   whole snapshot is one row per meeting, so the writer's keyed [MeetingRecordDao.replaceForKey]
 *   upsert is inherently atomic (guards S5-3); it then calls [DefaultValidator.markFresh] so the TTL
 *   window opens on the successful network write (S5-5 cold-start-stale guard).
 * - **Validator** — TTL 10m ([AppStoreRegistry.Ttl.MEETING_SUMMARY]) matching `data-flow.yaml`
 *   `cache.strategy: stale_while_revalidate`, `ttl_seconds: 600`.
 *
 * See API.md#stores — MeetingSummaryData.
 */
fun provideMeetingSummaryStore(
    api: MeetingRecordApi,
    dao: MeetingRecordDao,
): Store<String, MeetingSummaryData> {
    val validator = DefaultValidator.withTtl<MeetingSummaryData>(
        AppStoreRegistry.Ttl.MEETING_SUMMARY,
    )
    return StoreFactory.createStore(
        fetcher = Fetcher.of { key: String ->
            val (centerId, meetingNumber) = key.decodeMeetingKey()
            when (val result = api.getMeetingRecord(centerId = centerId, meetingNumber = meetingNumber)) {
                is NetworkResult.Success -> result.data.toDomainModel()
                // 404 = this meeting has NO record because it was never conducted (a scheduled/past
                // slot with no dt_meeting_record — e.g. a synthesized VSLA calendar date). Surface an
                // empty "not-conducted" snapshot as Content rather than throwing to Store5's error
                // channel, so the review screen shows a zero-activity meeting instead of a misleading
                // "Server error" (same tolerant-read pattern as MeetingConductRepositoryImpl's
                // getPreviousMeetingRecord 404 → null). Every OTHER error still throws.
                is NetworkResult.Error ->
                    if (result.error == NetworkError.NOT_FOUND) {
                        emptyNotConductedSummary(centerId, meetingNumber)
                    } else {
                        throw MeetingRecordFetchException(result.error)
                    }
            }
        },
        sourceOfTruth = SourceOfTruth.of(
            reader = { key: String ->
                dao.observeByKey(key).map { row -> row?.toDomain() }
            },
            writer = { key: String, data: MeetingSummaryData ->
                dao.replaceForKey(data.toEntity(key))
                validator.markFresh()
            },
            delete = { key: String -> dao.deleteByKey(key) },
            deleteAll = { dao.deleteAll() },
        ),
        validator = validator,
    )
}

/**
 * Builds the composite store key from its parts. Kept next to [decodeMeetingKey] so the encode /
 * decode pair is the single source of truth for the `"$centerId:$meetingNumber"` key shape.
 */
fun encodeMeetingKey(centerId: Int, meetingNumber: Int): String = "$centerId:$meetingNumber"

/** Splits the composite `"$centerId:$meetingNumber"` store key back into its parts. */
private fun String.decodeMeetingKey(): Pair<Int, Int> {
    val parts = split(":")
    val centerId = parts.getOrNull(0)?.toIntOrNull() ?: 0
    val meetingNumber = parts.getOrNull(1)?.toIntOrNull() ?: 0
    return centerId to meetingNumber
}

/**
 * Signals a failed meeting-record fetch to Store5's error channel. Carries the sealed [NetworkError]
 * so downstream error mapping (feature-layer) can branch on the exact cause (401 → login, 404 →
 * record-not-found → cache fallback per `data-flow.yaml`); the message is `categorize()`-friendly.
 */
class MeetingRecordFetchException(
    val networkError: NetworkError,
) : Exception("Meeting record fetch failed: $networkError")

/**
 * The zero-activity [MeetingSummaryData] returned for a meeting with no `dt_meeting_record` (a
 * scheduled/past slot that was never conducted — a 404 from the record read). [actualDate] is blank
 * (the signal a consumer can branch on to render a "not conducted yet" note); every total is 0 and
 * both breakdown lists are empty. Carries the [meetingNumber] so the review header still reads
 * "Meeting #N". [meetingId] uses the `"{centerId}-{meetingNumber}"` calendar-key shape.
 */
internal fun emptyNotConductedSummary(centerId: Int, meetingNumber: Int): MeetingSummaryData =
    MeetingSummaryData(
        meetingId = "$centerId-$meetingNumber",
        meetingNumber = meetingNumber,
        actualDate = "",
        attendanceCount = 0,
        totalMemberCount = 0,
        groupSavingsCollected = 0L,
        individualSavingsCollected = 0L,
        totalSavingsCollected = 0L,
        loansDisbursed = 0L,
        loansRepaid = 0L,
        finesCollected = 0L,
        openingCorpus = 0L,
        closingCorpus = 0L,
        savingsBreakdown = emptyList<SavingsBreakdownItem>(),
        loanItems = emptyList<LoanSummaryItem>(),
    )

// ---------------------------------------------------------------------------
// Inline entity <-> domain mapping — private to this store (LoanDetailStore precedent). core/store
// depends on core/model + core/database, so mapping lives here rather than adding a core/model
// dependency to core/database. The two nested lists round-trip through MeetingRecordCacheCodec
// (core/database owns kotlinx-serialization; core/store does not).
// ---------------------------------------------------------------------------

private fun MeetingSummaryData.toEntity(cacheKey: String): MeetingRecordCacheEntity {
    val now = Clock.System.now().toEpochMilliseconds()
    return MeetingRecordCacheEntity(
        cacheKey = cacheKey,
        centerId = cacheKey.substringBefore(":").toIntOrNull() ?: 0,
        meetingId = meetingId,
        meetingNumber = meetingNumber,
        actualDate = actualDate,
        attendanceCount = attendanceCount,
        totalMemberCount = totalMemberCount,
        groupSavingsCollected = groupSavingsCollected,
        individualSavingsCollected = individualSavingsCollected,
        totalSavingsCollected = totalSavingsCollected,
        loansDisbursed = loansDisbursed,
        loansRepaid = loansRepaid,
        finesCollected = finesCollected,
        openingCorpus = openingCorpus,
        closingCorpus = closingCorpus,
        savingsBreakdownJson = MeetingRecordCacheCodec.encodeSavings(
            savingsBreakdown.map { it.toPayload() },
        ),
        loanItemsJson = MeetingRecordCacheCodec.encodeLoans(
            loanItems.map { it.toPayload() },
        ),
        fetchedAt = now,
    )
}

private fun MeetingRecordCacheEntity.toDomain(): MeetingSummaryData = MeetingSummaryData(
    meetingId = meetingId,
    meetingNumber = meetingNumber,
    actualDate = actualDate,
    attendanceCount = attendanceCount,
    totalMemberCount = totalMemberCount,
    groupSavingsCollected = groupSavingsCollected,
    individualSavingsCollected = individualSavingsCollected,
    totalSavingsCollected = totalSavingsCollected,
    loansDisbursed = loansDisbursed,
    loansRepaid = loansRepaid,
    finesCollected = finesCollected,
    openingCorpus = openingCorpus,
    closingCorpus = closingCorpus,
    savingsBreakdown = MeetingRecordCacheCodec.decodeSavings(savingsBreakdownJson).map { it.toDomain() },
    loanItems = MeetingRecordCacheCodec.decodeLoans(loanItemsJson).map { it.toDomain() },
)

private fun SavingsBreakdownItem.toPayload(): CachedSavingsBreakdownItem = CachedSavingsBreakdownItem(
    memberId = memberId,
    memberName = memberName,
    groupSavings = groupSavings,
    individualSavings = individualSavings,
)

private fun CachedSavingsBreakdownItem.toDomain(): SavingsBreakdownItem = SavingsBreakdownItem(
    memberId = memberId,
    memberName = memberName,
    groupSavings = groupSavings,
    individualSavings = individualSavings,
)

private fun LoanSummaryItem.toPayload(): CachedLoanSummaryItem = CachedLoanSummaryItem(
    memberId = memberId,
    memberName = memberName,
    amountDisbursed = amountDisbursed,
    amountRepaid = amountRepaid,
    outstandingAfter = outstandingAfter,
)

private fun CachedLoanSummaryItem.toDomain(): LoanSummaryItem = LoanSummaryItem(
    memberId = memberId,
    memberName = memberName,
    amountDisbursed = amountDisbursed,
    amountRepaid = amountRepaid,
    outstandingAfter = outstandingAfter,
)
