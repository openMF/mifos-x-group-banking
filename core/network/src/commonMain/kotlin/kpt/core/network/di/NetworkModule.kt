/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.network.di

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestRetry
import kotlinx.serialization.json.Json
import kpt.core.base.network.SupabaseConfigClient
import kpt.core.base.network.SupabaseCredentials
import kpt.core.base.network.httpClient
import kpt.core.base.network.setupDefaultHttpClient
import org.koin.dsl.module
import org.mifos.groupbanking.core.network.auth.companionAuthHeaderPlugin
import org.mifos.groupbanking.core.network.config.BatchSyncApiConfig
import org.mifos.groupbanking.core.network.config.ChangePinApiConfig
import org.mifos.groupbanking.core.network.config.CompanionAuthApiConfig
import org.mifos.groupbanking.core.network.config.FieldOfficerApiConfig
import org.mifos.groupbanking.core.network.config.GroupApiConfig
import org.mifos.groupbanking.core.network.config.GroupCreateApiConfig
import org.mifos.groupbanking.core.network.config.GroupDashboardApiConfig
import org.mifos.groupbanking.core.network.config.GroupTypeConfigApiConfig
import org.mifos.groupbanking.core.network.config.InvitationApiConfig
import org.mifos.groupbanking.core.network.config.LoanApiConfig
import org.mifos.groupbanking.core.network.config.LoanApplyApiConfig
import org.mifos.groupbanking.core.network.config.LoanDetailApiConfig
import org.mifos.groupbanking.core.network.config.LoanRepaymentApiConfig
import org.mifos.groupbanking.core.network.config.LoanRequestApiConfig
import org.mifos.groupbanking.core.network.config.LoanWriteoffApiConfig
import org.mifos.groupbanking.core.network.config.MeetingApiConfig
import org.mifos.groupbanking.core.network.config.MeetingAttendanceApiConfig
import org.mifos.groupbanking.core.network.config.MeetingConductApiConfig
import org.mifos.groupbanking.core.network.config.MeetingRecordApiConfig
import org.mifos.groupbanking.core.network.config.MemberAddApiConfig
import org.mifos.groupbanking.core.network.config.MemberApiConfig
import org.mifos.groupbanking.core.network.config.MemberDashboardApiConfig
import org.mifos.groupbanking.core.network.config.MemberInviteApiConfig
import org.mifos.groupbanking.core.network.config.MemberProfileApiConfig
import org.mifos.groupbanking.core.network.config.OrganizerDashboardApiConfig
import org.mifos.groupbanking.core.network.config.SavingsApiConfig
import org.mifos.groupbanking.core.network.config.ShareOutApiConfig
import org.mifos.groupbanking.core.network.service.batchsync.BatchSyncApi
import org.mifos.groupbanking.core.network.service.batchsync.BatchSyncApiImpl
import org.mifos.groupbanking.core.network.service.changepin.ChangePinApi
import org.mifos.groupbanking.core.network.service.changepin.ChangePinApiImpl
import org.mifos.groupbanking.core.network.service.fieldofficerdashboard.FieldOfficerApi
import org.mifos.groupbanking.core.network.service.fieldofficerdashboard.FieldOfficerApiImpl
import org.mifos.groupbanking.core.network.service.groupcreate.GroupCreateApi
import org.mifos.groupbanking.core.network.service.groupcreate.GroupCreateApiImpl
import org.mifos.groupbanking.core.network.service.groupdashboard.GroupDashboardApi
import org.mifos.groupbanking.core.network.service.groupdashboard.GroupDashboardApiImpl
import org.mifos.groupbanking.core.network.service.grouplist.GroupApi
import org.mifos.groupbanking.core.network.service.grouplist.GroupApiImpl
import org.mifos.groupbanking.core.network.service.grouptypepicker.GroupTypeConfigApi
import org.mifos.groupbanking.core.network.service.grouptypepicker.GroupTypeConfigApiImpl
import org.mifos.groupbanking.core.network.service.joinwithcode.InvitationApi
import org.mifos.groupbanking.core.network.service.joinwithcode.InvitationApiImpl
import org.mifos.groupbanking.core.network.service.loanapply.LoanApplyApi
import org.mifos.groupbanking.core.network.service.loanapply.LoanApplyApiImpl
import org.mifos.groupbanking.core.network.service.loandetail.LoanDetailApi
import org.mifos.groupbanking.core.network.service.loandetail.LoanDetailApiImpl
import org.mifos.groupbanking.core.network.service.loanlist.LoanApi
import org.mifos.groupbanking.core.network.service.loanlist.LoanApiImpl
import org.mifos.groupbanking.core.network.service.loanrepayment.LoanRepaymentApi
import org.mifos.groupbanking.core.network.service.loanrepayment.LoanRepaymentApiImpl
import org.mifos.groupbanking.core.network.service.loanrequest.LoanRequestApi
import org.mifos.groupbanking.core.network.service.loanrequest.LoanRequestApiImpl
import org.mifos.groupbanking.core.network.service.loanwriteoff.LoanWriteoffApi
import org.mifos.groupbanking.core.network.service.loanwriteoff.LoanWriteoffApiImpl
import org.mifos.groupbanking.core.network.service.loginsignup.CompanionAuthApi
import org.mifos.groupbanking.core.network.service.loginsignup.CompanionAuthApiImpl
import org.mifos.groupbanking.core.network.service.meetingcalendar.MeetingApi
import org.mifos.groupbanking.core.network.service.meetingcalendar.MeetingApiImpl
import org.mifos.groupbanking.core.network.service.meetingconduct.MeetingConductApi
import org.mifos.groupbanking.core.network.service.meetingconduct.MeetingConductApiImpl
import org.mifos.groupbanking.core.network.service.meetingsummary.MeetingRecordApi
import org.mifos.groupbanking.core.network.service.meetingsummary.MeetingRecordApiImpl
import org.mifos.groupbanking.core.network.service.memberadd.MemberAddApi
import org.mifos.groupbanking.core.network.service.memberadd.MemberAddApiImpl
import org.mifos.groupbanking.core.network.service.memberinvite.MemberInviteApi
import org.mifos.groupbanking.core.network.service.memberinvite.MemberInviteApiImpl
import org.mifos.groupbanking.core.network.service.memberlist.MemberApi
import org.mifos.groupbanking.core.network.service.memberlist.MemberApiImpl
import org.mifos.groupbanking.core.network.service.memberprofile.MemberProfileApi
import org.mifos.groupbanking.core.network.service.memberprofile.MemberProfileApiImpl
import org.mifos.groupbanking.core.network.service.organizerdashboard.OrganizerDashboardApi
import org.mifos.groupbanking.core.network.service.organizerdashboard.OrganizerDashboardApiImpl
import org.mifos.groupbanking.core.network.service.personaldashboard.MemberDashboardApi
import org.mifos.groupbanking.core.network.service.personaldashboard.MemberDashboardApiImpl
import org.mifos.groupbanking.core.network.service.previousmeetingreview.MeetingAttendanceApi
import org.mifos.groupbanking.core.network.service.previousmeetingreview.MeetingAttendanceApiImpl
import org.mifos.groupbanking.core.network.service.savings.SavingsApi
import org.mifos.groupbanking.core.network.service.savings.SavingsApiImpl
import org.mifos.groupbanking.core.network.service.shareout.ShareOutApi
import org.mifos.groupbanking.core.network.service.shareout.ShareOutApiImpl
import kpt.core.network.config.SupabaseCredentials as GeneratedSupabaseCredentials

