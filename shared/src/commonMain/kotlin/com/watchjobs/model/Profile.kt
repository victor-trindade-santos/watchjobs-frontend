package com.watchjobs.model

import kotlinx.serialization.Serializable

// Perfil (F3): espelha os DTOs de /profile do backend. Datas como texto ISO (aaaa-mm-dd).

@Serializable
data class LocationRef(val id: Long = 0, val countryCode: String = "", val state: String? = null, val city: String? = null) {
    val label: String
        get() = listOfNotNull(city, state, countryCode.ifEmpty { null }).joinToString(", ")
}

@Serializable
data class LocationInput(val countryCode: String? = null, val state: String? = null, val city: String? = null)

@Serializable
data class Personal(
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val birthDate: String = "",
    val profileImageUrl: String? = null,
    val location: LocationRef? = null,
    val objective: String? = null,
    val availability: String? = null
)

@Serializable
data class PersonalRequest(
    val firstName: String? = null,
    val lastName: String? = null,
    val birthDate: String? = null,
    val location: LocationInput? = null,
    val objective: String? = null,
    val availability: String? = null
)

enum class Availability(val label: String) {
    IMMEDIATE("Imediata"),
    TWO_WEEKS("Em 2 semanas"),
    ONE_MONTH("Em 1 mês"),
    NOT_LOOKING("Não estou procurando")
}

@Serializable
data class LinkedInStatus(
    val imported: Boolean = false,
    val lastImportAt: String? = null,
    val importedExperiences: Int = 0,
    val importedSkills: Int = 0
)

@Serializable
data class ProfileStats(
    val completeness: Int = 0,
    val missing: List<String> = emptyList(),
    val skillCount: Int = 0,
    val compatibleJobs: Long = 0,
    val compatibleMinScore: Int = 70,
    val matchPrecision: Int = 0,
    val linkedin: LinkedInStatus = LinkedInStatus()
)

enum class ExperienceType(val label: String) {
    EMPLOYMENT("Emprego"),
    INTERNSHIP("Estágio"),
    FREELANCE("Freelance"),
    VOLUNTEER("Voluntário"),
    OTHER("Outro")
}

enum class SkillLevel(val label: String) {
    BEGINNER("Básico"),
    INTERMEDIATE("Intermediário"),
    ADVANCED("Avançado"),
    EXPERT("Especialista")
}

enum class ProficiencyLevel(val label: String) {
    A1("A1 · Iniciante"),
    A2("A2 · Básico"),
    B1("B1 · Intermediário"),
    B2("B2 · Intermediário avançado"),
    C1("C1 · Avançado"),
    C2("C2 · Fluente"),
    NATIVE("Nativo")
}

enum class PreferredWorkMode(val label: String) {
    REMOTE("Remoto"),
    HYBRID("Híbrido"),
    ON_SITE("Presencial"),
    ANY("Qualquer")
}

@Serializable
data class Experience(
    val id: Int,
    val jobTitle: String,
    val organizationId: Long = 0,
    val organizationName: String,
    val experienceType: String = "EMPLOYMENT",
    val description: String? = null,
    val startDate: String,
    val endDate: String? = null,
    val current: Boolean = false,
    val roleFamily: String? = null,
    val inferredLevel: ExperienceLevel? = null,
    val source: String = "MANUAL"
)

@Serializable
data class ExperienceRequest(
    val jobTitle: String,
    val organizationName: String,
    val experienceType: String,
    val description: String? = null,
    val startDate: String,
    val endDate: String? = null,
    val current: Boolean = false
)

@Serializable
data class Education(
    val id: Int,
    val educationId: Long,
    val educationLevel: String,
    val fieldOfStudy: String,
    val institution: String,
    val startDate: String,
    val endDate: String? = null,
    val current: Boolean = false,
    val source: String = "MANUAL"
)

@Serializable
data class EducationRequest(
    val educationId: Long,
    val institution: String,
    val startDate: String,
    val endDate: String? = null,
    val current: Boolean = false
)

@Serializable
data class UserSkill(
    val id: Int,
    val skillId: Long,
    val skillName: String,
    val skillLevel: String,
    val yearsExperience: Double? = null,
    val source: String = "MANUAL"
)

@Serializable
data class UserSkillRequest(val skillId: Long, val skillLevel: String, val yearsExperience: Double? = null)

@Serializable
data class UserLanguage(
    val id: Int,
    val languageId: Long,
    val languageName: String,
    val proficiencyLevel: String,
    val source: String = "MANUAL"
)

@Serializable
data class UserLanguageRequest(val languageId: Long, val proficiencyLevel: String)

@Serializable
data class SkillRef(val id: Long, val name: String)

@Serializable
data class LanguageRef(val id: Long, val name: String)

@Serializable
data class Preferences(
    val preferredRoles: List<String> = emptyList(),
    val keywords: List<String> = emptyList(),
    val preferredWorkMode: String = "ANY",
    val preferredExperienceLevel: ExperienceLevel? = null,
    val preferredJobType: JobType? = null,
    val salaryMin: Double? = null,
    val salaryMax: Double? = null,
    val location: LocationRef? = null,
    val desiredSkills: List<SkillRef> = emptyList(),
    val desiredLanguages: List<LanguageRef> = emptyList(),
    val autoSearchEnabled: Boolean = false,
    val profileVersion: Int = 1,
    val confirmed: Boolean = false,
    val roleFamilies: List<String> = emptyList()
)

