/*
 * Copyright 2024 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
<<<<<<<< HEAD:core/data/src/nonAndroidMain/kotlin/org/mifos/open/banking/core/data/di/TimeZoneMonitorImpl.kt
package org.mifos.open.banking.core.data.di
========
package org.mifos.groupbanking.groupbanking.core.data.di
>>>>>>>> c3e419f (feat(scaffold): customise kmp-project-template for group-banking (CommonPurse)):core/data/src/nonAndroidMain/kotlin/org/mifos/groupbanking/core/data/di/TimeZoneMonitorImpl.kt

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.TimeZone
<<<<<<<< HEAD:core/data/src/nonAndroidMain/kotlin/org/mifos/open/banking/core/data/di/TimeZoneMonitorImpl.kt
import org.mifos.open.banking.core.data.repository.TimeZoneMonitor
========
import org.mifos.groupbanking.groupbanking.core.data.repository.TimeZoneMonitor
>>>>>>>> c3e419f (feat(scaffold): customise kmp-project-template for group-banking (CommonPurse)):core/data/src/nonAndroidMain/kotlin/org/mifos/groupbanking/core/data/di/TimeZoneMonitorImpl.kt

class TimeZoneMonitorImpl : TimeZoneMonitor {
    override val currentTimeZone: Flow<TimeZone>
        get() = flowOf(TimeZone.currentSystemDefault())
}
