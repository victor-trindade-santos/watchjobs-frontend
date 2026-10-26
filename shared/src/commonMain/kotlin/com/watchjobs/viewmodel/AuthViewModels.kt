package com.watchjobs.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watchjobs.data.dto.SigninRequest
import com.watchjobs.data.dto.SignupRequest
import com.watchjobs.data.remote.ApiException
import com.watchjobs.data.repository.AuthRepository
import com.watchjobs.util.parseBrDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Erros por campo que vêm no "details" do backend (400 VALIDATION_ERROR).
fun Throwable.fieldErrors(): Map<String, String> =
    (this as? ApiException)?.error?.details
        ?.mapNotNull { (field, message) -> message?.let { field to it } }
        ?.toMap()
        ?: emptyMap()

// Mensagem geral: para erro de validação, aponta para os campos; nos outros casos, usa a do backend.
fun Throwable.generalMessage(): String = when {
    this is ApiException && error.code == "VALIDATION_ERROR" -> "Confira os campos destacados."
    this is ApiException -> error.message
    else -> message ?: "Erro inesperado."
}

// Executa uma chamada ao backend e entrega o erro já traduzido para a tela.
fun CoroutineScope.launchRequest(
    onError: (Throwable) -> Unit,
    block: suspend () -> Unit
) = launch {
    try {
        block()
    } catch (ex: CancellationException) {
        throw ex
    } catch (ex: Exception) {
        onError(ex)
    }
}

data class LoginState(
    val email: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val fieldErrors: Map<String, String> = emptyMap()
)

class LoginViewModel(
    private val authRepository: AuthRepository,
    initialEmail: String = ""
) : ViewModel() {
    private val mutableState = MutableStateFlow(LoginState(email = initialEmail))
    val state: StateFlow<LoginState> = mutableState.asStateFlow()

    fun onEmailChange(value: String) = mutableState.update { it.copy(email = value, fieldErrors = it.fieldErrors - "email") }

    fun onPasswordChange(value: String) = mutableState.update { it.copy(password = value, fieldErrors = it.fieldErrors - "password") }

    fun submit(onSuccess: () -> Unit) {
        val current = state.value
        val localErrors = buildMap {
            if (current.email.isBlank()) put("email", "E-mail é obrigatório")
            if (current.password.isBlank()) put("password", "Senha é obrigatória")
        }
        if (localErrors.isNotEmpty()) {
            mutableState.update { it.copy(fieldErrors = localErrors, error = null) }
            return
        }
        mutableState.update { it.copy(loading = true, error = null, fieldErrors = emptyMap()) }
        viewModelScope.launchRequest(
            onError = { ex -> mutableState.update { it.copy(loading = false, error = ex.generalMessage(), fieldErrors = ex.fieldErrors()) } }
        ) {
            authRepository.signin(SigninRequest(current.email.trim(), current.password))
            mutableState.update { it.copy(loading = false) }
            onSuccess()
        }
    }
}

data class SignupForm(
    val firstName: String = "",
    val lastName: String = "",
    val birthDate: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = ""
)

data class SignupState(
    val form: SignupForm = SignupForm(),
    val loading: Boolean = false,
    val error: String? = null,
    val fieldErrors: Map<String, String> = emptyMap()
)

class SignupViewModel(private val authRepository: AuthRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(SignupState())
    val state: StateFlow<SignupState> = mutableState.asStateFlow()

    fun update(field: String, transform: (SignupForm) -> SignupForm) =
        mutableState.update { it.copy(form = transform(it.form), fieldErrors = it.fieldErrors - field) }

    fun clear() = mutableState.update { SignupState() }

    fun submit(onSuccess: (email: String) -> Unit) {
        val form = state.value.form
        val birthDate = parseBrDate(form.birthDate)
        if (birthDate == null) {
            val message = if (form.birthDate.isBlank()) "Data de nascimento é obrigatória" else "Data inválida (use dd/mm/aaaa)"
            mutableState.update { it.copy(fieldErrors = it.fieldErrors + ("birthDate" to message), error = null) }
            return
        }
        mutableState.update { it.copy(loading = true, error = null, fieldErrors = emptyMap()) }
        viewModelScope.launchRequest(
            onError = { ex -> mutableState.update { it.copy(loading = false, error = ex.generalMessage(), fieldErrors = ex.fieldErrors()) } }
        ) {
            val user = authRepository.signup(
                SignupRequest(
                    firstName = form.firstName.trim(),
                    lastName = form.lastName.trim(),
                    birthDate = birthDate,
                    email = form.email.trim(),
                    password = form.password,
                    confirmPassword = form.confirmPassword
                )
            )
            mutableState.update { it.copy(loading = false) }
            onSuccess(user.email)
        }
    }
}

