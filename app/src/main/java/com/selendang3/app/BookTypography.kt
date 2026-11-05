package com.selendang3.app

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Bundled variable font works offline on every supported Android version (API 26+).
// Register each weight so semibold/bold use real outlines rather than synthetic bold.
@OptIn(ExperimentalTextApi::class)
private val Rubik = FontFamily(
    listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold).map { weight ->
        Font(R.font.rubik, weight = weight,
            variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))
    }
)

// Compact Rubik scale for data-heavy phone screens; sp preserves Android font scaling.
private val DefaultTypography = Typography()
val BookTypography = with(DefaultTypography) {
    Typography(
        displayLarge = displayLarge.copy(fontSize = 32.sp, lineHeight = 40.sp, fontFamily = Rubik),
        displayMedium = displayMedium.copy(fontSize = 28.sp, lineHeight = 36.sp, fontFamily = Rubik),
        displaySmall = displaySmall.copy(fontSize = 24.sp, lineHeight = 32.sp, fontFamily = Rubik),
        headlineLarge = headlineLarge.copy(fontSize = 32.sp, lineHeight = 40.sp, fontFamily = Rubik, fontWeight = FontWeight.SemiBold),
        headlineMedium = headlineMedium.copy(fontSize = 22.sp, lineHeight = 28.sp, fontFamily = Rubik, fontWeight = FontWeight.SemiBold),
        headlineSmall = headlineSmall.copy(fontSize = 20.sp, lineHeight = 26.sp, fontFamily = Rubik, fontWeight = FontWeight.SemiBold),
        titleLarge = titleLarge.copy(fontSize = 18.sp, lineHeight = 24.sp, fontFamily = Rubik, fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontSize = 15.sp, lineHeight = 22.sp, fontFamily = Rubik, fontWeight = FontWeight.SemiBold),
        titleSmall = titleSmall.copy(fontSize = 14.sp, lineHeight = 20.sp, fontFamily = Rubik, fontWeight = FontWeight.SemiBold),
        bodyLarge = bodyLarge.copy(fontSize = 14.sp, lineHeight = 20.sp, fontFamily = Rubik),
        bodyMedium = bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp, fontFamily = Rubik),
        bodySmall = bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp, fontFamily = Rubik),
        labelLarge = labelLarge.copy(fontSize = 13.sp, lineHeight = 18.sp, fontFamily = Rubik),
        labelMedium = labelMedium.copy(fontSize = 12.sp, lineHeight = 16.sp, fontFamily = Rubik),
        labelSmall = labelSmall.copy(fontSize = 11.sp, lineHeight = 16.sp, fontFamily = Rubik),
    )
}
