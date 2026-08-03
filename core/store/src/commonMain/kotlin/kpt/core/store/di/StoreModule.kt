/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.store.di

import kpt.core.store.AppStoreRegistry
import kpt.core.store.infra.StoreCacheManager
import kpt.core.store.infra.impl.StoreCacheManagerImpl
import org.koin.core.module.Module
import org.koin.dsl.module
import org.mifos.groupbanking.core.store.fieldofficerdashboard.impl.provideFieldOfficerDashboardStore
import org.mifos.groupbanking.core.store.groupdashboard.impl.provideGroupDashboardStore
import org.mifos.groupbanking.core.store.grouplist.impl.provideGroupsPagingStore
import org.mifos.groupbanking.core.store.grouptypepicker.impl.provideGroupTypeConfigStore
import org.mifos.groupbanking.core.store.loandetail.impl.provideLoanDetailStore
import org.mifos.groupbanking.core.store.loanlist.impl.provideLoansPagingStore
import org.mifos.groupbanking.core.store.meetingcalendar.impl.provideMeetingCalendarStore
import org.mifos.groupbanking.core.store.meetingsummary.impl.provideMeetingSummaryStore
import org.mifos.groupbanking.core.store.memberlist.impl.provideMembersPagingStore
import org.mifos.groupbanking.core.store.memberprofile.impl.provideMemberProfileStore
import org.mifos.groupbanking.core.store.organizerdashboard.impl.provideOrganizerDashboardStore
import org.mifos.groupbanking.core.store.personaldashboard.impl.provideMemberDashboardStore
import org.mifos.groupbanking.core.store.previousmeetingreview.impl.provideMeetingAttendanceStore

