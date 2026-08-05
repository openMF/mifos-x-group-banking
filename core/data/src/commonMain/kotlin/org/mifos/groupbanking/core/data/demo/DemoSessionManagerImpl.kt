/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.data.demo

import co.touchlab.kermit.Logger
import kotlinx.datetime.Instant
import kpt.core.base.store.infra.FetchedAtRepository
import org.mifos.groupbanking.core.database.groupdashboard.dao.GroupDashboardDao
import org.mifos.groupbanking.core.database.groupdashboard.entity.CachedActivityItem
import org.mifos.groupbanking.core.database.groupdashboard.entity.CachedGroupAccounts
import org.mifos.groupbanking.core.database.groupdashboard.entity.CachedGroupCorpus
import org.mifos.groupbanking.core.database.groupdashboard.entity.CachedGroupDashboard
import org.mifos.groupbanking.core.database.groupdashboard.entity.CachedGroupDetail
import org.mifos.groupbanking.core.database.groupdashboard.entity.CachedGroupInstanceConfig
import org.mifos.groupbanking.core.database.groupdashboard.entity.CachedViewerRoleInfo
import org.mifos.groupbanking.core.database.groupdashboard.entity.GroupDashboardCacheCodec
import org.mifos.groupbanking.core.database.groupdashboard.entity.GroupDashboardCacheEntity
import org.mifos.groupbanking.core.database.grouplist.dao.GroupListDao
import org.mifos.groupbanking.core.database.grouplist.entity.GroupListEntity
import org.mifos.groupbanking.core.database.loanlist.dao.LoanListDao
import org.mifos.groupbanking.core.database.loanlist.entity.LoanListEntity
import org.mifos.groupbanking.core.database.memberlist.dao.MemberListDao
import org.mifos.groupbanking.core.database.memberlist.entity.MemberListEntity
import org.mifos.groupbanking.core.database.organizerdashboard.dao.OrganizerDashboardDao
import org.mifos.groupbanking.core.database.organizerdashboard.entity.CachedOrganizerActivityItem
import org.mifos.groupbanking.core.database.organizerdashboard.entity.CachedOrganizerDashboard
import org.mifos.groupbanking.core.database.organizerdashboard.entity.CachedScheduledMeeting
import org.mifos.groupbanking.core.database.organizerdashboard.entity.OrganizerDashboardCacheCodec
import org.mifos.groupbanking.core.database.organizerdashboard.entity.OrganizerDashboardCacheEntity
import org.mifos.groupbanking.core.datastore.session.CompanionSessionStore
import org.mifos.groupbanking.core.model.ActivityType
import org.mifos.groupbanking.core.model.GroupContributionModel
import org.mifos.groupbanking.core.model.GroupTypeSlug
import org.mifos.groupbanking.core.model.LoanAccountStatus
import org.mifos.groupbanking.core.model.LoanStatus
import org.mifos.groupbanking.core.model.MemberRole
import org.mifos.groupbanking.core.model.OrganizerActivityType
import org.mifos.groupbanking.core.model.SavingsMechanism
import org.mifos.groupbanking.core.model.ViewerRole
import org.mifos.groupbanking.core.store.organizerdashboard.impl.ORGANIZER_DASHBOARD_KEY
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private const val TAG = "DemoSessionManager"

