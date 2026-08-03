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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kpt.core.designsystem.theme.spacing

/**
 * `groupTypeName` chip badge — `ui.yaml#components.group_type_banner`. The icon is decorative
 * (the surrounding [Modifier.semantics] block already carries the group's spoken description),
 * so its own `contentDescription` is `null` per standard "decorative icon inside a described
 * container" a11y practice. See API.md#screen.
 */
@Composable
fun GroupTypeBanner(
    groupTypeName: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val sp = MaterialTheme.spacing
    Row(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(50),
            )
            .padding(horizontal = sp.md, vertical = sp.xs)
            .semantics { this.contentDescription = contentDescription },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Category,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(end = sp.xs),
        )
        // groupTypeName is a bound state value (e.g. "VSLA"/"ROSCA"), not a translatable UI
        // literal — mirrors `ui.yaml#components.group_type_banner.i18n: skip`.
        Text(
            text = groupTypeName,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}