/**
 * Koin module for app-level Store wiring.
 *
 * Forks register their `Store` instances here, qualifier-bound via [AppStoreRegistry].
 * The 4 demo stores ship as forkable examples — add your own `single(qualifier = ...)`
 * blocks next to them.
 *
 * Wire into the Koin start-up:
 * ```kotlin
 * startKoin {
 *     modules(appStoreModule, /* ...other modules */)
 * }
 * ```
 */
val appStoreModule: Module = module {
    // Store cache manager — clears all registered caches on logout (registration-based)
    single<StoreCacheManager> {
        StoreCacheManagerImpl(
            bookkeeperDao = get(),
            draftDao = get(),
        )
    }

    // MODULE:GroupTypeConfig — group-type-picker seeded catalogue store (NETWORK_WITH_CACHE).
    // Internal to the store seam — exposed to UI only through GroupTypeConfigRepository.
    single(AppStoreRegistry.GroupTypeConfig) {
        provideGroupTypeConfigStore(api = get(), dao = get())
    }

    // MODULE:GroupTypeConfig — register for logout cache clearing (clears in-memory + Room SoT).
    single(createdAtStart = true) {
        val mgr = get<StoreCacheManager>() as StoreCacheManagerImpl
        mgr.register(get(AppStoreRegistry.GroupTypeConfig))
    }

    // MODULE:GroupList — group-list PAGINATED store (NETWORK_WITH_CACHE, COMP-GRP-001).
    // Internal to the store seam — exposed to UI only through GroupRepository.asPagingScreenStream().
    single(AppStoreRegistry.GroupList) {
        provideGroupsPagingStore(api = get(), dao = get())
    }

    // MODULE:GroupList — register for logout cache clearing (clears in-memory + Room SoT pages).
    single(createdAtStart = true) {
        val mgr = get<StoreCacheManager>() as StoreCacheManagerImpl
        mgr.register(get(AppStoreRegistry.GroupList))
    }

    // MODULE:MemberDashboard — personal-dashboard member home store (dynamic-key NETWORK_WITH_CACHE).
    // Internal to the store seam — exposed to UI only through MemberDashboardRepository.asScreenStream().
    single(AppStoreRegistry.MemberDashboard) {
        provideMemberDashboardStore(api = get(), dao = get())
    }

    // MODULE:MemberDashboard — register for logout cache clearing (clears in-memory + Room SoT rows).
    single(createdAtStart = true) {
        val mgr = get<StoreCacheManager>() as StoreCacheManagerImpl
        mgr.register(get(AppStoreRegistry.MemberDashboard))
    }

    // MODULE:GroupDashboard — group-dashboard COMPOSITE store (dynamic-key NETWORK_WITH_CACHE, COMP-GRP-001).
    // Internal to the store seam — exposed to UI only through GroupDashboardRepository.asScreenStream().
    single(AppStoreRegistry.GroupDashboard) {
        provideGroupDashboardStore(api = get(), dao = get())
    }

    // MODULE:GroupDashboard — register for logout cache clearing (clears in-memory + Room SoT rows).
    single(createdAtStart = true) {
        val mgr = get<StoreCacheManager>() as StoreCacheManagerImpl
        mgr.register(get(AppStoreRegistry.GroupDashboard))
    }

    // MODULE:MemberList — member-list PAGINATED store (NETWORK_WITH_CACHE, GET /groups/{groupId}/clients).
    // Internal to the store seam — exposed to UI only through MemberRepository.asPagingScreenStream().
    single(AppStoreRegistry.MemberList) {
        provideMembersPagingStore(api = get(), dao = get())
    }

    // MODULE:MemberList — register for logout cache clearing (clears in-memory + Room SoT pages).
    single(createdAtStart = true) {
        val mgr = get<StoreCacheManager>() as StoreCacheManagerImpl
        mgr.register(get(AppStoreRegistry.MemberList))
    }

    // MODULE:MemberProfile — member-profile COMPOSITE store (dynamic-key NETWORK_WITH_CACHE,
    // get_client + get_client_accounts + get_member_role). Internal to the store seam — exposed to
    // UI only through MemberProfileRepository.asScreenStream(); the update_member_role write
    // invalidates via store.clear(clientId) in MemberProfileRepositoryImpl.
    single(AppStoreRegistry.MemberProfile) {
        provideMemberProfileStore(api = get(), dao = get())
    }

    // MODULE:MemberProfile — register for logout cache clearing (clears in-memory + Room SoT rows).
    single(createdAtStart = true) {
        val mgr = get<StoreCacheManager>() as StoreCacheManagerImpl
        mgr.register(get(AppStoreRegistry.MemberProfile))
    }

    // MODULE:LoanList — loan-list PAGINATED store (NETWORK_WITH_CACHE, GET /groups/{groupId}/loans).
    // Internal to the store seam — exposed to UI only through LoanRepository.asPagingScreenStream().
    single(AppStoreRegistry.LoanList) {
        provideLoansPagingStore(api = get(), dao = get())
    }

    // MODULE:LoanList — register for logout cache clearing (clears in-memory + Room SoT pages).
    single(createdAtStart = true) {
        val mgr = get<StoreCacheManager>() as StoreCacheManagerImpl
        mgr.register(get(AppStoreRegistry.LoanList))
    }

    // MODULE:LoanDetail — loan-detail single-key COMPOSITE store (NETWORK_WITH_CACHE, GET /loans/{loanId}).
    // Internal to the store seam — exposed to UI only through LoanDetailRepository.asScreenStream().
    single(AppStoreRegistry.LoanDetail) {
        provideLoanDetailStore(api = get(), dao = get())
    }

    // MODULE:LoanDetail — register for logout cache clearing (clears in-memory + Room SoT rows).
    single(createdAtStart = true) {
        val mgr = get<StoreCacheManager>() as StoreCacheManagerImpl
        mgr.register(get(AppStoreRegistry.LoanDetail))
    }

    // MODULE:MeetingSummary — meeting-summary single-key COMPOSITE store (NETWORK_WITH_CACHE,
    // GET /datatables/dt_meeting_record/{centerId}). Internal to the store seam — exposed to UI
    // only through MeetingSummaryRepository.asScreenStream().
    single(AppStoreRegistry.MeetingSummary) {
        provideMeetingSummaryStore(api = get(), dao = get())
    }

    // MODULE:MeetingSummary — register for logout cache clearing (clears in-memory + Room SoT rows).
    single(createdAtStart = true) {
        val mgr = get<StoreCacheManager>() as StoreCacheManagerImpl
        mgr.register(get(AppStoreRegistry.MeetingSummary))
    }

    // MODULE:MeetingCalendar — meeting-calendar single-key store (NETWORK_WITH_CACHE,
    // GET /centers/{centerId}/meetings merged with dt_meeting_record). Internal to the store seam —
    // exposed to UI only through MeetingRepository.asScreenStream().
    single(AppStoreRegistry.MeetingCalendar) {
        provideMeetingCalendarStore(api = get(), dao = get())
    }

    // MODULE:MeetingCalendar — register for logout cache clearing (clears in-memory + Room SoT rows).
    single(createdAtStart = true) {
        val mgr = get<StoreCacheManager>() as StoreCacheManagerImpl
        mgr.register(get(AppStoreRegistry.MeetingCalendar))
    }

    // MODULE:FieldOfficerDashboard — field-officer-dashboard AGGREGATE store (composite dynamic-key
    // NETWORK_WITH_CACHE, FR-009 — parallel GET /centers + GET /groups). Internal to the store seam
    // — exposed to UI only through FieldOfficerDashboardRepository.asScreenStream().
    single(AppStoreRegistry.FieldOfficerDashboard) {
        provideFieldOfficerDashboardStore(api = get(), dao = get())
    }

    // MODULE:FieldOfficerDashboard — register for logout cache clearing (clears in-memory + Room SoT rows).
    single(createdAtStart = true) {
        val mgr = get<StoreCacheManager>() as StoreCacheManagerImpl
        mgr.register(get(AppStoreRegistry.FieldOfficerDashboard))
    }

    // MODULE:OrganizerDashboard — organizer-dashboard SINGLE-KEY store (NETWORK_WITH_CACHE,
    // GET /companion/organizer/dashboard). Internal to the store seam — exposed to UI only through
    // OrganizerDashboardRepository.asScreenStream().
    single(AppStoreRegistry.OrganizerDashboard) {
        provideOrganizerDashboardStore(api = get(), dao = get())
    }

    // MODULE:OrganizerDashboard — register for logout cache clearing (clears in-memory + Room SoT rows).
    single(createdAtStart = true) {
        val mgr = get<StoreCacheManager>() as StoreCacheManagerImpl
        mgr.register(get(AppStoreRegistry.OrganizerDashboard))
    }

    // MODULE:MeetingAttendance — previous-meeting-review per-member attendance SINGLE-KEY store
    // (NETWORK_WITH_CACHE, GET /datatables/dt_meeting_attendance/{meetingId}). Internal to the store
    // seam — exposed to UI only through PreviousMeetingReviewRepository (merged with the reused
    // MeetingSummaryStore record read).
    single(AppStoreRegistry.MeetingAttendance) {
        provideMeetingAttendanceStore(api = get(), dao = get())
    }

    // MODULE:MeetingAttendance — register for logout cache clearing (clears in-memory + Room SoT rows).
    single(createdAtStart = true) {
        val mgr = get<StoreCacheManager>() as StoreCacheManagerImpl
        mgr.register(get(AppStoreRegistry.MeetingAttendance))
    }
}
