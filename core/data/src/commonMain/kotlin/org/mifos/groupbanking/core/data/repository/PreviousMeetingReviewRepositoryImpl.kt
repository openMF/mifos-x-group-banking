/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.data.repository

import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kpt.core.base.store.infra.FetchedAtRepository
import kpt.core.base.store.screen.FetchPolicy
import kpt.core.base.store.screen.ScreenDataStream
import kpt.core.base.store.screen.ScreenState
import kpt.core.base.store.screen.asScreenStream
import kpt.core.base.store.screen.combineContent
import kpt.core.store.AppStoreRegistry
import org.mifos.groupbanking.core.model.AttendanceRecord
import org.mifos.groupbanking.core.model.MeetingSummaryData
import org.mifos.groupbanking.core.model.PreviousMeetingDetail
import org.mifos.groupbanking.core.model.UnresolvedItem
import org.mifos.groupbanking.core.model.UnresolvedType
import org.mobilenativefoundation.store.store5.Store

/**
 * Store5-backed implementation of [PreviousMeetingReviewRepository].
 *
 * REUSES [MeetingSummaryRepository.meetingSummaryStream] for the meeting record composite (totals +
 * savings + loans) — no second record store, DTO, or DAO is built — and wraps the NEW single-key
 * NETWORK_WITH_CACHE [meetingAttendanceStore] via [Store.asScreenStream] for the per-member
 * attendance roster. The two offline-first `ScreenState` streams are merged with [combineContent]:
 * the record stream drives the render lifecycle (Loading / Content / Error / NoNetwork /
 * Unauthenticated) and each Content is enriched with the latest attendance roster + the derived
 * unresolved items. Attendance that has not yet resolved contributes an empty list, so the record
 * still renders (graceful merge). No DAO-bypass read, no `try-catch`, no `Result` envelope
 * (RULE-IMPLEMENT-STORE5-001 S5-2).
 *
 * See API.md#stores — PreviousMeetingDetail.
 */
class PreviousMeetingReviewRepositoryImpl(
    private val meetingSummaryRepository: MeetingSummaryRepository,
    private val meetingAttendanceStore: Store<String, List<AttendanceRecord>>,
    private val networkMonitor: NetworkMonitor,
    private val fetchedAtRepository: FetchedAtRepository,
) : PreviousMeetingReviewRepository {

    override fun previousMeetingStream(
        centerId: Int,
        meetingNumber: Int,
        meetingId: String,
        scope: CoroutineScope,
    ): PreviousMeetingReviewStream {
        // Reused record read (offline-first) — totals + savings + loans.
        val recordStream: ScreenDataStream<MeetingSummaryData> =
            meetingSummaryRepository.meetingSummaryStream(
                centerId = centerId,
                meetingNumber = meetingNumber,
                scope = scope,
            )

        // New per-member attendance read (offline-first), keyed by meetingId.
        val attendanceStream: ScreenDataStream<List<AttendanceRecord>> =
            meetingAttendanceStore.asScreenStream(
                key = meetingId,
                networkMonitor = networkMonitor,
                fetchedAtRepository = fetchedAtRepository,
                cacheKey = "$ATTENDANCE_CACHE_KEY_PREFIX$meetingId",
                scope = scope,
                // An empty roster is a valid Content (attendance table may be absent) — never Empty.
                isEmpty = { false },
                fetchPolicy = FetchPolicy.CACHE_FIRST_SWR,
                ttl = AppStoreRegistry.Ttl.MEETING_ATTENDANCE,
            )

        // Attendance content projected to a plain Flow for the combineContent merge — a not-yet-Content
        // attendance state contributes an empty list so the record still renders.
        val attendanceContent: Flow<List<AttendanceRecord>> = attendanceStream.state.map { s ->
            if (s is ScreenState.Content) s.data else emptyList()
        }

        val merged: Flow<ScreenState<PreviousMeetingDetail>> =
            recordStream.state.combineContent(attendanceContent) { summary, attendance, _ ->
                PreviousMeetingDetail(
                    summary = summary,
                    attendanceRecords = attendance,
                    unresolvedItems = deriveUnresolvedItems(attendance),
                )
            }

        return PreviousMeetingReviewStream(
            state = merged,
            onRetry = {
                recordStream.retry()
                attendanceStream.retry()
            },
        )
    }

    private companion object {
        /** FetchedAtRepository key prefix — one freshness timestamp per meeting's attendance roster. */
        const val ATTENDANCE_CACHE_KEY_PREFIX = "previousmeetingreview:attendance:"

        /**
         * Derives the unresolved carry-over items surfaced in `ui.yaml#unresolved_alert_card` from the
         * attendance roster. Business rule (matches `demo-data.yaml#UnresolvedItem`): every member who
         * incurred a fine that has not been collected is an [UnresolvedType.UNPAID_FINE]. Pure — no I/O.
         *
         * (The `get_pending_loan_votes` datatable is the additional source for
         * [UnresolvedType.PENDING_LOAN_VOTE]; it is a per-loanId lookup with no closed loan-id set on
         * this read path, so that source is a flagged idea-layer follow-up — see the drain-request.)
         */
        fun deriveUnresolvedItems(attendance: List<AttendanceRecord>): List<UnresolvedItem> =
            attendance
                .filter { it.fineAmount > 0L }
                .map { record ->
                    UnresolvedItem(
                        type = UnresolvedType.UNPAID_FINE,
                        description = "${record.memberName} — fine KES ${record.fineAmount} not collected",
                        memberId = record.memberId,
                    )
                }
    }
}
