package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val SleekDarkColorScheme = darkColorScheme(
    primary = PrimaryContainerSleek,
    onPrimary = OnPrimaryContainerSleek,
    primaryContainer = PrimarySleek,
    onPrimaryContainer = OnPrimarySleek,
    secondary = SecondaryContainerSleek,
    onSecondary = OnSecondaryContainerSleek,
    background = OnBackgroundSleek,
    onBackground = BackgroundSleek,
    surface = OnBackgroundSleek,
    onSurface = BackgroundSleek,
    surfaceVariant = OnSurfaceVariantSleek,
    onSurfaceVariant = BackgroundSleek,
    outline = OutlineVariantSleek,
    outlineVariant = OutlineSleek
)

private val SleekLightColorScheme = lightColorScheme(
    primary = PrimarySleek,
    onPrimary = OnPrimarySleek,
    primaryContainer = PrimaryContainerSleek,
    onPrimaryContainer = OnPrimaryContainerSleek,
    secondary = SecondarySleek,
    onSecondary = OnSecondarySleek,
    secondaryContainer = SecondaryContainerSleek,
    onSecondaryContainer = OnSecondaryContainerSleek,
    background = BackgroundSleek,
    onBackground = OnBackgroundSleek,
    surface = SurfaceSleek,
    onSurface = OnSurfaceSleek,
    surfaceVariant = SurfaceVariantSleek,
    onSurfaceVariant = OnSurfaceVariantSleek,
    outline = OutlineSleek,
    outlineVariant = OutlineVariantSleek
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set to false to preserve the customized Sleek Interface branding
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) {
        SleekDarkColorScheme
    } else {
        SleekLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
