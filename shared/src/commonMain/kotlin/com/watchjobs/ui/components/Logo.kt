package com.watchjobs.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.watchjobs.ui.theme.WatchJobsColors

// A marca da logo (peça vermelha + ponto), desenhada em vetor a partir do PNG do Figma.
private fun logoMark(dotColor: Color): ImageVector =
    ImageVector.Builder(
        name = "WatchJobsMark",
        defaultWidth = 21.dp,
        defaultHeight = 31.dp,
        viewportWidth = 210f,
        viewportHeight = 310f
    ).apply {
        path(fill = SolidColor(WatchJobsColors.Red)) {
            moveTo(99f, 2f)
            lineTo(117f, 57f)
            lineTo(205f, 57f)
            lineTo(133f, 104f)
            lineTo(197f, 308f)
            lineTo(45f, 308f)
            close()
        }
        path(fill = SolidColor(dotColor)) {
            moveTo(0f, 79f)
            arcTo(18f, 18f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 36f, y1 = 79f)
            arcTo(18f, 18f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 0f, y1 = 79f)
            close()
        }
    }.build()

// Ícone da janela no Desktop: sempre com o ponto escuro da marca.
private val markBrand by lazy { logoMark(WatchJobsColors.BrandDark) }

val WatchJobsMark: ImageVector get() = markBrand

// O ponto acompanha o tema (tinta no claro, claro no escuro); sobre o painel escuro da marca é sempre branco.
@Composable
fun LogoMark(height: Dp, onDark: Boolean = false, modifier: Modifier = Modifier) {
    val dot = if (onDark) WatchJobsColors.OnAccent else WatchJobsColors.Ink
    val red = WatchJobsColors.Red
    val mark = remember(dot, red) { logoMark(dot) }
    Image(
        imageVector = mark,
        contentDescription = null,
        modifier = modifier.height(height)
    )
}

@Composable
fun WatchJobsLogo(
    modifier: Modifier = Modifier,
    onDark: Boolean = false,
    markHeight: Dp = 22.dp,
    fontSize: TextUnit = 18.sp
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(markHeight * 0.35f)
    ) {
        LogoMark(height = markHeight, onDark = onDark)
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = if (onDark) WatchJobsColors.OnAccent else WatchJobsColors.Ink)) { append("Watch") }
                withStyle(SpanStyle(color = WatchJobsColors.Red)) { append("Jobs") }
            },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            fontSize = fontSize,
            letterSpacing = (-0.4).sp
        )
    }
}
