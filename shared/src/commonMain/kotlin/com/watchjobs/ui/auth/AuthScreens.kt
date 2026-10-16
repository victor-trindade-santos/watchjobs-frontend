package com.watchjobs.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.watchjobs.viewmodel.LoginViewModel
import com.watchjobs.viewmodel.SignupViewModel

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoggedIn: () -> Unit,
    onCreateAccount: () -> Unit
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
                placeholder = "voce@email.com",
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
                placeholder = "voce@email.com",
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
