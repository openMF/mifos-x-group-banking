/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
<<<<<<<< HEAD:core/designsystem/src/nonAndroidMain/kotlin/org/mifos/open/banking/core/designsystem/theme/Theme.nonAndroid.kt
package org.mifos.open.banking.core.designsystem.theme
========
package org.mifos.groupbanking.groupbanking.core.designsystem.theme
>>>>>>>> c3e419f (feat(scaffold): customise kmp-project-template for group-banking (CommonPurse)):core/designsystem/src/nonAndroidMain/kotlin/org/mifos/groupbanking/core/designsystem/theme/Theme.nonAndroid.kt

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@Composable
actual fun platformColorScheme(
    useDarkTheme: Boolean,
    dynamicColor: Boolean,
): ColorScheme {
    return when (useDarkTheme) {
        true -> darkColorScheme()
        false -> lightColorScheme()
    }
}
