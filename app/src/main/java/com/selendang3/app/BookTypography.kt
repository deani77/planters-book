package com.selendang3.app

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight

// Bundled variable font works offline on every supported Android version (API 26+).
// Register each weight so semibold/bold use real outlines rather than synthetic bold.
@OptIn(ExperimentalTextApi::class)
private val Rubik = FontFamily(
    listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold).map { weight ->
        Font(R.font.rubik, weight = weight,
            variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))
    }
)

// Keep Material's scalable sizes, line heights and spacing while changing every role.
private val DefaultTypography = Typography()
val BookTypography = with(DefaultTypography) {
    Typography(
        displayLarge = displayLarge.copy(fontFamily = Rubik),
        displayMedium = displayMedium.copy(fontFamily = Rubik),
        displaySmall = displaySmall.copy(fontFamily = Rubik),
        headlineLarge = headlineLarge.copy(fontFamily = Rubik, fontWeight = FontWeight.SemiBold),
        headlineMedium = headlineMedium.copy(fontFamily = Rubik, fontWeight = FontWeight.SemiBold),
        headlineSmall = headlineSmall.copy(fontFamily = Rubik, fontWeight = FontWeight.SemiBold),
        titleLarge = titleLarge.copy(fontFamily = Rubik, fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontFamily = Rubik, fontWeight = FontWeight.SemiBold),
        titleSmall = titleSmall.copy(fontFamily = Rubik, fontWeight = FontWeight.SemiBold),
        bodyLarge = bodyLarge.copy(fontFamily = Rubik),
        bodyMedium = bodyMedium.copy(fontFamily = Rubik),
        bodySmall = bodySmall.copy(fontFamily = Rubik),
        labelLarge = labelLarge.copy(fontFamily = Rubik),
        labelMedium = labelMedium.copy(fontFamily = Rubik),
        labelSmall = labelSmall.copy(fontFamily = Rubik),
    )
}
