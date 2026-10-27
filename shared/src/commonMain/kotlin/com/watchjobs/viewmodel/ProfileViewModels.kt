package com.watchjobs.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watchjobs.data.repository.CatalogRepository
import com.watchjobs.data.repository.ProfileRepository
import com.watchjobs.model.EducationOption
import com.watchjobs.model.EducationRequest
import com.watchjobs.model.ExperienceLevel
import com.watchjobs.model.ExperienceRequest
import com.watchjobs.model.JobType
import com.watchjobs.model.LanguageOption
import com.watchjobs.model.LocationInput
import com.watchjobs.model.OrganizationOption
import com.watchjobs.model.PersonalRequest
import com.watchjobs.model.PreferencesRequest
import com.watchjobs.model.PreferencesSuggestion
import com.watchjobs.model.PreferredWorkMode
import com.watchjobs.model.Profile
import com.watchjobs.model.ProfileImport
import com.watchjobs.model.ProfileStats
import com.watchjobs.model.SkillOption
import com.watchjobs.model.SkillRef
import com.watchjobs.model.UserLanguageRequest
import com.watchjobs.model.UserSkillRequest
import com.watchjobs.ui.components.PickedFile
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ProfileState(
    val profile: Profile? = null,
    val stats: ProfileStats? = null,
    val loading: Boolean = true,
    val busy: Boolean = false,
    val error: String? = null,
    val languages: List<LanguageOption> = emptyList(),
    val educations: List<EducationOption> = emptyList(),
    val skillOptions: List<SkillOption> = emptyList(),
    val organizationOptions: List<OrganizationOption> = emptyList()
)

// Perfil (F3-11): dados pessoais e os quatro CRUDs. Toda alteração recarrega o perfil (e o perfil deduzido).
class ProfileViewModel(
    private val repository: ProfileRepository,
    private val catalog: CatalogRepository
) : ViewModel() {
    private val mutableState = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = mutableState.asStateFlow()
    private var searchJob: Job? = null

    init {
        reload()
        viewModelScope.launchRequest(onError = {}) {
            val languages = catalog.languages()
            val educations = catalog.educations()
            mutableState.update { it.copy(languages = languages, educations = educations) }
        }
    }

    fun reload() {
        viewModelScope.launchRequest(onError = { ex -> mutableState.update { it.copy(loading = false, error = ex.generalMessage()) } }) {
            val profile = repository.profile()
            mutableState.update { it.copy(profile = profile, loading = false, error = null) }
        }
        loadStats()
    }

    // Números do cartão de estatísticas; se falharem, o resto do perfil continua aparecendo.
    private fun loadStats() {
        viewModelScope.launchRequest(onError = {}) {
            val stats = repository.stats()
            mutableState.update { it.copy(stats = stats) }
        }
    }

    // Autocompletar: espera uma pausa na digitação antes de consultar o catálogo.
    fun searchSkills(q: String) = debounced {
        val options = if (q.isBlank()) emptyList() else catalog.skills(q)
        mutableState.update { it.copy(skillOptions = options) }
    }

    fun searchOrganizations(q: String, type: String? = null) = debounced {
        val options = if (q.isBlank()) emptyList() else catalog.organizations(q, type)
        mutableState.update { it.copy(organizationOptions = options) }
    }

    private fun debounced(block: suspend () -> Unit) {
        searchJob?.cancel()
        searchJob = viewModelScope.launchRequest(onError = {}) {
            delay(250)
            block()
        }
    }

    fun savePersonal(request: PersonalRequest, onResult: (String?) -> Unit) = mutate(onResult) {
        repository.updatePersonal(request)
    }

    fun saveExperience(id: Int?, request: ExperienceRequest, onResult: (String?) -> Unit) = mutate(onResult) {
        if (id == null) repository.createExperience(request) else repository.updateExperience(id, request)
    }

    fun deleteExperience(id: Int, onResult: (String?) -> Unit) = mutate(onResult) { repository.deleteExperience(id) }

    fun saveEducation(id: Int?, request: EducationRequest, onResult: (String?) -> Unit) = mutate(onResult) {
        if (id == null) repository.createEducation(request) else repository.updateEducation(id, request)
    }

    fun deleteEducation(id: Int, onResult: (String?) -> Unit) = mutate(onResult) { repository.deleteEducation(id) }

    fun saveSkill(id: Int?, request: UserSkillRequest, onResult: (String?) -> Unit) = mutate(onResult) {
        if (id == null) repository.createSkill(request) else repository.updateSkill(id, request)
    }

    fun deleteSkill(id: Int, onResult: (String?) -> Unit) = mutate(onResult) { repository.deleteSkill(id) }

    fun saveLanguage(id: Int?, request: UserLanguageRequest, onResult: (String?) -> Unit) = mutate(onResult) {
        if (id == null) repository.createLanguage(request) else repository.updateLanguage(id, request)
    }

    fun deleteLanguage(id: Int, onResult: (String?) -> Unit) = mutate(onResult) { repository.deleteLanguage(id) }

    // Correção manual da senioridade deduzida (null volta para a dedução automática).
    fun correctLevel(level: ExperienceLevel?, onResult: (String?) -> Unit) = mutate(onResult) {
        repository.correctLevel(level)
    }

    // onResult(null) = deu certo; onResult(texto) = erro para mostrar no diálogo.
    private fun mutate(onResult: (String?) -> Unit, block: suspend () -> Unit) {
        mutableState.update { it.copy(busy = true) }
        viewModelScope.launchRequest(onError = { ex ->
            mutableState.update { it.copy(busy = false) }
            onResult(ex.fieldErrors().values.firstOrNull() ?: ex.generalMessage())
        }) {
            block()
            val profile = repository.profile()
            mutableState.update { it.copy(profile = profile, busy = false) }
            loadStats()
            onResult(null)
        }
    }
}

