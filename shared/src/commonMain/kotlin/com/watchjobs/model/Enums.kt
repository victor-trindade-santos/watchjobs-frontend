package com.watchjobs.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

private class TolerantEnumSerializer<T : Enum<T>>(
    serialName: String,
    private val values: () -> List<T>,
    private val unknown: () -> T,
    private val aliases: Map<String, () -> T> = emptyMap()
) : KSerializer<T> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor(serialName, PrimitiveKind.STRING)


    override fun serialize(encoder: Encoder, value: T) {
        encoder.encodeString(value.name)
    }

    override fun deserialize(decoder: Decoder): T {
        val raw = decoder.decodeString()
        aliases[raw]?.let { return it() }
        return values().firstOrNull { it.name == raw } ?: unknown()
    }
}

object WorkModeSerializer : KSerializer<WorkMode> by TolerantEnumSerializer(
    "WorkMode",
    { WorkMode.entries },
    { WorkMode.UNKNOWN },
    mapOf("ONSITE" to { WorkMode.ON_SITE })
)

object ExperienceLevelSerializer : KSerializer<ExperienceLevel> by TolerantEnumSerializer(
    "ExperienceLevel",
    { ExperienceLevel.entries },
    { ExperienceLevel.UNKNOWN }
)

object JobTypeSerializer : KSerializer<JobType> by TolerantEnumSerializer(
    "JobType",
    { JobType.entries },
    { JobType.UNKNOWN }
)

@Serializable(with = WorkModeSerializer::class)
enum class WorkMode(val label: String) {
    REMOTE("Remoto"),
    HYBRID("Híbrido"),
    ON_SITE("Presencial"),
    UNKNOWN("Não informado");

    companion object {
        val selectable = listOf(REMOTE, HYBRID, ON_SITE)
    }
}


@Serializable(with = ExperienceLevelSerializer::class)
enum class ExperienceLevel(val label: String) {
    INTERN("Estágio"),
    TRAINEE("Trainee"),
    JUNIOR("Júnior"),
    MID("Pleno"),
    SENIOR("Sênior"),
    SPECIALIST("Especialista"),
    LEAD("Liderança"),
    UNKNOWN("Não informado");

    companion object {
        val selectable = listOf(INTERN, TRAINEE, JUNIOR, MID, SENIOR, SPECIALIST, LEAD)
    }
}

@Serializable(with = JobTypeSerializer::class)
enum class JobType(val label: String) {
    CLT("CLT"),
    PJ("PJ"),
    INTERNSHIP("Estágio"),
    FREELANCE("Freelance"),
    TEMPORARY("Temporário"),
    UNKNOWN("Não informado");

    companion object {
        val selectable = listOf(CLT, PJ, INTERNSHIP, FREELANCE, TEMPORARY)
    }
}

@Serializable
enum class SavedJobStatus(val label: String) {
    SAVED("Salva"),
    PLANNING_TO_APPLY("Planejando se candidatar"),
    APPLIED("Candidatado"),
    IN_PROCESS("Em processo"),
    OFFER_RECEIVED("Proposta recebida"),
    ACCEPTED("Aceita"),
    REJECTED("Recusada"),
    WITHDRAWN("Desistência");

    companion object {
        // Status que uma candidatura pode ter (os da vaga a partir de APPLIED).
        val applicationStatuses = listOf(APPLIED, IN_PROCESS, OFFER_RECEIVED, ACCEPTED, REJECTED, WITHDRAWN)

        fun labelOf(name: String): String = entries.firstOrNull { it.name == name }?.label ?: name
    }
}
