/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.serializer

import kotlinx.datetime.LocalDate
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int

/**
 * Normalizes a Fineract date onto an ISO `yyyy-MM-dd` [String], tolerating BOTH shapes Fineract and
 * the companion emit for the same logical field:
 *
 * ```
 *   [2026, 8, 2]  ->  "2026-08-02"   // Fineract's native [year, month, day] int array
 *   "2026-08-02"  ->  "2026-08-02"   // already an ISO string (companion facades)
 * ```
 *
 * Fineract serializes entity dates (activationDate, dueDate, …) as an int ARRAY and provides NO query
 * param to change it, so a raw passthrough (`/clients/{id}`, `/loans/{id}`) returns arrays while the
 * companion's own BFF facades return strings. The app's date DTO fields are `String`, so a raw
 * response array previously failed deserialization ("Expected beginning of the string, but got [").
 * Annotate such a field with `@Serializable(with = FineractDateAsIsoStringSerializer::class)` and it
 * accepts either shape. Uses kotlinx-datetime [LocalDate] so the produced string is always a valid
 * ISO date the downstream `LocalDate.parse` consumers accept.
 */
object FineractDateAsIsoStringSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FineractDateAsIsoString", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String {
        // Non-JSON formats (e.g. a test using a plain decoder) fall back to a straight string read.
        val json = decoder as? JsonDecoder ?: return decoder.decodeString()
        return when (val element = json.decodeJsonElement()) {
            is JsonArray -> {
                val parts = element.mapNotNull { (it as? JsonPrimitive)?.int }
                // Fineract dates are [y, m, d] (some datetime fields append h, m, s — take the date).
                if (parts.size >= 3) LocalDate(parts[0], parts[1], parts[2]).toString() else ""
            }
            JsonNull -> ""
            is JsonPrimitive -> element.content
            else -> ""
        }
    }

    override fun serialize(encoder: Encoder, value: String) = encoder.encodeString(value)
}
