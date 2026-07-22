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

    // REGISTRY:MemberDashboard — personal-dashboard member home (dynamic-key NETWORK_WITH_CACHE, COMP-DASH-001)
    val MemberDashboard = store("memberDashboard")

    // REGISTRY:GroupDashboard — group-dashboard composite home (composite dynamic-key NETWORK_WITH_CACHE, COMP-GRP-001)
    val GroupDashboard = store("groupDashboard")

    // REGISTRY:MemberList — member-list paginated roster (PAGINATED NETWORK_WITH_CACHE, GET /groups/{groupId}/clients)
    val MemberList = store("memberList")

    // REGISTRY:MemberProfile — member-profile composite home (composite dynamic-key NETWORK_WITH_CACHE, get_client + get_client_accounts + get_member_role)
    val MemberProfile = store("memberProfile")

    // REGISTRY:LoanList — loan-list paginated store (PAGINATED NETWORK_WITH_CACHE, GET /groups/{groupId}/loans)
    val LoanList = store("loanList")

    // REGISTRY:LoanDetail — loan-detail single-key composite store (NETWORK_WITH_CACHE, GET /loans/{loanId})
    val LoanDetail = store("loanDetail")

    /** Per-store TTL durations. */
    object Ttl {
        // REGISTRY:GroupTypeConfig — 24h matches data-flow.yaml ttl_seconds:86400; the seed
        // catalogue only changes on server deploys, so an aggressive TTL is correct.
        val GROUP_TYPE_CONFIG: Duration = 24.hours

        // REGISTRY:GroupList — 5m matches data-flow.yaml ttl_seconds:300 (stale-while-revalidate);
        // group membership + health rotate frequently, so a short TTL revalidates aggressively.
        val GROUP_LIST: Duration = 5.minutes

        // REGISTRY:MemberDashboard — 5m matches data-flow.yaml ttl_seconds:300 (stale-while-revalidate);
        // per-group dashboard balances + recent activity rotate frequently.
        val MEMBER_DASHBOARD: Duration = 5.minutes

        // REGISTRY:GroupDashboard — 5m matches data-flow.yaml ttl_seconds:300 (stale-while-revalidate);
        // the composite (group + role + corpus + accounts + activity) revalidates aggressively.
        val GROUP_DASHBOARD: Duration = 5.minutes

        // REGISTRY:MemberList — 2m matches data-flow.yaml ttl_seconds:120 (stale-while-revalidate);
        // a group's roster + per-member balance/loan-status rotate frequently, so a short TTL
        // revalidates aggressively.
        val MEMBER_LIST: Duration = 2.minutes

        // REGISTRY:MemberProfile — 5m matches data-flow.yaml ttl_seconds:300 (stale-while-revalidate);
        // the composite (identity + savings/loan accounts + role datatable) revalidates aggressively.
        val MEMBER_PROFILE: Duration = 5.minutes

        // REGISTRY:LoanList — 3m matches data-flow.yaml ttl_seconds:180 (stale-while-revalidate);
        // a group's loan accounts + per-row outstanding/overdue amounts rotate frequently, so a
        // short TTL revalidates aggressively.
        val LOAN_LIST: Duration = 3.minutes

        // REGISTRY:LoanDetail — 2m matches data-flow.yaml cache_strategy:stale_while_revalidate ttl:120;
        // a loan's outstanding/overdue balances + repayment schedule/history rotate as repayments post,
        // so a short TTL revalidates aggressively.
        val LOAN_DETAIL: Duration = 2.minutes
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
        // REGISTRY:MemberDashboard
        "memberDashboard" to Ttl.MEMBER_DASHBOARD,
        // REGISTRY:GroupDashboard
        "groupDashboard" to Ttl.GROUP_DASHBOARD,
        // REGISTRY:MemberList
        "memberList" to Ttl.MEMBER_LIST,
        // REGISTRY:MemberProfile
        "memberProfile" to Ttl.MEMBER_PROFILE,
        // REGISTRY:LoanList
        "loanList" to Ttl.LOAN_LIST,
        // REGISTRY:LoanDetail
        "loanDetail" to Ttl.LOAN_DETAIL,
    )
}
