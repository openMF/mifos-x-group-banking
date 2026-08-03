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
import org.mifos.groupbanking.feature.fieldofficerdashboard.di.FieldOfficerDashboardModule
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
import org.mifos.groupbanking.feature.meetingcalendar.di.MeetingCalendarModule
import org.mifos.groupbanking.feature.meetingconduct.di.MeetingConductModule
import org.mifos.groupbanking.feature.meetingsummary.di.MeetingSummaryModule
import org.mifos.groupbanking.feature.memberadd.di.MemberAddModule
import org.mifos.groupbanking.feature.memberinvite.di.MemberInviteModule
import org.mifos.groupbanking.feature.memberlist.di.MemberListModule
import org.mifos.groupbanking.feature.memberprofile.di.MemberProfileModule
import org.mifos.groupbanking.feature.membersavingsdetail.di.MemberSavingsDetailModule
import org.mifos.groupbanking.feature.organizerdashboard.di.OrganizerDashboardModule
import org.mifos.groupbanking.feature.personaldashboard.di.PersonalDashboardModule
import org.mifos.groupbanking.feature.personalloans.di.PersonalLoansModule
import org.mifos.groupbanking.feature.personalsavings.di.PersonalSavingsModule
import org.mifos.groupbanking.feature.previousmeetingreview.di.PreviousMeetingReviewModule
import org.mifos.groupbanking.feature.savingsdashboard.di.SavingsDashboardModule
import org.mifos.groupbanking.feature.settingslogoutdialog.di.SettingsLogoutDialogModule
import org.mifos.groupbanking.feature.shareoutexecute.di.ShareOutExecuteModule
import org.mifos.groupbanking.feature.shareoutpreview.di.ShareOutPreviewModule
import org.mifos.groupbanking.feature.syncstatus.di.SyncStatusModule
import org.mifos.groupbanking.feature.settings.di.SettingsModule as GroupBankingSettingsModule

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
            HomeModule,
            LoginSignupModule,
            GroupTypePickerModule,
            GroupListModule,
            JoinWithCodeModule,
            PersonalDashboardModule,
            GroupCreateModule,
            GroupDashboardModule,
            MemberListModule,
            MemberProfileModule,
            MemberAddModule,
            MemberInviteModule,
            LoanListModule,
            LoanDetailModule,
            LoanApplyModule,
            LoanRequestModule,
            PersonalLoansModule,
            LoanRepaymentDialogModule,
            LoanMarkDefaultedDialogModule,
            SyncStatusModule,
            SettingsLogoutDialogModule,
            GroupBankingSettingsModule,
            PersonalSavingsModule,
            MemberSavingsDetailModule,
            SavingsDashboardModule,
            ShareOutPreviewModule,
            ShareOutExecuteModule,
            MeetingSummaryModule,
            MeetingCalendarModule,
            FieldOfficerDashboardModule,
            OrganizerDashboardModule,
            MeetingConductModule,
            PreviousMeetingReviewModule,
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