// NOTE: Backend URLs are sourced from Koin-injected config classes (FredApiConfig,
// FrankfurterApiConfig, WorldBankApiConfig), each carrying a `baseUrl: String` field that
// defaults to the public production endpoint. Forks override these `single { ... }` bindings
// at their app-module level to swap in mocks / mirrors / per-environment endpoints.
//
// TODO: If/when BuildKonfig is added to the project for FRED_API_KEY (the canonical Plan 10
// strategy), thread BuildKonfig.FRED_BASE_URL / FRANKFURTER_BASE_URL / WORLDBANK_BASE_URL
// through here. Today (no BuildKonfig in-tree), the default-param approach on each config class
// provides the same fork-customisation surface without the buildscript complexity.
// Dynamic server config (consumer-facing, from core-base/network):
//   - SupabaseConfigClient is registered below so forks can fetch runtime server config from a
//     Supabase `app_config`-style table. Its credentials are generated from the gitignored
//     `secrets/supabaseCredentialsFile.json` (SupabaseConfigConventionPlugin); absent that file the
//     creds are empty, so the client stays inert (isConfigured == false). Forks drop in the file, or
//     override the `single<SupabaseCredentials>` binding.
//   - DynamicBaseUrlPlugin is an opt-in of setupDefaultHttpClient(...): a fork implements
//     DynamicUrlConfigProvider (reads its selected server, keyed by AppUrlTypes) and passes it as
//     `dynamicUrlProvider = get()` on any client that should switch base URL at runtime. The
//     toolkit's own fixed-URL APIs (FRED / World Bank / CoinGecko / Frankfurter) don't use it.
val NetworkModule = module {

    // Supabase-backed dynamic config — overridable by forks. The credentials object is generated
    // from secrets/supabaseCredentialsFile.json by SupabaseConfigConventionPlugin (empty when the
    // file is absent, so the client stays inert until a fork provides a project).
    single<SupabaseCredentials> { GeneratedSupabaseCredentials }
    single { SupabaseConfigClient(credentials = get()) }

    // Companion auth bridge (COMP-AUTH-001/002/003) — login-signup feature client stack.
    // CompanionAuthApiConfig.baseUrl default-param is overridden per-fork/per-environment by
    // re-registering the single<CompanionAuthApiConfig> binding (see that class's KDoc).
    single<CompanionAuthApiConfig> { CompanionAuthApiConfig() }
    single<HttpClient> {
        val defaultConfig = setupDefaultHttpClient(
            baseUrl = get<CompanionAuthApiConfig>().baseUrl,
            // EC30 client-side half: ignoreUnknownKeys + coerceInputValues so a server-added
            // field/enum value never crashes a staggered old client (pairs with the DTO
            // SCHEMA_VERSION + @SerialName("UNKNOWN") enum fallback in LoginSignupDto.kt).
            jsonConfig = Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            },
        )
        httpClient {
            defaultConfig()
            // SC3 (RULE-IMPLEMENT-SCALE-CODEGEN-001): setupDefaultHttpClient already installs
            // HttpTimeout; HttpRequestRetry is layered on top here (core-base/network is
            // non-writable — Hard Rule #8) so this client never spins forever on a transient
            // 5xx/connection blip.
            install(HttpRequestRetry) {
                retryOnServerErrors(maxRetries = 3)
                exponentialDelay()
            }
            // Attach the companion session bearer token to EVERY request from this shared client,
            // so the server can resolve the authenticated caller (real name on the organizer
            // dashboard, and any per-user-scoped endpoint) — not just the explicit /me call.
            // Skips pre-login calls (no session yet) and never overwrites an explicit header.
            install(companionAuthHeaderPlugin(get()))
        }
    }
    single<CompanionAuthApi> { CompanionAuthApiImpl(httpClient = get()) }

    // Group-type catalogue (COMP-DT-003) — group-type-picker feature client stack. Reuses the
    // shared HttpClient singleton above (same companion server, no second engine). The config
    // binding is registered for override-surface symmetry with CompanionAuthApiConfig even
    // though the shared client is the one actually dispatching requests today.
    single<GroupTypeConfigApiConfig> { GroupTypeConfigApiConfig() }
    single<GroupTypeConfigApi> { GroupTypeConfigApiImpl(httpClient = get()) }

    // Group list (COMP-GRP-001) — group-list feature client stack. Reuses the shared HttpClient
    // singleton above (same companion server, no second engine). The config binding is
    // registered for override-surface symmetry with CompanionAuthApiConfig even though the
    // shared client is the one actually dispatching requests today. The Repository/Store5
    // wrapper consuming GroupApi is emitted by a downstream kmp-store-gen/kmp-client-gen
    // generation step, not registered here.
    single<GroupApiConfig> { GroupApiConfig() }
    single<GroupApi> { GroupApiImpl(httpClient = get()) }

    // Field-officer dashboard (FR-009) — raw Fineract reads (GET /centers, GET /groups,
    // GET /runreports/FieldOfficerGroupReport) scoped by staffId. Reuses the shared HttpClient
    // singleton above (no second engine). The config binding is registered for override-surface
    // symmetry with CompanionAuthApiConfig even though the shared client dispatches the requests.
    // The composite Store5 read-store + repository consuming FieldOfficerApi is wired downstream in
    // appStoreModule / RepositoryModule, not registered here.
    single<FieldOfficerApiConfig> { FieldOfficerApiConfig() }
    single<FieldOfficerApi> { FieldOfficerApiImpl(httpClient = get()) }

    // join-with-code (COMP-DT-004 + COMP-GRP-003) — join-with-code feature client stack.
    // Reuses the shared HttpClient singleton above (same companion server, no second engine).
    // The config binding is registered for override-surface symmetry with CompanionAuthApiConfig
    // even though the shared client is the one actually dispatching requests today. The
    // Repository consuming InvitationApi (Store5-free mutation orchestration — no read-stream
    // to cache) is registered in RepositoryModule.kt.
    single<InvitationApiConfig> { InvitationApiConfig() }
    single<InvitationApi> { InvitationApiImpl(httpClient = get()) }

    // member-invite organizer-side stack (COMP-DT-002 generate / COMP-DT-003 list / COMP-DT-005
    // revoke) — DISTINCT from the join-with-code recipient-side InvitationApi above (disjoint HTTP
    // verbs on the same invitations datatable). Reuses the shared HttpClient singleton (same
    // companion server, no second engine). The config binding is registered for override-surface
    // symmetry with CompanionAuthApiConfig even though the shared client dispatches the requests.
    // The Repository consuming MemberInviteApi (Store5-free submit-mutation — no read-stream to
    // cache) is registered in RepositoryModule.kt.
    single<MemberInviteApiConfig> { MemberInviteApiConfig() }
    single<MemberInviteApi> { MemberInviteApiImpl(httpClient = get()) }

    // Group-create wizard (COMP-GRP-001 + raw Fineract `/offices` passthrough) — group-create
    // feature client stack. Reuses the shared HttpClient singleton above (same companion host,
    // no second engine). The config binding is registered for override-surface symmetry with
    // CompanionAuthApiConfig even though the shared client is the one actually dispatching
    // requests today. The Repository consuming GroupCreateApi (Store5-free mutation
    // orchestration for createGroup; getOffices pending a future kmp-store-gen OfficeStore — see
    // GroupCreateRepository KDoc) is registered in RepositoryModule.kt.
    single<GroupCreateApiConfig> { GroupCreateApiConfig() }
    single<GroupCreateApi> { GroupCreateApiImpl(httpClient = get()) }

    // Personal dashboard (COMP-DASH-001) — personal-dashboard feature client stack. Reuses the
    // shared HttpClient singleton above (same companion server, no second engine). The config
    // binding is registered for override-surface symmetry with CompanionAuthApiConfig even
    // though the shared client is the one actually dispatching requests today. The
    // Repository/Store5 wrapper consuming MemberDashboardApi is emitted by a downstream
    // kmp-store-gen/kmp-client-gen generation step, not registered here.
    single<MemberDashboardApiConfig> { MemberDashboardApiConfig() }
    single<MemberDashboardApi> { MemberDashboardApiImpl(httpClient = get()) }

    // Organizer dashboard (GET /companion/organizer/dashboard) — organizer-dashboard feature client
    // stack. Reuses the shared HttpClient singleton above (same companion server, no second engine).
    // The config binding is registered for override-surface symmetry with CompanionAuthApiConfig
    // even though the shared client is the one actually dispatching requests today. The
    // Repository/Store5 wrapper consuming OrganizerDashboardApi is wired downstream in
    // appStoreModule / RepositoryModule, not registered here.
    single<OrganizerDashboardApiConfig> { OrganizerDashboardApiConfig() }
    single<OrganizerDashboardApi> { OrganizerDashboardApiImpl(httpClient = get()) }

    // Group dashboard (COMP-GRP-001 read path) — group-dashboard feature client stack. Reuses
    // the shared HttpClient singleton above (same companion server, no second engine). The
    // config binding is registered for override-surface symmetry with CompanionAuthApiConfig
    // even though the shared client is the one actually dispatching requests today. The 4-way
    // parallel-combine into the composite dashboard model + Store5 wrapper consuming
    // GroupDashboardApi is emitted by a downstream kmp-store-gen/kmp-client-gen generation step,
    // not registered here.
    single<GroupDashboardApiConfig> { GroupDashboardApiConfig() }
    single<GroupDashboardApi> { GroupDashboardApiImpl(httpClient = get()) }

    // Member list (get_group_members) — member-list feature client stack. Reuses the shared
    // HttpClient singleton above (same companion server, no second engine). The config binding is
    // registered for override-surface symmetry with CompanionAuthApiConfig even though the shared
    // client is the one actually dispatching requests today. The Repository/Store5 wrapper
    // consuming MemberApi is emitted by a downstream kmp-store-gen/kmp-client-gen generation
    // step, not registered here.
    single<MemberApiConfig> { MemberApiConfig() }
    single<MemberApi> { MemberApiImpl(httpClient = get()) }

    // Loan list (get_group_loans) — loan-list feature client stack. Reuses the shared
    // HttpClient singleton above (same companion server, no second engine). The config binding
    // is registered for override-surface symmetry with CompanionAuthApiConfig even though the
    // shared client is the one actually dispatching requests today. The Repository/Store5
    // wrapper consuming LoanApi is emitted by a downstream kmp-store-gen/kmp-client-gen
    // generation step, not registered here.
    single<LoanApiConfig> { LoanApiConfig() }
    single<LoanApi> { LoanApiImpl(httpClient = get()) }

    // Loan detail (get_loan_detail / get_loan) — loan-detail feature client stack. Reuses the
    // shared HttpClient singleton above (same companion server, no second engine). The config
    // binding is registered for override-surface symmetry with CompanionAuthApiConfig even
    // though the shared client is the one actually dispatching requests today. The
    // Repository/Store5 wrapper consuming LoanDetailApi (`stale_while_revalidate`, ttl=120 per
    // `data-flow.yaml#cache_strategy`) is emitted by a downstream kmp-store-gen/kmp-client-gen
    // generation step, not registered here.
    single<LoanDetailApiConfig> { LoanDetailApiConfig() }
    single<LoanDetailApi> { LoanDetailApiImpl(httpClient = get()) }

    // Meeting record (get_meeting_record) — meeting-summary feature client stack. Reuses the
    // shared HttpClient singleton above (same companion server, no second engine). The config
    // binding is registered for override-surface symmetry. The Repository/Store5 wrapper consuming
    // MeetingRecordApi (`stale_while_revalidate`, ttl=600 per `data-flow.yaml#cache.strategy`) is
    // wrapped by provideMeetingSummaryStore (appStoreModule) + MeetingSummaryRepositoryImpl.
    single<MeetingRecordApiConfig> { MeetingRecordApiConfig() }
    single<MeetingRecordApi> { MeetingRecordApiImpl(httpClient = get()) }

    // Meeting calendar (get_center_meetings + get_meeting_records_datatable) — meeting-calendar
    // feature client stack. Reuses the shared HttpClient singleton above (same server, no second
    // engine). MeetingApi is wrapped by provideMeetingCalendarStore (appStoreModule) +
    // MeetingRepositoryImpl. Config binding registered for override-surface symmetry.
    single<MeetingApiConfig> { MeetingApiConfig() }
    single<MeetingApi> { MeetingApiImpl(httpClient = get()) }

    // Meeting attendance (get_meeting_attendance) — previous-meeting-review feature client stack.
    // Reuses the shared HttpClient singleton above (same server, no second engine).
    // MeetingAttendanceApi is wrapped by provideMeetingAttendanceStore (appStoreModule) +
    // PreviousMeetingReviewRepositoryImpl (merged with the reused MeetingSummaryStore record read).
    single<MeetingAttendanceApiConfig> { MeetingAttendanceApiConfig() }
    single<MeetingAttendanceApi> { MeetingAttendanceApiImpl(httpClient = get()) }

    // Loan repayment (make_repayment) — loan-repayment-dialog feature client stack. Reuses the
    // shared HttpClient singleton above (same companion server, no second engine). The config
    // binding is registered for override-surface symmetry with CompanionAuthApiConfig even
    // though the shared client is the one actually dispatching requests today. The Repository
    // consuming LoanRepaymentApi (Store5-free mutation orchestration — no read-stream of its own,
    // invalidates the already-registered AppStoreRegistry.LoanDetail store on success) is
    // registered in RepositoryModule.kt.
    single<LoanRepaymentApiConfig> { LoanRepaymentApiConfig() }
    single<LoanRepaymentApi> { LoanRepaymentApiImpl(httpClient = get()) }

    // Loan write-off (write_off_loan) — loan-mark-defaulted-dialog feature client stack. Reuses
    // the shared HttpClient singleton above (same companion server, no second engine). The
    // config binding is registered for override-surface symmetry with CompanionAuthApiConfig
    // even though the shared client is the one actually dispatching requests today. This
    // mutation is IRREVERSIBLE and has NO offline queue. The Repository consuming LoanWriteoffApi
    // (Store5-free mutation orchestration — no read-stream of its own, invalidates the
    // already-registered AppStoreRegistry.LoanDetail store on success) is registered in
    // RepositoryModule.kt.
    single<LoanWriteoffApiConfig> { LoanWriteoffApiConfig() }
    single<LoanWriteoffApi> { LoanWriteoffApiImpl(httpClient = get()) }

    // Member profile (get_client / get_client_accounts / get_member_role / update_member_role)
    // — member-profile feature client stack. Reuses the shared HttpClient singleton above (same
    // companion server, no second engine), even though every path here is a raw Fineract
    // passthrough rather than a `/companion/…` one — same convention as GroupCreateApi.getOffices.
    // The config binding is registered for override-surface symmetry with CompanionAuthApiConfig
    // even though the shared client is the one actually dispatching requests today. The composite
    // Store5 read-store + write-invalidation repository consuming MemberProfileApi is emitted by
    // a downstream kmp-store-gen/kmp-client-gen generation step, not registered here.
    single<MemberProfileApiConfig> { MemberProfileApiConfig() }
    single<MemberProfileApi> { MemberProfileApiImpl(httpClient = get()) }

    // member-add create-chain (create_client -> assign_member_role -> optional upload_photo) —
    // member-add feature client stack. Reuses the shared HttpClient singleton above (same
    // companion host, no second engine), even though every path here is a raw Fineract
    // passthrough rather than a `/companion/…` one — same convention as
    // GroupCreateApi.getOffices / MemberProfileApi. The config binding is registered for
    // override-surface symmetry with CompanionAuthApiConfig even though the shared client is the
    // one actually dispatching requests today. The Repository consuming MemberAddApi (Store5-free
    // mutation orchestration — offline-queue-backed create-chain, no read-stream to cache) is
    // registered in RepositoryModule.kt.
    single<MemberAddApiConfig> { MemberAddApiConfig() }
    single<MemberAddApi> { MemberAddApiImpl(httpClient = get()) }

    // Loan-apply form (get_group_members / get_loan_products / get_loan_template /
    // get_member_savings / get_group_corpus / get_group_config / create_new_loan) — loan-apply
    // feature client stack. Reuses the shared HttpClient singleton above (same companion host, no
    // second engine), even though every path here is a raw Fineract passthrough rather than a
    // `/companion/…` one — same convention as GroupCreateApi.getOffices / MemberAddApi. The
    // config binding is registered for override-surface symmetry with CompanionAuthApiConfig even
    // though the shared client is the one actually dispatching requests today. The Repository
    // consuming LoanApplyApi (Store5-free today — `business_logic.kind: composite` with no
    // AppStoreRegistry entry yet, pending a future kmp-store-gen LoanApplyStore) is registered in
    // RepositoryModule.kt.
    single<LoanApplyApiConfig> { LoanApplyApiConfig() }
    single<LoanApplyApi> { LoanApplyApiImpl(httpClient = get()) }

    // Loan-request form (submit_loan_request) — loan-request feature client stack. Reuses the
    // shared HttpClient singleton above (same companion host, no second engine), even though
    // this path is a raw Fineract datatable passthrough rather than a `/companion/…` one — same
    // convention as MemberAddApi / LoanApplyApi. The config binding is registered for
    // override-surface symmetry with CompanionAuthApiConfig even though the shared client is the
    // one actually dispatching requests today. The Repository consuming LoanRequestApi (Store5-free
    // — `business_logic.kind: crud`, offline-queue-backed via SyncQueueRepository, no read-stream
    // to cache) is registered in RepositoryModule.kt.
    single<LoanRequestApiConfig> { LoanRequestApiConfig() }
    single<LoanRequestApi> { LoanRequestApiImpl(httpClient = get()) }

    // sync-status batch-drain (batch_sync, Fineract Batch API) — sync-status feature client
    // stack. Reuses the shared HttpClient singleton above (same companion host, no second
    // engine), even though this path is the raw Fineract Batch API rather than a `/companion/…`
    // one — same convention as MemberAddApi / LoanApplyApi / LoanRequestApi. The config binding
    // is registered for override-surface symmetry with CompanionAuthApiConfig even though the
    // shared client is the one actually dispatching requests today. `SyncManager` (Store5-free —
    // `data-flow.yaml#cache.strategy: no_cache` on every entry, no read-stream to cache), which
    // consumes BatchSyncApi + the shared SyncQueueRepository + SyncMetadataStore, is registered
    // in RepositoryModule.kt.
    single<BatchSyncApiConfig> { BatchSyncApiConfig() }
    single<BatchSyncApi> { BatchSyncApiImpl(httpClient = get()) }

    // Change-PIN (change_pin) — settings screen client stack. Reuses the shared HttpClient
    // singleton above (same companion host, no second engine), even though this path is a raw
    // Fineract self-service passthrough (`/fineract-provider/api/v1/self/user/updatePassword`)
    // rather than a `/companion/…` one — same convention as MemberProfileApi / MemberAddApi. The
    // config binding is registered for override-surface symmetry with CompanionAuthApiConfig even
    // though the shared client is the one actually dispatching requests today. The Repository
    // consuming ChangePinApi (Store5-free — `business_logic.kind: crud`, no read-stream to cache)
    // is registered in RepositoryModule.kt.
    single<ChangePinApiConfig> { ChangePinApiConfig() }
    single<ChangePinApi> { ChangePinApiImpl(httpClient = get()) }

    // Shared savings client stack (get_group_linked_transactions/get_individual_transactions —
    // personal-savings; get_member_savings_detail — member-savings-detail;
    // get_group_savings_summary/get_individual_savings_summary — savings-dashboard) — built ONCE
    // for the 3 consumers that share Fineract savings shapes. Reuses the shared HttpClient
    // singleton above (same companion host, no second engine), even though
    // getSavingsTransactions is a raw Fineract self-service passthrough rather than a
    // `/companion/…` one — same convention as MemberProfileApi. The config binding is registered
    // for override-surface symmetry with CompanionAuthApiConfig even though the shared client is
    // the one actually dispatching requests today. The Repository consuming SavingsApi (Store5-free
    // today — no AppStoreRegistry.Savings entry yet, pending a future kmp-store-gen SavingsStore)
    // is registered in RepositoryModule.kt.
    single<SavingsApiConfig> { SavingsApiConfig() }
    single<SavingsApi> { SavingsApiImpl(httpClient = get()) }

    // share-out-preview companion read (get_shareout_preview — COMP-DIST-001 preview,
    // GET /companion/groups/{groupId}/shareout/preview). Reuses the shared HttpClient singleton
    // above (same companion host, no second engine). The config binding is registered for
    // override-surface symmetry with SavingsApiConfig even though the shared client dispatches the
    // request. The Repository consuming ShareOutApi (Store5-free today — no AppStoreRegistry entry
    // yet, pending a future kmp-store-gen ShareOutStore) is registered in RepositoryModule.kt.
    single<ShareOutApiConfig> { ShareOutApiConfig() }
    single<ShareOutApi> { ShareOutApiImpl(httpClient = get()) }

    // meeting-conduct wizard (get_previous_meeting_record / get_group_members / get_group_corpus /
    // get_active_loans / get_loan_votes + the ordered submit sequence post_meeting_record →
    // post_meeting_attendance → post_savings_transaction → post_loan_repayment → post_loan_disbursal
    // → patch_corpus) — reuses the shared HttpClient singleton above (same companion host, no second
    // engine). The config binding is registered for override-surface symmetry with LoanApplyApiConfig.
    // The Store5-free composite Repository consuming MeetingConductApi (+ shared SyncQueueRepository
    // for the offline enqueue seam) is registered in RepositoryModule.kt.
    single<MeetingConductApiConfig> { MeetingConductApiConfig() }
    single<MeetingConductApi> { MeetingConductApiImpl(httpClient = get()) }
}
