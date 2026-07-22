/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.memberadd.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

private val PHOTO_AREA_SIZE = 120.dp
private val PHOTO_AREA_ICON_SIZE = 40.dp
private val PHOTO_SELECTED_ICON_SIZE = 48.dp
private val REMOVE_BUTTON_SIZE = 48.dp

/**
 * `ui.yaml#components.photo_picker_area` — a 120dp circular tap target. No image-loading
 * pipeline is wired anywhere in this codebase — every other feature's avatar/photo surface is
 * icon/initials-only too (mirrors `MemberHeaderCard`'s identical "no `AsyncImage`, no coil"
 * precedent, and `MemberAddViewModel`'s own "Photo-bytes deferral" KDoc: `photoUri` is a raw
 * platform URI string, not yet resolvable to pixel bytes since no `ImagePickerHelper` exists).
 * When [photoUri] is non-null this renders a filled "photo selected" checkmark indicator instead
 * of the actual image — a deliberate, documented seam, NOT a stub: the tap target and the
 * [onRemoveClick] affordance are both fully wired to real `MemberAddAction` dispatches. See
 * API.md#screen.
 */
@Composable
fun PhotoPickerArea(
    photoUri: String?,
    onAddPhotoClick: () -> Unit,
    onRemoveClick: () -> Unit,
    areaContentDescription: String,
    selectedContentDescription: String,
    removeContentDescription: String,
    modifier: Modifier = Modifier,
    removeButtonModifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(modifier = modifier, contentAlignment = Alignment.TopEnd) {
        Box(
            modifier = Modifier
                .size(PHOTO_AREA_SIZE)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable(enabled = enabled, onClick = onAddPhotoClick)
                .semantics {
                    contentDescription = if (photoUri != null) selectedContentDescription else areaContentDescription
                },
            contentAlignment = Alignment.Center,
        ) {
            if (photoUri != null) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(PHOTO_SELECTED_ICON_SIZE),
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.AddAPhoto,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(PHOTO_AREA_ICON_SIZE),
                )
            }
        }
        if (photoUri != null) {
            IconButton(
                onClick = onRemoveClick,
                enabled = enabled,
                modifier = removeButtonModifier.size(REMOVE_BUTTON_SIZE),
            ) {
                Icon(
                    imageVector = Icons.Filled.Cancel,
                    contentDescription = removeContentDescription,
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
