/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.groupcreate

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
 * `/groups/create` — the 4-step group-create wizard route (`ui.yaml#route`). [GroupTypeConfig]
 * (`core/model`) is a plain, non-`@Serializable` domain `data class` (see its own KDoc), so it
 * cannot be carried directly as a type-safe Navigation argument. [GroupCreateRoute] instead
 * flattens every [GroupTypeConfig] field into primitive `@Serializable` route args — the
 * navigation-layer equivalent of `GroupTypePickerRoute.kt`'s `onNavigateToGroupCreate:
 * (GroupTypeConfig) -> Unit` typed-callback nav-arg contract it flags as pending. `.toRoute()` /
 * `.toGroupTypeConfig()` round-trip losslessly (every [GroupTypeConfig] field has a matching
 * route arg — see the field-for-field mapping below). See API.md#route.
 */
@Serializable
data class GroupCreateRoute(
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

/** [GroupTypeConfig] -> [GroupCreateRoute] — field-for-field flatten, called by [navigateToGroupCreate]. */
fun GroupTypeConfig.toGroupCreateRoute(): GroupCreateRoute = GroupCreateRoute(
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

/** [GroupCreateRoute] -> [GroupTypeConfig] — round-trip reconstruction, called by [groupCreateScreen]. */
fun GroupCreateRoute.toGroupTypeConfig(): GroupTypeConfig = GroupTypeConfig(
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

fun NavController.navigateToGroupCreate(typeConfig: GroupTypeConfig, navOptions: NavOptions? = null) =
    navigate(typeConfig.toGroupCreateRoute(), navOptions)

/**
 * Registers [GroupCreateScreen] on the host [NavGraphBuilder]. Every callback here closes a
 * [GroupCreateEvent] navigation branch consumed by the Container (RULE-PROTO-COMPOSE-DEAD-CLICK-001
 * DC3) — `flow.yaml#navigates_to` declares both targets this Route closes: `group-dashboard`
 * ([onNavigateToGroupDashboard]) and `group-list` (the `OnBack`/pop target, wired by the
 * caller's [onNavigateBack] — same generic pass-through convention as `LoginSignupRoute.kt` /
 * `JoinWithCodeRoute.kt`; `flow.yaml` notes `OnBack` pops through `group-type-picker` back onto
 * `group-list`, so no separate hardcoded destination is threaded here). No callback carries a
 * `= {}` default — DC3 count-assertion: 0 defaults / 0 overrides / 0 suppressed. See API.md#route.
 */
fun NavGraphBuilder.groupCreateScreen(
    onNavigateToGroupDashboard: (groupId: String) -> Unit,
    onNavigateBack: () -> Unit,
) {
    composableWithRootPushTransitions<GroupCreateRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<GroupCreateRoute>()
        GroupCreateScreen(
            typeConfig = route.toGroupTypeConfig(),
            onNavigateToGroupDashboard = onNavigateToGroupDashboard,
            onNavigateBack = onNavigateBack,
        )
    }
}
