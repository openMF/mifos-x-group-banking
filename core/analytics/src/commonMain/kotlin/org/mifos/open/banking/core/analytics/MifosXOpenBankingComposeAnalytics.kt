/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
@file:Suppress("SpreadOperator")

<<<<<<<< HEAD:core/analytics/src/commonMain/kotlin/org/mifos/open/banking/core/analytics/MifosXOpenBankingComposeAnalytics.kt
package org.mifos.open.banking.core.analytics
========
package org.mifos.groupbanking.groupbanking.core.analytics
>>>>>>>> c3e419f (feat(scaffold): customise kmp-project-template for group-banking (CommonPurse)):core/analytics/src/commonMain/kotlin/org/mifos/groupbanking/core/analytics/CommonPurseComposeAnalytics.kt

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import template.core.base.analytics.AnalyticsHelper
import template.core.base.analytics.rememberAnalyticsHelper

/** Mifos-specific Compose analytics utilities */

/** Track Mifos screen views with additional business context */
@Composable
<<<<<<<< HEAD:core/analytics/src/commonMain/kotlin/org/mifos/open/banking/core/analytics/MifosXOpenBankingComposeAnalytics.kt
fun TrackMifosXOpenBankingScreen(
========
fun TrackCommonPurseScreen(
>>>>>>>> c3e419f (feat(scaffold): customise kmp-project-template for group-banking (CommonPurse)):core/analytics/src/commonMain/kotlin/org/mifos/groupbanking/core/analytics/CommonPurseComposeAnalytics.kt
    screenName: String,
    clientId: String? = null,
    loanId: String? = null,
    groupId: String? = null,
    additionalParams: Map<String, String> = emptyMap(),
) {
    val analytics = rememberAnalyticsHelper()
<<<<<<<< HEAD:core/analytics/src/commonMain/kotlin/org/mifos/open/banking/core/analytics/MifosXOpenBankingComposeAnalytics.kt
    val mifosxopenbankingTracker = remember(analytics) { analytics.mifosxopenbankingTracker() }

    LaunchedEffect(screenName) {
        val params = mutableMapOf<String, String>()
        clientId?.let { params[MifosXOpenBankingParamKeys.CLIENT_ID] = it }
        loanId?.let { params[MifosXOpenBankingParamKeys.LOAN_ID] = it }
        groupId?.let { params[MifosXOpenBankingParamKeys.GROUP_ID] = it }
========
    val commonpurseTracker = remember(analytics) { analytics.commonpurseTracker() }

    LaunchedEffect(screenName) {
        val params = mutableMapOf<String, String>()
        clientId?.let { params[CommonPurseParamKeys.CLIENT_ID] = it }
        loanId?.let { params[CommonPurseParamKeys.LOAN_ID] = it }
        groupId?.let { params[CommonPurseParamKeys.GROUP_ID] = it }
>>>>>>>> c3e419f (feat(scaffold): customise kmp-project-template for group-banking (CommonPurse)):core/analytics/src/commonMain/kotlin/org/mifos/groupbanking/core/analytics/CommonPurseComposeAnalytics.kt
        params.putAll(additionalParams)

        analytics.logScreenView(screenName)
        if (params.isNotEmpty()) {
            analytics.logEvent("mifos_screen_context", params)
        }
    }
}

/** Track client-related button clicks */
fun Modifier.trackClientAction(
    action: String,
    clientId: String? = null,
): Modifier = this.then(
<<<<<<<< HEAD:core/analytics/src/commonMain/kotlin/org/mifos/open/banking/core/analytics/MifosXOpenBankingComposeAnalytics.kt
    Modifier.trackMifosXOpenBankingAction(
        "client_action",
        mapOf(
            "action" to action,
            *clientId?.let { arrayOf(MifosXOpenBankingParamKeys.CLIENT_ID to it) } ?: emptyArray(),
========
    Modifier.trackCommonPurseAction(
        "client_action",
        mapOf(
            "action" to action,
            *clientId?.let { arrayOf(CommonPurseParamKeys.CLIENT_ID to it) } ?: emptyArray(),
>>>>>>>> c3e419f (feat(scaffold): customise kmp-project-template for group-banking (CommonPurse)):core/analytics/src/commonMain/kotlin/org/mifos/groupbanking/core/analytics/CommonPurseComposeAnalytics.kt
        ),
    ),
)

/** Track loan-related button clicks */
fun Modifier.trackLoanAction(
    action: String,
    loanId: String? = null,
    loanProductId: String? = null,
): Modifier = this.then(
<<<<<<<< HEAD:core/analytics/src/commonMain/kotlin/org/mifos/open/banking/core/analytics/MifosXOpenBankingComposeAnalytics.kt
    Modifier.trackMifosXOpenBankingAction(
        "loan_action",
        mapOf(
            "action" to action,
            *loanId?.let { arrayOf(MifosXOpenBankingParamKeys.LOAN_ID to it) } ?: emptyArray(),
            *loanProductId?.let { arrayOf(MifosXOpenBankingParamKeys.LOAN_PRODUCT_ID to it) } ?: emptyArray(),
========
    Modifier.trackCommonPurseAction(
        "loan_action",
        mapOf(
            "action" to action,
            *loanId?.let { arrayOf(CommonPurseParamKeys.LOAN_ID to it) } ?: emptyArray(),
            *loanProductId?.let { arrayOf(CommonPurseParamKeys.LOAN_PRODUCT_ID to it) } ?: emptyArray(),
>>>>>>>> c3e419f (feat(scaffold): customise kmp-project-template for group-banking (CommonPurse)):core/analytics/src/commonMain/kotlin/org/mifos/groupbanking/core/analytics/CommonPurseComposeAnalytics.kt
        ),
    ),
)

/** Track savings-related button clicks */
fun Modifier.trackSavingsAction(
    action: String,
    accountId: String? = null,
): Modifier = this.then(
<<<<<<<< HEAD:core/analytics/src/commonMain/kotlin/org/mifos/open/banking/core/analytics/MifosXOpenBankingComposeAnalytics.kt
    Modifier.trackMifosXOpenBankingAction(
        "savings_action",
        mapOf(
            "action" to action,
            *accountId?.let { arrayOf(MifosXOpenBankingParamKeys.SAVINGS_ACCOUNT_ID to it) } ?: emptyArray(),
========
    Modifier.trackCommonPurseAction(
        "savings_action",
        mapOf(
            "action" to action,
            *accountId?.let { arrayOf(CommonPurseParamKeys.SAVINGS_ACCOUNT_ID to it) } ?: emptyArray(),
>>>>>>>> c3e419f (feat(scaffold): customise kmp-project-template for group-banking (CommonPurse)):core/analytics/src/commonMain/kotlin/org/mifos/groupbanking/core/analytics/CommonPurseComposeAnalytics.kt
        ),
    ),
)

/** Generic Mifos action tracker */
@Suppress("UnusedParameter")
<<<<<<<< HEAD:core/analytics/src/commonMain/kotlin/org/mifos/open/banking/core/analytics/MifosXOpenBankingComposeAnalytics.kt
private fun Modifier.trackMifosXOpenBankingAction(
========
private fun Modifier.trackCommonPurseAction(
>>>>>>>> c3e419f (feat(scaffold): customise kmp-project-template for group-banking (CommonPurse)):core/analytics/src/commonMain/kotlin/org/mifos/groupbanking/core/analytics/CommonPurseComposeAnalytics.kt
    eventType: String,
    params: Map<String, String>,
): Modifier = this.clickable {
    // Note: In a real implementation, you'd need to access the analytics helper here
    // This is a simplified version for demonstration
}

/** Track form field interactions in Mifos forms */
@Composable
<<<<<<<< HEAD:core/analytics/src/commonMain/kotlin/org/mifos/open/banking/core/analytics/MifosXOpenBankingComposeAnalytics.kt
fun TrackMifosXOpenBankingFormField(
========
fun TrackCommonPurseFormField(
>>>>>>>> c3e419f (feat(scaffold): customise kmp-project-template for group-banking (CommonPurse)):core/analytics/src/commonMain/kotlin/org/mifos/groupbanking/core/analytics/CommonPurseComposeAnalytics.kt
    fieldName: String,
    formName: String,
    fieldType: String = "text",
) {
    val analytics = rememberAnalyticsHelper()

    LaunchedEffect(fieldName, formName) {
        analytics.logEvent(
            "form_field_focused",
            "field_name" to fieldName,
            "form_name" to formName,
            "field_type" to fieldType,
        )
    }
}

/** Track Mifos business flow completion */
@Composable
<<<<<<<< HEAD:core/analytics/src/commonMain/kotlin/org/mifos/open/banking/core/analytics/MifosXOpenBankingComposeAnalytics.kt
fun TrackMifosXOpenBankingFlowCompletion(
========
fun TrackCommonPurseFlowCompletion(
>>>>>>>> c3e419f (feat(scaffold): customise kmp-project-template for group-banking (CommonPurse)):core/analytics/src/commonMain/kotlin/org/mifos/groupbanking/core/analytics/CommonPurseComposeAnalytics.kt
    flowName: String,
    step: String,
    totalSteps: Int,
    entityId: String? = null,
) {
    val analytics = rememberAnalyticsHelper()

    LaunchedEffect(step) {
        val params = mutableMapOf(
            "flow_name" to flowName,
            "current_step" to step,
            "total_steps" to totalSteps.toString(),
            "progress_percentage" to "${(step.toIntOrNull() ?: 0) * 100 / totalSteps}",
        )
        entityId?.let { params["entity_id"] = it }

        analytics.logEvent("mifos_flow_progress", params)
    }
}

/** Track navigation within Mifos workflows */
@Composable
<<<<<<<< HEAD:core/analytics/src/commonMain/kotlin/org/mifos/open/banking/core/analytics/MifosXOpenBankingComposeAnalytics.kt
fun TrackMifosXOpenBankingNavigation(
========
fun TrackCommonPurseNavigation(
>>>>>>>> c3e419f (feat(scaffold): customise kmp-project-template for group-banking (CommonPurse)):core/analytics/src/commonMain/kotlin/org/mifos/groupbanking/core/analytics/CommonPurseComposeAnalytics.kt
    fromScreen: String,
    toScreen: String,
    navigationTrigger: String = "user_action",
    workflowName: String? = null,
) {
    val analytics = rememberAnalyticsHelper()

    LaunchedEffect(fromScreen, toScreen) {
        val params = mutableMapOf(
            "from_screen" to fromScreen,
            "to_screen" to toScreen,
            "trigger" to navigationTrigger,
        )
        workflowName?.let { params["workflow"] = it }

        analytics.logEvent("mifos_navigation", params)
    }
}

/** Track document operations in Mifos */
@Composable
<<<<<<<< HEAD:core/analytics/src/commonMain/kotlin/org/mifos/open/banking/core/analytics/MifosXOpenBankingComposeAnalytics.kt
fun rememberMifosXOpenBankingDocumentTracker(): DocumentTracker {
========
fun rememberCommonPurseDocumentTracker(): DocumentTracker {
>>>>>>>> c3e419f (feat(scaffold): customise kmp-project-template for group-banking (CommonPurse)):core/analytics/src/commonMain/kotlin/org/mifos/groupbanking/core/analytics/CommonPurseComposeAnalytics.kt
    val analytics = rememberAnalyticsHelper()
    return remember(analytics) { DocumentTracker(analytics) }
}

class DocumentTracker(private val analytics: AnalyticsHelper) {
    fun trackUpload(documentType: String, fileSize: Long, success: Boolean = true) {
        analytics.trackDocumentOperation("upload", documentType, fileSize, success)
    }

    fun trackDownload(documentType: String, success: Boolean = true) {
        analytics.trackDocumentOperation("download", documentType, success = success)
    }

    fun trackView(documentType: String) {
        analytics.trackDocumentOperation("view", documentType, success = true)
    }
}

/** Track survey interactions in Mifos */
@Composable
<<<<<<<< HEAD:core/analytics/src/commonMain/kotlin/org/mifos/open/banking/core/analytics/MifosXOpenBankingComposeAnalytics.kt
fun TrackMifosXOpenBankingSurvey(
========
fun TrackCommonPurseSurvey(
>>>>>>>> c3e419f (feat(scaffold): customise kmp-project-template for group-banking (CommonPurse)):core/analytics/src/commonMain/kotlin/org/mifos/groupbanking/core/analytics/CommonPurseComposeAnalytics.kt
    surveyId: String,
    // "started", "answered", "completed", "abandoned"
    action: String,
    questionId: String? = null,
) {
    val analytics = rememberAnalyticsHelper()

    LaunchedEffect(surveyId, questionId, action) {
        val params = mutableMapOf(
<<<<<<<< HEAD:core/analytics/src/commonMain/kotlin/org/mifos/open/banking/core/analytics/MifosXOpenBankingComposeAnalytics.kt
            MifosXOpenBankingParamKeys.SURVEY_ID to surveyId,
            "survey_action" to action,
        )
        questionId?.let { params[MifosXOpenBankingParamKeys.QUESTION_ID] = it }
========
            CommonPurseParamKeys.SURVEY_ID to surveyId,
            "survey_action" to action,
        )
        questionId?.let { params[CommonPurseParamKeys.QUESTION_ID] = it }
>>>>>>>> c3e419f (feat(scaffold): customise kmp-project-template for group-banking (CommonPurse)):core/analytics/src/commonMain/kotlin/org/mifos/groupbanking/core/analytics/CommonPurseComposeAnalytics.kt

        analytics.logEvent("mifos_survey_interaction", params)
    }
}

/** Track report generation in Mifos */
@Composable
<<<<<<<< HEAD:core/analytics/src/commonMain/kotlin/org/mifos/open/banking/core/analytics/MifosXOpenBankingComposeAnalytics.kt
fun rememberMifosXOpenBankingReportTracker(): ReportTracker {
========
fun rememberCommonPurseReportTracker(): ReportTracker {
>>>>>>>> c3e419f (feat(scaffold): customise kmp-project-template for group-banking (CommonPurse)):core/analytics/src/commonMain/kotlin/org/mifos/groupbanking/core/analytics/CommonPurseComposeAnalytics.kt
    val analytics = rememberAnalyticsHelper()
    return remember(analytics) { ReportTracker(analytics) }
}

class ReportTracker(private val analytics: AnalyticsHelper) {
<<<<<<<< HEAD:core/analytics/src/commonMain/kotlin/org/mifos/open/banking/core/analytics/MifosXOpenBankingComposeAnalytics.kt
    val analyticsTracker = MifosXOpenBankingAnalyticsTracker(analytics)
========
    val analyticsTracker = CommonPurseAnalyticsTracker(analytics)
>>>>>>>> c3e419f (feat(scaffold): customise kmp-project-template for group-banking (CommonPurse)):core/analytics/src/commonMain/kotlin/org/mifos/groupbanking/core/analytics/CommonPurseComposeAnalytics.kt
    fun trackGeneration(
        reportName: String,
        filters: Map<String, String> = emptyMap(),
        duration: Long,
        success: Boolean = true,
    ) {
        analyticsTracker.trackReportGeneration(
            reportType = reportName,
            filterParams = filters,
            generationTime = duration,
            success = success,
        )
    }

    fun trackExport(reportName: String, format: String, success: Boolean = true) {
        analytics.logEvent(
            "report_exported",
<<<<<<<< HEAD:core/analytics/src/commonMain/kotlin/org/mifos/open/banking/core/analytics/MifosXOpenBankingComposeAnalytics.kt
            MifosXOpenBankingParamKeys.REPORT_NAME to reportName,
            MifosXOpenBankingParamKeys.EXPORT_FORMAT to format,
========
            CommonPurseParamKeys.REPORT_NAME to reportName,
            CommonPurseParamKeys.EXPORT_FORMAT to format,
>>>>>>>> c3e419f (feat(scaffold): customise kmp-project-template for group-banking (CommonPurse)):core/analytics/src/commonMain/kotlin/org/mifos/groupbanking/core/analytics/CommonPurseComposeAnalytics.kt
            "success" to success.toString(),
        )
    }

    fun trackShare(reportName: String, method: String) {
        analytics.logEvent(
            "report_shared",
<<<<<<<< HEAD:core/analytics/src/commonMain/kotlin/org/mifos/open/banking/core/analytics/MifosXOpenBankingComposeAnalytics.kt
            MifosXOpenBankingParamKeys.REPORT_NAME to reportName,
========
            CommonPurseParamKeys.REPORT_NAME to reportName,
>>>>>>>> c3e419f (feat(scaffold): customise kmp-project-template for group-banking (CommonPurse)):core/analytics/src/commonMain/kotlin/org/mifos/groupbanking/core/analytics/CommonPurseComposeAnalytics.kt
            "share_method" to method,
        )
    }
}
