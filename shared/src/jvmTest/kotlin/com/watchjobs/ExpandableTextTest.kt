package com.watchjobs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.watchjobs.ui.components.ExpandableText
import kotlin.test.Test

// F08: textos longos do perfil aparecem recolhidos com "Ver mais", sem perder conteúdo nem quebras de linha.
@OptIn(ExperimentalTestApi::class)
class ExpandableTextTest {
    private val longText = (1..30).joinToString("\n") { "$it. Liderei a migração de serviços críticos com foco em disponibilidade." }

    // Janela alta: expandido, o texto longo (~1200px) não pode espremer o curto nem empurrar o botão para fora da tela.
    @Test
    fun longTextShowsToggleAndKeepsFullContent() = runDesktopComposeUiTest(width = 600, height = 2000) {
          setContent {
            Column(Modifier.width(400.dp)) {
                ExpandableText(longText, collapsedLines = 3, tag = "summary")
                ExpandableText("Curto.", collapsedLines = 3, tag = "short")
            }
        }
        waitForIdle()
        onNodeWithTag("summary").assertTextEquals(longText)
        onNodeWithText("Ver mais").assertExists()
        onNodeWithTag("summary_toggle").performClick()
        onNodeWithText("Ver menos").assertExists()
        onNodeWithTag("summary").assertTextEquals(longText)
        onNodeWithTag("summary_toggle").performClick()
        onNodeWithText("Ver mais").assertExists()
        onNodeWithTag("short_toggle").assertDoesNotExist()
    }
}
