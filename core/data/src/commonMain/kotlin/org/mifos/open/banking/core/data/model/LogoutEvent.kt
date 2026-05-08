/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
<<<<<<<< HEAD:core/data/src/commonMain/kotlin/org/mifos/open/banking/core/data/model/LogoutEvent.kt
package org.mifos.open.banking.core.data.model
========
package org.mifos.groupbanking.groupbanking.core.data.model
>>>>>>>> c3e419f (feat(scaffold): customise kmp-project-template for group-banking (CommonPurse)):core/data/src/commonMain/kotlin/org/mifos/groupbanking/core/data/model/LogoutEvent.kt

/**
 * Result class to share the [loggedOutUserId] of a user
 * that was successfully logged out.
 */
data class LogoutEvent(
    val loggedOutUserId: Long,
)
