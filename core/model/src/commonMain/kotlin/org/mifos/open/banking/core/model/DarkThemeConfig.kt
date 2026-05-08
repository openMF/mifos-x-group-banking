/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
<<<<<<<< HEAD:core/model/src/commonMain/kotlin/org/mifos/open/banking/core/model/DarkThemeConfig.kt
package org.mifos.open.banking.core.model
========
package org.mifos.groupbanking.groupbanking.core.model
>>>>>>>> c3e419f (feat(scaffold): customise kmp-project-template for group-banking (CommonPurse)):core/model/src/commonMain/kotlin/org/mifos/groupbanking/core/model/DarkThemeConfig.kt

enum class DarkThemeConfig(val configName: String, val osValue: Int) {
    FOLLOW_SYSTEM("Follow System", -1),
    LIGHT("Light", 1),
    DARK("Dark", 2),
    ;

    companion object {
        fun fromString(value: String): DarkThemeConfig {
            return entries.find { it.configName.equals(value, ignoreCase = true) } ?: FOLLOW_SYSTEM
        }
    }
}
