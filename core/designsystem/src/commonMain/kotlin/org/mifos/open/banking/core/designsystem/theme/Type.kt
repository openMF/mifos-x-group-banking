/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
<<<<<<<< HEAD:core/designsystem/src/commonMain/kotlin/org/mifos/open/banking/core/designsystem/theme/Type.kt
package org.mifos.open.banking.core.designsystem.theme
========
package org.mifos.groupbanking.groupbanking.core.designsystem.theme
>>>>>>>> c3e419f (feat(scaffold): customise kmp-project-template for group-banking (CommonPurse)):core/designsystem/src/commonMain/kotlin/org/mifos/groupbanking/core/designsystem/theme/Type.kt

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.Font
<<<<<<<< HEAD:core/designsystem/src/commonMain/kotlin/org/mifos/open/banking/core/designsystem/theme/Type.kt
import org.mifos.open.banking.core.designsystem.generated.resources.Res
import org.mifos.open.banking.core.designsystem.generated.resources.outfit_black
import org.mifos.open.banking.core.designsystem.generated.resources.outfit_bold
import org.mifos.open.banking.core.designsystem.generated.resources.outfit_extra_bold
import org.mifos.open.banking.core.designsystem.generated.resources.outfit_extra_light
import org.mifos.open.banking.core.designsystem.generated.resources.outfit_light
import org.mifos.open.banking.core.designsystem.generated.resources.outfit_medium
import org.mifos.open.banking.core.designsystem.generated.resources.outfit_regular
import org.mifos.open.banking.core.designsystem.generated.resources.outfit_semi_bold
import org.mifos.open.banking.core.designsystem.generated.resources.outfit_thin
========
import org.mifos.groupbanking.groupbanking.core.designsystem.generated.resources.Res
import org.mifos.groupbanking.groupbanking.core.designsystem.generated.resources.outfit_black
import org.mifos.groupbanking.groupbanking.core.designsystem.generated.resources.outfit_bold
import org.mifos.groupbanking.groupbanking.core.designsystem.generated.resources.outfit_extra_bold
import org.mifos.groupbanking.groupbanking.core.designsystem.generated.resources.outfit_extra_light
import org.mifos.groupbanking.groupbanking.core.designsystem.generated.resources.outfit_light
import org.mifos.groupbanking.groupbanking.core.designsystem.generated.resources.outfit_medium
import org.mifos.groupbanking.groupbanking.core.designsystem.generated.resources.outfit_regular
import org.mifos.groupbanking.groupbanking.core.designsystem.generated.resources.outfit_semi_bold
import org.mifos.groupbanking.groupbanking.core.designsystem.generated.resources.outfit_thin
>>>>>>>> c3e419f (feat(scaffold): customise kmp-project-template for group-banking (CommonPurse)):core/designsystem/src/commonMain/kotlin/org/mifos/groupbanking/core/designsystem/theme/Type.kt

val fontFamily: FontFamily
    @Composable get() = FontFamily(
        Font(Res.font.outfit_black, FontWeight.Black),
        Font(Res.font.outfit_bold, FontWeight.Bold),
        Font(Res.font.outfit_semi_bold, FontWeight.SemiBold),
        Font(Res.font.outfit_medium, FontWeight.Medium),
        Font(Res.font.outfit_regular, FontWeight.Normal),
        Font(Res.font.outfit_light, FontWeight.Light),
        Font(Res.font.outfit_thin, FontWeight.Thin),
        Font(Res.font.outfit_extra_light, FontWeight.ExtraLight),
        Font(Res.font.outfit_extra_bold, FontWeight.ExtraBold),
    )
