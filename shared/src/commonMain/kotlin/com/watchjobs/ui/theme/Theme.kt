package com.watchjobs.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Paleta tirada dos protótipos do Figma: vermelho da logo, tinta quase preta e cinzas claros.
object WatchJobsColors {
    val Red = Color(0xFFE5392E)
    val RedDark = Color(0xFFC42C22)
    val RedSoft = Color(0xFFFCE9E7)
    val Ink = Color(0xFF1B1B1B)
    val InkSoft = Color(0xFF2A2A2A)
    val Muted = Color(0xFF6E6E6E)
    val Background = Color(0xFFF0F0EE)
    val Surface = Color(0xFFF8F8F7)
    val White = Color(0xFFFFFFFF)
    val Border = Color(0xFFB8B8B8)
    val Divider = Color(0xFFD6D6D4)
    val Error = Color(0xFFB3261E)
    val Success = Color(0xFF2E7D32)
}

private val colorScheme = lightColorScheme(
    primary = WatchJobsColors.Red,
    onPrimary = WatchJobsColors.White,
    primaryContainer = WatchJobsColors.RedSoft,
    onPrimaryContainer = WatchJobsColors.RedDark,
    secondary = WatchJobsColors.Ink,
    onSecondary = WatchJobsColors.White,
    background = WatchJobsColors.Background,
    onBackground = WatchJobsColors.Ink,
    surface = WatchJobsColors.Surface,
    onSurface = WatchJobsColors.Ink,
    surfaceVariant = WatchJobsColors.White,
    onSurfaceVariant = WatchJobsColors.Muted,
    surfaceContainer = WatchJobsColors.White,
    surfaceContainerHigh = WatchJobsColors.White,
    surfaceContainerHighest = WatchJobsColors.White,
    outline = WatchJobsColors.Border,
    outlineVariant = WatchJobsColors.Divider,
    error = WatchJobsColors.Error,
    onError = WatchJobsColors.White
)

private val font = FontFamily.SansSerif

private val typography = Typography(
    displaySmall = TextStyle(fontFamily = font, fontWeight = FontWeight.ExtraBold, fontSize = 40.sp, lineHeight = 44.sp, letterSpacing = (-1).sp),
    headlineLarge = TextStyle(fontFamily = font, fontWeight = FontWeight.ExtraBold, fontSize = 34.sp, lineHeight = 40.sp, letterSpacing = (-0.8).sp),
    headlineMedium = TextStyle(fontFamily = font, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.5).sp),
    titleLarge = TextStyle(fontFamily = font, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontFamily = font, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = font, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = font, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = font, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 19.sp),
    bodySmall = TextStyle(fontFamily = font, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = font, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = font, fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 1.2.sp),
    labelSmall = TextStyle(fontFamily = font, fontWeight = FontWeight.Bold, fontSize = 10.sp, lineHeight = 13.sp, letterSpacing = 1.sp)
)

// Cantos retos, como nos protótipos.
private val shapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(0.dp),
    large = RoundedCornerShape(0.dp),
    extraLarge = RoundedCornerShape(0.dp)
)

@Composable
fun WatchJobsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        shapes = shapes,
        content = content
    )
}
