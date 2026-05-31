package com.websnake.recentstv.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import com.websnake.recentstv.R

/**
 * Design tokens lifted verbatim from the prototype's tv/styles.css.
 * The prototype is authored at 1920x1080 CSS px; this device is 960x540 dp
 * (320dpi / density 2.0), so every design px is mapped to dp at a 0.5 factor
 * to reproduce the prototype proportions pixel-for-pixel.
 */
object Tokens {
    // base
    val bg = Color(0xFF0A0B0D)
    val running = Color(0xFF101317)
    val surface = Color(0xFF15171B)
    val surface2 = Color(0xFF1D2026)
    val card = Color(0xFF23262D)
    val line = Color(0x14FFFFFF)        // rgba(255,255,255,0.08)

    // text
    val text = Color(0xFFF3F5F7)
    val textDim = Color(0xFF9AA0A8)
    val textFaint = Color(0xFF5E646C)

    // accent — Android green
    val accent = Color(0xFF3DDC84)
    val accentStrong = Color(0xFF2FC973)
    val accentTint = Color(0x243DDC84)   // ~0.14 alpha
    val accentGlow = Color(0x8C3DDC84)   // ~0.55 alpha
    val accentOnDark = Color(0xFF08210F) // dark text on solid-accent fills
    val killGlyphOnFocus = Color(0xFF0A2417)

    // scrim — the system dim provides the real 0.66 scrim; this is a light top-up
    val scrim = Color(0xA6000000)

    // type scale (design px → sp at 0.5)
    val tHero = 31.sp
    val tTitle = 23.sp
    val tRow = 20.sp
    val tLabel = 17.sp
    val tBody = 15.sp
    val tSmall = 13.sp
    val tMono = 11.sp
}

// Single variable Manrope TTF; the renderer applies the requested weight axis.
private val manrope = FontFamily(
    Font(R.font.manrope, FontWeight.Medium),
    Font(R.font.manrope, FontWeight.Bold),
    Font(R.font.manrope, FontWeight.ExtraBold),
)

object Type {
    val hero = TextStyle(fontFamily = manrope, fontWeight = FontWeight.ExtraBold, fontSize = Tokens.tHero)
    val title = TextStyle(fontFamily = manrope, fontWeight = FontWeight.Bold, fontSize = Tokens.tTitle)
    val row = TextStyle(fontFamily = manrope, fontWeight = FontWeight.Bold, fontSize = Tokens.tRow)
    val label = TextStyle(fontFamily = manrope, fontWeight = FontWeight.Bold, fontSize = Tokens.tLabel)
    val body = TextStyle(fontFamily = manrope, fontWeight = FontWeight.Medium, fontSize = Tokens.tBody)
    val small = TextStyle(fontFamily = manrope, fontWeight = FontWeight.Medium, fontSize = Tokens.tSmall)
    val mono = TextStyle(fontFamily = manrope, fontWeight = FontWeight.Bold, fontSize = Tokens.tMono)
}

/** Card sizing driven by Settings · Size (design px → dp at 0.5). */
enum class CardSizeOpt(val dp: androidx.compose.ui.unit.Dp) {
    SMALL(71.dp), MEDIUM(90.dp), LARGE(115.dp)
}

/** Card corner style driven by Settings · Shape. */
enum class CardShapeOpt {
    SQUARE, ROUNDED, CIRCLE;

    fun shape(sizeDp: androidx.compose.ui.unit.Dp) = when (this) {
        SQUARE -> RoundedCornerShape(8.dp)
        ROUNDED -> RoundedCornerShape(16.dp)
        CIRCLE -> RoundedCornerShape(percent = 50)
    }
}

/** A full accent palette derived from one base colour, so the accent is themeable. */
data class AccentPalette(val base: Color, val tint: Color, val glow: Color, val onDark: Color)

fun accentPaletteFor(base: Color) = AccentPalette(
    base = base,
    tint = base.copy(alpha = 0.16f),
    glow = base.copy(alpha = 0.55f),
    onDark = Color(0xFF07140A),     // dark text/glyph on a solid-accent fill
)

val LocalAccent = androidx.compose.runtime.staticCompositionLocalOf { accentPaletteFor(Tokens.accent) }

/** User-selectable accent colours (Settings · Accent). */
enum class AccentColor(val color: Color) {
    GREEN(Color(0xFF3DDC84)), BLUE(Color(0xFF40A9FF)), PURPLE(Color(0xFF9B6CFF)),
    AMBER(Color(0xFFF2A93B)), PINK(Color(0xFFEC4899))
}

/** Overlay backdrop tint (Settings · Background). */
enum class BgTint(val color: Color) {
    BLACK(Color(0xFF000000)), CHARCOAL(Color(0xFF0A0B0D)),
    NAVY(Color(0xFF0A1428)), GREY(Color(0xFF181A1E))
}

@Composable
fun RecentsTheme(accent: Color = Tokens.accent, content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        primary = accent,
        background = Tokens.bg,
        surface = Tokens.surface,
        onSurface = Tokens.text,
    )
    CompositionLocalProvider(
        LocalContentColor provides Tokens.text,
        LocalAccent provides accentPaletteFor(accent),
    ) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
