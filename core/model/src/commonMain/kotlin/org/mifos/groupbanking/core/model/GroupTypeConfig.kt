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
 * Domain model for a single seeded group-type catalogue row (COMP-DT-003) — pure business
 * shape, no wire concerns. Presented as one of the 9 selectable type cards on the
 * group-type-picker screen and forwarded as a navigation argument to group-create, which reads
 * [savingsMechanism], [contributionMode], [lendingEnabled], [hasSocialFund], [hasBankLinkage],
 * and [welfareOnlyMode] to conditionally show corpus, payout-order, welfare-only, and
 * bank-linkage wizard sections. Read-only catalogue seed — no local mutation.
 *
 * See API.md#models — GroupTypeConfig.
 */
data class GroupTypeConfig(
    val typeSlug: GroupTypeSlug,
    val displayName: String,
    val tagline: String,
    val savingsMechanism: SavingsMechanism,
    val contributionMode: ContributionMode,
    val lendingEnabled: Boolean,
    val hasSocialFund: Boolean,
    val hasBankLinkage: Boolean,
    val welfareOnlyMode: Boolean,
    val formallyRegistered: Boolean,
    val defaultLoanMultiplier: Double,
    val defaultInterestRatePct: Double,
    val defaultCycleLengthMonths: Int,
    val maxMembers: Int,
    val minMembers: Int,
)

/**
 * Domain enum mirroring the wire `GroupTypeSlugDto` one-to-one (pure Kotlin — no
 * `@Serializable`). All 9 seeded group types from COMP-DT-003 plus [UNKNOWN], which absorbs any
 * wire slug value this client build does not yet recognize.
 * See API.md#models — GroupTypeSlug.
 */
enum class GroupTypeSlug {
    VSLA,
    ROSCA,
    ASCA,
    SILC,
    SHG,
    SACCO,
    CBO_VILLAGE_BANK,
    BURIAL_WELFARE,
    JLG,
    UNKNOWN,
}

/**
 * Domain enum mirroring the wire `SavingsMechanismDto` one-to-one — the internal-pool axis of a
 * group type: ACCUMULATING (corpus grows via interest + retained profit until share-out),
 * ROTATING_PAYOUT (fixed pot rotates one winner per period), NONE (no internal pool, e.g. JLG).
 * [UNKNOWN] absorbs any wire value this client build does not yet recognize.
 * See API.md#models — SavingsMechanism.
 */
enum class SavingsMechanism {
    ACCUMULATING,
    ROTATING_PAYOUT,
    NONE,
    UNKNOWN,
}

/**
 * Domain enum mirroring the wire `ContributionModeDto` one-to-one — how members contribute:
 * SHARE_BASED_VARIABLE (buy a variable number of shares each meeting), FIXED (equal amount per
 * period), MINIMAL (token contribution only, e.g. JLG). [UNKNOWN] absorbs any wire value this
 * client build does not yet recognize.
 * See API.md#models — ContributionMode.
 */
enum class ContributionMode {
    SHARE_BASED_VARIABLE,
    FIXED,
    MINIMAL,
    UNKNOWN,
}
