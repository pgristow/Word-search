package com.wordsearch.ui.theme

import androidx.compose.ui.graphics.Color

// ===================== Calm Modern palette =====================
// Text / ink
val Ink = Color(0xFF2D2A32)         // primary text + letters
val InkSoft = Color(0xFF6B6770)     // secondary text

// Surfaces
val AppBackground = Color(0xFFF4F1EA) // warm off-white backdrop
val AppSurface = Color(0xFFFFFFFF)    // cards
val BoardTrough = Color(0xFFE9E4D8)   // board behind the letter tiles
val GridTile = Color(0xFFFFFFFF)      // a single letter tile

// Brand / accents
val BrandIndigo = Color(0xFF4C63B6)
val BrandIndigoContainer = Color(0xFFDFE3F7)
val SelectBlue = Color(0xFF5B8DEF)    // active selection sweep
val FoundTeal = Color(0xFF3FB59A)     // found target word
val TealContainer = Color(0xFFCDEFE7)
val BonusAmber = Color(0xFFF2A03D)    // bonus word
val BonusAmberContainer = Color(0xFFFBE7CC)
val HintViolet = Color(0xFF8B7FD6)    // hint reveal
val DangerRed = Color(0xFFD7625B)

// A harmonised set used to colour multiple found words (calm, not rainbow).
val FoundWordPalette = listOf(
    Color(0xFF3FB59A), // teal
    Color(0xFF5B8DEF), // soft blue
    Color(0xFF7FB069), // sage
    Color(0xFFE0A458), // soft amber
    Color(0xFF9B8BD0), // muted violet
    Color(0xFF5FAAD1), // sky
    Color(0xFFC98BB9), // dusty rose
    Color(0xFF7AC2A8), // mint
)

// ===================== Legacy names =====================
// Kept (remapped to the new palette) so existing screens keep compiling.
val Purple80 = Color(0xFFBFC8F0)
val PurpleGrey80 = Color(0xFFCAC9D6)
val Pink80 = Color(0xFFF3D7BC)
val Purple40 = BrandIndigo
val PurpleGrey40 = Color(0xFF5C6275)
val Pink40 = BonusAmber

val WordFound = FoundTeal
val WordSelected = SelectBlue
val GridBackground = BoardTrough
val LetterBackground = GridTile
val BossLevelRed = DangerRed
val Background = AppBackground
val Surface = AppSurface
val Success = FoundTeal
val Error = DangerRed
