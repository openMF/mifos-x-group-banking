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
import kpt.feature.fieldofficerdashboard.di.FieldOfficerDashboardModule
import kpt.feature.groupcreate.di.GroupCreateModule
import kpt.feature.groupdashboard.di.GroupDashboardModule
import kpt.feature.grouplist.di.GroupListModule
import kpt.feature.grouptypepicker.di.GroupTypePickerModule
import kpt.feature.home.di.HomeModule
import kpt.feature.joinwithcode.di.JoinWithCodeModule
import kpt.feature.loanapply.di.LoanApplyModule
import kpt.feature.loandetail.di.LoanDetailModule
import kpt.feature.loanlist.di.LoanListModule
import kpt.feature.loanmarkdefaulteddialog.di.LoanMarkDefaultedDialogModule
import kpt.feature.loanrepaymentdialog.di.LoanRepaymentDialogModule
import kpt.feature.loanrequest.di.LoanRequestModule
import kpt.feature.loginsignup.di.LoginSignupModule
import kpt.feature.meetingcalendar.di.MeetingCalendarModule
import kpt.feature.meetingconduct.di.MeetingConductModule
import kpt.feature.meetingsummary.di.MeetingSummaryModule
import kpt.feature.memberadd.di.MemberAddModule
import kpt.feature.memberinvite.di.MemberInviteModule
import kpt.feature.memberlist.di.MemberListModule
import kpt.feature.memberprofile.di.MemberProfileModule
import kpt.feature.membersavingsdetail.di.MemberSavingsDetailModule
import kpt.feature.organizerdashboard.di.OrganizerDashboardModule
import kpt.feature.personaldashboard.di.PersonalDashboardModule
import kpt.feature.personalloans.di.PersonalLoansModule
import kpt.feature.personalsavings.di.PersonalSavingsModule
import kpt.feature.previousmeetingreview.di.PreviousMeetingReviewModule
import kpt.feature.savingsdashboard.di.SavingsDashboardModule
import kpt.feature.settingslogoutdialog.di.SettingsLogoutDialogModule
import kpt.feature.shareoutexecute.di.ShareOutExecuteModule
import kpt.feature.shareoutpreview.di.ShareOutPreviewModule
import kpt.feature.syncstatus.di.SyncStatusModule
import kpt.sync.di.SyncModule
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import kpt.feature.settings.di.SettingsModule as GroupBankingSettingsModule

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
