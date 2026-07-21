/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.service.loginsignup

import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import org.mifos.groupbanking.core.network.model.AuthResponseDto
import org.mifos.groupbanking.core.network.model.LoginRequestDto
import org.mifos.groupbanking.core.network.model.SelfRegisterRequestDto
import org.mifos.groupbanking.core.network.model.UserProfileDto

/**
 * Ktor client for the companion auth bridge — self-registration, login, and the
 * biometric-refresh profile fetch. See `idea-layer/screens/login-signup/api.yaml` +
 * API.md#services for the endpoint contract (COMP-AUTH-001/002/003).
 *
 * Every method returns [NetworkResult] — never a raw [Result] envelope, never a thrown
 * exception (Mandatory Rule 2). `NetworkResult`/`NetworkError` are the framework's
 * `core-base/network` sealed types (consumed, never edited — Hard Rule #8).
 * [org.mifos.groupbanking.core.data.repository.AuthRepositoryImpl] switches on the sealed
 * result directly, with no try-catch of its own (Mandatory Rule 4).
 */
interface CompanionAuthApi {

    /**
     * `POST /companion/auth/self-register` (COMP-AUTH-001). No auth token required. Creates a
     * companion user account and a linked Fineract client in one atomic request.
     */
    suspend fun selfRegister(request: SelfRegisterRequestDto): NetworkResult<AuthResponseDto, NetworkError>

    /**
     * `POST /companion/auth/login` (COMP-AUTH-002). No auth token required.
     */
    suspend fun login(request: LoginRequestDto): NetworkResult<AuthResponseDto, NetworkError>

    /**
     * `GET /companion/auth/me` (COMP-AUTH-003). Requires a Bearer [sessionToken] — called after
     * a biometric unlock to refresh the session and current group memberships. This endpoint
     * does NOT return a new token; callers keep using [sessionToken] until it expires.
     */
    suspend fun me(sessionToken: String): NetworkResult<UserProfileDto, NetworkError>
}
