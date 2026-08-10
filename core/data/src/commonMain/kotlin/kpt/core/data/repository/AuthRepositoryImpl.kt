/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.data.repository

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.Flow
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.data.user.UserDataRepository
import kpt.core.datastore.session.CompanionSessionStore
import kpt.core.model.AuthSession
import kpt.core.model.LoginCredentials
import kpt.core.model.SelfRegistration
import kpt.core.model.UserProfile
import kpt.core.network.mapper.toDomainModel
import kpt.core.network.mapper.toDto
import kpt.core.network.service.loginsignup.CompanionAuthApi

private const val TAG = "AuthRepository"

/**
 * See [AuthRepository] KDoc for the Store5-branch rationale (`business_logic.kind: processor`).
 * No try-catch here — every method is a plain `when` over [CompanionAuthApi]'s [NetworkResult].
 *
 * See API.md#repositories — AuthRepository.
 */
class AuthRepositoryImpl(
    private val api: CompanionAuthApi,
    private val sessionStore: CompanionSessionStore,
    private val cacheCleaner: LocalCacheCleaner,
    private val userDataRepository: UserDataRepository,
) : AuthRepository {

    // Persist the resolvable auth flag alongside the session token so RootNavViewModel can route
    // an already-signed-in user straight to the dashboard on next app open (instead of always
    // showing login). isUnlocked=true because this app has no passcode gate by default
    // (isPasscodeEnabled=false); the passcode flow, when enabled, still governs UserLocked.
    private suspend fun markAuthenticated() {
        userDataRepository.setIsAuthenticated(true)
        userDataRepository.setIsUnlocked(true)
    }

    override val currentSession: Flow<AuthSession?> = sessionStore.session

    override suspend fun selfRegister(registration: SelfRegistration): NetworkResult<AuthSession, NetworkError> {
        Logger.d(TAG) { "selfRegister: submitting" }
        return when (val result = api.selfRegister(registration.toDto())) {
            is NetworkResult.Success -> {
                val session = result.data.toDomainModel()
                sessionStore.save(session.userId, session.sessionToken, session.tokenExpiresAt)
                markAuthenticated()
                Logger.i(TAG) { "selfRegister succeeded userId=${session.userId}" }
                NetworkResult.Success(session)
            }
            is NetworkResult.Error -> {
                Logger.e(TAG) { "selfRegister failed: ${result.error}" }
                result
            }
        }
    }

    override suspend fun login(credentials: LoginCredentials): NetworkResult<AuthSession, NetworkError> {
        Logger.d(TAG) { "login: submitting" }
        return when (val result = api.login(credentials.toDto())) {
            is NetworkResult.Success -> {
                val session = result.data.toDomainModel()
                sessionStore.save(session.userId, session.sessionToken, session.tokenExpiresAt)
                markAuthenticated()
                Logger.i(TAG) { "login succeeded userId=${session.userId}" }
                NetworkResult.Success(session)
            }
            is NetworkResult.Error -> {
                Logger.e(TAG) { "login failed: ${result.error}" }
                result
            }
        }
    }

    override suspend fun refreshSession(sessionToken: String): NetworkResult<UserProfile, NetworkError> {
        Logger.d(TAG) { "refreshSession: biometric/me refresh" }
        return when (val result = api.me(sessionToken)) {
            is NetworkResult.Success -> {
                Logger.i(TAG) { "refreshSession succeeded userId=${result.data.userId}" }
                NetworkResult.Success(result.data.toDomainModel())
            }
            is NetworkResult.Error -> {
                Logger.e(TAG) { "refreshSession failed: ${result.error}" }
                result
            }
        }
    }

    override suspend fun clearSession() {
        sessionStore.clear()
        // Clear the resolvable auth flag so the NEXT app open resolves to login (not dashboard).
        userDataRepository.setIsAuthenticated(false)
        userDataRepository.setIsUnlocked(false)
        // Wipe ALL locally-cached user-scoped state (per-user dashboards, loans, savings, member
        // lists, freshness stamps, AND the offline sync-queue). Room caches are keyed by domain id
        // (e.g. member_dashboard_cache by groupId), NOT by user — so without this, the NEXT user to
        // sign in on this device reads the PREVIOUS user's cached rows (e.g. the personal dashboard
        // greeting stuck on the prior member's name). Clearing on logout is exactly the contract the
        // logout dialog already promises ("Any unsynced changes will be lost").
        cacheCleaner.clearAll()
        Logger.i(TAG) { "session cleared + local caches wiped" }
    }
}