data class PreferencesForm(
    val roles: String = "",
    val keywords: String = "",
    val workMode: PreferredWorkMode = PreferredWorkMode.ANY,
    val level: ExperienceLevel? = null,
    val jobType: JobType? = null,
    val salaryMin: String = "",
    val salaryMax: String = "",
    val country: String = "BR",
    val state: String = "",
    val city: String = "",
    val desiredSkills: List<SkillRef> = emptyList(),
    val autoSearch: Boolean = true
)

data class PreferencesState(
    val form: PreferencesForm = PreferencesForm(),
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: String? = null,
    val fieldErrors: Map<String, String> = emptyMap(),
    val skillOptions: List<SkillOption> = emptyList(),
    val suggestionAvailable: Boolean = false,
    val confirmed: Boolean = false,
    val autoFilled: Boolean = false
)

// Preferências (F3-04, F3-11) com "Usar sugestões" (F3-12): a sugestão preenche o formulário e o usuário confirma.
class PreferencesViewModel(
    private val repository: ProfileRepository,
    private val catalog: CatalogRepository,
    autoFill: Boolean = false
) : ViewModel() {
    private val mutableState = MutableStateFlow(PreferencesState())
    val state: StateFlow<PreferencesState> = mutableState.asStateFlow()
    private var searchJob: Job? = null

    init {
        viewModelScope.launchRequest(onError = { ex -> mutableState.update { it.copy(loading = false, error = ex.generalMessage()) } }) {
            val current = repository.preferences()
            val suggestion = runCatching { repository.suggestion() }.getOrNull()
            val form = current?.let { p ->
                PreferencesForm(
                    roles = p.preferredRoles.joinToString(", "),
                    keywords = p.keywords.joinToString(", "),
                    workMode = PreferredWorkMode.entries.firstOrNull { it.name == p.preferredWorkMode } ?: PreferredWorkMode.ANY,
                    level = p.preferredExperienceLevel,
                    jobType = p.preferredJobType,
                    salaryMin = p.salaryMin?.toLong()?.toString().orEmpty(),
                    salaryMax = p.salaryMax?.toLong()?.toString().orEmpty(),
                    country = p.location?.countryCode ?: "BR",
                    state = p.location?.state.orEmpty(),
                    city = p.location?.city.orEmpty(),
                    desiredSkills = p.desiredSkills,
                    autoSearch = if (p.confirmed) p.autoSearchEnabled else true
                )
            } ?: PreferencesForm()
            val usable = suggestion != null && (suggestion.preferredRoles.isNotEmpty() || suggestion.preferredExperienceLevel != null)
            // Depois de importar um currículo, o formulário já vem preenchido; nada é salvo até o usuário confirmar.
            val prefill = autoFill && usable && current?.confirmed != true
            mutableState.update {
                it.copy(
                    form = if (prefill && suggestion != null) form.withSuggestion(suggestion) else form,
                    loading = false,
                    confirmed = current?.confirmed == true,
                    suggestionAvailable = usable,
                    autoFilled = prefill
                )
            }
        }
    }

    fun update(change: (PreferencesForm) -> PreferencesForm) = mutableState.update { it.copy(form = change(it.form)) }

    fun searchSkills(q: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launchRequest(onError = {}) {
            delay(250)
            val options = if (q.isBlank()) emptyList() else catalog.skills(q)
            mutableState.update { it.copy(skillOptions = options) }
        }
    }

    fun addSkill(option: SkillOption) = update { form ->
        if (form.desiredSkills.any { it.id == option.id }) form else form.copy(desiredSkills = form.desiredSkills + SkillRef(option.id, option.name))
    }

    fun removeSkill(id: Long) = update { form -> form.copy(desiredSkills = form.desiredSkills.filterNot { it.id == id }) }

    // Preenche só o que o motor deduz (cargos, nível, skills e local). Modo, salário e contrato o usuário escolhe.
    fun useSuggestion(showMessage: (String) -> Unit) {
        viewModelScope.launchRequest(onError = { showMessage(it.generalMessage()) }) {
            val suggestion = repository.suggestion()
            update { it.withSuggestion(suggestion) }
            showMessage("Sugestões aplicadas. Confira e salve.")
        }
    }

    private fun PreferencesForm.withSuggestion(suggestion: PreferencesSuggestion) = copy(
        roles = suggestion.preferredRoles.joinToString(", ").ifEmpty { roles },
        level = suggestion.preferredExperienceLevel ?: level,
        desiredSkills = (desiredSkills + suggestion.desiredSkills).distinctBy { it.id },
        country = suggestion.location?.countryCode ?: country,
        state = suggestion.location?.state ?: state,
        city = suggestion.location?.city ?: city
    )

    fun save(onSaved: (String) -> Unit) {
        val form = state.value.form
        val roles = form.roles.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        val min = form.salaryMin.trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull()
        val max = form.salaryMax.trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull()
        val errors = buildMap {
            if (roles.isEmpty()) put("preferredRoles", "Informe pelo menos um cargo")
            if (form.salaryMin.isNotBlank() && min == null) put("salaryMin", "Número inválido")
            if (form.salaryMax.isNotBlank() && max == null) put("salaryMax", "Número inválido")
        }
        if (errors.isNotEmpty()) {
            mutableState.update { it.copy(fieldErrors = errors, error = "Confira os campos destacados.") }
            return
        }
        mutableState.update { it.copy(saving = true, error = null, fieldErrors = emptyMap()) }
        viewModelScope.launchRequest(onError = { ex ->
            mutableState.update { it.copy(saving = false, error = ex.generalMessage(), fieldErrors = ex.fieldErrors()) }
        }) {
            repository.savePreferences(
                PreferencesRequest(
                    preferredRoles = roles,
                    keywords = form.keywords.split(',').map { it.trim() }.filter { it.isNotEmpty() },
                    preferredWorkMode = form.workMode.name,
                    preferredExperienceLevel = form.level,
                    preferredJobType = form.jobType,
                    salaryMin = min,
                    salaryMax = max,
                    location = form.country.trim().takeIf { it.length == 2 }?.let {
                        LocationInput(it.uppercase(), form.state.trim().ifEmpty { null }, form.city.trim().ifEmpty { null })
                    },
                    desiredSkillIds = form.desiredSkills.map { it.id },
                    autoSearchEnabled = form.autoSearch
                )
            )
            mutableState.update { it.copy(saving = false, confirmed = true) }
            onSaved("Preferências salvas. O feed já usa o perfil novo.")
        }
    }
}

