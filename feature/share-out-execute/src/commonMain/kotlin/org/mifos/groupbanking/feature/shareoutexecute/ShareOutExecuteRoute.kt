/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.shareoutexecute

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kpt.core.base.ui.nav.composableWithRootPushTransitions
import org.mifos.groupbanking.core.model.ContributionMode
import org.mifos.groupbanking.core.model.GroupTypeConfig
import org.mifos.groupbanking.core.model.GroupTypeSlug
import org.mifos.groupbanking.core.model.MemberPayout
import org.mifos.groupbanking.core.model.SavingsMechanism

/**
 * Route-local Json for encoding [memberPayoutsJson]. [MemberPayout] (`core/model`) is a plain,
 * non-`@Serializable` domain type, so it cannot be a type-safe Navigation arg directly (same reason
 * `ShareOutPreviewRoute` flattens `GroupTypeConfig`). The list is instead round-tripped through a
 * `@Serializable` [ExecutePayoutArg] projection encoded into ONE `String` route arg — the only
 * Navigation arg type that carries an arbitrary-length structured payload without a custom `NavType`.
 */
private val executeRouteJson = Json { ignoreUnknownKeys = true }

/**
 * `@Serializable` wire projection of [MemberPayout] used solely for the [ShareOutExecuteRoute.memberPayoutsJson]
 * nav arg. Round-trips every field so the execute screen reconstructs the exact payout list the
 * preview confirmed.
 */
@Serializable
data class ExecutePayoutArg(
    val memberId: String,
    val memberName: String,
    val sharesHeld: Int? = null,
    val totalSavings: Double? = null,
    val sharePercent: Double,
    val payoutAmount: Double,
)

private fun MemberPayout.toArg(): ExecutePayoutArg = ExecutePayoutArg(
    memberId = memberId,
    memberName = memberName,
    sharesHeld = sharesHeld,
    totalSavings = totalSavings,
    sharePercent = sharePercent,
    payoutAmount = payoutAmount,
)

private fun ExecutePayoutArg.toDomain(): MemberPayout = MemberPayout(
    memberId = memberId,
    memberName = memberName,
    sharesHeld = sharesHeld,
    totalSavings = totalSavings,
    sharePercent = sharePercent,
    payoutAmount = payoutAmount,
)

/** Encodes a payout list to the [ShareOutExecuteRoute.memberPayoutsJson] arg (`"[]"` for empty). */
fun encodeMemberPayouts(payouts: List<MemberPayout>): String =
    executeRouteJson.encodeToString(ListSerializer(ExecutePayoutArg.serializer()), payouts.map { it.toArg() })

/** Reverse of [encodeMemberPayouts] — decodes the [ShareOutExecuteRoute.memberPayoutsJson] arg back to domain payouts. */
fun decodeMemberPayouts(json: String): List<MemberPayout> =
    if (json.isBlank()) {
        emptyList()
    } else {
        executeRouteJson.decodeFromString(ListSerializer(ExecutePayoutArg.serializer()), json).map { it.toDomain() }
    }

/**
 * `/groups/{groupId}/share-out/execute` — the share-out-execute route (`ui.yaml#route`,
 * `ui.yaml#nav_params: { groupId, typeConfig, totalPool, memberPayouts }`). [GroupTypeConfig] is
 * flattened field-for-field (identical to `ShareOutPreviewRoute`); [memberPayoutsJson] carries the
 * `List<MemberPayout>` as a single JSON `String` arg (see [encodeMemberPayouts]). See API.md#route.
 */
@Serializable
data class ShareOutExecuteRoute(
    val groupId: String,
    val totalPool: Double,
    val memberPayoutsJson: String,
    val typeSlug: String = GroupTypeSlug.UNKNOWN.name,
    val displayName: String = "",
    val tagline: String = "",
    val savingsMechanism: String = SavingsMechanism.UNKNOWN.name,
    val contributionMode: String = ContributionMode.UNKNOWN.name,
    val lendingEnabled: Boolean = false,
    val hasSocialFund: Boolean = false,
    val hasBankLinkage: Boolean = false,
    val welfareOnlyMode: Boolean = false,
    val formallyRegistered: Boolean = false,
    val defaultLoanMultiplier: Double = 0.0,
    val defaultInterestRatePct: Double = 0.0,
    val defaultCycleLengthMonths: Int = 0,
    val maxMembers: Int = 0,
    val minMembers: Int = 0,
)

