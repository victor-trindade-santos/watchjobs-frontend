package com.watchjobs

import com.watchjobs.model.ProfileSetupStatus
import com.watchjobs.model.SetupRequirements
import com.watchjobs.viewmodel.SetupStep
import com.watchjobs.viewmodel.SignupViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// F05: cadastro entra direto e as etapas do assistente seguem os requisitos do backend.
class ProfileSetupFlowTest {
    @Test
    fun signupLogsInAutomatically() = runBlocking {
        val container = AppContainer(demoMode = true)
        val viewModel = SignupViewModel(container.authRepository)
        viewModel.update("firstName") { it.copy(firstName = "Ana") }
        viewModel.update("lastName") { it.copy(lastName = "Lima") }
        viewModel.update("birthDate") { it.copy(birthDate = "10/05/1995") }
        viewModel.update("email") { it.copy(email = "ana.auto@watchjobs.com") }
        viewModel.update("password") { it.copy(password = "senha1234") }
        viewModel.update("confirmPassword") { it.copy(confirmPassword = "senha1234") }

        val result = CompletableDeferred<Pair<String, Boolean>>()
        viewModel.submit { email, loggedIn -> result.complete(email to loggedIn) }

        val (email, loggedIn) = withTimeout(10_000) { result.await() }
        assertEquals("ana.auto@watchjobs.com", email)
        assertTrue(loggedIn)
        assertTrue(container.authRepository.isLoggedIn())
        assertEquals("ana.auto@watchjobs.com", container.authRepository.currentUser()?.email)
    }

    @Test
    fun stepsFollowBackendRequirements() {
        val empty = ProfileSetupStatus()
        assertTrue(SetupStep.RESUME.done(empty))
        assertFalse(SetupStep.SKILLS.done(empty))
        val partial = ProfileSetupStatus(requirements = SetupRequirements(summary = true, experiences = 1, skills = 2))
        assertTrue(SetupStep.SUMMARY.done(partial))
        assertTrue(SetupStep.EXPERIENCE.done(partial))
        assertFalse(SetupStep.SKILLS.done(partial))
        assertFalse(SetupStep.PREFERENCES.done(partial))
        val full = ProfileSetupStatus(requirements = SetupRequirements(true, 2, 3, true), canComplete = true)
        assertTrue(SetupStep.entries.filter { it != SetupStep.REVIEW }.all { it.done(full) })
        assertEquals(SetupStep.RESUME, SetupStep.of(null))
        assertEquals(SetupStep.SKILLS, SetupStep.of("SKILLS"))
    }
}
