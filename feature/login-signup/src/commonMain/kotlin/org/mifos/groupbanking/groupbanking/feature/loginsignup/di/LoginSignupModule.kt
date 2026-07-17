/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.groupbanking.feature.loginsignup.di

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.mifos.groupbanking.groupbanking.feature.loginsignup.data.AuthRepository
import org.mifos.groupbanking.groupbanking.feature.loginsignup.data.AuthRepositoryImpl
import org.mifos.groupbanking.groupbanking.feature.loginsignup.data.InMemorySessionStore
import org.mifos.groupbanking.groupbanking.feature.loginsignup.data.SessionStore
import org.mifos.groupbanking.groupbanking.feature.loginsignup.network.CompanionAuthApi
import org.mifos.groupbanking.groupbanking.feature.loginsignup.ui.LoginSignupViewModel

/** Koin qualifier for the feature-local companion-auth HttpClient. */
private val CompanionAuthHttpClient = named("companion-auth-http-client")

val LoginSignupModule = module {

    // Feature-local Ktor client for the companion-auth endpoints (COMP-AUTH-*).
    // Kept isolated (named) so it never collides with the app's shared clients.
    single(CompanionAuthHttpClient) {
        HttpClient {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                        explicitNulls = false
                    },
                )
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 30_000
            }
        }
    }

    single { CompanionAuthApi(client = get(CompanionAuthHttpClient)) }

    // AC6 — encrypted session store. Swap this binding for a secure-prefs impl
    // (core-base/security) when wiring the app; the interface stays the same.
    single<SessionStore> { InMemorySessionStore() }

    single<AuthRepository> {
        AuthRepositoryImpl(
            api = get(),
            sessionStore = get(),
            // NetworkMonitor provided by core:data's DataModule.
            networkMonitor = get(),
        )
    }

    viewModel { LoginSignupViewModel(authRepository = get()) }
}
