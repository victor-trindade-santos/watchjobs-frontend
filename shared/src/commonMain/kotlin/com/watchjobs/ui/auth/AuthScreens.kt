package com.watchjobs.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.watchjobs.ui.components.ErrorBanner
import com.watchjobs.ui.components.GhostButton
import com.watchjobs.ui.components.Hairline
import com.watchjobs.ui.components.LabeledTextField
import com.watchjobs.ui.components.PrimaryButton
import com.watchjobs.ui.components.SecondaryButton
import com.watchjobs.ui.components.SectionLabel
import com.watchjobs.ui.theme.WatchJobsColors
import com.watchjobs.viewmodel.DeleteAccountViewModel
import com.watchjobs.viewmodel.ForgotPasswordViewModel
import com.watchjobs.viewmodel.LoginViewModel
import com.watchjobs.viewmodel.SignupViewModel

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoggedIn: () -> Unit,
    onCreateAccount: () -> Unit,
    onForgotPassword: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    AuthLayout {
        SectionLabel("Entrar", color = WatchJobsColors.Red)
        Spacer(Modifier.height(10.dp))
        Text("Bem-vindo de volta.", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(6.dp))
        Text(
            "Acesse sua conta para acompanhar suas vagas e candidaturas.",
            style = MaterialTheme.typography.bodyLarge,
            color = WatchJobsColors.Muted
        )
        Spacer(Modifier.height(28.dp))
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            state.error?.let { ErrorBanner(it) }
            LabeledTextField(
                label = "E-mail",
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                error = state.fieldErrors["email"],
                placeholder = "Digite o seu e-mail",
                keyboardType = KeyboardType.Email,
                tag = "loginEmail"
            )
            LabeledTextField(
                label = "Senha",
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                error = state.fieldErrors["password"],
                isPassword = true,
                tag = "loginPassword"
            )
            PrimaryButton(
                text = "Entrar",
                onClick = { viewModel.submit(onLoggedIn) },
                loading = state.loading,
                modifier = Modifier.fillMaxWidth().testTag("loginButton")
            )
            GhostButton(
                "Esqueci minha senha",
                onForgotPassword,
                modifier = Modifier.fillMaxWidth().testTag("forgotPassword")
            )
        }
        Spacer(Modifier.height(28.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Hairline(Modifier.weight(1f))
            Text("  AINDA NÃO TEM CONTA?  ", style = MaterialTheme.typography.labelSmall, color = WatchJobsColors.Muted)
            Hairline(Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
        SecondaryButton("Criar conta", onCreateAccount, modifier = Modifier.fillMaxWidth().testTag("goToSignup"))
    }
}

@Composable
fun SignupScreen(
    viewModel: SignupViewModel,
    onSignedUp: (email: String) -> Unit,
    onBackToLogin: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val form = state.form
    val errors = state.fieldErrors
    AuthLayout {
        SectionLabel("Criar conta", color = WatchJobsColors.Red)
        Spacer(Modifier.height(10.dp))
        Text("Comece a acompanhar suas vagas.", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(6.dp))
        Text("Leva menos de um minuto.", style = MaterialTheme.typography.bodyLarge, color = WatchJobsColors.Muted)
        Spacer(Modifier.height(24.dp))
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            state.error?.let { ErrorBanner(it) }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LabeledTextField(
                    label = "Nome",
                    value = form.firstName,
                    onValueChange = { v -> viewModel.update("firstName") { it.copy(firstName = v) } },
                    error = errors["firstName"],
                    modifier = Modifier.weight(1f),
                    tag = "signupFirstName"
                )
                LabeledTextField(
                    label = "Sobrenome",
                    value = form.lastName,
                    onValueChange = { v -> viewModel.update("lastName") { it.copy(lastName = v) } },
                    error = errors["lastName"],
                    modifier = Modifier.weight(1f),
                    tag = "signupLastName"
                )
            }
            LabeledTextField(
                label = "Data de nascimento",
                value = form.birthDate,
                onValueChange = { v -> viewModel.update("birthDate") { it.copy(birthDate = v) } },
                error = errors["birthDate"],
                placeholder = "dd/mm/aaaa",
                keyboardType = KeyboardType.Number,
                tag = "signupBirthDate"
            )
            LabeledTextField(
                label = "E-mail",
                value = form.email,
                onValueChange = { v -> viewModel.update("email") { it.copy(email = v) } },
                error = errors["email"],
                placeholder = "Digite o seu e-mail",
                keyboardType = KeyboardType.Email,
                tag = "signupEmail"
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LabeledTextField(
                    label = "Senha",
                    value = form.password,
                    onValueChange = { v -> viewModel.update("password") { it.copy(password = v) } },
                    error = errors["password"],
                    isPassword = true,
                    modifier = Modifier.weight(1f),
                    tag = "signupPassword"
                )
                LabeledTextField(
                    label = "Confirmar senha",
                    value = form.confirmPassword,
                    onValueChange = { v -> viewModel.update("confirmPassword") { it.copy(confirmPassword = v) } },
                    error = errors["confirmPassword"],
                    isPassword = true,
                    modifier = Modifier.weight(1f),
                    tag = "signupConfirmPassword"
                )
            }
            Text(
                "Mínimo de 8 caracteres, com pelo menos uma letra e um número.",
                style = MaterialTheme.typography.bodySmall,
                color = WatchJobsColors.Muted
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PrimaryButton(
                    text = "Cadastrar",
                    onClick = { viewModel.submit(onSignedUp) },
                    loading = state.loading,
                    modifier = Modifier.weight(1f).testTag("signupButton")
                )
                SecondaryButton(
                    text = "Limpar Campos",
                    onClick = viewModel::clear,
                    modifier = Modifier.weight(1f).testTag("signupClear")
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        GhostButton("Já tenho conta · Entrar", onBackToLogin, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
fun ForgotPasswordScreen(
    viewModel: ForgotPasswordViewModel,
    onDone: (String) -> Unit,
    onBackToLogin: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    AuthLayout {
        SectionLabel("Recuperar senha", color = WatchJobsColors.Red)
        Spacer(Modifier.height(10.dp))
        Text(
            if (state.step == 1) "Informe o e-mail da conta." else "Digite o código e a nova senha.",
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Se o e-mail existir, enviamos um código de 6 dígitos. Ele vale por 15 minutos.",
            style = MaterialTheme.typography.bodyLarge,
            color = WatchJobsColors.Muted
        )
        Spacer(Modifier.height(28.dp))
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            state.error?.let { ErrorBanner(it) }
            state.info?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = WatchJobsColors.Ink) }
            if (state.step == 1) {
                LabeledTextField(
                    label = "E-mail",
                    value = state.email,
                    onValueChange = viewModel::onEmailChange,
                    error = state.fieldErrors["email"],
                    placeholder = "Digite o seu e-mail",
                    keyboardType = KeyboardType.Email,
                    tag = "forgotEmail"
                )
                PrimaryButton(
                    text = "Enviar código",
                    onClick = viewModel::requestCode,
                    loading = state.loading,
                    modifier = Modifier.fillMaxWidth().testTag("forgotSend")
                )
            } else {
                LabeledTextField(
                    label = "Código",
                    value = state.code,
                    onValueChange = viewModel::onCodeChange,
                    error = state.fieldErrors["code"],
                    placeholder = "000000",
                    keyboardType = KeyboardType.Number,
                    tag = "forgotCode"
                )
                LabeledTextField(
                    label = "Nova senha",
                    value = state.password,
                    onValueChange = viewModel::onPasswordChange,
                    error = state.fieldErrors["newPassword"],
                    isPassword = true,
                    tag = "forgotPasswordNew"
                )
                LabeledTextField(
                    label = "Confirmar senha",
                    value = state.confirmPassword,
                    onValueChange = viewModel::onConfirmChange,
                    error = state.fieldErrors["confirmPassword"],
                    isPassword = true,
                    tag = "forgotPasswordConfirm"
                )
                Text(
                    "Mínimo de 8 caracteres, com pelo menos uma letra e um número.",
                    style = MaterialTheme.typography.bodySmall,
                    color = WatchJobsColors.Muted
                )
                PrimaryButton(
                    text = "Redefinir senha",
                    onClick = { viewModel.reset(onDone) },
                    loading = state.loading,
                    modifier = Modifier.fillMaxWidth().testTag("forgotReset")
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        GhostButton("Voltar ao login", onBackToLogin, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
fun DeleteAccountDialog(
    viewModel: DeleteAccountViewModel,
    onDeleted: () -> Unit,
    onDismiss: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Excluir conta") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Esta ação apaga a conta e todos os dados. Não dá para desfazer.")
                state.error?.let { ErrorBanner(it) }
                LabeledTextField(
                    label = "Senha",
                    value = state.password,
                    onValueChange = viewModel::onPasswordChange,
                    isPassword = true,
                    tag = "deleteAccountPassword"
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = state.confirmed,
                        onCheckedChange = viewModel::onConfirmedChange,
                        modifier = Modifier.testTag("deleteAccountConfirm")
                    )
                    Text("Confirmo que quero excluir a conta.")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { viewModel.submit(onDeleted) }, enabled = !state.loading, modifier = Modifier.testTag("deleteAccountSubmit")) {
                Text(if (state.loading) "Excluindo..." else "Excluir", color = WatchJobsColors.Red)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