@Serializable
data class PreferencesRequest(
    val preferredRoles: List<String>,
    val keywords: List<String> = emptyList(),
    val preferredWorkMode: String,
    val preferredExperienceLevel: ExperienceLevel? = null,
    val preferredJobType: JobType? = null,
    val salaryMin: Double? = null,
    val salaryMax: Double? = null,
    val location: LocationInput? = null,
    val desiredSkillIds: List<Long> = emptyList(),
    val desiredLanguageIds: List<Long> = emptyList(),
    val autoSearchEnabled: Boolean? = null
)

@Serializable
data class ProfileSummary(
    val primaryRoleFamily: String? = null,
    val experienceLevel: ExperienceLevel? = null,
    val levelConfidence: String? = null,
    val levelSource: String = "RULES",
    val totalYears: Double = 0.0,
    val internshipYears: Double = 0.0,
    val yearsByFamily: Map<String, Double> = emptyMap(),
    val yearsBySkill: Map<String, Double> = emptyMap(),
    val reasons: List<String> = emptyList(),
    val profileVersion: Int = 1
) {
    val confidenceLabel: String
        get() = when (levelConfidence) {
            "HIGH" -> "confiança alta"
            "MEDIUM" -> "confiança média"
            "LOW" -> "confiança baixa"
            else -> "sem dados"
        }
}

@Serializable
data class LevelCorrection(val experienceLevel: ExperienceLevel?)

@Serializable
data class PreferencesSuggestion(
    val preferredRoles: List<String> = emptyList(),
    val roleFamily: String? = null,
    val preferredExperienceLevel: ExperienceLevel? = null,
    val desiredSkills: List<SkillRef> = emptyList(),
    val location: LocationRef? = null
)

@Serializable
data class Profile(
    val personal: Personal = Personal(),
    val experiences: List<Experience> = emptyList(),
    val educations: List<Education> = emptyList(),
    val skills: List<UserSkill> = emptyList(),
    val languages: List<UserLanguage> = emptyList(),
    val preferences: Preferences? = null,
    val summary: ProfileSummary? = null
)

// Catálogos (autocompletar)
@Serializable
data class SkillOption(val id: Long, val name: String, val category: String = "")

@Serializable
data class OrganizationOption(val id: Long, val name: String, val type: String = "COMPANY")

@Serializable
data class LanguageOption(val id: Long, val name: String)

@Serializable
data class EducationOption(val id: Long, val level: String, val fieldOfStudy: String) {
    val label: String
        get() = "${educationLevelLabel(level)} · $fieldOfStudy"
}

@Serializable
data class RoleFamilyOption(val code: String, val name: String)

fun educationLevelLabel(level: String): String = when (level) {
    "HIGH_SCHOOL" -> "Ensino médio"
    "TECHNICAL" -> "Técnico"
    "TECHNOLOGIST" -> "Tecnólogo"
    "BACHELOR" -> "Graduação"
    "POSTGRADUATE" -> "Pós-graduação"
    "MBA" -> "MBA"
    "MASTER" -> "Mestrado"
    "DOCTORATE" -> "Doutorado"
    else -> level
}

// Importação do LinkedIn (F3-10)
@Serializable
data class ImportExperienceItem(
    val ref: String,
    val status: String,
    val jobTitle: String,
    val organizationName: String,
    val startDate: String? = null,
    val endDate: String? = null,
    val current: Boolean = false
)

@Serializable
data class ImportEducationItem(
    val ref: String,
    val status: String,
    val institution: String,
    val degree: String? = null,
    val fieldOfStudy: String? = null,
    val startDate: String? = null
)

@Serializable
data class ImportSkillItem(val ref: String, val status: String, val name: String)

@Serializable
data class ImportLanguageItem(val ref: String, val status: String, val name: String, val proficiencyLevel: String = "")

@Serializable
data class ImportProfileItem(val headline: String? = null, val location: String? = null)

@Serializable
data class ImportPreview(
    val experiences: List<ImportExperienceItem> = emptyList(),
    val educations: List<ImportEducationItem> = emptyList(),
    val skills: List<ImportSkillItem> = emptyList(),
    val languages: List<ImportLanguageItem> = emptyList(),
    val profile: ImportProfileItem? = null
)

@Serializable
data class ProfileImport(
    val id: Long,
    val status: String,
    val importSource: String = "LINKEDIN_ZIP",
    val itemsTotal: Int = 0,
    val itemsApplied: Int = 0,
    val preview: ImportPreview = ImportPreview()
)

@Serializable
data class ApplyImportRequest(val refs: List<String>)

fun importStatusLabel(status: String): String = when (status) {
    "NEW" -> "Novo"
    "EXISTS" -> "Já existe"
    "CONFLICT" -> "Conflita com o que você digitou"
    "UNRECOGNIZED" -> "Não reconhecido"
    "INVALID" -> "Incompleto"
    else -> status
}