/**
 * Offline-only [DemoSessionManager]. Seeds the read caches the Demo-Explore mode browses OFFLINE
 * (no companion server, no Fineract) from the bundled `PROJECT_DEMO_DATA.yaml` fixture, persists a
 * synthetic demo session, and does ZERO network I/O.
 *
 * Caches seeded (each keyed exactly as its owning repository/store reads it, each written atomically
 * per RULE-IMPLEMENT-STORE5-001 S5-3 — one `replaceForKey`/`replacePage` per cache — with a matching
 * `FetchedAtRepository` freshness stamp so the offline session performs NO network revalidate):
 *   1. organizer-dashboard  (`organizerdashboard:organizerDashboard`) — the Demo-Explore landing.
 *   2. group-list           (`grouplist:groups`) — "All Groups" shows Mwangaza.
 *   3. group-dashboard      (`groupdashboard:demo-group-001`) — the populated single-group hub.
 *   4. member-list          (`memberlist:members:demo-group-001`) — the 5-member roster.
 *   5. loan-list            (`loanlist:loans:0`) — Grace's disbursed loan. See [buildDemoLoanListEntities]
 *      for why the key is `0` (the `group-dashboard → loan-list` `toLongOrNull()` nav-param drift).
 *
 * The `savings-dashboard` is deliberately NOT seeded: its `SavingsRepository` is Store5-free /
 * network-only (`getGroupSavingsSummary` + `getIndividualSavingsSummary` hit `SavingsApi` directly —
 * there is NO Room SourceOfTruth to write), so it cannot be surfaced offline from here. Seeding it
 * would require either a network call (defeats the offline guarantee) or a fake cache table that does
 * not exist — flagged, not half-built.
 *
 * The seed values below are the exact ids/names/balances of `idea-layer/PROJECT_DEMO_DATA.yaml`
 * (`demo_explore` + `groups[demo-group-001]` + `members` + `active_loans` + `corpus` +
 * `meeting_records`) — demo FIXTURE data (like the live `server-layer/migrations/seed-demo/
 * demo-fixture.json`), not localizable UI chrome, so they are literals here by design
 * (RULE-IMPL-NO-HARDCODED-STRING scopes Compose UI text, not repository seed fixtures). The demo
 * user Amina is treated as the group ORGANIZER (viewerRole = ORGANIZER) so Demo-Explore unlocks the
 * group-dashboard overflow menu + every organizer action — the point of the mode.
 */
