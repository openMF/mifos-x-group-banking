/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.savingsdashboard

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import kpt.core.base.ui.nav.composableWithRootPushTransitions
import org.mifos.groupbanking.core.model.ContributionMode
import org.mifos.groupbanking.core.model.GroupTypeConfig
import org.mifos.groupbanking.core.model.GroupTypeSlug
import org.mifos.groupbanking.core.model.SavingsMechanism

/**
 * `/groups/{groupId}/savings` — the savings-dashboard route (`ui.yaml#route`,
 * `ui.yaml#nav_params: { groupId, typeConfig }`). [GroupTypeConfig] (`core/model`) is a plain,
 * non-`@Serializable` domain `data class`, so it cannot be carried directly as a type-safe
 * Navigation argument — [SavingsDashboardRoute] flattens every field into primitive `@Serializable`
 * route args, IDENTICAL to `MemberSavingsDetailRoute`'s `toMemberSavingsDetailRoute()` /
 * `toGroupTypeConfig()` round-trip.
 *
 * **Every [GroupTypeConfig] field defaults (drift bridge, flagged for the cross-feature repair
 * station):** `ui.yaml#entry_points[0]` declares this screen's sole entry as `group-dashboard`'s
 * `OnViewSavings (all roles)`, but `GroupDashboardViewModel` emits that as
 * `NavigateToMemberSavingsDetail(groupId)` carrying ONLY `groupId` — its live per-group config is a
 * [org.mifos.groupbanking.core.model.GroupInstanceConfig], NOT the COMP-DT-003 catalogue
 * [GroupTypeConfig] this screen's `nav_params` declare (the same `GroupTypeConfig`-vs-instance
 * naming drift documented on `GroupInstanceConfig`'s KDoc). Until that cross-feature type drift is
 * unified, [navigateToSavingsDashboard] is called with `groupId` alone and the flattened fields
 * fall back to defaults — the group/individual tabs, member rows, cycle progress and totals all
 * render from the real loaded [SavingsDashboardSummary]; only the contribution-model-adaptive text
 * degrades to the FIXED/meetings variant ([ContributionMode.UNKNOWN]). Callers that DO hold a real
 * catalogue [GroupTypeConfig] pass it and get the full SHARE_BASED_VARIABLE rendering. See
 * API.md#route.
 */
@Serializable
data class SavingsDashboardRoute(
    val groupId: String,
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

/** [GroupTypeConfig] -> [SavingsDashboardRoute] — field-for-field flatten, called by [navigateToSavingsDashboard]. */
fun GroupTypeConfig.toSavingsDashboardRoute(groupId: String): SavingsDashboardRoute = SavingsDashboardRoute(
    groupId = groupId,
    typeSlug = typeSlug.name,
    displayName = displayName,
    tagline = tagline,
    savingsMechanism = savingsMechanism.name,
    contributionMode = contributionMode.name,
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

/** [SavingsDashboardRoute] -> [GroupTypeConfig] — round-trip reconstruction, called by [savingsDashboardScreen]. */
fun SavingsDashboardRoute.toGroupTypeConfig(): GroupTypeConfig = GroupTypeConfig(
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
 * Navigates to the savings-dashboard. [typeConfig] is optional — pass it when the caller holds the
 * real catalogue [GroupTypeConfig] (full contribution-model-adaptive rendering); omit it to nav by
 * `groupId` alone (see [SavingsDashboardRoute] KDoc "drift bridge").
 */
fun NavController.navigateToSavingsDashboard(
    groupId: String,
    typeConfig: GroupTypeConfig? = null,
    navOptions: NavOptions? = null,
) = navigate(
    typeConfig?.toSavingsDashboardRoute(groupId) ?: SavingsDashboardRoute(groupId = groupId),
    navOptions,
)

/**
 * Registers [SavingsDashboardScreen] on the host [NavGraphBuilder]. [onNavigateToMemberDetail]
 * closes the [SavingsDashboardEvent.NavigateToMemberDetail] branch (member-row tap →
 * `member-savings-detail`), [onNavigateBack] closes the top-bar back branch. No callback carries a
 * `= {}` default (RULE-IMPL-DEAD-CLICKABLE-001 DC3). See API.md#route.
 */
fun NavGraphBuilder.savingsDashboardScreen(
    onNavigateToMemberDetail: (memberId: String, groupId: String, typeConfig: GroupTypeConfig) -> Unit,
    onNavigateBack: () -> Unit,
) {
    composableWithRootPushTransitions<SavingsDashboardRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<SavingsDashboardRoute>()
        SavingsDashboardScreen(
            groupId = route.groupId,
            typeConfig = route.toGroupTypeConfig(),
            onNavigateToMemberDetail = onNavigateToMemberDetail,
            onNavigateBack = onNavigateBack,
        )
    }
}
