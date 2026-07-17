/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.store

import kpt.core.base.store.infra.StoreRegistry

/**
 * Application-level [StoreRegistry] — the single named-qualifier registry for every
 * `org.mobilenativefoundation.store.store5.Store` the app exposes.
 *
 * The 4 demo stores ([ExchangeRates], [RateHistory], [CoinMarkets], [CoinDetail]) live
 * here as the canonical example shape for forks. Add your own next to them, e.g.:
 *
 * ```kotlin
 * object AppStoreRegistry : StoreRegistry() {
 *     // Demo stores (kept as forkable examples)
 *     val ExchangeRates = store("exchangeRates")
 *     // …
 *
 *     // Your app's stores
 *     val UserProfile  = store("userProfile")
 *     val Transactions = store("transactions")
 * }
 * ```
 *
 * Then reference the qualifier from Koin DI in [appStoreModule]:
 *
 * ```kotlin
 * single<Store<UserId, UserProfile>>(qualifier = AppStoreRegistry.UserProfile) { ... }
 * ```
 *
 * Centralizing here gives a one-place audit of every Store the app owns and prevents
 * qualifier-name collisions across feature modules.
 */
object AppStoreRegistry : StoreRegistry()
