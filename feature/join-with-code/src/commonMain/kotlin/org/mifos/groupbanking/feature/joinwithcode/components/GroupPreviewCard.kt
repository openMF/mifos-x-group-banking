/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.joinwithcode.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kpt.core.base.designsystem.component.AppCard
import kpt.core.designsystem.theme.spacing
import org.jetbrains.compose.resources.stringResource
import org.mifos.groupbanking.core.model.GroupPreview
import org.mifos.groupbanking.feature.joinwithcode.JoinWithCodeTestTags
import org.mifos.groupbanking.feature.joinwithcode.generated.resources.Res
import org.mifos.groupbanking.feature.joinwithcode.generated.resources.screens_join_with_code_label_group_type
import org.mifos.groupbanking.feature.joinwithcode.generated.resources.screens_join_with_code_label_member_count
import org.mifos.groupbanking.feature.joinwithcode.generated.resources.screens_join_with_code_label_members
import org.mifos.groupbanking.feature.joinwithcode.generated.resources.screens_join_with_code_label_organizer
import org.mifos.groupbanking.feature.joinwithcode.generated.resources.screens_join_with_code_preview_card_cd
import org.mifos.groupbanking.feature.joinwithcode.generated.resources.screens_join_with_code_preview_title
import org.mifos.groupbanking.feature.joinwithcode.generated.resources.screens_join_with_code_role_chip_cd

/**
 * Group preview confirmation card — `ui.yaml#components.group_preview_card`. Displays the
 * resolved [GroupPreview] (name / type / organiser / member count / assigned role) before the
 * invitee confirms joining. [GroupPreview.groupType] / [GroupPreview.roleToAssign] render via
 * their raw enum `.name` — no humanized `displayName` is attached to the domain [GroupPreview]
 * shape (same flagged idea-layer follow-up already documented on
 * `feature/group-list/.../GroupListCard.kt`, which renders `Group.groupType.name` for the same
 * reason). See API.md#screen.
 */
@Composable
fun GroupPreviewCard(preview: GroupPreview, modifier: Modifier = Modifier) {
    val sp = MaterialTheme.spacing
    val cardCd = stringResource(Res.string.screens_join_with_code_preview_card_cd)
    val roleCd = stringResource(Res.string.screens_join_with_code_role_chip_cd)
    val typeLabel = stringResource(Res.string.screens_join_with_code_label_group_type)
    val organizerLabel = stringResource(Res.string.screens_join_with_code_label_organizer)
    val memberLabel = stringResource(Res.string.screens_join_with_code_label_member_count)
    val membersSuffix = stringResource(Res.string.screens_join_with_code_label_members)
    val previewTitle = stringResource(Res.string.screens_join_with_code_preview_title)

    AppCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag(JoinWithCodeTestTags.PREVIEW_CARD)
            .semantics { contentDescription = cardCd },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(sp.sm)) {
            Text(text = previewTitle, style = MaterialTheme.typography.titleMedium)
            Text(
                text = preview.groupName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            PreviewDetailRow(label = typeLabel, value = preview.groupType.name)
            PreviewDetailRow(label = organizerLabel, value = preview.organizerName)
            PreviewDetailRow(label = memberLabel, value = "${preview.memberCount} $membersSuffix")

            AssistChip(
                onClick = {},
                enabled = false,
                label = { Text(text = preview.roleToAssign.name, style = MaterialTheme.typography.labelSmall) },
                shape = RoundedCornerShape(8.dp),
                colors = AssistChipDefaults.assistChipColors(
                    disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    disabledLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
                border = null,
                modifier = Modifier.semantics { contentDescription = roleCd },
            )
        }
    }
}

/** `ui.yaml#components.group_preview_card.content` label/value row (type · organiser · member count). */
@Composable
private fun PreviewDetailRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
