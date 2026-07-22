/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
@file:OptIn(org.koin.core.annotation.KoinInternalApi::class)

package kpt.core.data.di

import kpt.core.base.store.submit.SubmitOutbox
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Verifies the outbox-collision invariant [OutboxQualifiers] documents: every
 * `SubmitOutbox<*>` `single<>` registered in [DataModule] MUST carry a qualifier.
 * Koin keys DI entries by erased `KClass`, so two unqualified `single<SubmitOutbox<*>>`
 * bindings collide under `SubmitOutbox::class` and the last-registered silently wins —
 * surfacing as a `ClassCastException` at the first `.saveByUniqueKey(payload)` call.
 *
 * This is the fork-current form of the check. The Money-Toolkit template shipped four
 * per-type outbox qualifiers (`OutboxQualifiers.{Loan, BillReminder, LoanCalcScenario,
 * PriceAlert}`) whose demo payload types (and qualifier entries) were removed in the
 * group-banking fork — the earlier per-type assertions referenced those deleted types and
 * were stale template debt. The invariant they guarded is preserved here generically:
 * whatever `SubmitOutbox<*>` bindings the fork registers (zero today), none may be
 * unqualified. Adding a qualified outbox in future needs no change to this test.
 *
 * Implementation note: we introspect [DataModule]'s `mappings` table (the static
 * registration map) rather than eager-instantiating the graph — that would require
 * platform actuals (`platformModule`, `platformSecurityModule`, `AppDatabase`) absent from
 * `:core:data:commonTest`. Static introspection checks the exact contract without that cost.
 */
class RepositoryModuleVerifyTest {

    @Test
    fun everySubmitOutboxBindingIsQualified_noUnqualifiedCollisionRisk() {
        val unqualified = DataModule.mappings.values.filter { factory ->
            val def = factory.beanDefinition
            def.primaryType == SubmitOutbox::class && def.qualifier == null
        }
        assertTrue(
            unqualified.isEmpty(),
            "Every SubmitOutbox<*> single<> in DataModule must declare a qualifier " +
                "(see OutboxQualifiers KDoc) — unqualified bindings collide under " +
                "SubmitOutbox::class and the last-registered silently wins. Offending: " +
                unqualified.map { it.beanDefinition },
        )
    }
}