@OptIn(ExperimentalTime::class)
class DemoSessionManagerImpl(
    private val sessionStore: CompanionSessionStore,
    private val organizerDashboardDao: OrganizerDashboardDao,
    private val groupListDao: GroupListDao,
    private val groupDashboardDao: GroupDashboardDao,
    private val memberListDao: MemberListDao,
    private val loanListDao: LoanListDao,
    private val fetchedAtRepository: FetchedAtRepository,
) : DemoSessionManager {

    private var demoActive: Boolean = false

    override suspend fun startDemoSession(): Result<DemoSession> = runCatching {
        val now = Clock.System.now()

        // 1. Seed every offline read cache from PROJECT_DEMO_DATA. Each write is a single keyed
        //    upsert / single-page replace — inherently atomic (S5-3 / S5-PAGE-ATOMIC).
        organizerDashboardDao.replaceForKey(buildDemoDashboardEntity())
        groupListDao.replacePage(DEMO_PAGE_INDEX, buildDemoGroupListEntities())
        groupDashboardDao.replaceForKey(buildDemoGroupDashboardEntity())
        memberListDao.replacePage(DEMO_GROUP_ID, DEMO_PAGE_INDEX, buildDemoMemberListEntities())
        loanListDao.replacePage(DEMO_LOAN_LIST_GROUP_ID, DEMO_PAGE_INDEX, buildDemoLoanListEntities())

        // 2. Mark each store fresh (key mirrors the owning repository's cacheKey) so the offline
        //    demo session performs NO network revalidate.
        fetchedAtRepository.write(ORGANIZER_DASHBOARD_FETCHED_AT_KEY, now)
        fetchedAtRepository.write(GROUP_LIST_FETCHED_AT_KEY, now)
        fetchedAtRepository.write(GROUP_DASHBOARD_FETCHED_AT_KEY, now)
        fetchedAtRepository.write(MEMBER_LIST_FETCHED_AT_KEY, now)
        fetchedAtRepository.write(LOAN_LIST_FETCHED_AT_KEY, now)

        // 3. Persist a synthetic demo session so downstream screens treat the app as authenticated
        //    as the demo organizer. SECURE-backed CompanionSessionStore — no companion API call.
        sessionStore.save(
            userId = DEMO_USER_ID,
            sessionToken = DEMO_SESSION_TOKEN,
            tokenExpiresAt = DEMO_SESSION_EXPIRES_AT,
        )

        demoActive = true
        Logger.i(TAG) { "demo session started (offline) userId=$DEMO_USER_ID group=$DEMO_GROUP_ID" }
        DemoSession(
            userId = DEMO_USER_ID,
            groupId = DEMO_GROUP_ID,
            organizerName = DEMO_ORGANIZER_NAME,
        )
    }.onFailure { t ->
        Logger.e(TAG, t) { "demo session seed failed" }
    }

    override fun isDemoSession(): Boolean = demoActive

    override suspend fun clearDemoSession() {
        // Mirror every cache seeded in startDemoSession so logout leaves no orphaned offline rows.
        organizerDashboardDao.deleteByKey(ORGANIZER_DASHBOARD_KEY)
        groupListDao.deletePage(DEMO_PAGE_INDEX)
        groupDashboardDao.deleteByKey(DEMO_GROUP_ID)
        memberListDao.deleteGroup(DEMO_GROUP_ID)
        loanListDao.deleteGroup(DEMO_LOAN_LIST_GROUP_ID)
        sessionStore.clear()
        demoActive = false
        Logger.i(TAG) { "demo session cleared" }
    }

    /** Builds the organizer-dashboard cache row from the demo fixture (Mwangaza Women's Group). */
    private fun buildDemoDashboardEntity(): OrganizerDashboardCacheEntity {
        val payload = CachedOrganizerDashboard(
            organizerName = DEMO_ORGANIZER_NAME,
            myGroupCount = 1,
            totalMembers = 5,
            pendingShareOutCount = 0,
            meetingsTodayCount = 1,
            fieldOfficerEnabled = false,
            todaySchedule = listOf(
                CachedScheduledMeeting(
                    groupId = DEMO_GROUP_ID,
                    groupName = DEMO_GROUP_NAME,
                    meetingTime = "14:00",
                    memberCount = 5,
                    location = null,
                ),
            ),
            recentActivity = listOf(
                CachedOrganizerActivityItem(
                    id = "demo-act-loan-501",
                    type = OrganizerActivityType.LOAN.name,
                    description = "Loan disbursed to Grace Wanjiru",
                    amount = 5000.0,
                    date = "2026-01-20",
                    memberName = "Grace Wanjiru",
                    groupName = DEMO_GROUP_NAME,
                ),
                CachedOrganizerActivityItem(
                    id = "demo-act-savings-mtg3",
                    type = OrganizerActivityType.DEPOSIT.name,
                    description = "Weekly savings collected",
                    amount = 3500.0,
                    date = "2026-01-20",
                    memberName = null,
                    groupName = DEMO_GROUP_NAME,
                ),
            ),
        )
        return OrganizerDashboardCacheEntity(
            cacheKey = ORGANIZER_DASHBOARD_KEY,
            dashboardJson = OrganizerDashboardCacheCodec.encode(payload),
            fetchedAt = Clock.System.now().toEpochMilliseconds(),
        )
    }

    /**
     * Builds the group-list cache (page 0) — one row for Mwangaza so "All Groups" renders it
     * offline. `groupType`/`viewerRole` are the enum `.name` strings the store maps back
     * ([GroupListEntity] → `Group`); `lastMeetingDate` is the ISO string parsed to `LocalDate`.
     */
    private fun buildDemoGroupListEntities(): List<GroupListEntity> = listOf(
        GroupListEntity(
            groupId = DEMO_GROUP_ID,
            pageIndex = DEMO_PAGE_INDEX,
            rowOrder = 0,
            name = DEMO_GROUP_NAME,
            groupType = GroupTypeSlug.VSLA.name,
            viewerRole = ViewerRole.ORGANIZER.name,
            cycleNumber = 1,
            memberCount = 5,
            lastMeetingDate = "2026-01-20",
            overdueRate = 0.0,
            status = "active",
            fineractGroupId = DEMO_FINERACT_GROUP_ID,
            fetchedAt = Clock.System.now().toEpochMilliseconds(),
        ),
    )

    /**
     * Builds the group-dashboard composite cache row (the 4-way `get_group` + `get_viewer_role` +
     * `get_group_corpus` + `get_group_accounts` fan-in, folded to JSON via
     * [GroupDashboardCacheCodec]). Corpus / accounts numbers come straight from the fixture's
     * `corpus` + `members[].group_savings_balance` + `active_loans` blocks.
     */
    private fun buildDemoGroupDashboardEntity(): GroupDashboardCacheEntity {
        val payload = CachedGroupDashboard(
            group = CachedGroupDetail(
                id = DEMO_GROUP_ID,
                fineractGroupId = DEMO_FINERACT_GROUP_ID,
                name = DEMO_GROUP_NAME,
                cycleNumber = 1,
                cycleLengthMonths = 12,
                meetingFrequency = "weekly",
                memberCount = 5,
                overdueLoansCount = 0,
                status = "active",
                typeConfig = CachedGroupInstanceConfig(
                    groupType = GroupTypeSlug.VSLA.name,
                    poolModel = SavingsMechanism.ACCUMULATING.name,
                    contributionModel = GroupContributionModel.SHARE_BASED_VARIABLE.name,
                    shareoutFormula = "PRORATA_SHARES",
                    payoutOrderMethod = "NONE",
                    shareValue = 100.0,
                    contributionAmount = 100.0,
                    socialFundEnabled = false,
                    cycleLengthMonths = 12,
                    loanMultiplier = 3.0,
                    interestRate = 0.10,
                    fineAmount = 50.0,
                ),
            ),
            viewerRole = CachedViewerRoleInfo(
                role = ViewerRole.ORGANIZER.name,
                memberId = DEMO_ORGANIZER_CLIENT_ID,
            ),
            corpus = CachedGroupCorpus(
                currentBalance = 15500.0,
                openingBalance = 11000.0,
                totalContributionsThisCycle = 9500.0,
                totalLoansOutstanding = 5000.0,
                lastUpdated = "2026-01-20",
                isCycleEnd = false,
                rotationPosition = null,
                nextRecipientName = null,
                nextRecipientPosition = null,
            ),
            accounts = CachedGroupAccounts(
                savingsBalance = 15500.0,
                loansOutstanding = 5000.0,
                activeLoanCount = 1,
                shareOutProjection = 16000.0,
                recentActivity = listOf(
                    CachedActivityItem(
                        id = "demo-act-loan-501",
                        type = ActivityType.LOAN.name,
                        description = "Loan disbursed to Grace Wanjiru",
                        amount = 5000.0,
                        date = "2026-01-20",
                        memberName = "Grace Wanjiru",
                    ),
                    CachedActivityItem(
                        id = "demo-act-savings-mtg3",
                        type = ActivityType.DEPOSIT.name,
                        description = "Weekly savings collected",
                        amount = 3500.0,
                        date = "2026-01-20",
                        memberName = null,
                    ),
                ),
            ),
        )
        return GroupDashboardCacheEntity(
            groupId = DEMO_GROUP_ID,
            dashboardJson = GroupDashboardCacheCodec.encode(payload),
            fetchedAt = Clock.System.now().toEpochMilliseconds(),
        )
    }

    /**
     * Builds the member-list cache (page 0 of `demo-group-001`) — the 5 fixture members. `role` /
     * `loanStatus` are enum `.name` strings the store maps back; `savingsBalance` is each member's
     * group-linked balance. Grace carries the one ACTIVE loan; everyone else has NONE.
     */
    private fun buildDemoMemberListEntities(): List<MemberListEntity> {
        val now = Clock.System.now().toEpochMilliseconds()
        fun member(
            clientId: Long,
            name: String,
            role: MemberRole,
            savings: Double,
            loanStatus: LoanStatus,
            rowOrder: Int,
        ) = MemberListEntity(
            groupId = DEMO_GROUP_ID,
            memberId = clientId.toString(),
            pageIndex = DEMO_PAGE_INDEX,
            rowOrder = rowOrder,
            fineractClientId = clientId,
            displayName = name,
            photoUri = null,
            role = role.name,
            savingsBalance = savings,
            loanStatus = loanStatus.name,
            fetchedAt = now,
        )
        return listOf(
            member(301, "Amina Otieno", MemberRole.TREASURER, 4200.0, LoanStatus.NONE, 0),
            member(302, "Joseph Mwangi", MemberRole.CHAIRPERSON, 3800.0, LoanStatus.NONE, 1),
            member(303, "Grace Wanjiru", MemberRole.SECRETARY, 3000.0, LoanStatus.ACTIVE, 2),
            member(304, "Peter Kamau", MemberRole.MEMBER, 2500.0, LoanStatus.NONE, 3),
            member(305, "Mary Achieng", MemberRole.MEMBER, 2000.0, LoanStatus.NONE, 4),
        )
    }

    /**
     * Builds the loan-list cache (page 0) — Grace's single disbursed loan (KES 5000, active,
     * not overdue) from `active_loans[0]`.
     *
     * KEY NOTE: the loan-list store keys pages by the `Long` groupId threaded through
     * `PageKey.query`, but the ONLY way into loan-list in the demo is the group-dashboard "Loans"
     * quick action, which the NavHost bridges via `groupId.toLongOrNull() ?: 0L`. Since the demo
     * group id `"demo-group-001"` is non-numeric, that coerces to `0L` — so the store reads the
     * cache under groupId `"0"` (see [DEMO_LOAN_LIST_GROUP_ID]). Seeding under `"demo-group-001"`
     * would never surface. This tracks a DOCUMENTED nav-param drift (`GroupBankingNavHost`
     * `onNavigateToLoanList` `toLongOrNull` bridge); if that drift is fixed to forward the numeric
     * Fineract group id, this key must move with it.
     */
    private fun buildDemoLoanListEntities(): List<LoanListEntity> = listOf(
        LoanListEntity(
            groupId = DEMO_LOAN_LIST_GROUP_ID,
            loanId = 501L,
            pageIndex = DEMO_PAGE_INDEX,
            rowOrder = 0,
            memberId = 303L,
            memberName = "Grace Wanjiru",
            memberPhotoUrl = null,
            loanProductName = "VSLA Group Loan",
            principalAmount = 5000.0,
            outstandingBalance = 5000.0,
            overdueAmount = 0.0,
            status = LoanAccountStatus.ACTIVE.name,
            nextRepaymentDate = "2026-01-27",
            isOverdue = false,
            fineractLoanId = 501L,
            fetchedAt = Clock.System.now().toEpochMilliseconds(),
        ),
    )

    private companion object {
        // PROJECT_DEMO_DATA.yaml#demo_explore + groups[demo-group-001]
        const val DEMO_USER_ID = "demo-user-amina"
        const val DEMO_ORGANIZER_NAME = "Amina Otieno"
        const val DEMO_GROUP_ID = "demo-group-001"
        const val DEMO_GROUP_NAME = "Mwangaza Women's Group"

        /** Amina's Fineract client id (`members[0].fineract_client_id`) — the ORGANIZER viewer's memberId. */
        const val DEMO_ORGANIZER_CLIENT_ID = 301L

        /** `groups[0].fineract_center_id`. */
        const val DEMO_FINERACT_GROUP_ID = 101L

        /** Single seeded page for every paginated cache (group-list / member-list / loan-list). */
        const val DEMO_PAGE_INDEX = 0

        /**
         * The loan-list cache's groupId key. `"0"` (not `"demo-group-001"`) because the
         * group-dashboard → loan-list nav bridge coerces the non-numeric demo id via
         * `toLongOrNull() ?: 0L` — see [buildDemoLoanListEntities].
         */
        const val DEMO_LOAN_LIST_GROUP_ID = "0"

        /** Synthetic offline session token — never issued by the companion server. */
        const val DEMO_SESSION_TOKEN = "demo-session-offline"

        /** Far-future expiry so the offline demo session never appears expired. */
        val DEMO_SESSION_EXPIRES_AT: Instant = Instant.parse("2099-01-01T00:00:00Z")

        // FetchedAtRepository keys — each mirrors the owning repository's cacheKey exactly.
        /** Mirrors OrganizerDashboardRepositoryImpl.CACHE_KEY. */
        const val ORGANIZER_DASHBOARD_FETCHED_AT_KEY = "organizerdashboard:organizerDashboard"

        /** Mirrors GroupRepositoryImpl.CACHE_KEY. */
        const val GROUP_LIST_FETCHED_AT_KEY = "grouplist:groups"

        /** Mirrors GroupDashboardRepositoryImpl.CACHE_KEY_PREFIX + groupId. */
        const val GROUP_DASHBOARD_FETCHED_AT_KEY = "groupdashboard:$DEMO_GROUP_ID"

        /** Mirrors MemberRepositoryImpl.CACHE_KEY_PREFIX + groupId. */
        const val MEMBER_LIST_FETCHED_AT_KEY = "memberlist:members:$DEMO_GROUP_ID"

        /** Mirrors LoanRepositoryImpl.CACHE_KEY_PREFIX + groupId (the coerced 0L — see above). */
        const val LOAN_LIST_FETCHED_AT_KEY = "loanlist:loans:$DEMO_LOAN_LIST_GROUP_ID"
    }
}
