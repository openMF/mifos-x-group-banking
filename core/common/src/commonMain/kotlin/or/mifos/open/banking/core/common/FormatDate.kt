/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
<<<<<<<< HEAD:core/common/src/commonMain/kotlin/or/mifos/open/banking/core/common/FormatDate.kt
package or.mifos.open.banking.core.common
========
package org.mifos.groupbanking.groupbanking.core.common
>>>>>>>> c3e419f (feat(scaffold): customise kmp-project-template for group-banking (CommonPurse)):core/common/src/commonMain/kotlin/org/mifos/groupbanking/core/common/FormatDate.kt

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun formatDate(millis: Long): String {
    val dateTime = Instant
        .fromEpochMilliseconds(millis)
        .toLocalDateTime(TimeZone.currentSystemDefault())

    val day = dateTime.dayOfMonth.toString().padStart(2, '0')
    val month = dateTime.monthNumber.toString().padStart(2, '0')
    val year = dateTime.year
    return "$day/$month/$year"
}
