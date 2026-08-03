/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.groupcreate.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import kpt.core.designsystem.theme.spacing

/**
 * One labeled key-value block on the Step 4 review card — `ui.yaml#components.review_card`'s
 * `section` sub-components (`review_identity_section` / `review_rules_section` /
 * `review_members_section`). Each row's label (`k`) is a localized field label; each row's value
 * (`v`) is a bound state value already formatted by the caller (currency/units resolved from
 * live [org.mifos.groupbanking.feature.groupcreate.GroupCreateState] fields rather than
 * `ui.yaml`'s hardcoded `"KES {{shareValue}}"` literal — see `GroupCreateScreen.kt`'s
 * `reviewRulesRows` KDoc for the flagged deviation). See API.md#screen.
 */
@Composable
fun ReviewSection(
    label: String,
    rows: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
    accessibilityLabel: String? = null,
) {
    val sp = MaterialTheme.spacing
    val sectionModifier = if (accessibilityLabel != null) {
        modifier.semantics { contentDescription = accessibilityLabel }
    } else {
        modifier
    }

    Column(modifier = sectionModifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(modifier = Modifier.padding(top = sp.sm)) {
            rows.forEach { (k, v) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = sp.xs),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = k, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = v, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
