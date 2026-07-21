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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

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
object AppStoreRegistry : StoreRegistry() {
    // REGISTRY:GroupTypeConfig — group-type-picker seeded catalogue (NETWORK_WITH_CACHE, COMP-DT-003)
    val GroupTypeConfig = store("groupTypeConfig")

    // REGISTRY:GroupList — group-list paginated store (PAGINATED NETWORK_WITH_CACHE, COMP-GRP-001)
    val GroupList = store("groupList")

    /** Per-store TTL durations. */
    object Ttl {
        // REGISTRY:GroupTypeConfig — 24h matches data-flow.yaml ttl_seconds:86400; the seed
        // catalogue only changes on server deploys, so an aggressive TTL is correct.
        val GROUP_TYPE_CONFIG: Duration = 24.hours

        // REGISTRY:GroupList — 5m matches data-flow.yaml ttl_seconds:300 (stale-while-revalidate);
        // group membership + health rotate frequently, so a short TTL revalidates aggressively.
        val GROUP_LIST: Duration = 5.minutes
    }

    /**
     * Runtime lookup of per-store TTL by qualifier name — paired with the compile-time [Ttl]
     * object so `ScreenDataStream` / `asScreenStream` can resolve a store's TTL dynamically when
     * the qualifier is only known at runtime.
     */
    val ttlByName: Map<String, Duration> = mapOf(
        // REGISTRY:GroupTypeConfig
        "groupTypeConfig" to Ttl.GROUP_TYPE_CONFIG,
        // REGISTRY:GroupList
        "groupList" to Ttl.GROUP_LIST,
    )
}
