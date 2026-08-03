/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.data.repository

import kotlinx.coroutines.test.runTest
import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import org.mifos.groupbanking.core.model.ChangePinRequest
import org.mifos.groupbanking.core.network.model.ChangePinRequestDto
import org.mifos.groupbanking.core.network.model.ChangePinResponseDto
import org.mifos.groupbanking.core.network.service.changepin.ChangePinApi
import kotlin.test.Test
import kotlin.test.assertEquals

/** Fake [ChangePinApi] — named uniquely to avoid the K2 same-package private-declaration
 * redeclaration collision documented in [RepositoryTestFakes.kt]'s KDoc (top-level `private`
 * classes with the same simple name across files in this package DO collide under K2, so every
 * per-feature fake in this test package carries a feature-qualified name).
 */
private class FakeChangePinApi(
    private val result: NetworkResult<ChangePinResponseDto, NetworkError>? = null,
) : ChangePinApi {

    var lastRequest: ChangePinRequestDto? = null
    var callCount = 0

    override suspend fun changePin(request: ChangePinRequestDto): NetworkResult<ChangePinResponseDto, NetworkError> {
        lastRequest = request
        callCount++
        return result ?: error("result not stubbed")
    }
}

/**
 * TDD RED-first coverage for [ChangePinRepository] / [ChangePinRepositoryImpl] — a pure network
 * passthrough over [ChangePinApi]. `business_logic.kind` for settings is `crud`
 * (`ui.yaml#business_logic.kind`) — the legacy template path (RULE-IDEA-IMPL-INTELLIGENCE-001
 * AC-03i, Store5-free zero-regression), same branch as [LoanRequestRepositoryImpl] /
 * [MemberAddRepositoryImpl]. No try-catch anywhere in the repository under test (Mandatory
 * Rule 4) — every branch below is a plain `when` over the fake service's [NetworkResult]. This
 * repository is deliberately SEPARATE from [AuthRepository] — [AuthRepositoryTest] stays green,
 * untouched by this file.
 */
class ChangePinRepositoryTest {

    private val request = ChangePinRequest(currentPin = "1234", newPin = "5678")

    private val responseDto = ChangePinResponseDto(resourceId = 42L)

    // ---------- changePin (online path) ----------

    @Test
    fun changePin_success_mapsApiResponseToDomainResult() = runTest {
        val api = FakeChangePinApi(result = NetworkResult.Success(responseDto))
        val repo = ChangePinRepositoryImpl(api = api)

        val result = repo.changePin(request)

        check(result is NetworkResult.Success)
        assertEquals(42L, result.data.resourceId)
        assertEquals(1, api.callCount)
    }

    @Test
    fun changePin_threadsNewPinIntoBothWireFieldsAndExcludesCurrentPinFromTheBody() = runTest {
        val api = FakeChangePinApi(result = NetworkResult.Success(responseDto))
        val repo = ChangePinRepositoryImpl(api = api)

        repo.changePin(request)

        assertEquals("5678", api.lastRequest?.password)
        assertEquals("5678", api.lastRequest?.repeatPassword)
        // ChangePinRequestDto has no currentPin field at all — the type system itself enforces
        // the wire-only exclusion; nothing further to assert about "1234" appearing in the body.
    }

    @Test
    fun changePin_validationFailure400_returnsErrorUntouched() = runTest {
        val api = FakeChangePinApi(result = NetworkResult.Error(NetworkError.BAD_REQUEST))
        val repo = ChangePinRepositoryImpl(api = api)

        val result = repo.changePin(request)

        assertEquals(NetworkResult.Error(NetworkError.BAD_REQUEST), result)
    }

    @Test
    fun changePin_sessionExpired401_returnsErrorUntouched() = runTest {
        val api = FakeChangePinApi(result = NetworkResult.Error(NetworkError.UNAUTHORIZED))
        val repo = ChangePinRepositoryImpl(api = api)

        val result = repo.changePin(request)

        assertEquals(NetworkResult.Error(NetworkError.UNAUTHORIZED), result)
    }

    @Test
    fun changePin_serverError500_returnsErrorUntouched() = runTest {
        val api = FakeChangePinApi(result = NetworkResult.Error(NetworkError.SERVER))
        val repo = ChangePinRepositoryImpl(api = api)

        val result = repo.changePin(request)

        assertEquals(NetworkResult.Error(NetworkError.SERVER), result)
    }
}
