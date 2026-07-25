/*
 * Copyright 2024 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package cmp.navigation.di

import cmp.navigation.AppViewModel
import cmp.navigation.authenticatednavbar.AuthenticatedNavbarNavigationViewModel
import cmp.navigation.rootnav.RootNavViewModel
import kpt.core.base.analytics.di.analyticsModule
import kpt.core.base.common.di.CommonModule
import kpt.core.base.observability.di.observabilityModule
import kpt.core.base.platform.di.platformModule
import kpt.core.base.security.di.SecurityModule
import kpt.core.data.di.DataModule
import kpt.core.database.di.DatabaseModule
import kpt.core.datastore.di.DatastoreModule
import kpt.core.store.di.appStoreModule
import kpt.feature.home.di.HomeModule
import kpt.sync.di.SyncModule
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.mifos.groupbanking.feature.groupcreate.di.GroupCreateModule
import org.mifos.groupbanking.feature.groupdashboard.di.GroupDashboardModule
import org.mifos.groupbanking.feature.grouplist.di.GroupListModule
import org.mifos.groupbanking.feature.grouptypepicker.di.GroupTypePickerModule
import org.mifos.groupbanking.feature.joinwithcode.di.JoinWithCodeModule
import org.mifos.groupbanking.feature.loanapply.di.LoanApplyModule
import org.mifos.groupbanking.feature.loandetail.di.LoanDetailModule
import org.mifos.groupbanking.feature.loanlist.di.LoanListModule
import org.mifos.groupbanking.feature.loanmarkdefaulteddialog.di.LoanMarkDefaultedDialogModule
import org.mifos.groupbanking.feature.loanrepaymentdialog.di.LoanRepaymentDialogModule
import org.mifos.groupbanking.feature.loanrequest.di.LoanRequestModule
import org.mifos.groupbanking.feature.loginsignup.di.LoginSignupModule
import org.mifos.groupbanking.feature.memberadd.di.MemberAddModule
import org.mifos.groupbanking.feature.memberlist.di.MemberListModule
import org.mifos.groupbanking.feature.memberprofile.di.MemberProfileModule
import org.mifos.groupbanking.feature.membersavingsdetail.di.MemberSavingsDetailModule
import org.mifos.groupbanking.feature.personaldashboard.di.PersonalDashboardModule
import org.mifos.groupbanking.feature.personalloans.di.PersonalLoansModule
import org.mifos.groupbanking.feature.personalsavings.di.PersonalSavingsModule
import org.mifos.groupbanking.feature.savingsdashboard.di.SavingsDashboardModule
import org.mifos.groupbanking.feature.shareoutexecute.di.ShareOutExecuteModule
import org.mifos.groupbanking.feature.shareoutpreview.di.ShareOutPreviewModule
// `org.mifos.groupbanking.feature.settings.di.SettingsModule` — the REAL, now-sole group-banking
// `settings-screen` MVI module (idea-layer/screens/settings/ui.yaml). The legacy
// `kpt.feature.settings.SettingsModule`/`SettingsScreen`/`SettingsRoute` shell has been migrated
// away (Screen/Route/TestTags/DI all superseded — see `feature/settings/src/commonMain/kotlin/
// kpt/feature/settings/*` removal notes); the `GroupBankingSettingsModule` alias is kept so this
// import line and the `includes(...)` call site below don't need touching again.
import org.mifos.groupbanking.feature.settings.di.SettingsModule as GroupBankingSettingsModule
import org.mifos.groupbanking.feature.settingslogoutdialog.di.SettingsLogoutDialogModule
import org.mifos.groupbanking.feature.syncstatus.di.SyncStatusModule

object KoinModules {
    private val dataModule = module {
        includes(DataModule, appStoreModule)
    }

    private val dispatcherModule = module {
        includes(CommonModule)
    }

    private val AppModule = module {
        includes(platformModule)

        viewModelOf(::AppViewModel)
        viewModelOf(::AuthenticatedNavbarNavigationViewModel)
        viewModelOf(::RootNavViewModel)
    }

    private val featureModule = module {
        includes(
            // shell (framework) — kept
            HomeModule,
            LoginSignupModule,
            GroupTypePickerModule,
            GroupListModule,
            JoinWithCodeModule, // <- added by kmp-viewmodel-gen
            PersonalDashboardModule, // <- added by kmp-viewmodel-gen
            GroupCreateModule, // <- added by kmp-viewmodel-gen
            GroupDashboardModule, // <- added by kmp-viewmodel-gen
            MemberListModule, // <- added by kmp-viewmodel-gen
            MemberProfileModule, // <- added by kmp-viewmodel-gen
            MemberAddModule, // <- added by kmp-viewmodel-gen
            LoanListModule, // <- added by kmp-viewmodel-gen
            LoanDetailModule, // <- added by kmp-viewmodel-gen
            LoanApplyModule, // <- added by kmp-viewmodel-gen
            LoanRequestModule, // <- added by kmp-viewmodel-gen
            PersonalLoansModule, // <- added by kmp-viewmodel-gen
            LoanRepaymentDialogModule, // <- added by kmp-viewmodel-gen
            LoanMarkDefaultedDialogModule, // <- added by kmp-viewmodel-gen
            SyncStatusModule, // <- added by kmp-viewmodel-gen
            SettingsLogoutDialogModule, // <- added by kmp-viewmodel-gen
            GroupBankingSettingsModule, // <- added by kmp-viewmodel-gen (real settings-screen VM; legacy SettingsModule migrated away)
            PersonalSavingsModule, // <- added by kmp-viewmodel-gen
            MemberSavingsDetailModule, // <- added by kmp-viewmodel-gen
            SavingsDashboardModule, // <- added by kmp-viewmodel-gen
            ShareOutPreviewModule, // <- added by kmp-viewmodel-gen
            ShareOutExecuteModule, // <- added by kmp-viewmodel-gen
        )
    }

    val allModules = listOf(
        SecurityModule,
        dataModule,
        DatabaseModule,
        dispatcherModule,
        analyticsModule,
        observabilityModule,
        DatastoreModule,
        featureModule,
        AppModule,
        SyncModule,
    )
}
