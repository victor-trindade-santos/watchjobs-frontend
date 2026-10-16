package com.watchjobs.model

import kotlinx.serialization.Serializable

@Serializable
enum class WorkMode(val label: String) {
    REMOTE("Remoto"),
    HYBRID("Híbrido"),
    ONSITE("Presencial")
}

@Serializable
enum class ExperienceLevel(val label: String) {
    INTERN("Estágio"),
    JUNIOR("Júnior"),
    MID("Pleno"),
    SENIOR("Sênior"),
    LEAD("Liderança")
}

@Serializable
enum class JobType(val label: String) {
    CLT("CLT"),
    PJ("PJ"),
    INTERNSHIP("Estágio"),
    FREELANCE("Freelance"),
    TEMPORARY("Temporário")
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
