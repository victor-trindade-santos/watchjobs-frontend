package com.watchjobs.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import com.watchjobs.ui.theme.WatchJobsColors

// Texto longo recolhido em poucas linhas com "Ver mais"/"Ver menos". O texto inteiro continua salvo;
// o botão só aparece quando o conteúdo realmente não cabe (ou quando já foi expandido).
@Composable
fun ExpandableText(
    text: String,
    modifier: Modifier = Modifier,
    collapsedLines: Int = 4,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = WatchJobsColors.Ink,
    tag: String = "expandableText"
) {
    var expanded by rememberSaveable(text) { mutableStateOf(false) }
    var overflows by rememberSaveable(text) { mutableStateOf(false) }
    Column(modifier) {
        Text(
            text,
            style = style,
            color = color,
            maxLines = if (expanded) Int.MAX_VALUE else collapsedLines,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { if (!expanded) overflows = it.hasVisualOverflow },
            modifier = Modifier.testTag(tag)
        )
        if (expanded || overflows) {
            GhostButton(
                if (expanded) "Ver menos" else "Ver mais",
                { expanded = !expanded },
                Modifier.testTag("${tag}_toggle")
            )
        }
    }
}
