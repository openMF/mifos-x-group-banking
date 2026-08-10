/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.membersavingsdetail

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import kpt.core.base.ui.nav.composableWithRootPushTransitions
import kpt.core.model.ContributionMode
import kpt.core.model.GroupTypeConfig
import kpt.core.model.GroupTypeSlug
import kpt.core.model.SavingsMechanism

/**
 * `/groups/{groupId}/members/{memberId}/savings` — the member-savings-detail route
 * (`ui.yaml#route`, `ui.yaml#nav_params: { memberId, groupId, typeConfig }`). [GroupTypeConfig]
 * (`core/model`) is a plain, non-`@Serializable` domain `data class` (see its own KDoc), so it
 * cannot be carried directly as a type-safe Navigation argument. [MemberSavingsDetailRoute]
 * instead flattens every [GroupTypeConfig] field into primitive `@Serializable` route args —
 * IDENTICAL convention to `GroupCreateRoute.kt`'s `toGroupCreateRoute()`/`toGroupTypeConfig()`
 * round-trip (field-for-field flatten, `.toRoute()` reconstructs losslessly). See API.md#route.
 */
@Serializable
data class MemberSavingsDetailRoute(
    val memberId: String,
    val groupId: String,
    val typeSlug: String,
    val displayName: String,
    val tagline: String,
    val savingsMechanism: String,
    val contributionMode: String,
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

/** [GroupTypeConfig] -> [MemberSavingsDetailRoute] — field-for-field flatten, called by [navigateToMemberSavingsDetail]. */
fun GroupTypeConfig.toMemberSavingsDetailRoute(memberId: String, groupId: String): MemberSavingsDetailRoute = MemberSavingsDetailRoute(
    memberId = memberId,
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

/** [MemberSavingsDetailRoute] -> [GroupTypeConfig] — round-trip reconstruction, called by [memberSavingsDetailScreen]. */
fun MemberSavingsDetailRoute.toGroupTypeConfig(): GroupTypeConfig = GroupTypeConfig(
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

fun NavController.navigateToMemberSavingsDetail(
    memberId: String,
    groupId: String,
    typeConfig: GroupTypeConfig,
    navOptions: NavOptions? = null,
) = navigate(typeConfig.toMemberSavingsDetailRoute(memberId = memberId, groupId = groupId), navOptions)

/**
 * Registers [MemberSavingsDetailScreen] on the host [NavGraphBuilder]. [onNavigateBack] closes the
 * single [MemberSavingsDetailEvent.NavigateBack] navigation branch consumed by the Container
 * (RULE-PROTO-COMPOSE-DEAD-CLICK-001 DC3) — `flow.yaml#navigates_to` declares no forward
 * destination out of `member-savings-detail` (it is a terminal read-only leaf screen; every
 * interactive element besides the back arrow is an in-place filter/expand/refresh transform, not a
 * navigation). No callback carries a `= {}` default — DC3 count-assertion: 1 default / 1 override /
 * 0 suppressed. See API.md#route.
 */
fun NavGraphBuilder.memberSavingsDetailScreen(onNavigateBack: () -> Unit) {
    composableWithRootPushTransitions<MemberSavingsDetailRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<MemberSavingsDetailRoute>()
        MemberSavingsDetailScreen(
            memberId = route.memberId,
            groupId = route.groupId,
            typeConfig = route.toGroupTypeConfig(),
            onNavigateBack = onNavigateBack,
        )
    }
}
