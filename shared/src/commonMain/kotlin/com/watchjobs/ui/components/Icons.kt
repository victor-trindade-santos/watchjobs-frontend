package com.watchjobs.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.watchjobs.ui.theme.WatchJobsColors

// Ícones desenhados à mão: as fontes do Web nem sempre trazem setas e marcas, e o material-icons-extended não é usado.

// Seta para cima e para a direita, no botão de mover o card.
@Composable
fun MoveIcon(modifier: Modifier = Modifier.size(18.dp), color: Color = WatchJobsColors.Ink) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = 1.8.dp.toPx()
        drawLine(color, Offset(w * 0.2f, h * 0.8f), Offset(w * 0.8f, h * 0.2f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.35f, h * 0.2f), Offset(w * 0.8f, h * 0.2f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.8f, h * 0.2f), Offset(w * 0.8f, h * 0.65f), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

@Composable
fun CheckIcon(modifier: Modifier = Modifier.size(10.dp), color: Color = WatchJobsColors.White) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = 1.6.dp.toPx()
        drawLine(color, Offset(w * 0.1f, h * 0.55f), Offset(w * 0.4f, h * 0.85f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.4f, h * 0.85f), Offset(w * 0.92f, h * 0.15f), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

@Composable
fun CloseIcon(modifier: Modifier = Modifier.size(8.dp), color: Color = WatchJobsColors.Ink) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = 1.5.dp.toPx()
        drawLine(color, Offset(0f, 0f), Offset(w, h), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(w, 0f), Offset(0f, h), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}
