package com.watchjobs.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Paleta tirada dos protótipos do Figma: vermelho da logo, tinta quase preta e cinzas claros.
// No tema escuro os papéis se mantêm: "Ink" é sempre o texto principal e "White" é sempre a superfície dos cards.
@Immutable
data class WatchJobsPalette(
    val dark: Boolean,
    val red: Color,
    val redDark: Color,
    val redSoft: Color,
    val ink: Color,
    val inkSoft: Color,
    val muted: Color,
    val background: Color,
    val surface: Color,
    val white: Color,
    val border: Color,
    val divider: Color,
    val error: Color,
    val success: Color
)

val LightPalette = WatchJobsPalette(
    dark = false,
    red = Color(0xFFE5392E),
    redDark = Color(0xFFC42C22),
    redSoft = Color(0xFFFCE9E7),
    ink = Color(0xFF1B1B1B),
    inkSoft = Color(0xFF2A2A2A),
    muted = Color(0xFF6E6E6E),
    background = Color(0xFFF0F0EE),
    surface = Color(0xFFF8F8F7),
    white = Color(0xFFFFFFFF),
    border = Color(0xFFB8B8B8),
    divider = Color(0xFFD6D6D4),
    error = Color(0xFFB3261E),
    success = Color(0xFF2E7D32)
)

// Contraste conferido para texto normal (WCAG AA, 4.5:1): Ink, Muted e RedDark sobre White/Background/RedSoft.
val DarkPalette = WatchJobsPalette(
    dark = true,
    red = Color(0xFFEF5045),
    redDark = Color(0xFFFF9C94),
    redSoft = Color(0xFF3A1E1C),
    ink = Color(0xFFECECEA),
    inkSoft = Color(0xFFD4D4D2),
    muted = Color(0xFFA6A6A3),
    background = Color(0xFF121212),
    surface = Color(0xFF191919),
    white = Color(0xFF1F1F1F),
    border = Color(0xFF5C5C5A),
    divider = Color(0xFF363634),
    error = Color(0xFFF2B8B5),
    success = Color(0xFF81C784)
)

enum class ThemeMode { LIGHT, DARK }

object WatchJobsColors {
    // Estado observável: trocar o tema recompõe tudo o que lê estas cores (inclusive desenhos em Canvas).
    var palette: WatchJobsPalette by mutableStateOf(LightPalette)
        private set

    val mode: ThemeMode get() = if (palette.dark) ThemeMode.DARK else ThemeMode.LIGHT

    fun apply(mode: ThemeMode) {
        val next = if (mode == ThemeMode.DARK) DarkPalette else LightPalette
        if (palette != next) palette = next
    }

    val Red: Color get() = palette.red
    val RedDark: Color get() = palette.redDark
    val RedSoft: Color get() = palette.redSoft
    val Ink: Color get() = palette.ink
    val InkSoft: Color get() = palette.inkSoft
    val Muted: Color get() = palette.muted
    val Background: Color get() = palette.background
    val Surface: Color get() = palette.surface
    val White: Color get() = palette.white
    val Border: Color get() = palette.border
    val Divider: Color get() = palette.divider
    val Error: Color get() = palette.error
    val Success: Color get() = palette.success

    // Fixas nos dois temas: texto sobre o vermelho e o painel escuro da marca (login).
    val OnAccent = Color(0xFFFFFFFF)
    val BrandDark = Color(0xFF1B1B1B)
    val BrandMuted = Color(0xFFBDBDBD)
    val BrandFaint = Color(0xFF8A8A8A)
}

private fun colorScheme(p: WatchJobsPalette) = if (p.dark) {
    darkColorScheme(
        primary = p.red,
        onPrimary = WatchJobsColors.OnAccent,
        primaryContainer = p.redSoft,
        onPrimaryContainer = p.redDark,
        secondary = p.ink,
        onSecondary = p.white,
        background = p.background,
        onBackground = p.ink,
        surface = p.surface,
        onSurface = p.ink,
        surfaceVariant = p.white,
        onSurfaceVariant = p.muted,
        surfaceContainer = p.white,
        surfaceContainerHigh = p.white,
        surfaceContainerHighest = p.white,
        outline = p.border,
        outlineVariant = p.divider,
        error = p.error,
        onError = p.background
    )
} else {
    lightColorScheme(
        primary = p.red,
        onPrimary = WatchJobsColors.OnAccent,
        primaryContainer = p.redSoft,
        onPrimaryContainer = p.redDark,
        secondary = p.ink,
        onSecondary = p.white,
        background = p.background,
        onBackground = p.ink,
        surface = p.surface,
        onSurface = p.ink,
        surfaceVariant = p.white,
        onSurfaceVariant = p.muted,
        surfaceContainer = p.white,
        surfaceContainerHigh = p.white,
        surfaceContainerHighest = p.white,
        outline = p.border,
        outlineVariant = p.divider,
        error = p.error,
        onError = WatchJobsColors.OnAccent
    )
}

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

// Lê a paleta atual: ao trocar o tema, o MaterialTheme (campos, menus, diálogos) também troca.
@Composable
fun WatchJobsTheme(content: @Composable () -> Unit) {
    val palette = WatchJobsColors.palette
    val scheme = remember(palette) { colorScheme(palette) }
    MaterialTheme(
        colorScheme = scheme,
        typography = typography,
        shapes = shapes
    ) {
        // Textos e ícones sem cor explícita seguem a tinta do tema (o padrão seria preto, ilegível no escuro).
        CompositionLocalProvider(LocalContentColor provides palette.ink, content = content)
    }
}
