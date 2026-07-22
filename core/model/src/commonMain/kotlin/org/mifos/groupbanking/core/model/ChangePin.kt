/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.model

/**
 * Domain model for the settings screen's change-PIN dialog submission input
 * (`SettingsAction.OnSubmitPinChange`, `idea-layer/screens/settings/ui.yaml#components.change_pin_dialog`).
 * [currentPin] authenticates the request via `BasicAuth` at the mapper/service boundary — it is
 * NOT part of `ChangePinRequestDto`'s body (see `ChangePinMappers.kt` kdoc), same "wire-only
 * exclusion" precedent as `LoanRequestPayload.submittedAt`/`.status`. [newPin] resolves to BOTH
 * `ChangePinRequestDto.password` and `.repeatPassword` (Fineract requires the confirmation pair,
 * the member enters it once).
 *
 * See API.md#models — ChangePinRequest.
 */
data class ChangePinRequest(
    val currentPin: String,
    val newPin: String,
)

/**
 * Domain result of a successful change-PIN submission — mirrors `ChangePinResponseDto` 1:1.
 *
 * See API.md#models — ChangePinResult.
 */
data class ChangePinResult(
    val resourceId: Long,
)
