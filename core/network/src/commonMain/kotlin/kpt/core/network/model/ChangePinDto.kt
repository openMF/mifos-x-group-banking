/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire request DTO for the settings screen's change-PIN submission — `PUT
 * /fineract-provider/api/v1/self/user/updatePassword` (`idea-layer/screens/settings/api.yaml#api[0]`,
 * `change_pin`), matching `api.yaml#dtos.ChangePinRequest` verbatim (`password: String`,
 * `repeatPassword: String`). Both fields carry the SAME new-PIN value — Fineract's self-service
 * `updatePassword` endpoint authenticates the caller via the request's `BasicAuth` header (the
 * member's CURRENT PIN), not a body field, so [currentPin][kpt.core.model.ChangePinRequest.currentPin]
 * on the domain side has no wire counterpart on this DTO (see `ChangePinMappers.kt` kdoc). No
 * field carries a default per `api.yaml` — both are required, so no `@EncodeDefault` is needed
 * here (contrast `LoanRequestPayloadDto.status`).
 *
 * See API.md#dtos — ChangePinRequest.
 */
@Serializable
data class ChangePinRequestDto(
    @SerialName("password") val password: String,
    @SerialName("repeatPassword") val repeatPassword: String,
) {
    companion object {
        /** Bumped when this DTO shape changes (registry `version:` driven). See EC30. */
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire response DTO for the change-PIN submission — the standard Fineract self-service
 * `updatePassword` command-processing envelope, matching `api.yaml#dtos.ChangePinResponse`
 * verbatim (`resourceId: Long`).
 *
 * See API.md#dtos — ChangePinResponse.
 */
@Serializable
data class ChangePinResponseDto(
    @SerialName("resourceId") val resourceId: Long,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}
