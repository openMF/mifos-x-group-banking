/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.data.demo

import co.touchlab.kermit.Logger
import kotlinx.datetime.Instant
import kpt.core.base.store.infra.FetchedAtRepository
import org.mifos.groupbanking.core.database.organizerdashboard.dao.OrganizerDashboardDao
import org.mifos.groupbanking.core.database.organizerdashboard.entity.CachedOrganizerActivityItem
import org.mifos.groupbanking.core.database.organizerdashboard.entity.CachedOrganizerDashboard
import org.mifos.groupbanking.core.database.organizerdashboard.entity.CachedScheduledMeeting
import org.mifos.groupbanking.core.database.organizerdashboard.entity.OrganizerDashboardCacheCodec
import org.mifos.groupbanking.core.database.organizerdashboard.entity.OrganizerDashboardCacheEntity
import org.mifos.groupbanking.core.datastore.session.CompanionSessionStore
import org.mifos.groupbanking.core.model.OrganizerActivityType
import org.mifos.groupbanking.core.store.organizerdashboard.impl.ORGANIZER_DASHBOARD_KEY
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private const val TAG = "DemoSessionManager"

/**
 * Offline-only [DemoSessionManager]. Seeds the organizer-dashboard read cache (the Demo Explore
 * landing screen) from the bundled `PROJECT_DEMO_DATA.yaml` fixture, persists a synthetic demo
 * session, and does ZERO network I/O.
 *
 * The seed values below are the exact ids/names/balances of `idea-layer/PROJECT_DEMO_DATA.yaml`
 * (`demo_explore` + `groups[demo-group-001]` + `members` + `active_loans` + `meeting_records`) —
 * demo FIXTURE data (like the live `server-layer/migrations/seed-demo/demo-fixture.json`), not
 * localizable UI chrome, so they are literals here by design (RULE-IMPL-NO-HARDCODED-STRING scopes
 * Compose UI text, not repository seed fixtures).
 */
@OptIn(ExperimentalTime::class)
class DemoSessionManagerImpl(
    private val sessionStore: CompanionSessionStore,
    private val organizerDashboardDao: OrganizerDashboardDao,
    private val fetchedAtRepository: FetchedAtRepository,
) : DemoSessionManager {

    private var demoActive: Boolean = false

    override suspend fun startDemoSession(): Result<DemoSession> = runCatching {
        // 1. Seed the offline landing-screen cache (organizer-dashboard) from PROJECT_DEMO_DATA.
        //    A single-row keyed upsert is inherently atomic (S5-3).
        organizerDashboardDao.replaceForKey(buildDemoDashboardEntity())

        // 2. Mark the store fresh so the offline demo session performs NO network revalidate.
        //    Key mirrors OrganizerDashboardRepositoryImpl.CACHE_KEY.
        fetchedAtRepository.write(ORGANIZER_DASHBOARD_FETCHED_AT_KEY, Clock.System.now())

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
        organizerDashboardDao.deleteByKey(ORGANIZER_DASHBOARD_KEY)
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

    private companion object {
        // PROJECT_DEMO_DATA.yaml#demo_explore + groups[demo-group-001]
        const val DEMO_USER_ID = "demo-user-amina"
        const val DEMO_ORGANIZER_NAME = "Amina Otieno"
        const val DEMO_GROUP_ID = "demo-group-001"
        const val DEMO_GROUP_NAME = "Mwangaza Women's Group"

        /** Synthetic offline session token — never issued by the companion server. */
        const val DEMO_SESSION_TOKEN = "demo-session-offline"

        /** Far-future expiry so the offline demo session never appears expired. */
        val DEMO_SESSION_EXPIRES_AT: Instant = Instant.parse("2099-01-01T00:00:00Z")

        /** Mirrors OrganizerDashboardRepositoryImpl.CACHE_KEY (FetchedAtRepository store key). */
        const val ORGANIZER_DASHBOARD_FETCHED_AT_KEY = "organizerdashboard:organizerDashboard"
    }
}
