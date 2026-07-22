/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.mapper

import org.mifos.groupbanking.core.model.ChangePinRequest
import org.mifos.groupbanking.core.model.ChangePinResult
import org.mifos.groupbanking.core.network.model.ChangePinRequestDto
import org.mifos.groupbanking.core.network.model.ChangePinResponseDto

/**
 * DTO <-> domain mappers for the settings screen's change-PIN wire contract (`PUT
 * /fineract-provider/api/v1/self/user/updatePassword`). Every field on every DTO declared in
 * `ChangePinDto.kt` is mapped — no field left unmapped. [ChangePinRequest.currentPin]
 * deliberately does NOT map to any `ChangePinRequestDto` field — it authenticates the request via
 * the `BasicAuth` header the calling `SettingsRepository`/Ktor service attaches, not the JSON
 * body (same "wire-only field excluded" precedent as `LoanRequestPayload.submittedAt`/`.status`).
 */

// ---------- ChangePinRequest -> ChangePinRequestDto ----------

fun ChangePinRequest.toDto(): ChangePinRequestDto = ChangePinRequestDto(
    password = newPin,
    repeatPassword = newPin,
)

// ---------- ChangePinResponseDto -> ChangePinResult ----------

fun ChangePinResponseDto.toDomainModel(): ChangePinResult = ChangePinResult(
    resourceId = resourceId,
)
