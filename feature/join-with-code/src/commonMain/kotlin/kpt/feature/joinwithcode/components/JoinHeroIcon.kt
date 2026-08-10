/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.joinwithcode.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kpt.feature.joinwithcode.generated.resources.Res
import kpt.feature.joinwithcode.generated.resources.screens_join_with_code_hero_icon_cd
import org.jetbrains.compose.resources.stringResource

/**
 * Circular hero illustration for the invite-entry screen — `ui.yaml#components.hero_icon`
 * (icon: `group_add`, 64dp, centered). Purely decorative content wrapped by an accessible
 * [contentDescription] on the icon itself (mirrors `ui.yaml#components.hero_icon
 * .accessibility_label`). See API.md#screen.
 */
@Composable
fun JoinHeroIcon(modifier: Modifier = Modifier) {
    val cd = stringResource(Res.string.screens_join_with_code_hero_icon_cd)
    Box(
        modifier = modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.GroupAdd,
            contentDescription = cd,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp),
        )
    }
}
