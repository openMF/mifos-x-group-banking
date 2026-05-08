/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
<<<<<<<< HEAD:core/data/src/commonMain/kotlin/org/mifos/open/banking/core/data/repository/UserLogoutManager.kt
package org.mifos.open.banking.core.data.repository

import kotlinx.coroutines.flow.SharedFlow
import org.mifos.open.banking.core.data.model.LogoutEvent
import org.mifos.open.banking.core.data.model.LogoutReason
========
package org.mifos.groupbanking.groupbanking.core.data.repository

import kotlinx.coroutines.flow.SharedFlow
import org.mifos.groupbanking.groupbanking.core.data.model.LogoutEvent
import org.mifos.groupbanking.groupbanking.core.data.model.LogoutReason
>>>>>>>> c3e419f (feat(scaffold): customise kmp-project-template for group-banking (CommonPurse)):core/data/src/commonMain/kotlin/org/mifos/groupbanking/core/data/repository/UserLogoutManager.kt

interface UserLogoutManager {
    /**
     * Observable flow of [LogoutEvent]s
     */
    val logoutEventFlow: SharedFlow<LogoutEvent>

    /**
     * Completely logs out the given [userId], removing all data. The [reason] indicates why the
     * user is being logged out.
     */
    fun logout(userId: Long, reason: LogoutReason)

    /**
     * Partially logs out the given [userId]. All data for the given [userId] will be removed with
     * the exception of basic account data. The [reason] indicates why the user is being logged out.
     */
    fun softLogout(userId: Long, reason: LogoutReason)
}
