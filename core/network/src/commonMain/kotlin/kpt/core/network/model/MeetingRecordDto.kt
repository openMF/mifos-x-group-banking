/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire DTO for one completed meeting record
 * (`GET /datatables/dt_meeting_record/{groupId}?meetingNumber=N`) — the persisted meeting totals
 * plus the per-member savings breakdown and per-member loan activity, resolved in one round trip.
 * Generated from meeting-summary's own approved `api.yaml#dtos.MeetingRecordDetail`; the companion
 * bridge normalizes Fineract's raw `dt_meeting_record` datatable row before this client's Ktor
 * service ever sees it (same precedent as `LoanDetailResponseDto`).
 *
 * Money fields are `Long` (whole KES per `api.yaml#dtos`); [attendanceCount] / [totalMemberCount] /
 * [meetingNumber] are `Int`. See API.md#dtos — MeetingRecordDetail.
 *
 * Named `MeetingSummaryRecordDto` (not `MeetingRecordDetailDto`) to deconflict with the
 * `meeting-conduct` feature's own partial `MeetingRecordDetailDto` projection in
 * `MeetingConductDto.kt` — two `@Serializable` classes of the same name in one package collide at
 * the serialization compiler plugin. This is meeting-summary's full read-model shape.
 */
@Serializable
data class MeetingSummaryRecordDto(
    @SerialName("meetingId") val meetingId: String,
    @SerialName("meetingNumber") val meetingNumber: Int,
    @SerialName("actualDate") val actualDate: String,
    @SerialName("meetingTime") val meetingTime: String = "",
    @SerialName("attendanceCount") val attendanceCount: Int,
    @SerialName("totalMemberCount") val totalMemberCount: Int,
    @SerialName("groupSavingsCollected") val groupSavingsCollected: Long,
    @SerialName("individualSavingsCollected") val individualSavingsCollected: Long,
    @SerialName("totalSavingsCollected") val totalSavingsCollected: Long,
    @SerialName("loansDisbursed") val loansDisbursed: Long,
    @SerialName("loansRepaid") val loansRepaid: Long,
    @SerialName("finesCollected") val finesCollected: Long,
    @SerialName("openingCorpus") val openingCorpus: Long,
    @SerialName("closingCorpus") val closingCorpus: Long,
    @SerialName("savingsBreakdown") val savingsBreakdown: List<SavingsBreakdownItemDto> = emptyList(),
    @SerialName("loanItems") val loanItems: List<LoanSummaryItemDto> = emptyList(),
) {
    companion object {
        /** Bumped when this DTO shape changes (registry `version:` driven). */
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire DTO for a single per-member savings breakdown row
 * (`api.yaml#dtos.SavingsBreakdownItem`). See API.md#dtos — SavingsBreakdownItem.
 */
@Serializable
data class SavingsBreakdownItemDto(
    @SerialName("memberId") val memberId: String,
    @SerialName("memberName") val memberName: String,
    @SerialName("groupSavings") val groupSavings: Long,
    @SerialName("individualSavings") val individualSavings: Long,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire DTO for a single per-member loan activity row (`api.yaml#dtos.LoanSummaryItem`). See
 * API.md#dtos — LoanSummaryItem.
 */
@Serializable
data class LoanSummaryItemDto(
    @SerialName("memberId") val memberId: String,
    @SerialName("memberName") val memberName: String,
    @SerialName("amountDisbursed") val amountDisbursed: Long,
    @SerialName("amountRepaid") val amountRepaid: Long,
    @SerialName("outstandingAfter") val outstandingAfter: Long,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}