data class ForgotPasswordState(
    val step: Int = 1,
    val email: String = "",
    val code: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val info: String? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val fieldErrors: Map<String, String> = emptyMap()
)

class ForgotPasswordViewModel(
    private val authRepository: AuthRepository,
    initialEmail: String = ""
) : ViewModel() {
    private val mutableState = MutableStateFlow(ForgotPasswordState(email = initialEmail))
    val state: StateFlow<ForgotPasswordState> = mutableState.asStateFlow()

    fun onEmailChange(value: String) = mutableState.update { it.copy(email = value, fieldErrors = it.fieldErrors - "email") }
    fun onCodeChange(value: String) = mutableState.update { it.copy(code = value, fieldErrors = it.fieldErrors - "code") }
    fun onPasswordChange(value: String) = mutableState.update { it.copy(password = value, fieldErrors = it.fieldErrors - "newPassword") }
    fun onConfirmChange(value: String) = mutableState.update { it.copy(confirmPassword = value, fieldErrors = it.fieldErrors - "confirmPassword") }

    fun requestCode() {
        val email = state.value.email.trim()
        if (email.isBlank()) {
            mutableState.update { it.copy(fieldErrors = mapOf("email" to "E-mail é obrigatório"), error = null) }
            return
        }
        mutableState.update { it.copy(loading = true, error = null, fieldErrors = emptyMap()) }
        viewModelScope.launchRequest(
            onError = { ex -> mutableState.update { it.copy(loading = false, error = ex.generalMessage(), fieldErrors = ex.fieldErrors()) } }
        ) {
            val demoCode = authRepository.forgotPassword(email)
            val extra = demoCode?.let { " No modo demonstração, o código é $it." }.orEmpty()
            mutableState.update {
                it.copy(
                    loading = false,
                    step = 2,
                    info = "Se o e-mail existir, enviamos um código.$extra"
                )
            }
        }
    }

    fun reset(onSuccess: (String) -> Unit) {
        val current = state.value
        val errors = buildMap {
            if (current.code.length != 6 || current.code.any { !it.isDigit() }) put("code", "Código deve ter 6 dígitos")
            if (current.password.length !in 8..255 || !current.password.any { it.isLetter() } || !current.password.any { it.isDigit() }) {
                put("newPassword", "Senha deve ter entre 8 e 255 caracteres, com pelo menos uma letra e um número")
            }
            if (current.confirmPassword != current.password) put("confirmPassword", "A senha e a confirmação de senha não coincidem")
        }
        if (errors.isNotEmpty()) {
            mutableState.update { it.copy(fieldErrors = errors, error = null) }
            return
        }
        mutableState.update { it.copy(loading = true, error = null, fieldErrors = emptyMap()) }
        viewModelScope.launchRequest(
            onError = { ex -> mutableState.update { it.copy(loading = false, error = ex.generalMessage(), fieldErrors = ex.fieldErrors()) } }
        ) {
            authRepository.resetPassword(current.email.trim(), current.code.trim(), current.password, current.confirmPassword)
            mutableState.update { it.copy(loading = false) }
            onSuccess(current.email.trim())
        }
    }
}

data class DeleteAccountState(
    val password: String = "",
    val confirmed: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null
)

class DeleteAccountViewModel(private val authRepository: AuthRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(DeleteAccountState())
    val state: StateFlow<DeleteAccountState> = mutableState.asStateFlow()

    fun onPasswordChange(value: String) = mutableState.update { it.copy(password = value, error = null) }
    fun onConfirmedChange(value: Boolean) = mutableState.update { it.copy(confirmed = value) }

    fun submit(onSuccess: () -> Unit) {
        val current = state.value
        if (current.password.isBlank()) {
            mutableState.update { it.copy(error = "Senha é obrigatória") }
            return
        }
        if (!current.confirmed) {
            mutableState.update { it.copy(error = "Confirme que deseja excluir a conta.") }
            return
        }
        mutableState.update { it.copy(loading = true, error = null) }
        viewModelScope.launchRequest(
            onError = { ex -> mutableState.update { it.copy(loading = false, error = ex.generalMessage()) } }
        ) {
            authRepository.deleteAccount(current.password)
            mutableState.update { it.copy(loading = false) }
            onSuccess()
        }
    }
}
