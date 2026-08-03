/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.model

/**
 * Composite domain model for the member-profile screen — the client-side fan-in of the THREE
 * independent companion reads the screen fires in parallel on mount/refresh/retry (`get_client`
 * + `get_client_accounts` + `get_member_role`, `idea-layer/screens/member-profile/api.yaml`).
 *
 * Deliberately a distinct composite from the identity-only [MemberProfile] — exactly as the
 * group-dashboard archetype's `GroupDashboard` composite wraps its identity-only `GroupDetail`.
 * [member] is the identity header (`get_client`), [accounts] is the savings/loan card
 * (`get_client_accounts`), and [roles] is the full member-role datatable ([roles] is a `List`
 * because `get_member_role` has `response.type: array`; the ViewModel selects the current-group
 * role and computes the attendance summary from other sources — attendance is NOT part of this
 * composite and is layered in the VM).
 *
 * This is the value type of the composite dynamic-key NETWORK_WITH_CACHE `MemberProfileStore`
 * (keyed by `clientId`), persisted as one JSON row per client in `member_profile_cache` so a cold
 * start with no network still renders the last-seen profile (`data-flow.yaml#cache.offline:
 * show_cached`, SC2 — never memory-only).
 *
 * See API.md#models — MemberProfileDetail.
 */
data class MemberProfileDetail(
    val member: MemberProfile,
    val accounts: MemberAccounts,
    val roles: List<MemberRoleInfo>,
)
