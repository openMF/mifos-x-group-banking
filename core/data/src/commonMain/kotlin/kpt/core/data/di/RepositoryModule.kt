/*
 * Copyright 2024 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.data.di

import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkMonitorProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kpt.core.base.common.di.CommonModule
import kpt.core.base.store.infra.FetchedAtRepository
import kpt.core.data.infra.NetworkMonitor
import kpt.core.data.infra.impl.RoomFetchedAtRepository
import kpt.core.data.user.UserDataRepository
import kpt.core.data.user.UserLogoutManager
import kpt.core.data.user.impl.UserDataRepositoryImpl
import kpt.core.data.user.impl.UserLogoutManagerImpl
import kpt.core.database.AppDatabase
import kpt.core.database.di.DatabaseModule
import kpt.core.datastore.di.DatastoreModule
import kpt.core.network.di.NetworkModule
import kpt.core.store.AppStoreRegistry
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.mifos.groupbanking.core.data.demo.DemoSessionManager
import org.mifos.groupbanking.core.data.demo.DemoSessionManagerImpl
import org.mifos.groupbanking.core.data.repository.AuthRepository
import org.mifos.groupbanking.core.data.repository.AuthRepositoryImpl
import org.mifos.groupbanking.core.data.repository.ChangePinRepository
import org.mifos.groupbanking.core.data.repository.ChangePinRepositoryImpl
import org.mifos.groupbanking.core.data.repository.FieldOfficerDashboardRepository
import org.mifos.groupbanking.core.data.repository.FieldOfficerDashboardRepositoryImpl
import org.mifos.groupbanking.core.data.repository.GroupCreateRepository
import org.mifos.groupbanking.core.data.repository.GroupCreateRepositoryImpl
import org.mifos.groupbanking.core.data.repository.GroupDashboardRepository
import org.mifos.groupbanking.core.data.repository.GroupDashboardRepositoryImpl
import org.mifos.groupbanking.core.data.repository.GroupRepository
import org.mifos.groupbanking.core.data.repository.GroupRepositoryImpl
import org.mifos.groupbanking.core.data.repository.GroupTypeConfigRepository
import org.mifos.groupbanking.core.data.repository.GroupTypeConfigRepositoryImpl
import org.mifos.groupbanking.core.data.repository.InvitationRepository
import org.mifos.groupbanking.core.data.repository.InvitationRepositoryImpl
import org.mifos.groupbanking.core.data.repository.LoanApplyRepository
import org.mifos.groupbanking.core.data.repository.LoanApplyRepositoryImpl
import org.mifos.groupbanking.core.data.repository.LoanDetailRepository
import org.mifos.groupbanking.core.data.repository.LoanDetailRepositoryImpl
import org.mifos.groupbanking.core.data.repository.LoanRepaymentRepository
import org.mifos.groupbanking.core.data.repository.LoanRepaymentRepositoryImpl
import org.mifos.groupbanking.core.data.repository.LoanRepository
import org.mifos.groupbanking.core.data.repository.LoanRepositoryImpl
import org.mifos.groupbanking.core.data.repository.LoanRequestRepository
import org.mifos.groupbanking.core.data.repository.LoanRequestRepositoryImpl
import org.mifos.groupbanking.core.data.repository.LoanWriteoffRepository
import org.mifos.groupbanking.core.data.repository.LoanWriteoffRepositoryImpl
import org.mifos.groupbanking.core.data.repository.LocalCacheCleaner
import org.mifos.groupbanking.core.data.repository.MeetingConductRepository
import org.mifos.groupbanking.core.data.repository.MeetingConductRepositoryImpl
import org.mifos.groupbanking.core.data.repository.MeetingRepository
import org.mifos.groupbanking.core.data.repository.MeetingRepositoryImpl
import org.mifos.groupbanking.core.data.repository.MeetingSummaryRepository
import org.mifos.groupbanking.core.data.repository.MeetingSummaryRepositoryImpl
import org.mifos.groupbanking.core.data.repository.MemberAddRepository
import org.mifos.groupbanking.core.data.repository.MemberAddRepositoryImpl
import org.mifos.groupbanking.core.data.repository.MemberDashboardRepository
import org.mifos.groupbanking.core.data.repository.MemberDashboardRepositoryImpl
import org.mifos.groupbanking.core.data.repository.MemberInviteRepository
import org.mifos.groupbanking.core.data.repository.MemberInviteRepositoryImpl
import org.mifos.groupbanking.core.data.repository.MemberProfileRepository
import org.mifos.groupbanking.core.data.repository.MemberProfileRepositoryImpl
import org.mifos.groupbanking.core.data.repository.MemberRepository
import org.mifos.groupbanking.core.data.repository.MemberRepositoryImpl
import org.mifos.groupbanking.core.data.repository.OrganizerDashboardRepository
import org.mifos.groupbanking.core.data.repository.OrganizerDashboardRepositoryImpl
import org.mifos.groupbanking.core.data.repository.PreviousMeetingReviewRepository
import org.mifos.groupbanking.core.data.repository.PreviousMeetingReviewRepositoryImpl
import org.mifos.groupbanking.core.data.repository.RoomLocalCacheCleaner
import org.mifos.groupbanking.core.data.repository.SavingsRepository
import org.mifos.groupbanking.core.data.repository.SavingsRepositoryImpl
import org.mifos.groupbanking.core.data.repository.ShareOutRepository
import org.mifos.groupbanking.core.data.repository.ShareOutRepositoryImpl
import org.mifos.groupbanking.core.data.repository.SyncManager
import org.mifos.groupbanking.core.data.repository.SyncManagerImpl
import org.mifos.groupbanking.core.data.repository.SyncQueueRepository
import org.mifos.groupbanking.core.data.repository.SyncQueueRepositoryImpl

val DataModule = module {
    includes(platformModule, CommonModule, DatabaseModule, DatastoreModule, NetworkModule)

    single<NetworkMonitor> { NetworkMonitorProvider.install() }
    singleOf(::UserDataRepositoryImpl) bind UserDataRepository::class

    // login-signup client stack (COMP-AUTH-001/002/003) — Store5-free (business_logic.kind:
    // processor), wraps CompanionAuthApi (NetworkModule) + CompanionSessionStore (DatastoreModule).
    single<LocalCacheCleaner> { RoomLocalCacheCleaner(database = get<AppDatabase>()) }
    single<AuthRepository> { AuthRepositoryImpl(api = get(), sessionStore = get(), cacheCleaner = get(), userDataRepository = get()) }

    // login-signup Demo Explore offline guest session (ui.yaml#demo_confirm_dialog,
    // flow.yaml#on_demo_confirm). Seeds the offline read caches the Demo-Explore mode browses —
    // organizer-dashboard (landing) + group-list + group-dashboard + member-list + loan-list — from
    // the bundled PROJECT_DEMO_DATA fixture, then persists a synthetic demo session. 100% offline,
    // no companion API / Fineract. NOT seeded: savings-dashboard (its SavingsRepository is
    // Store5-free / network-only — no Room SourceOfTruth to write). Not a Store5 read-store
    // (business_logic.kind: processor) — a session-scoped seed manager, same branch as SyncManager /
    // UserLogoutManager below. Depends on CompanionSessionStore (DatastoreModule) + the four read
    // DAOs it seeds (DatabaseModule) + FetchedAtRepository (below).
    single<DemoSessionManager> {
        DemoSessionManagerImpl(
            sessionStore = get(),
            organizerDashboardDao = get<AppDatabase>().organizerDashboardDao,
            groupListDao = get<AppDatabase>().groupListDao,
            groupDashboardDao = get<AppDatabase>().groupDashboardDao,
            memberListDao = get<AppDatabase>().memberListDao,
            loanListDao = get<AppDatabase>().loanListDao,
            fetchedAtRepository = get(),
        )
    }

    // group-type-picker seeded catalogue (COMP-DT-003) — wraps the NETWORK_WITH_CACHE
    // GroupTypeConfigStore (bound via AppStoreRegistry.GroupTypeConfig in appStoreModule) and
    // surfaces the offline-first .asScreenStream() read.
    single<GroupTypeConfigRepository> {
        GroupTypeConfigRepositoryImpl(
            groupTypeConfigStore = get(AppStoreRegistry.GroupTypeConfig),
            networkMonitor = get(),
            fetchedAtRepository = get(),
        )
    }

    // group-list (COMP-GRP-001) — wraps the PAGINATED NETWORK_WITH_CACHE GroupsPagingStore
    // (bound via AppStoreRegistry.GroupList in appStoreModule) and surfaces the offline-first
    // .asPagingScreenStream() read (paged ScreenState<List<Group>> + load-more + pull-to-refresh).
    single<GroupRepository> {
        GroupRepositoryImpl(
            groupsPagingStore = get(AppStoreRegistry.GroupList),
            networkMonitor = get(),
            fetchedAtRepository = get(),
        )
    }

    // personal-dashboard member home (COMP-DASH-001) — wraps the dynamic-key NETWORK_WITH_CACHE
    // MemberDashboardStore (bound via AppStoreRegistry.MemberDashboard in appStoreModule) and
    // surfaces the offline-first .asScreenStream() read (per-group ScreenState<MemberDashboard>).
    single<MemberDashboardRepository> {
        MemberDashboardRepositoryImpl(
            memberDashboardStore = get(AppStoreRegistry.MemberDashboard),
            networkMonitor = get(),
            fetchedAtRepository = get(),
        )
    }

    // group-dashboard (COMP-GRP-001) — wraps the COMPOSITE dynamic-key NETWORK_WITH_CACHE
    // GroupDashboardStore (bound via AppStoreRegistry.GroupDashboard in appStoreModule) and
    // surfaces the offline-first .asScreenStream() read (per-group ScreenState<GroupDashboard>,
    // the 4-way parallel fan-in of get_group + get_viewer_role + get_group_corpus + get_group_accounts).
    single<GroupDashboardRepository> {
        GroupDashboardRepositoryImpl(
            groupDashboardStore = get(AppStoreRegistry.GroupDashboard),
            networkMonitor = get(),
            fetchedAtRepository = get(),
        )
    }

    // field-officer-dashboard (FR-009) — wraps the composite dynamic-key NETWORK_WITH_CACHE
    // FieldOfficerDashboardStore (bound via AppStoreRegistry.FieldOfficerDashboard in appStoreModule)
    // and surfaces the offline-first .asScreenStream() read (per-staff ScreenState<FieldOfficerDashboard>,
    // the parallel fan-in of get_centers_for_staff + get_groups_for_staff). The Store5-free CSV export
    // (runReport) takes FieldOfficerApi directly (get()) — no read-stream to cache.
    single<FieldOfficerDashboardRepository> {
        FieldOfficerDashboardRepositoryImpl(
            fieldOfficerDashboardStore = get(AppStoreRegistry.FieldOfficerDashboard),
            fieldOfficerApi = get(),
            networkMonitor = get(),
            fetchedAtRepository = get(),
        )
    }

    // organizer-dashboard hub (GET /companion/organizer/dashboard) — wraps the single-key
    // NETWORK_WITH_CACHE OrganizerDashboardStore (bound via AppStoreRegistry.OrganizerDashboard in
    // appStoreModule) and surfaces the offline-first .asScreenStream() read
    // (ScreenState<OrganizerDashboardSummary>, KPIs scoped to "my groups" + today's schedule +
    // recent activity). Read-only — no write path (data-flow.yaml#sync_queue: []).
    single<OrganizerDashboardRepository> {
        OrganizerDashboardRepositoryImpl(
            organizerDashboardStore = get(AppStoreRegistry.OrganizerDashboard),
            networkMonitor = get(),
            fetchedAtRepository = get(),
        )
    }

    // member-list (GET /groups/{groupId}/clients) — wraps the PAGINATED NETWORK_WITH_CACHE
    // MembersPagingStore (bound via AppStoreRegistry.MemberList in appStoreModule) and surfaces the
    // offline-first .asPagingScreenStream() read (paged ScreenState<List<Member>> + load-more +
    // pull-to-refresh, per-group via the groupId threaded into the store key).
    single<MemberRepository> {
        MemberRepositoryImpl(
            membersPagingStore = get(AppStoreRegistry.MemberList),
            networkMonitor = get(),
            fetchedAtRepository = get(),
        )
    }

    // member-profile (get_client + get_client_accounts + get_member_role composite read;
    // update_member_role write) — wraps the COMPOSITE dynamic-key NETWORK_WITH_CACHE
    // MemberProfileStore (bound via AppStoreRegistry.MemberProfile in appStoreModule) and surfaces
    // the offline-first .asScreenStream() read (per-client ScreenState<MemberProfileDetail>). The
    // chairperson-gated updateMemberRole write also takes the MemberProfileApi directly (get()) to
    // dispatch the PUT and invalidate the per-client cache via store.clear(clientId) on success
    // (data-flow.yaml#cache.strategy: invalidate) — routed through the store, never a DAO bypass.
    single<MemberProfileRepository> {
        MemberProfileRepositoryImpl(
            memberProfileStore = get(AppStoreRegistry.MemberProfile),
            memberProfileApi = get(),
            networkMonitor = get(),
            fetchedAtRepository = get(),
        )
    }

    // loan-list (GET /groups/{groupId}/loans) — wraps the PAGINATED NETWORK_WITH_CACHE
    // LoansPagingStore (bound via AppStoreRegistry.LoanList in appStoreModule) and surfaces the
    // offline-first .asPagingScreenStream() read (paged ScreenState<List<LoanSummary>> + load-more +
    // pull-to-refresh, per-group via the groupId threaded into the store key). Status-filter chips
    // filter the accumulated list client-side in the ViewModel — no re-key, no DAO bypass.
    //
    // Also wires getLoansForClient (GET /clients/{clientId}/loans) — the personal-loans
    // member-loans read, ADDITIVE to loansPagingStream. Store5-free direct passthrough over the
    // ALREADY-REGISTERED LoanApi single below (NetworkModule) — no new store, no new DI binding.
    single<LoanRepository> {
        LoanRepositoryImpl(
            loansPagingStore = get(AppStoreRegistry.LoanList),
            networkMonitor = get(),
            fetchedAtRepository = get(),
            loanApi = get(),
        )
    }

    // loan-detail (GET /loans/{loanId}?associations=repaymentSchedule,transactions) — wraps the
    // single-key composite NETWORK_WITH_CACHE LoanDetailStore (bound via AppStoreRegistry.LoanDetail
    // in appStoreModule) and surfaces the offline-first .asScreenStream() read (per-loan
    // ScreenState<LoanDetailResponse> = loan header + repayment schedule + transaction history). NEW
    // repository — deliberately separate from the paginated LoanRepository (loan-list) so the
    // single-key composite read never overloads the paged list surface.
    single<LoanDetailRepository> {
        LoanDetailRepositoryImpl(
            loanDetailStore = get(AppStoreRegistry.LoanDetail),
            networkMonitor = get(),
            fetchedAtRepository = get(),
        )
    }

    // meeting-summary (GET /datatables/dt_meeting_record/{groupId}?meetingNumber=N) — wraps the
    // single-key composite NETWORK_WITH_CACHE MeetingSummaryStore (bound via
    // AppStoreRegistry.MeetingSummary in appStoreModule) and surfaces the offline-first
    // .asScreenStream() read (per-meeting ScreenState<MeetingSummaryData> = totals + savings
    // breakdown + loan activity). Read-only screen — no write path.
    single<MeetingSummaryRepository> {
        MeetingSummaryRepositoryImpl(
            meetingSummaryStore = get(AppStoreRegistry.MeetingSummary),
            networkMonitor = get(),
            fetchedAtRepository = get(),
            meetingRecordDao = get(),
        )
    }

    // previous-meeting-review (FR-019) — COMPOSITE read that REUSES the meeting-summary record read
    // (MeetingSummaryRepository above → MeetingSummaryStore) and merges it with the NEW single-key
    // NETWORK_WITH_CACHE MeetingAttendanceStore (bound via AppStoreRegistry.MeetingAttendance in
    // appStoreModule). Surfaces one offline-first ScreenState<PreviousMeetingDetail> = record totals +
    // savings + loans + per-member attendance + derived unresolved items. Read-only — no write path.
    single<PreviousMeetingReviewRepository> {
        PreviousMeetingReviewRepositoryImpl(
            meetingSummaryRepository = get(),
            meetingAttendanceStore = get(AppStoreRegistry.MeetingAttendance),
            networkMonitor = get(),
            fetchedAtRepository = get(),
        )
    }

    // loan-repayment-dialog (POST /loans/{loanId}/transactions?command=repayment) — Store5-free
    // mutation orchestration for recordRepayment (business_logic.kind: processor, no read-stream
    // of its own), wraps LoanRepaymentApi (NetworkModule) directly. Surfaces NetworkResult, never
    // .asScreenStream()/.write() — same branch as InvitationRepository/GroupCreateRepository
    // above. On success it invalidates the ALREADY-REGISTERED AppStoreRegistry.LoanDetail store
    // for the paid loan (same Store<Long, LoanDetailResponse> singleton LoanDetailRepositoryImpl
    // reads through) so the loan-detail screen re-fetches with the new outstanding balance.
    single<LoanRepaymentRepository> {
        LoanRepaymentRepositoryImpl(
            api = get(),
            loanDetailStore = get(AppStoreRegistry.LoanDetail),
        )
    }

    // loan-mark-defaulted-dialog (POST /loans/{loanId}/transactions?command=writeoff) —
    // Store5-free mutation orchestration for writeoffLoan (business_logic.kind: processor, no
    // read-stream of its own), wraps LoanWriteoffApi (NetworkModule) directly. Surfaces
    // NetworkResult, never .asScreenStream()/.write() — same branch as LoanRepaymentRepository
    // above. This mutation is IRREVERSIBLE and has NO offline queue — a failed/offline write is
    // surfaced immediately, never retried/queued. On success it invalidates the
    // ALREADY-REGISTERED AppStoreRegistry.LoanDetail store for the defaulted loan (same
    // Store<Long, LoanDetailResponse> singleton LoanDetailRepositoryImpl reads through) so the
    // loan-detail screen re-fetches with the now-defaulted status.
    single<LoanWriteoffRepository> {
        LoanWriteoffRepositoryImpl(
            api = get(),
            loanDetailStore = get(AppStoreRegistry.LoanDetail),
        )
    }

    // join-with-code (COMP-DT-004 + COMP-GRP-003) — Store5-free mutation orchestration
    // (business_logic.kind: processor, no read-stream to cache), wraps InvitationApi
    // (NetworkModule) directly. Surfaces NetworkResult, never .asScreenStream()/.write() — same
    // branch as AuthRepository above.
    single<InvitationRepository> { InvitationRepositoryImpl(api = get()) }

    // member-invite organizer-side (COMP-DT-002 generate + COMP-DT-003 list + COMP-DT-005 revoke)
    // — Store5-free submit-mutation (business_logic.kind: crud, no read-stream to cache, no
    // offline queue per data-flow.yaml#sync_queue.entries: []), wraps MemberInviteApi
    // (NetworkModule) directly. Surfaces NetworkResult, never .asScreenStream()/.write() — same
    // branch as InvitationRepository/MeetingConductRepository/LoanApplyRepository above. DISTINCT
    // from InvitationRepository (recipient-side join flow) — organizer-vs-recipient bounded context.
    single<MemberInviteRepository> { MemberInviteRepositoryImpl(api = get(), syncQueueRepository = get()) }

    // group-create wizard (COMP-GRP-001 + raw Fineract /offices) — Store5-free mutation
    // orchestration for createGroup (business_logic.kind: processor, no read-stream to cache),
    // wraps GroupCreateApi (NetworkModule) directly. Surfaces NetworkResult, never
    // .asScreenStream()/.write() — same branch as AuthRepository/InvitationRepository above.
    // getOffices is pending a future kmp-store-gen OfficeStore for its declared
    // stale-while-revalidate cache_strategy (SC2) — see GroupCreateRepository KDoc; not
    // half-built here, this repo's getOffices is a plain pass-through today.
    single<GroupCreateRepository> { GroupCreateRepositoryImpl(api = get(), syncQueueRepository = get()) }

    // member-add create-chain (create_client -> assign_member_role -> optional upload_photo) —
    // Store5-free mutation orchestration (business_logic.kind: processor, offline-queue-backed,
    // no read-stream to cache), wraps MemberAddApi (NetworkModule) directly. Surfaces
    // NetworkResult, never .asScreenStream()/.write() — same branch as
    // AuthRepository/InvitationRepository/GroupCreateRepository above.
    single<MemberAddRepository> { MemberAddRepositoryImpl(api = get(), syncQueueRepository = get()) }

    // loan-apply form (get_group_members + 5-way parallel combine into LoanApplyTemplate +
    // create_new_loan submit) — Store5-free today (business_logic.kind: composite, no
    // AppStoreRegistry entry yet — pending a future kmp-store-gen LoanApplyStore), wraps
    // LoanApplyApi (NetworkModule) directly. Surfaces NetworkResult, never
    // .asScreenStream()/.write() — same branch as AuthRepository/InvitationRepository/
    // GroupCreateRepository/MemberAddRepository above.
    single<LoanApplyRepository> { LoanApplyRepositoryImpl(api = get()) }

    // loan-request form (submit_loan_request) — Store5-free (business_logic.kind: crud, the
    // legacy template path per RULE-IDEA-IMPL-INTELLIGENCE-001 AC-03i — no read-stream to cache),
    // wraps LoanRequestApi (NetworkModule) directly plus the shared SyncQueueRepository for the
    // offline-enqueue seam (data-flow.yaml#offline_behavior strategy: enqueue_to_sync_queue,
    // entity_type LOAN_REQUEST). Surfaces NetworkResult from submit(), never
    // .asScreenStream()/.write() — same branch as AuthRepository/InvitationRepository/
    // GroupCreateRepository/MemberAddRepository/LoanApplyRepository above. The online-vs-offline
    // decision itself is made by the ViewModel (informed by NetworkMonitor), never here.
    single<LoanRequestRepository> {
        LoanRequestRepositoryImpl(api = get(), syncQueueRepository = get())
    }

    // sync-queue offline write-queue (shared infra) — Room-backed WRITE-QUEUE, NOT a Store5
    // read-store (no org.mobilenativefoundation.store anywhere in this stack). Concrete backing for
    // the SyncQueueRepository.enqueue(...) seam declared in MemberAddRepository /
    // MemberAddViewModel / GroupCreateViewModel KDocs: mutation features (member-add's
    // CREATE_MEMBER, loan-request's LOAN_REQUEST) enqueue a serialized payload when offline; the
    // sync-status feature reads observePending()/observeCounts(). Wraps SyncQueueDao (DatabaseModule).
    single<SyncQueueRepository> { SyncQueueRepositoryImpl(dao = get()) }

    // sync-status batch-drain coordinator (batch_sync, Fineract Batch API) — Store5-free
    // (data-flow.yaml#cache.strategy: no_cache on every entry, no read-stream to cache), wraps
    // BatchSyncApi (NetworkModule) + the shared SyncQueueRepository (above) + SyncMetadataStore
    // (DatastoreModule, lastSyncAt persistence). Surfaces triggerSync()/getLastSyncAt() as plain
    // Flows and retryItem() as a suspend SyncResult — never .asScreenStream()/.write(), same
    // Store5-free branch as MemberAddRepository/LoanApplyRepository/LoanRequestRepository above.
    single<SyncManager> {
        SyncManagerImpl(
            syncQueueRepository = get(),
            batchSyncApi = get(),
            syncMetadataStore = get(),
        )
    }

    // settings change-PIN (change_pin) — Store5-free (business_logic.kind: crud, the legacy
    // template path per RULE-IDEA-IMPL-INTELLIGENCE-001 AC-03i — no read-stream to cache), wraps
    // ChangePinApi (NetworkModule) directly. Surfaces NetworkResult, never
    // .asScreenStream()/.write() — same branch as AuthRepository/InvitationRepository/
    // GroupCreateRepository/MemberAddRepository/LoanApplyRepository/LoanRequestRepository above.
    // Deliberately separate from AuthRepository — does not extend or wrap it.
    single<ChangePinRepository> { ChangePinRepositoryImpl(api = get()) }

    // savings — shared repository for the 3 consumers that share Fineract savings shapes:
    // personal-savings (getSavingsTransactions standalone + loadMemberSavings composite),
    // member-savings-detail (getMemberSavingsDetail), savings-dashboard (getGroupSavingsSummary +
    // getIndividualSavingsSummary standalone + loadSavingsDashboard 2-way parallel combine).
    // Store5-free today (no AppStoreRegistry.Savings entry yet — SP-03 kmp-store-gen has not run
    // for this feature set), wraps SavingsApi (NetworkModule) directly. Surfaces NetworkResult,
    // never .asScreenStream()/.write() — same branch as AuthRepository/InvitationRepository/
    // GroupCreateRepository/MemberAddRepository/LoanApplyRepository above.
    single<SavingsRepository> { SavingsRepositoryImpl(api = get()) }

    // share-out-preview + share-out-execute — wraps ShareOutApi (NetworkModule) for the companion
    // read GET /companion/groups/{groupId}/shareout/preview (COMP-DIST-001) AND the two execute
    // writes POST .../shareout/execute (COMP-DIST-001) + .../rotation/execute (COMP-DIST-002).
    // Store5-free today (no AppStoreRegistry entry yet — SP-03 kmp-store-gen has not run). Surfaces
    // NetworkResult, never .asScreenStream(); the execute writes reuse the shared SyncQueueRepository
    // for the offline enqueue seam — same branch as LoanRequestRepository above.
    single<ShareOutRepository> { ShareOutRepositoryImpl(api = get(), syncQueueRepository = get()) }

    // meeting-conduct 7-step wizard (5-way on-mount parallel combine + ordered submit sequence) —
    // Store5-free composite (business_logic.kind: composite, no AppStoreRegistry entry), wraps
    // MeetingConductApi (NetworkModule) directly plus the shared SyncQueueRepository for the offline
    // enqueue seam (flow.yaml#submit_meeting.offline, SUBMIT_MEETING). Surfaces NetworkResult, never
    // .asScreenStream()/.write() — same branch as ShareOutRepository/LoanRequestRepository above.
    single<MeetingConductRepository> {
        MeetingConductRepositoryImpl(api = get(), syncQueueRepository = get(), meetingSummaryRepository = get())
    }

    // meeting-calendar (GET /datatables/dt_meeting_schedule/{groupId} + get_meeting_records_datatable) — wraps
    // the single-key NETWORK_WITH_CACHE MeetingCalendarStore (bound via AppStoreRegistry.MeetingCalendar
    // in appStoreModule) and surfaces the offline-first .asScreenStream() read (per-center
    // ScreenState<List<MeetingListItem>>, the 2-way parallel merge of get_meeting_schedule +
    // get_meeting_records_datatable). Read-only — no write path (data-flow.yaml#sync_queue: []).
    single<MeetingRepository> {
        MeetingRepositoryImpl(
            meetingCalendarStore = get(AppStoreRegistry.MeetingCalendar),
            networkMonitor = get(),
            fetchedAtRepository = get(),
            // G3 / F6: the schedule-editor RescheduleMeeting write is server-gated (companion
            // companion_update_calendar pending), so it offline-queues via the shared SyncQueueRepository.
            syncQueueRepository = get(),
        )
    }

    // Framework FetchedAtRepository — durable lastFetchedAt persistence backing
    // DataFreshnessIndicator timestamps. Room-only by design (no in-memory fallback).
    single<FetchedAtRepository> { RoomFetchedAtRepository(get<AppDatabase>().fetchedAtDao) }

    // Framework DraftDao — backing store for SubmitOutbox / DraftSubmitHandler
    single { get<AppDatabase>().draftDao }

    // App-scoped CoroutineScope for cross-VM long-running coroutines (framework infra).
    single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    single<UserLogoutManager> { UserLogoutManagerImpl(get(), get(), get()) }
}

expect val platformModule: Module
