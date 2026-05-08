/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
<<<<<<<< HEAD:feature/profile/src/commonMain/kotlin/org/mifos/open/banking/feature/profile/ProfileScreen.kt
package org.mifos.open.banking.feature.profile
========
package org.mifos.groupbanking.groupbanking.feature.profile
>>>>>>>> c3e419f (feat(scaffold): customise kmp-project-template for group-banking (CommonPurse)):feature/profile/src/commonMain/kotlin/org/mifos/groupbanking/feature/profile/ProfileScreen.kt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
<<<<<<<< HEAD:feature/profile/src/commonMain/kotlin/org/mifos/open/banking/feature/profile/ProfileScreen.kt
import org.mifos.open.banking.core.ui.scaffold.KptScaffold
========
import org.mifos.groupbanking.groupbanking.core.ui.scaffold.KptScaffold
>>>>>>>> c3e419f (feat(scaffold): customise kmp-project-template for group-banking (CommonPurse)):feature/profile/src/commonMain/kotlin/org/mifos/groupbanking/feature/profile/ProfileScreen.kt

@Composable
internal fun ProfileScreen(modifier: Modifier = Modifier) {
    ProfileScreenContent(
        modifier = modifier.fillMaxSize(),
    )
}

@Composable
internal fun ProfileScreenContent(
    modifier: Modifier = Modifier,
) {
    KptScaffold(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // ProfileScreenContent
            Text(text = "Profile Screen", fontWeight = FontWeight.SemiBold)
        }
    }
}