data class ImportState(
    val uploading: Boolean = false,
    val applying: Boolean = false,
    val current: ProfileImport? = null,
    val selected: Set<String> = emptySet(),
    val error: String? = null
)

// Importar dados (F3-12): enviar o currículo (PDF, DOCX, TXT) ou o ZIP do LinkedIn, ver a prévia, escolher o que aplicar.
class LinkedInImportViewModel(private val repository: ProfileRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(ImportState())
    val state: StateFlow<ImportState> = mutableState.asStateFlow()

    fun uploadResume(file: PickedFile) = startUpload { repository.uploadResume(file.name, file.bytes) }

    fun uploadLinkedIn(file: PickedFile) = startUpload { repository.uploadLinkedIn(file.name, file.bytes) }

    private fun startUpload(call: suspend () -> ProfileImport) {
        mutableState.update { it.copy(uploading = true, error = null) }
        viewModelScope.launchRequest(onError = { ex -> mutableState.update { it.copy(uploading = false, error = ex.generalMessage()) } }) {
            val result = call()
            val preview = result.preview
            val newRefs = (preview.experiences.filter { it.status == "NEW" }.map { it.ref } +
                preview.educations.filter { it.status == "NEW" }.map { it.ref } +
                preview.skills.filter { it.status == "NEW" }.map { it.ref } +
                preview.languages.filter { it.status == "NEW" }.map { it.ref }).toSet()
            mutableState.update { it.copy(uploading = false, current = result, selected = newRefs) }
        }
    }

    fun toggle(ref: String) = mutableState.update {
        it.copy(selected = if (ref in it.selected) it.selected - ref else it.selected + ref)
    }

    // reviewPreferences: algo entrou no perfil e as preferências ainda não foram confirmadas.
    fun apply(onDone: (message: String, reviewPreferences: Boolean) -> Unit) {
        val current = state.value.current ?: return
        mutableState.update { it.copy(applying = true, error = null) }
        viewModelScope.launchRequest(onError = { ex -> mutableState.update { it.copy(applying = false, error = ex.generalMessage()) } }) {
            val result = repository.applyImport(current.id, state.value.selected.toList())
            mutableState.update { it.copy(applying = false, current = result) }
            val origin = if (result.importSource == "RESUME") "do currículo" else "do LinkedIn"
            val review = result.itemsApplied > 0 && runCatching { repository.preferences() }.getOrNull()?.confirmed != true
            onDone("${result.itemsApplied} itens importados $origin.", review)
        }
    }

    fun discard(onDone: () -> Unit) {
        val current = state.value.current ?: return onDone()
        viewModelScope.launchRequest(onError = { onDone() }) {
            repository.discardImport(current.id)
            mutableState.update { ImportState() }
            onDone()
        }
    }
}
