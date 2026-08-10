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
 * Wire DTO for a single seeded group-type catalogue row — `GET
 * /companion/datatables/group_type_config/{entityId}` (COMP-DT-003; entityId=0 = global seed
 * catalogue, not per-group). The response is a JSON array of these rows; the 9 seeded rows
 * (VSLA, ROSCA, ASCA, SILC, SHG, SACCO, CBO_VILLAGE_BANK, BURIAL_WELFARE, JLG) back the
 * group-type-picker card list. Cached SWR (`ttl=86400`, `offline=show_cached`).
 *
 * See API.md#dtos — GroupTypeConfig.
 */
@Serializable
data class GroupTypeConfigDto(
    @SerialName("typeSlug") val typeSlug: GroupTypeSlugDto = GroupTypeSlugDto.UNKNOWN,
    @SerialName("displayName") val displayName: String,
    @SerialName("tagline") val tagline: String,
    @SerialName("savingsMechanism") val savingsMechanism: SavingsMechanismDto = SavingsMechanismDto.UNKNOWN,
    @SerialName("contributionMode") val contributionMode: ContributionModeDto = ContributionModeDto.UNKNOWN,
    @SerialName("lendingEnabled") val lendingEnabled: Boolean,
    @SerialName("hasSocialFund") val hasSocialFund: Boolean,
    @SerialName("hasBankLinkage") val hasBankLinkage: Boolean,
    @SerialName("welfareOnlyMode") val welfareOnlyMode: Boolean,
    @SerialName("formallyRegistered") val formallyRegistered: Boolean,
    @SerialName("defaultLoanMultiplier") val defaultLoanMultiplier: Double,
    @SerialName("defaultInterestRatePct") val defaultInterestRatePct: Double,
    @SerialName("defaultCycleLengthMonths") val defaultCycleLengthMonths: Int,
    @SerialName("maxMembers") val maxMembers: Int,
    @SerialName("minMembers") val minMembers: Int,
) {
    companion object {
        /** Bumped when this DTO shape changes (registry `version:` driven). See EC30. */
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire enum for the seeded group-type slug (COMP-DT-003). Carries an [UNKNOWN] fallback
 * (T7/EC30) so a server-added 10th group type never crashes an old client — combined with
 * `ignoreUnknownKeys` + `coerceInputValues` on the shared `Json` instance in `NetworkModule`
 * (emitted by `kmp-client-gen`, see its NetworkModule.kt Json config), an unrecognized wire
 * value on the `typeSlug` property is coerced to this default rather than throwing.
 */
@Serializable
enum class GroupTypeSlugDto {
    @SerialName("VSLA")
    VSLA,

    @SerialName("ROSCA")
    ROSCA,

    @SerialName("ASCA")
    ASCA,

    @SerialName("SILC")
    SILC,

    @SerialName("SHG")
    SHG,

    @SerialName("SACCO")
    SACCO,

    @SerialName("CBO_VILLAGE_BANK")
    CBO_VILLAGE_BANK,

    @SerialName("BURIAL_WELFARE")
    BURIAL_WELFARE,

    @SerialName("JLG")
    JLG,

    @SerialName("UNKNOWN")
    UNKNOWN,
}

/**
 * Wire enum for the internal-pool axis of a group type: ACCUMULATING (corpus grows via interest
 * + retained profit until share-out), ROTATING_PAYOUT (fixed pot rotates one winner per
 * period), NONE (no internal pool, e.g. JLG). [UNKNOWN] fallback per T7/EC30.
 */
@Serializable
enum class SavingsMechanismDto {
    @SerialName("ACCUMULATING")
    ACCUMULATING,

    @SerialName("ROTATING_PAYOUT")
    ROTATING_PAYOUT,

    @SerialName("NONE")
    NONE,

    @SerialName("UNKNOWN")
    UNKNOWN,
}

/**
 * Wire enum for how members contribute: SHARE_BASED_VARIABLE (buy a variable number of shares
 * each meeting), FIXED (equal amount per period), MINIMAL (token contribution only, e.g. JLG).
 * [UNKNOWN] fallback per T7/EC30.
 */
@Serializable
enum class ContributionModeDto {
    @SerialName("SHARE_BASED_VARIABLE")
    SHARE_BASED_VARIABLE,

    @SerialName("FIXED")
    FIXED,

    @SerialName("MINIMAL")
    MINIMAL,

    @SerialName("UNKNOWN")
    UNKNOWN,
}
