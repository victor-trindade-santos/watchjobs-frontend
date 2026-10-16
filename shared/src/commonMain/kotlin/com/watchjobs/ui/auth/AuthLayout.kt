package com.watchjobs.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.watchjobs.ui.components.Hairline
import com.watchjobs.ui.components.SectionLabel
import com.watchjobs.ui.components.WatchJobsLogo
import com.watchjobs.ui.theme.WatchJobsColors

// Layout das telas de entrada: painel da marca à esquerda (telas largas) e o formulário à direita.
@Composable
fun AuthLayout(content: @Composable ColumnScope.() -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().background(WatchJobsColors.Background)) {
        if (maxWidth >= 900.dp) {
            Row(Modifier.fillMaxSize()) {
                BrandPanel(Modifier.weight(1.1f).fillMaxHeight())
                Box(
                    Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.Center
                ) {
                    Column(Modifier.widthIn(max = 440.dp).fillMaxWidth().padding(40.dp), content = content)
                }
            }
        } else {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Column(
                    Modifier.fillMaxWidth().background(WatchJobsColors.Ink).padding(horizontal = 24.dp, vertical = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    WatchJobsLogo(onDark = true)
                    Text(
                        "Cada vaga, do interesse à proposta.",
                        style = MaterialTheme.typography.titleLarge,
                        color = WatchJobsColors.White
                    )
                }
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                    Column(Modifier.widthIn(max = 480.dp).fillMaxWidth().padding(24.dp), content = content)
                }
            }
        }
    }
}

@Composable
private fun BrandPanel(modifier: Modifier) {
    Column(modifier.background(WatchJobsColors.Ink).padding(horizontal = 56.dp, vertical = 44.dp)) {
        WatchJobsLogo(onDark = true, markHeight = 28.dp, fontSize = MaterialTheme.typography.titleLarge.fontSize * 1.15f)
        Spacer(Modifier.weight(1f))
        SectionLabel("Acompanhe sua busca", color = WatchJobsColors.Red)
        Spacer(Modifier.height(14.dp))
        Text(
            "Cada vaga,\ndo interesse\nà proposta.",
            style = MaterialTheme.typography.displaySmall,
            color = WatchJobsColors.White
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Salve as vagas que importam, registre cada candidatura e saiba sempre qual é a próxima entrevista.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFFBDBDBD),
            modifier = Modifier.widthIn(max = 420.dp)
        )
        Spacer(Modifier.height(36.dp))
        // Mini-quadro de candidaturas, com o mesmo desenho do My Applications.
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            PreviewColumn("Em processo") {
                PreviewCard("Northlight Systems", "Senior Product Designer", "Entrevista RH", "Qui 09:30")
            }
            PreviewColumn("Propostas") {
                PreviewCard("Ferrolane", "Staff Product Designer", "Proposta", "Seg 14:00")
            }
        }
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(40.dp))
        Text("© 2026 WatchJobs", style = MaterialTheme.typography.bodySmall, color = Color(0xFF8A8A8A))
    }
}

@Composable
private fun PreviewColumn(title: String, content: @Composable () -> Unit) {
    Column(Modifier.width(230.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title.uppercase(), style = MaterialTheme.typography.labelMedium, color = WatchJobsColors.White)
        Box(Modifier.fillMaxWidth().height(2.dp).background(WatchJobsColors.Red))
        content()
    }
}

// Miniatura de um cartão do quadro de candidaturas (mesmo desenho do My Applications).
@Composable
private fun PreviewCard(company: String, role: String, stage: String, highlight: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(WatchJobsColors.Surface)
            .border(1.dp, WatchJobsColors.Border)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(company, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = WatchJobsColors.Muted)
        Text(role, style = MaterialTheme.typography.titleMedium, color = WatchJobsColors.Ink)
        Hairline()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stage, style = MaterialTheme.typography.bodySmall, color = WatchJobsColors.Muted, modifier = Modifier.weight(1f))
            Text(highlight, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = WatchJobsColors.Red)
        }
    }
}
