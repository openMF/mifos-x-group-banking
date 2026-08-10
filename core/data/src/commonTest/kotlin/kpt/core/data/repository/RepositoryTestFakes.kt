/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.data.repository

import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkChangeEvent
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkMonitor
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkStatus
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kpt.core.base.store.infra.FetchedAtRepository
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Shared offline-first test doubles for the `kpt.core.data.repository` test
 * package. Previously each repository test declared its own byte-identical `private` copy of these
 * two fakes; under the K2 compiler (Kotlin 2.x) same-name top-level declarations collide within a
 * package even when `private`, so they are consolidated here as `internal` singles.
 */
internal class FakeNetworkMonitor(initialStatus: NetworkStatus) : NetworkMonitor {
    private val _networkStatus = MutableStateFlow(initialStatus)
    override val networkStatus: StateFlow<NetworkStatus> = _networkStatus.asStateFlow()
    override val isOnline: StateFlow<Boolean> =
        MutableStateFlow(initialStatus is NetworkStatus.Available).asStateFlow()
    override val networkChanges: SharedFlow<NetworkChangeEvent> =
        MutableSharedFlow<NetworkChangeEvent>().asSharedFlow()
    override fun close() = Unit
}

@OptIn(ExperimentalTime::class)
internal class InMemoryFetchedAtRepository : FetchedAtRepository {
    private val map = mutableMapOf<String, Instant>()
    override suspend fun read(storeKey: String): Instant? = map[storeKey]
    override suspend fun write(storeKey: String, instant: Instant) {
        map[storeKey] = instant
    }
}
