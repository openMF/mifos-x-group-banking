/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
<<<<<<<< HEAD:feature/settings/src/androidMain/kotlin/org/mifos/open/banking/feature/settings/Platform.android.kt
package org.mifos.open.banking.feature.settings
========
package org.mifos.groupbanking.groupbanking.feature.settings
>>>>>>>> c3e419f (feat(scaffold): customise kmp-project-template for group-banking (CommonPurse)):feature/settings/src/androidMain/kotlin/org/mifos/groupbanking/feature/settings/Platform.android.kt

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

actual fun getPlatform(): Platform {
    return Platform.Android
}

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
actual fun supportsDynamicTheming(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
}
