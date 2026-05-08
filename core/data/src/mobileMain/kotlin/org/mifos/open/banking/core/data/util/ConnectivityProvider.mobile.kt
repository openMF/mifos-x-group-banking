/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
<<<<<<<< HEAD:core/data/src/mobileMain/kotlin/org/mifos/open/banking/core/data/util/ConnectivityProvider.mobile.kt
package org.mifos.open.banking.core.data.util
========
package org.mifos.groupbanking.groupbanking.core.model.fintech
>>>>>>>> c3e419f (feat(scaffold): customise kmp-project-template for group-banking (CommonPurse)):core/model/src/commonMain/kotlin/org/mifos/groupbanking/core/model/fintech/EmiResult.kt

import dev.jordond.connectivity.Connectivity

actual val connectivityProvider: Connectivity
    get() = Connectivity {
        autoStart(true)
    }
