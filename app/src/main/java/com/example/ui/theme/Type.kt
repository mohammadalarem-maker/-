package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.example.R

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val CairoFont = GoogleFont("Cairo")
val CairoFontFamily = FontFamily(
    Font(googleFont = CairoFont, fontProvider = provider),
    Font(googleFont = CairoFont, fontProvider = provider, weight = FontWeight.Bold),
    Font(googleFont = CairoFont, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = CairoFont, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = CairoFont, fontProvider = provider, weight = FontWeight.Light)
)

// Set of Material typography styles to start with
private val defaultTypography = Typography()
val Typography = Typography(
    displayLarge = defaultTypography.displayLarge.copy(fontFamily = CairoFontFamily),
    displayMedium = defaultTypography.displayMedium.copy(fontFamily = CairoFontFamily),
    displaySmall = defaultTypography.displaySmall.copy(fontFamily = CairoFontFamily),

    headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = CairoFontFamily),
    headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = CairoFontFamily),
    headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = CairoFontFamily),

    titleLarge = defaultTypography.titleLarge.copy(fontFamily = CairoFontFamily),
    titleMedium = defaultTypography.titleMedium.copy(fontFamily = CairoFontFamily),
    titleSmall = defaultTypography.titleSmall.copy(fontFamily = CairoFontFamily),

    bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = CairoFontFamily),
    bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = CairoFontFamily),
    bodySmall = defaultTypography.bodySmall.copy(fontFamily = CairoFontFamily),

    labelLarge = defaultTypography.labelLarge.copy(fontFamily = CairoFontFamily),
    labelMedium = defaultTypography.labelMedium.copy(fontFamily = CairoFontFamily),
    labelSmall = defaultTypography.labelSmall.copy(fontFamily = CairoFontFamily)
)
