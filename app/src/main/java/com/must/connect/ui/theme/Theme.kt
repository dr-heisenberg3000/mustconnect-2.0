package com.must.connect.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// MUST-CONNECT always uses a fixed light scheme — no dynamic colour,
// no dark-mode override.  The splash handles its own dark gradient.
private val MustLightColorScheme = lightColorScheme(
    primary            = PrimaryLight,
    onPrimary          = OnPrimaryLight,
    primaryContainer   = PrimaryContainer,
    onPrimaryContainer = BrandNavy,
    secondary          = SecondaryLight,
    onSecondary        = OnPrimaryLight,
    tertiary           = TertiaryLight,
    background         = BackgroundLight,
    onBackground       = TextPrimary,
    surface            = SurfaceCard,
    onSurface          = TextPrimary,
    surfaceVariant     = SubtleGrey,
    onSurfaceVariant   = TextSecondary,
    outline            = DividerColor,
)

@Composable
fun MUSTCONNECTTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MustLightColorScheme,
        typography  = Typography,
        content     = content,
    )
}