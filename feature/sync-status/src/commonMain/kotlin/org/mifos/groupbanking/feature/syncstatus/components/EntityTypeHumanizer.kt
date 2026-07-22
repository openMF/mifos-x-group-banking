/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.feature.syncstatus.components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import org.mifos.groupbanking.core.model.EntityType
import org.mifos.groupbanking.feature.syncstatus.generated.resources.Res
import org.mifos.groupbanking.feature.syncstatus.generated.resources.screens_sync_status_entity_attendance
import org.mifos.groupbanking.feature.syncstatus.generated.resources.screens_sync_status_entity_loan
import org.mifos.groupbanking.feature.syncstatus.generated.resources.screens_sync_status_entity_meeting
import org.mifos.groupbanking.feature.syncstatus.generated.resources.screens_sync_status_entity_member
import org.mifos.groupbanking.feature.syncstatus.generated.resources.screens_sync_status_entity_savings
import org.mifos.groupbanking.feature.syncstatus.generated.resources.screens_sync_status_entity_share_out

/**
 * `ui.yaml#i18n.en.entity_row_label: "{{entityType | humanize}}"` — shared humanization bridge
 * consumed by both `pending_breakdown_card`'s per-entity rows and `failed_operations_section`'s
 * per-item title. Exhaustive `when` over every [EntityType] member — deliberately no `else`
 * branch, so a 7th [EntityType] added later fails to compile here instead of silently falling
 * through to a wrong label.
 */
@Composable
fun EntityType.humanizedLabel(): String = when (this) {
    EntityType.MEETING -> stringResource(Res.string.screens_sync_status_entity_meeting)
    EntityType.LOAN -> stringResource(Res.string.screens_sync_status_entity_loan)
    EntityType.SAVINGS -> stringResource(Res.string.screens_sync_status_entity_savings)
    EntityType.ATTENDANCE -> stringResource(Res.string.screens_sync_status_entity_attendance)
    EntityType.SHARE_OUT -> stringResource(Res.string.screens_sync_status_entity_share_out)
    EntityType.MEMBER -> stringResource(Res.string.screens_sync_status_entity_member)
}
