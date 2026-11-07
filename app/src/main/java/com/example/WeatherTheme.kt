package com.example

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val OutfitFontFamily = FontFamily(
    Font(R.font.outfit, FontWeight.Normal),
    Font(R.font.outfit, FontWeight.Medium),
    Font(R.font.outfit, FontWeight.SemiBold),
    Font(R.font.outfit, FontWeight.Bold)
)

val PlusJakartaSansFontFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold)
)

@Immutable
data class GlassmorphismPalette(
    val isDark: Boolean,
    val primaryText: Color,
    val secondaryText: Color,
    val mutedText: Color,
    val accentCyan: Color,
    val accentPurple: Color,
    val accentGold: Color,
    val accentEmerald: Color,
    val cardSurfaceTop: Color,
    val cardSurfaceBottom: Color,
    val cardHighlightSurface: Color,
    val cardBorderLight: Color,
    val cardBorderSubtle: Color,
    val pillBackground: Color,
    val navBarSurface: Color
) {
    val glassCardBrush: Brush
        get() = Brush.verticalGradient(
            colors = listOf(cardSurfaceTop, cardSurfaceBottom)
        )

    val glassBorderBrush: Brush
        get() = Brush.linearGradient(
            colors = listOf(cardBorderLight, cardBorderSubtle, cardBorderLight.copy(alpha = 0.15f))
        )
}

val DarkGlassPalette = GlassmorphismPalette(
    isDark = true,
    primaryText = Color(0xFFF8FAFC),
    secondaryText = Color(0xFFCBD5E1),
    mutedText = Color(0xFF94A3B8),
    accentCyan = Color(0xFF38BDF8),
    accentPurple = Color(0xFFA855F7),
    accentGold = Color(0xFFFBBF24),
    accentEmerald = Color(0xFF34D399),
    cardSurfaceTop = Color(0x2EFFFFFF),
    cardSurfaceBottom = Color(0x12FFFFFF),
    cardHighlightSurface = Color(0x3838BDF8),
    cardBorderLight = Color(0x59FFFFFF),
    cardBorderSubtle = Color(0x14FFFFFF),
    pillBackground = Color(0x280F172A),
    navBarSurface = Color(0xD9090D1A)
)

val LightGlassPalette = GlassmorphismPalette(
    isDark = false,
    primaryText = Color(0xFF0F172A),
    secondaryText = Color(0xFF1E293B),
    mutedText = Color(0xFF475569),
    accentCyan = Color(0xFF0284C7),
    accentPurple = Color(0xFF7C3AED),
    accentGold = Color(0xFFD97706),
    accentEmerald = Color(0xFF059669),
    cardSurfaceTop = Color(0xCCFFFFFF),
    cardSurfaceBottom = Color(0x99F1F5F9),
    cardHighlightSurface = Color(0x4D0284C7),
    cardBorderLight = Color(0xE6FFFFFF),
    cardBorderSubtle = Color(0x4D94A3B8),
    pillBackground = Color(0x80FFFFFF),
    navBarSurface = Color(0xE6F8FAFC)
)

val LocalGlassPalette = staticCompositionLocalOf { DarkGlassPalette }

private val DarkWeatherColorScheme = darkColorScheme(
    primary = Color(0xFF38BDF8),
    onPrimary = Color(0xFF06162E),
    primaryContainer = Color(0xFF1E293B),
    onPrimaryContainer = Color(0xFFE0F2FE),
    secondary = Color(0xFFA855F7),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF2E1065),
    onSecondaryContainer = Color(0xFFF3E8FF),
    tertiary = Color(0xFFFBBF24),
    background = Color(0xFF060913),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF0F172A),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFFCBD5E1)
)

private val LightWeatherColorScheme = lightColorScheme(
    primary = Color(0xFF0284C7),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0C4A6E),
    secondary = Color(0xFF7C3AED),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEDE9FE),
    onSecondaryContainer = Color(0xFF4C1D95),
    tertiary = Color(0xFFD97706),
    background = Color(0xFFE0F2FE),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFF8FAFC),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF334155)
)

val WeatherTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 76.sp,
        lineHeight = 80.sp,
        letterSpacing = (-2.5).sp
    ),
    displayMedium = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 44.sp,
        lineHeight = 50.sp,
        letterSpacing = (-1.0).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp
    ),
    titleLarge = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp
    ),
    titleSmall = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.4.sp
    ),
    labelMedium = TextStyle(
        fontFamily = PlusJakartaSansFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

val WeatherShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(32.dp),
    extraLarge = RoundedCornerShape(40.dp)
)

@Composable
fun WeatherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkWeatherColorScheme else LightWeatherColorScheme
    val glassPalette = if (darkTheme) DarkGlassPalette else LightGlassPalette

    CompositionLocalProvider(LocalGlassPalette provides glassPalette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = WeatherTypography,
            shapes = WeatherShapes,
            content = content
        )
    }
}
