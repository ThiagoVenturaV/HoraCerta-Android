package com.thiagoventura.horacerta.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.thiagoventura.horacerta.R

val Cobalt = Color(0xFF0757CF)
val CobaltDark = Color(0xFF003793)
val CobaltSoft = Color(0xFFE5F0FF)
val Ivory = Color(0xFFFEFBF8)
val SurfaceWhite = Color(0xFFFFFFFF)
val Ink = Color(0xFF062052)
val Muted = Color(0xFF324D73)
val Success = Color(0xFF187A50)
val Warning = Color(0xFFE88B2B)
val Danger = Color(0xFFC0362C)

private val HoraCertaColors = lightColorScheme(
    primary = Cobalt,
    onPrimary = Color.White,
    primaryContainer = CobaltSoft,
    onPrimaryContainer = CobaltDark,
    secondary = Warning,
    background = Ivory,
    onBackground = Ink,
    surface = SurfaceWhite,
    onSurface = Ink,
    surfaceVariant = Color(0xFFF1EEE6),
    onSurfaceVariant = Muted,
    error = Danger,
)

val HoraCertaFont = FontFamily(
    Font(R.font.roboto_condensed_regular, FontWeight.Normal),
    Font(R.font.roboto_condensed_bold, FontWeight.SemiBold),
    Font(R.font.roboto_condensed_bold, FontWeight.Bold),
    Font(R.font.roboto_condensed_bold, FontWeight.ExtraBold),
)

private val HoraCertaTypography = Typography(
    displayLarge = TextStyle(fontFamily = HoraCertaFont, fontWeight = FontWeight.Bold, fontSize = 56.sp, lineHeight = 58.sp),
    displayMedium = TextStyle(fontFamily = HoraCertaFont, fontWeight = FontWeight.Bold, fontSize = 46.sp, lineHeight = 48.sp),
    headlineLarge = TextStyle(fontFamily = HoraCertaFont, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 39.sp),
    headlineMedium = TextStyle(fontFamily = HoraCertaFont, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 33.sp),
    headlineSmall = TextStyle(fontFamily = HoraCertaFont, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 27.sp),
    titleLarge = TextStyle(fontFamily = HoraCertaFont, fontWeight = FontWeight.Bold, fontSize = 23.sp, lineHeight = 27.sp),
    titleMedium = TextStyle(fontFamily = HoraCertaFont, fontWeight = FontWeight.Bold, fontSize = 19.sp, lineHeight = 23.sp),
    titleSmall = TextStyle(fontFamily = HoraCertaFont, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = HoraCertaFont, fontSize = 18.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontFamily = HoraCertaFont, fontSize = 16.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = HoraCertaFont, fontSize = 14.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = HoraCertaFont, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 21.sp),
    labelMedium = TextStyle(fontFamily = HoraCertaFont, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 18.sp),
    labelSmall = TextStyle(fontFamily = HoraCertaFont, fontSize = 12.sp, lineHeight = 16.sp),
)

@Composable
fun HoraCertaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = HoraCertaColors,
        typography = HoraCertaTypography,
        content = content,
    )
}
