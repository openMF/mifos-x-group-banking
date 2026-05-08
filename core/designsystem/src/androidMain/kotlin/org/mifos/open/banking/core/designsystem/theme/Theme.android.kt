/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
<<<<<<<< HEAD:core/designsystem/src/androidMain/kotlin/org/mifos/open/banking/core/designsystem/theme/Theme.android.kt
package org.mifos.open.banking.core.designsystem.theme
========
package org.mifos.groupbanking.groupbanking.core.designsystem.theme
>>>>>>>> c3e419f (feat(scaffold): customise kmp-project-template for group-banking (CommonPurse)):core/designsystem/src/androidMain/kotlin/org/mifos/groupbanking/core/designsystem/theme/Theme.android.kt

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun platformColorScheme(
    useDarkTheme: Boolean,
    dynamicColor: Boolean,
): ColorScheme {
    return when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        useDarkTheme -> darkScheme
        else -> lightScheme
    }
}
