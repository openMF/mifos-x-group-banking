/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.grouptypepicker.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CorporateFare
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.PeopleAlt
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Savings
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import kpt.core.model.GroupTypeSlug
import kpt.feature.grouptypepicker.generated.resources.Res
import kpt.feature.grouptypepicker.generated.resources.screens_group_type_picker_asca_feature_1
import kpt.feature.grouptypepicker.generated.resources.screens_group_type_picker_asca_feature_2
import kpt.feature.grouptypepicker.generated.resources.screens_group_type_picker_asca_feature_3
import kpt.feature.grouptypepicker.generated.resources.screens_group_type_picker_burial_feature_1
import kpt.feature.grouptypepicker.generated.resources.screens_group_type_picker_burial_feature_2
import kpt.feature.grouptypepicker.generated.resources.screens_group_type_picker_cbo_feature_1
import kpt.feature.grouptypepicker.generated.resources.screens_group_type_picker_cbo_feature_2
import kpt.feature.grouptypepicker.generated.resources.screens_group_type_picker_cbo_feature_3
import kpt.feature.grouptypepicker.generated.resources.screens_group_type_picker_jlg_feature_1
import kpt.feature.grouptypepicker.generated.resources.screens_group_type_picker_jlg_feature_2
import kpt.feature.grouptypepicker.generated.resources.screens_group_type_picker_jlg_feature_3
import kpt.feature.grouptypepicker.generated.resources.screens_group_type_picker_rosca_feature_1
import kpt.feature.grouptypepicker.generated.resources.screens_group_type_picker_rosca_feature_2
import kpt.feature.grouptypepicker.generated.resources.screens_group_type_picker_sacco_feature_1
import kpt.feature.grouptypepicker.generated.resources.screens_group_type_picker_sacco_feature_2
import kpt.feature.grouptypepicker.generated.resources.screens_group_type_picker_sacco_feature_3
import kpt.feature.grouptypepicker.generated.resources.screens_group_type_picker_shg_feature_1
import kpt.feature.grouptypepicker.generated.resources.screens_group_type_picker_shg_feature_2
import kpt.feature.grouptypepicker.generated.resources.screens_group_type_picker_shg_feature_3
import kpt.feature.grouptypepicker.generated.resources.screens_group_type_picker_silc_feature_1
import kpt.feature.grouptypepicker.generated.resources.screens_group_type_picker_silc_feature_2
import kpt.feature.grouptypepicker.generated.resources.screens_group_type_picker_silc_feature_3
import kpt.feature.grouptypepicker.generated.resources.screens_group_type_picker_vsla_feature_1
import kpt.feature.grouptypepicker.generated.resources.screens_group_type_picker_vsla_feature_2
import kpt.feature.grouptypepicker.generated.resources.screens_group_type_picker_vsla_feature_3
import org.jetbrains.compose.resources.stringResource

/**
 * Per-[GroupTypeSlug] leading icon for the type card avatar — mirrors `ui.yaml#components.*.icon`
 * (`savings`, `rotate_right`, `account_balance`, `people_alt`, `groups`, `corporate_fare`,
 * `location_city`, `favorite`, `handshake`). Purely decorative (`ui.yaml` sets
 * `accessibility_label: ""` on every type icon) — the containing [GroupTypeCard] carries the
 * accessible label via [kpt.core.model.GroupTypeConfig.displayName], so the
 * icon itself is rendered with `contentDescription = null`, per standard Compose a11y practice for
 * a decorative glyph paired with adjacent visible text. See API.md#screen.
 */
fun groupTypeIcon(slug: GroupTypeSlug): ImageVector = when (slug) {
    GroupTypeSlug.VSLA -> Icons.Filled.Savings
    GroupTypeSlug.ROSCA -> Icons.Filled.RotateRight
    GroupTypeSlug.ASCA -> Icons.Filled.AccountBalance
    GroupTypeSlug.SILC -> Icons.Filled.PeopleAlt
    GroupTypeSlug.SHG -> Icons.Filled.Groups
    GroupTypeSlug.SACCO -> Icons.Filled.CorporateFare
    GroupTypeSlug.CBO_VILLAGE_BANK -> Icons.Filled.LocationCity
    GroupTypeSlug.BURIAL_WELFARE -> Icons.Filled.Favorite
    GroupTypeSlug.JLG -> Icons.Filled.Handshake
    GroupTypeSlug.UNKNOWN -> Icons.Filled.Groups
}

/**
 * Resolves the static feature-chip labels for a [GroupTypeSlug] — `ui.yaml#components.*_features`.
 * [kpt.core.model.GroupTypeConfig] carries no `features` field (only the 2
 * config axes + boolean flags), so the 2–3 distinguishing-feature chip labels shown per card are a
 * static per-type catalogue, verbatim from `ui.yaml#i18n`. See API.md#screen.
 */
@Composable
fun groupTypeFeatureChips(slug: GroupTypeSlug): List<String> = when (slug) {
    GroupTypeSlug.VSLA -> listOf(
        stringResource(Res.string.screens_group_type_picker_vsla_feature_1),
        stringResource(Res.string.screens_group_type_picker_vsla_feature_2),
        stringResource(Res.string.screens_group_type_picker_vsla_feature_3),
    )

    GroupTypeSlug.ROSCA -> listOf(
        stringResource(Res.string.screens_group_type_picker_rosca_feature_1),
        stringResource(Res.string.screens_group_type_picker_rosca_feature_2),
    )

    GroupTypeSlug.ASCA -> listOf(
        stringResource(Res.string.screens_group_type_picker_asca_feature_1),
        stringResource(Res.string.screens_group_type_picker_asca_feature_2),
        stringResource(Res.string.screens_group_type_picker_asca_feature_3),
    )

    GroupTypeSlug.SILC -> listOf(
        stringResource(Res.string.screens_group_type_picker_silc_feature_1),
        stringResource(Res.string.screens_group_type_picker_silc_feature_2),
        stringResource(Res.string.screens_group_type_picker_silc_feature_3),
    )

    GroupTypeSlug.SHG -> listOf(
        stringResource(Res.string.screens_group_type_picker_shg_feature_1),
        stringResource(Res.string.screens_group_type_picker_shg_feature_2),
        stringResource(Res.string.screens_group_type_picker_shg_feature_3),
    )

    GroupTypeSlug.SACCO -> listOf(
        stringResource(Res.string.screens_group_type_picker_sacco_feature_1),
        stringResource(Res.string.screens_group_type_picker_sacco_feature_2),
        stringResource(Res.string.screens_group_type_picker_sacco_feature_3),
    )

    GroupTypeSlug.CBO_VILLAGE_BANK -> listOf(
        stringResource(Res.string.screens_group_type_picker_cbo_feature_1),
        stringResource(Res.string.screens_group_type_picker_cbo_feature_2),
        stringResource(Res.string.screens_group_type_picker_cbo_feature_3),
    )

    GroupTypeSlug.BURIAL_WELFARE -> listOf(
        stringResource(Res.string.screens_group_type_picker_burial_feature_1),
        stringResource(Res.string.screens_group_type_picker_burial_feature_2),
    )

    GroupTypeSlug.JLG -> listOf(
        stringResource(Res.string.screens_group_type_picker_jlg_feature_1),
        stringResource(Res.string.screens_group_type_picker_jlg_feature_2),
        stringResource(Res.string.screens_group_type_picker_jlg_feature_3),
    )

    GroupTypeSlug.UNKNOWN -> emptyList()
}