/** [ShareOutExecuteRoute] -> [GroupTypeConfig] — round-trip reconstruction, called by [shareOutExecuteScreen]. */
fun ShareOutExecuteRoute.toGroupTypeConfig(): GroupTypeConfig = GroupTypeConfig(
    typeSlug = runCatching { GroupTypeSlug.valueOf(typeSlug) }.getOrDefault(GroupTypeSlug.UNKNOWN),
    displayName = displayName,
    tagline = tagline,
    savingsMechanism = runCatching { SavingsMechanism.valueOf(savingsMechanism) }.getOrDefault(SavingsMechanism.UNKNOWN),
    contributionMode = runCatching { ContributionMode.valueOf(contributionMode) }.getOrDefault(ContributionMode.UNKNOWN),
    lendingEnabled = lendingEnabled,
    hasSocialFund = hasSocialFund,
    hasBankLinkage = hasBankLinkage,
    welfareOnlyMode = welfareOnlyMode,
    formallyRegistered = formallyRegistered,
    defaultLoanMultiplier = defaultLoanMultiplier,
    defaultInterestRatePct = defaultInterestRatePct,
    defaultCycleLengthMonths = defaultCycleLengthMonths,
    maxMembers = maxMembers,
    minMembers = minMembers,
)

/**
 * Navigates to share-out-execute — called from `share-out-preview`'s confirm handoff
 * (`ShareOutPreviewEvent.NavigateToShareOutExecute`), the sole entry point (`ui.yaml#entry_points`).
 */
fun NavController.navigateToShareOutExecute(
    groupId: String,
    typeConfig: GroupTypeConfig,
    totalPool: Double,
    memberPayouts: List<MemberPayout>,
    navOptions: NavOptions? = null,
) = navigate(
    ShareOutExecuteRoute(
        groupId = groupId,
        totalPool = totalPool,
        memberPayoutsJson = encodeMemberPayouts(memberPayouts),
        typeSlug = typeConfig.typeSlug.name,
        displayName = typeConfig.displayName,
        tagline = typeConfig.tagline,
        savingsMechanism = typeConfig.savingsMechanism.name,
        contributionMode = typeConfig.contributionMode.name,
        lendingEnabled = typeConfig.lendingEnabled,
        hasSocialFund = typeConfig.hasSocialFund,
        hasBankLinkage = typeConfig.hasBankLinkage,
        welfareOnlyMode = typeConfig.welfareOnlyMode,
        formallyRegistered = typeConfig.formallyRegistered,
        defaultLoanMultiplier = typeConfig.defaultLoanMultiplier,
        defaultInterestRatePct = typeConfig.defaultInterestRatePct,
        defaultCycleLengthMonths = typeConfig.defaultCycleLengthMonths,
        maxMembers = typeConfig.maxMembers,
        minMembers = typeConfig.minMembers,
    ),
    navOptions,
)

/**
 * Registers [ShareOutExecuteScreen] on the host [NavGraphBuilder]. [onNavigateToGroupDashboard]
 * closes the [ShareOutExecuteEvent.NavigateToGroupDashboard] branch (done → group-dashboard);
 * [onNavigateBack] closes the top-bar back branch (abandon before execution). No callback carries a
 * `= {}` default (RULE-IMPL-DEAD-CLICKABLE-001 DC3). See API.md#route.
 */
fun NavGraphBuilder.shareOutExecuteScreen(
    onNavigateToGroupDashboard: (groupId: String) -> Unit,
    onNavigateBack: () -> Unit,
) {
    composableWithRootPushTransitions<ShareOutExecuteRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<ShareOutExecuteRoute>()
        ShareOutExecuteScreen(
            groupId = route.groupId,
            typeConfig = route.toGroupTypeConfig(),
            totalPool = route.totalPool,
            memberPayouts = decodeMemberPayouts(route.memberPayoutsJson),
            onNavigateToGroupDashboard = onNavigateToGroupDashboard,
            onNavigateBack = onNavigateBack,
        )
    }
}
