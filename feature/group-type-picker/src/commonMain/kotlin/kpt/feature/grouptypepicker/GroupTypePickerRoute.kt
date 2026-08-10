/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.feature.grouptypepicker

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import kotlinx.serialization.Serializable
import kpt.core.base.ui.nav.composableWithRootPushTransitions
import kpt.core.model.GroupTypeConfig

/**
 * `/groups/create/type` — the group-type selection step of the group-create wizard
 * (`ui.yaml#route`). See API.md#route.
 */
@Serializable
data object GroupTypePickerRoute

fun NavController.navigateToGroupTypePicker(navOptions: NavOptions? = null) =
    navigate(GroupTypePickerRoute, navOptions)

/**
 * Registers [GroupTypePickerScreen] on the host [NavGraphBuilder]. Every callback here closes a
 * [GroupTypePickerEvent] navigation branch consumed by the Container
 * (RULE-PROTO-COMPOSE-DEAD-CLICK-001 DC3) — `flow.yaml#navigates_to` declares both targets
 * (`group-create`, `group-list`). [onNavigateToGroupCreate] receives the resolved
 * [GroupTypeConfig] nav-arg the ViewModel packages on card tap (`flow.yaml#on_type_card_tap`);
 * `group-create` is not yet a generated feature module in this codebase, so this callback is a
 * typed nav-arg contract for the caller to wire once that route exists, rather than a
 * `@Serializable` type-safe route param (`GroupTypeConfig` is a plain domain `data class`, not
 * `@Serializable`, per `core/model` — see `GroupTypeConfig.kt` KDoc). See API.md#route.
 */
fun NavGraphBuilder.groupTypePickerScreen(
    onNavigateToGroupCreate: (GroupTypeConfig) -> Unit,
    onNavigateBack: () -> Unit,
) {
    composableWithRootPushTransitions<GroupTypePickerRoute> {
        GroupTypePickerScreen(
            onNavigateToGroupCreate = onNavigateToGroupCreate,
            onNavigateBack = onNavigateBack,
        )
    }
}
