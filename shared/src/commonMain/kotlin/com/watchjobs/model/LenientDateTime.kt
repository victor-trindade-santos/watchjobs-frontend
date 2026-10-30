package com.watchjobs.model

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.time.Instant

// O backend manda instantes com fuso ("2026-10-29T15:00:00Z"); o app mostra a hora local do aparelho.
// Texto sem fuso (modo demonstração) continua valendo como hora local.
object LenientLocalDateTimeSerializer : KSerializer<LocalDateTime> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("LenientLocalDateTime", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): LocalDateTime {
        val text = decoder.decodeString()
        return runCatching { LocalDateTime.parse(text) }
            .getOrElse { Instant.parse(text).toLocalDateTime(TimeZone.currentSystemDefault()) }
    }
}
