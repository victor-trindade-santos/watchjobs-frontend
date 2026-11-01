package com.watchjobs

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.watchjobs.ui.components.MainScaffold
import com.watchjobs.ui.components.MainTab
import com.watchjobs.ui.components.UserMenuAction
import kotlin.test.Test
import kotlin.test.assertEquals

// Navbar: sem o botão "Excluir"; avatar + seta abrem o menu do usuário com as quatro opções.
@OptIn(ExperimentalTestApi::class)
class NavbarMenuTest {
    @Test
    fun userMenuOpensAndDispatchesEachOption() = runDesktopComposeUiTest(width = 1280, height = 800) {
        val clicked = mutableListOf<UserMenuAction>()
        setContent {
            MainScaffold(
                initials = "MR",
                currentTab = MainTab.JOB_SEARCH,
                onTabSelected = {},
                onUserMenu = { clicked += it },
                unreadCount = 3
            ) {}
        }
        onAllNodesWithTag("deleteAccountButton").assertCountEquals(0)
        onAllNodesWithText("Excluir").assertCountEquals(0)
        onNodeWithTag("unreadBadge", useUnmergedTree = true).assertExists()

        UserMenuAction.standard.forEach { action ->
            onNodeWithTag("userMenu").performClick()
            waitForIdle()
            onAllNodesWithText(action.label).assertCountEquals(1)
            val tag = if (action == UserMenuAction.LOGOUT) "logoutButton" else "userMenu_${action.name}"
            onNodeWithTag(tag).performClick()
            waitForIdle()
        }
        assertEquals(UserMenuAction.standard, clicked)
    }
}
