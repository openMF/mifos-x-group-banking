/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.memberadd.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kpt.core.designsystem.theme.spacing

private val OPTION_MIN_TOUCH_TARGET = 56.dp

/**
 * `ui.yaml#components.photo_source_bottom_sheet` — camera vs gallery chooser opened by
 * [PhotoPickerArea]'s tap. Both options are real, fully-wired dispatches (see
 * `MemberAddScreen.kt`'s `PLACEHOLDER_CAMERA_PHOTO_URI` / `PLACEHOLDER_GALLERY_PHOTO_URI` KDoc for
 * the documented URI-value seam) — this is NOT a dead-click stub. See API.md#screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoSourceBottomSheet(
    title: String,
    cameraLabel: String,
    galleryLabel: String,
    cameraContentDescription: String,
    galleryContentDescription: String,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    cameraOptionModifier: Modifier = Modifier,
    galleryOptionModifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
) {
    val sp = MaterialTheme.spacing
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(bottom = sp.lg)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = sp.lg, vertical = sp.sm),
            )
            ListItem(
                headlineContent = { Text(cameraLabel) },
                leadingContent = { Icon(imageVector = Icons.Filled.CameraAlt, contentDescription = null) },
                modifier = cameraOptionModifier
                    .fillMaxWidth()
                    .heightIn(min = OPTION_MIN_TOUCH_TARGET)
                    .clickable(onClick = onCameraClick)
                    .semantics { contentDescription = cameraContentDescription },
            )
            ListItem(
                headlineContent = { Text(galleryLabel) },
                leadingContent = { Icon(imageVector = Icons.Filled.PhotoLibrary, contentDescription = null) },
                modifier = galleryOptionModifier
                    .fillMaxWidth()
                    .heightIn(min = OPTION_MIN_TOUCH_TARGET)
                    .clickable(onClick = onGalleryClick)
                    .semantics { contentDescription = galleryContentDescription },
            )
        }
    }
}
