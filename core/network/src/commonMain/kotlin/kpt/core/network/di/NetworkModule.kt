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
import org.mifos.groupbanking.core.network.config.CompanionAuthApiConfig
import org.mifos.groupbanking.core.network.config.GroupApiConfig
import org.mifos.groupbanking.core.network.config.GroupCreateApiConfig
import org.mifos.groupbanking.core.network.config.GroupDashboardApiConfig
import org.mifos.groupbanking.core.network.config.GroupTypeConfigApiConfig
import org.mifos.groupbanking.core.network.config.InvitationApiConfig
import org.mifos.groupbanking.core.network.config.LoanApiConfig
import org.mifos.groupbanking.core.network.config.LoanDetailApiConfig
import org.mifos.groupbanking.core.network.config.LoanRepaymentApiConfig
import org.mifos.groupbanking.core.network.config.MemberAddApiConfig
import org.mifos.groupbanking.core.network.config.MemberApiConfig
import org.mifos.groupbanking.core.network.config.MemberDashboardApiConfig
import org.mifos.groupbanking.core.network.config.MemberProfileApiConfig
import org.mifos.groupbanking.core.network.service.groupdashboard.GroupDashboardApi
import org.mifos.groupbanking.core.network.service.groupdashboard.GroupDashboardApiImpl
import org.mifos.groupbanking.core.network.service.grouplist.GroupApi
import org.mifos.groupbanking.core.network.service.grouplist.GroupApiImpl
import org.mifos.groupbanking.core.network.service.groupcreate.GroupCreateApi
import org.mifos.groupbanking.core.network.service.groupcreate.GroupCreateApiImpl
import org.mifos.groupbanking.core.network.service.grouptypepicker.GroupTypeConfigApi
import org.mifos.groupbanking.core.network.service.grouptypepicker.GroupTypeConfigApiImpl
import org.mifos.groupbanking.core.network.service.joinwithcode.InvitationApi
import org.mifos.groupbanking.core.network.service.joinwithcode.InvitationApiImpl
import org.mifos.groupbanking.core.network.service.loandetail.LoanDetailApi
import org.mifos.groupbanking.core.network.service.loandetail.LoanDetailApiImpl
import org.mifos.groupbanking.core.network.service.loanlist.LoanApi
import org.mifos.groupbanking.core.network.service.loanlist.LoanApiImpl
import org.mifos.groupbanking.core.network.service.loanrepayment.LoanRepaymentApi
import org.mifos.groupbanking.core.network.service.loanrepayment.LoanRepaymentApiImpl
import org.mifos.groupbanking.core.network.service.loginsignup.CompanionAuthApi
import org.mifos.groupbanking.core.network.service.loginsignup.CompanionAuthApiImpl
import org.mifos.groupbanking.core.network.service.memberadd.MemberAddApi
import org.mifos.groupbanking.core.network.service.memberadd.MemberAddApiImpl
import org.mifos.groupbanking.core.network.service.memberlist.MemberApi
import org.mifos.groupbanking.core.network.service.memberlist.MemberApiImpl
import org.mifos.groupbanking.core.network.service.memberprofile.MemberProfileApi
import org.mifos.groupbanking.core.network.service.memberprofile.MemberProfileApiImpl
import org.mifos.groupbanking.core.network.service.personaldashboard.MemberDashboardApi
import org.mifos.groupbanking.core.network.service.personaldashboard.MemberDashboardApiImpl
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

    // join-with-code (COMP-DT-004 + COMP-GRP-003) — join-with-code feature client stack.
    // Reuses the shared HttpClient singleton above (same companion server, no second engine).
    // The config binding is registered for override-surface symmetry with CompanionAuthApiConfig
    // even though the shared client is the one actually dispatching requests today. The
    // Repository consuming InvitationApi (Store5-free mutation orchestration — no read-stream
    // to cache) is registered in RepositoryModule.kt.
    single<InvitationApiConfig> { InvitationApiConfig() }
    single<InvitationApi> { InvitationApiImpl(httpClient = get()) }

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

    // Loan repayment (make_repayment) — loan-repayment-dialog feature client stack. Reuses the
    // shared HttpClient singleton above (same companion server, no second engine). The config
    // binding is registered for override-surface symmetry with CompanionAuthApiConfig even
    // though the shared client is the one actually dispatching requests today. The Repository
    // consuming LoanRepaymentApi (Store5-free mutation orchestration — no read-stream of its own,
    // invalidates the already-registered AppStoreRegistry.LoanDetail store on success) is
    // registered in RepositoryModule.kt.
    single<LoanRepaymentApiConfig> { LoanRepaymentApiConfig() }
    single<LoanRepaymentApi> { LoanRepaymentApiImpl(httpClient = get()) }

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
}
