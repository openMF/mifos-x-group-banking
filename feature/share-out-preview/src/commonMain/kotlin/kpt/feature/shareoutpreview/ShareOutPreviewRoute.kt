/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.shareoutpreview

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import kpt.core.base.ui.nav.composableWithRootPushTransitions
import kpt.core.model.ContributionMode
import kpt.core.model.GroupTypeConfig
import kpt.core.model.GroupTypeSlug
import kpt.core.model.MemberPayout
import kpt.core.model.SavingsMechanism

/**
 * `/groups/{groupId}/share-out/preview` — the share-out-preview route (`ui.yaml#route`,
 * `ui.yaml#nav_params: { groupId, typeConfig }`). [GroupTypeConfig] (`core/model`) is a plain,
 * non-`@Serializable` domain `data class`, so it cannot be carried directly as a type-safe
 * Navigation argument — [ShareOutPreviewRoute] flattens every field into primitive `@Serializable`
 * route args, IDENTICAL to `SavingsDashboardRoute`'s `toSavingsDashboardRoute()` /
 * `toGroupTypeConfig()` round-trip.
 *
 * **Every [GroupTypeConfig] field defaults (drift bridge):** `ui.yaml#entry_points[0]` declares
 * this screen's entry as `group-dashboard`'s `Share Out (cycle-end action)`, but that seam's
 * `onNavigateToShareOut(groupId, distributionStrategy: String)` callback carries only `groupId` +
 * a strategy string — not the catalogue [GroupTypeConfig] the `nav_params` declare (the same
 * catalogue-vs-instance drift documented on `SavingsDashboardRoute`). Until that cross-feature type
 * drift is unified, [navigateToShareOutPreview] is called with `groupId` alone and the flattened
 * fields fall back to defaults — this is harmless because the authoritative `poolModel`/
 * `shareoutFormula` (which drive the accumulating-table vs rotating-card rendering) always come
 * from the companion preview response (COMP-DIST-001), never from the nav-param `typeConfig`.
 * Callers that DO hold a real catalogue [GroupTypeConfig] pass it and get the strategy chip seeded
 * before the read resolves. See API.md#route.
 */
@Serializable
data class ShareOutPreviewRoute(
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

/** [GroupTypeConfig] -> [ShareOutPreviewRoute] — field-for-field flatten, called by [navigateToShareOutPreview]. */
fun GroupTypeConfig.toShareOutPreviewRoute(groupId: String): ShareOutPreviewRoute = ShareOutPreviewRoute(
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

/** [ShareOutPreviewRoute] -> [GroupTypeConfig] — round-trip reconstruction, called by [shareOutPreviewScreen]. */
fun ShareOutPreviewRoute.toGroupTypeConfig(): GroupTypeConfig = GroupTypeConfig(
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
 * Navigates to share-out-preview. [typeConfig] is optional — pass it when the caller holds the real
 * catalogue [GroupTypeConfig] (strategy chip seeded before the read); omit it to nav by `groupId`
 * alone (see [ShareOutPreviewRoute] KDoc "drift bridge").
 */
fun NavController.navigateToShareOutPreview(
    groupId: String,
    typeConfig: GroupTypeConfig? = null,
    navOptions: NavOptions? = null,
) = navigate(
    typeConfig?.toShareOutPreviewRoute(groupId) ?: ShareOutPreviewRoute(groupId = groupId),
    navOptions,
)

/**
 * Registers [ShareOutPreviewScreen] on the host [NavGraphBuilder]. [onNavigateToShareOutExecute]
 * closes the [ShareOutPreviewEvent.NavigateToShareOutExecute] branch (confirm → `share-out-execute`),
 * [onNavigateBack] closes the top-bar back branch. `share-out-execute` is not yet a generated
 * feature module — this is a typed nav-arg contract for the caller to wire (mirroring
 * `GroupDashboardRoute`'s identical not-yet-generated-target convention). No callback carries a
 * `= {}` default (RULE-IMPL-DEAD-CLICKABLE-001 DC3). See API.md#route.
 */
fun NavGraphBuilder.shareOutPreviewScreen(
    onNavigateToShareOutExecute: (
        groupId: String,
        typeConfig: GroupTypeConfig,
        totalPool: Double,
        memberPayouts: List<MemberPayout>,
        cycleNumber: Int,
    ) -> Unit,
    onNavigateBack: () -> Unit,
) {
    composableWithRootPushTransitions<ShareOutPreviewRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<ShareOutPreviewRoute>()
        ShareOutPreviewScreen(
            groupId = route.groupId,
            typeConfig = route.toGroupTypeConfig(),
            onNavigateToShareOutExecute = onNavigateToShareOutExecute,
            onNavigateBack = onNavigateBack,
        )
    }
}
