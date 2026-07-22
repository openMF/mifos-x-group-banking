/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Builds an unknown-tolerant [KSerializer] for a wire enum whose members are transmitted as their
 * own constant name (`@SerialName` == constant name for every entry). Any wire string that does
 * not match a known entry deserializes to [unknown] instead of throwing.
 *
 * This is the production-grade EC30 (cross-version-safety) mechanism the group-banking wire enums
 * advertise: `coerceInputValues = true` alone only coerces an unknown value when the enum is a
 * PROPERTY that carries a default — a BARE top-level `decodeFromString(EnumDto.serializer(), ...)`
 * (and a property WITHOUT a default) still throws `SerializationException`. Wiring this serializer
 * via `@Serializable(with = <Enum>.Serializer::class)` makes the fallback hold in EVERY context
 * (top-level, property-with-default, property-without-default) regardless of the decoder config,
 * so a server that ships a new enum value never crashes an older client.
 *
 * @param serialName the descriptor name (conventionally the enum's simple name).
 * @param entries the enum's `entries` list — matched by [Enum.name] against the wire string.
 * @param unknown the fallback member returned for any unrecognized wire string.
 */
internal fun <T : Enum<T>> unknownFallbackEnumSerializer(
    serialName: String,
    entries: List<T>,
    unknown: T,
): KSerializer<T> = object : KSerializer<T> {
    override val descriptor = PrimitiveSerialDescriptor(serialName, PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: T) = encoder.encodeString(value.name)

    override fun deserialize(decoder: Decoder): T {
        val raw = decoder.decodeString()
        return entries.firstOrNull { it.name == raw } ?: unknown
    }
}
