/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
<<<<<<<< HEAD:feature/settings/src/commonMain/kotlin/org/mifos/open/banking/feature/settings/NotificationScreen.kt
package org.mifos.open.banking.feature.settings
========
package org.mifos.groupbanking.groupbanking.feature.settings
>>>>>>>> c3e419f (feat(scaffold): customise kmp-project-template for group-banking (CommonPurse)):feature/settings/src/commonMain/kotlin/org/mifos/groupbanking/feature/settings/NotificationScreen.kt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
<<<<<<<< HEAD:feature/settings/src/commonMain/kotlin/org/mifos/open/banking/feature/settings/NotificationScreen.kt
import org.mifos.open.banking.core.ui.scaffold.KptScaffold
========
import org.mifos.groupbanking.groupbanking.core.ui.scaffold.KptScaffold
>>>>>>>> c3e419f (feat(scaffold): customise kmp-project-template for group-banking (CommonPurse)):feature/settings/src/commonMain/kotlin/org/mifos/groupbanking/feature/settings/NotificationScreen.kt

@Composable
internal fun NotificationScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
) {
    NotificationScreenContent(
        modifier = modifier,
        onBackClick = onBackClick,
    )
}

@Composable
internal fun NotificationScreenContent(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
) {
    KptScaffold(
        onNavigationIconClick = onBackClick,
        title = "Notification",
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // NotificationScreenContent
            Text(text = "Notification Screen", fontWeight = FontWeight.SemiBold)
        }
    }
}
