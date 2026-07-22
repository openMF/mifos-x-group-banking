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
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import kpt.core.store.AppStoreRegistry
import org.mifos.groupbanking.core.data.repository.AuthRepository
import org.mifos.groupbanking.core.data.repository.AuthRepositoryImpl
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
import org.mifos.groupbanking.core.data.repository.LoanDetailRepository
import org.mifos.groupbanking.core.data.repository.LoanDetailRepositoryImpl
import org.mifos.groupbanking.core.data.repository.LoanRepaymentRepository
import org.mifos.groupbanking.core.data.repository.LoanRepaymentRepositoryImpl
import org.mifos.groupbanking.core.data.repository.LoanRepository
import org.mifos.groupbanking.core.data.repository.LoanRepositoryImpl
import org.mifos.groupbanking.core.data.repository.LoanWriteoffRepository
import org.mifos.groupbanking.core.data.repository.LoanWriteoffRepositoryImpl
import org.mifos.groupbanking.core.data.repository.MemberAddRepository
import org.mifos.groupbanking.core.data.repository.MemberAddRepositoryImpl
import org.mifos.groupbanking.core.data.repository.MemberDashboardRepository
import org.mifos.groupbanking.core.data.repository.MemberDashboardRepositoryImpl
import org.mifos.groupbanking.core.data.repository.MemberProfileRepository
import org.mifos.groupbanking.core.data.repository.MemberProfileRepositoryImpl
import org.mifos.groupbanking.core.data.repository.MemberRepository
import org.mifos.groupbanking.core.data.repository.MemberRepositoryImpl

val DataModule = module {
    includes(platformModule, CommonModule, DatabaseModule, DatastoreModule, NetworkModule)

    single<NetworkMonitor> { NetworkMonitorProvider.install() }
    singleOf(::UserDataRepositoryImpl) bind UserDataRepository::class

    // login-signup client stack (COMP-AUTH-001/002/003) — Store5-free (business_logic.kind:
    // processor), wraps CompanionAuthApi (NetworkModule) + CompanionSessionStore (DatastoreModule).
    single<AuthRepository> { AuthRepositoryImpl(api = get(), sessionStore = get()) }

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
    single<LoanRepository> {
        LoanRepositoryImpl(
            loansPagingStore = get(AppStoreRegistry.LoanList),
            networkMonitor = get(),
            fetchedAtRepository = get(),
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

    // group-create wizard (COMP-GRP-001 + raw Fineract /offices) — Store5-free mutation
    // orchestration for createGroup (business_logic.kind: processor, no read-stream to cache),
    // wraps GroupCreateApi (NetworkModule) directly. Surfaces NetworkResult, never
    // .asScreenStream()/.write() — same branch as AuthRepository/InvitationRepository above.
    // getOffices is pending a future kmp-store-gen OfficeStore for its declared
    // stale-while-revalidate cache_strategy (SC2) — see GroupCreateRepository KDoc; not
    // half-built here, this repo's getOffices is a plain pass-through today.
    single<GroupCreateRepository> { GroupCreateRepositoryImpl(api = get()) }

    // member-add create-chain (create_client -> assign_member_role -> optional upload_photo) —
    // Store5-free mutation orchestration (business_logic.kind: processor, offline-queue-backed,
    // no read-stream to cache), wraps MemberAddApi (NetworkModule) directly. Surfaces
    // NetworkResult, never .asScreenStream()/.write() — same branch as
    // AuthRepository/InvitationRepository/GroupCreateRepository above.
    single<MemberAddRepository> { MemberAddRepositoryImpl(api = get()) }

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
