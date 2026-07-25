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

    // REGISTRY:SavingsDashboard — savings-dashboard single-key composite store (NETWORK_WITH_CACHE,
    // parallel GET /companion/groups/{groupId}/savings + .../savings/individual)
    val SavingsDashboard = store("savingsDashboard")

    // REGISTRY:ShareOutPreview — share-out-preview single-key store (NETWORK_WITH_CACHE,
    // GET /companion/groups/{groupId}/shareout/preview, COMP-DIST-001)
    val ShareOutPreview = store("shareOutPreview")

    // REGISTRY:MeetingSummary — meeting-summary single-key composite store (NETWORK_WITH_CACHE,
    // GET /datatables/dt_meeting_record/{centerId}, keyed by "$centerId:$meetingNumber")
    val MeetingSummary = store("meetingSummary")

    // REGISTRY:MeetingCalendar — meeting-calendar single-key store (NETWORK_WITH_CACHE,
    // GET /centers/{centerId}/meetings merged with dt_meeting_record)
    val MeetingCalendar = store("meetingCalendar")

    // REGISTRY:FieldOfficerDashboard — field-officer-dashboard aggregate store (composite dynamic-key
    // NETWORK_WITH_CACHE, FR-009 — parallel GET /centers + GET /groups fanned into KPIs + health list)
    val FieldOfficerDashboard = store("fieldOfficerDashboard")

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

        // REGISTRY:SavingsDashboard — 5m matches savings-dashboard/data-flow.yaml
        // cache_strategy:stale_while_revalidate ttl_seconds:300; per-group contribution summaries
        // rotate as members contribute, so a short TTL revalidates aggressively.
        val SAVINGS_DASHBOARD: Duration = 5.minutes

        // REGISTRY:ShareOutPreview — 2m matches share-out-preview/data-flow.yaml
        // cache_strategy:stale_while_revalidate ttl:120; the server-computed distribution preview
        // shifts as savings post pre-share-out, so a short TTL revalidates aggressively.
        val SHARE_OUT_PREVIEW: Duration = 2.minutes

        // REGISTRY:MeetingSummary — 10m matches meeting-summary/data-flow.yaml
        // cache.strategy:stale_while_revalidate ttl_seconds:600; a completed meeting record is
        // effectively immutable once closed, so a longer TTL is correct (still revalidates on entry).
        val MEETING_SUMMARY: Duration = 10.minutes

        // REGISTRY:MeetingCalendar — 5m matches data-flow.yaml ttl_seconds:300 (stale-while-revalidate);
        // a center's scheduled/past meetings + attendance/collected figures rotate as meetings run.
        val MEETING_CALENDAR: Duration = 5.minutes

        // REGISTRY:FieldOfficerDashboard — 5m matches field-officer-dashboard/data-flow.yaml
        // ttl_seconds:300 (stale-while-revalidate); cross-group KPIs + per-group health rotate as
        // members contribute / loans post, so a short TTL revalidates aggressively.
        val FIELD_OFFICER_DASHBOARD: Duration = 5.minutes
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
        // REGISTRY:SavingsDashboard
        "savingsDashboard" to Ttl.SAVINGS_DASHBOARD,
        // REGISTRY:ShareOutPreview
        "shareOutPreview" to Ttl.SHARE_OUT_PREVIEW,
        // REGISTRY:MeetingSummary
        "meetingSummary" to Ttl.MEETING_SUMMARY,
        // REGISTRY:MeetingCalendar
        "meetingCalendar" to Ttl.MEETING_CALENDAR,
        // REGISTRY:FieldOfficerDashboard
        "fieldOfficerDashboard" to Ttl.FIELD_OFFICER_DASHBOARD,
    )
}
