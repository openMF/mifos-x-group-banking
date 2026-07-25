/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.fieldofficerdashboard

import androidx.compose.runtime.Composable
import kpt.core.designsystem.theme.KptTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter
import org.jetbrains.compose.ui.tooling.preview.PreviewParameterProvider
import org.mifos.groupbanking.core.model.FieldOfficerDashboard
import org.mifos.groupbanking.core.model.GroupHealthSummary
import org.mifos.groupbanking.core.model.HealthIndicator

/**
 * `@Preview` gallery for `FieldOfficerDashboardScreen.kt`. Data source: `demo-data.yaml` /
 * `ui.yaml#states.content.demo_data` seeded rows (Mwangaza Women's Group / Tumaini Savings Circle /
 * Umoja Welfare Group). See API.md#preview.
 */
private val previewGroups: List<GroupHealthSummary> = listOf(
    GroupHealthSummary(1, 1, "Mwangaza Women's Group", "Nairobi East", "ACTIVE", 12, 24000.0, 9000.0, 0.00, HealthIndicator.GREEN, 1),
    GroupHealthSummary(2, 2, "Tumaini Savings Circle", "Nairobi West", "ACTIVE", 10, 18500.0, 12000.0, 0.12, HealthIndicator.AMBER, 3),
    GroupHealthSummary(3, 3, "Umoja Welfare Group", "Nairobi East", "ACTIVE", 8, 11200.0, 9800.0, 0.25, HealthIndicator.RED, 2),
)

private fun contentState(): FieldOfficerDashboardState = FieldOfficerDashboardState(
    isLoading = false,
    staffId = 12L,
    userRole = FieldOfficerDashboard.ROLE_FIELD_OFFICER,
    canExport = true,
    totalGroupsCount = 3,
    totalActiveMembers = 30,
    totalSavingsThisMonth = 53700.0,
    totalLoansOutstanding = 30800.0,
    groups = previewGroups,
    filteredGroups = previewGroups,
    availableRegions = listOf("Nairobi East", "Nairobi West"),
)

private class FieldOfficerStatePreviewProvider : PreviewParameterProvider<FieldOfficerDashboardState> {
    override val values: Sequence<FieldOfficerDashboardState> = sequenceOf(
        FieldOfficerDashboardState(isLoading = true),
        contentState(),
        FieldOfficerDashboardState(isLoading = false, groups = emptyList(), filteredGroups = emptyList()),
        FieldOfficerDashboardState(isLoading = false, error = FieldOfficerDashboardError.Network),
    )
}

@Preview
@Composable
private fun FieldOfficerDashboardContentPreview(
    @PreviewParameter(FieldOfficerStatePreviewProvider::class)
    state: FieldOfficerDashboardState,
) {
    KptTheme {
        FieldOfficerDashboardContent(state = state, onAction = {})
    }
}
