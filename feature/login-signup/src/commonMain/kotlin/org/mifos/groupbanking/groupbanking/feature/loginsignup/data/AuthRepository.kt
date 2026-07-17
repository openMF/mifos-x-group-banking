/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.groupbanking.feature.loginsignup.data

import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkMonitor
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.RetryPolicy
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.executeWithRetry
import org.mifos.groupbanking.groupbanking.feature.loginsignup.model.AuthSession
import org.mifos.groupbanking.groupbanking.feature.loginsignup.model.LoginRequest
import org.mifos.groupbanking.groupbanking.feature.loginsignup.model.SelfRegisterRequest
import org.mifos.groupbanking.groupbanking.feature.loginsignup.model.UserProfile
import org.mifos.groupbanking.groupbanking.feature.loginsignup.network.CompanionAuthApi
import org.mifos.groupbanking.groupbanking.feature.loginsignup.network.LoginRequestDto
import org.mifos.groupbanking.groupbanking.feature.loginsignup.network.SelfRegisterRequestDto

/**
 * Repository over the companion-auth tools (COMP-AUTH-001/002/003).
 *
 * Auth is **network-required** — every call is guarded by the network monitor's
 * `executeWithRetry`, which raises `OfflineException` when there is no connectivity
 * (AC7 / no offline fallback). On a successful auth the [sessionToken] is persisted
 * to the [SessionStore] (AC6).
 */
interface AuthRepository {
    /** COMP-AUTH-001 — self-register a new companion user. */
    suspend fun selfRegister(request: SelfRegisterRequest): AuthSession

    /** COMP-AUTH-002 — login with password or PIN. */
    suspend fun login(request: LoginRequest): AuthSession

    /**
     * COMP-AUTH-003 — refresh the profile + group memberships for the currently
     * stored session token (used by biometric re-auth, AC4).
     */
    suspend fun me(): UserProfile
}

class AuthRepositoryImpl(
    private val api: CompanionAuthApi,
    private val sessionStore: SessionStore,
    private val networkMonitor: NetworkMonitor,
) : AuthRepository {

    override suspend fun selfRegister(request: SelfRegisterRequest): AuthSession =
        networkMonitor.executeWithRetry(RetryPolicy { maxAttempts = 1 }) {
            api.selfRegister(
                SelfRegisterRequestDto(
                    name = request.name,
                    emailPhone = request.emailPhone,
                    password = request.password,
                ),
            ).toDomain()
        }.also { sessionStore.write(it.sessionToken) }

    override suspend fun login(request: LoginRequest): AuthSession =
        networkMonitor.executeWithRetry(RetryPolicy { maxAttempts = 1 }) {
            api.login(
                LoginRequestDto(
                    emailPhone = request.emailPhone,
                    password = request.password,
                    pin = request.pin,
                ),
            ).toDomain()
        }.also { sessionStore.write(it.sessionToken) }

    override suspend fun me(): UserProfile {
        val token = sessionStore.read()
            ?: throw IllegalStateException("No active session token — sign in first")
        return networkMonitor.executeWithRetry(RetryPolicy { maxAttempts = 1 }) {
            api.me(token).toDomain()
        }
    }
}
