package id.sadarbudget.mobile.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// Light palette — white fused with modern blue
val SbBackgroundLight = Color(0xFFF7F9FC)
val SbSurfaceLight = Color(0xFFFFFFFF)
val SbSurfaceAltLight = Color(0xFFF2F6FB)
val SbTextLight = Color(0xFF0F172A)
val SbMutedLight = Color(0xFF64748B)
val SbLineLight = Color(0xFFD9E2F0)
val SbPrimary = Color(0xFF2563EB)
val SbPrimaryStrong = Color(0xFF1D4ED8)
val SbPrimarySoft = Color(0xFFDBEAFE)
val SbSecondary = Color(0xFF38BDF8)
val SbSecondarySoft = Color(0xFFE0F2FE)
val SbSuccess = Color(0xFF0284C7)
val SbSuccessSoft = Color(0xFFE0F2FE)
val SbDanger = Color(0xFFDC2626)
val SbDangerSoft = Color(0xFFFFF0F0)
val SbWarning = Color(0xFFB45309)

// Dark palette — near-black fused with modern blue
val SbBackgroundDark = Color(0xFF05070B)
val SbSurfaceDark = Color(0xFF090D14)
val SbSurfaceRaisedDark = Color(0xFF0F1722)
val SbTextDark = Color(0xFFF3F7FF)
val SbMutedDark = Color(0xFF94A3B8)
val SbLineDark = Color(0xFF22304A)
val SbPrimaryDark = Color(0xFF60A5FA)
val SbPrimaryContainerDark = Color(0xFF102A56)
val SbSecondaryDark = Color(0xFF7DD3FC)
val SbSuccessDark = Color(0xFF38BDF8)
val SbDangerDark = Color(0xFFFF8989)

private val Light = lightColorScheme(
    primary = SbPrimary,
    onPrimary = Color.White,
    primaryContainer = SbPrimarySoft,
    onPrimaryContainer = SbPrimaryStrong,
    secondary = SbSecondary,
    onSecondary = Color(0xFF082032),
    secondaryContainer = SbSecondarySoft,
    onSecondaryContainer = Color(0xFF0C4A6E),
    tertiary = Color(0xFF0EA5E9),
    background = SbBackgroundLight,
    onBackground = SbTextLight,
    surface = SbSurfaceLight,
    onSurface = SbTextLight,
    surfaceVariant = SbSurfaceAltLight,
    onSurfaceVariant = SbMutedLight,
    surfaceContainer = Color(0xFFF4F7FB),
    surfaceContainerHigh = Color(0xFFEAF1F8),
    outline = SbLineLight,
    outlineVariant = Color(0xFFE5ECF5),
    error = SbDanger,
    onError = Color.White,
    errorContainer = SbDangerSoft,
    onErrorContainer = Color(0xFF671313),
)

private val Dark = darkColorScheme(
    primary = SbPrimaryDark,
    onPrimary = Color(0xFF071426),
    primaryContainer = SbPrimaryContainerDark,
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = SbSecondaryDark,
    onSecondary = Color(0xFF071426),
    secondaryContainer = Color(0xFF0D2438),
    onSecondaryContainer = Color(0xFFE0F2FE),
    tertiary = Color(0xFF38BDF8),
    background = SbBackgroundDark,
    onBackground = SbTextDark,
    surface = SbSurfaceDark,
    onSurface = SbTextDark,
    surfaceVariant = SbSurfaceRaisedDark,
    onSurfaceVariant = SbMutedDark,
    surfaceContainer = Color(0xFF0C121D),
    surfaceContainerHigh = Color(0xFF111A27),
    outline = SbLineDark,
    outlineVariant = Color(0xFF182437),
    error = SbDangerDark,
    onError = Color(0xFF210606),
    errorContainer = Color(0xFF331417),
    onErrorContainer = Color(0xFFFFDAD9),
)

private val DefaultTypography = Typography()
private val BaseTypography = Typography(
    displayLarge = DefaultTypography.displayLarge.copy(fontSize = 52.sp, lineHeight = 58.sp),
    displayMedium = DefaultTypography.displayMedium.copy(fontSize = 42.sp, lineHeight = 48.sp),
    displaySmall = DefaultTypography.displaySmall.copy(fontSize = 34.sp, lineHeight = 40.sp),
    headlineLarge = DefaultTypography.headlineLarge.copy(fontSize = 30.sp, lineHeight = 36.sp),
    headlineMedium = DefaultTypography.headlineMedium.copy(fontSize = 24.sp, lineHeight = 30.sp),
    headlineSmall = DefaultTypography.headlineSmall.copy(fontSize = 20.sp, lineHeight = 26.sp),
    titleLarge = DefaultTypography.titleLarge.copy(fontSize = 18.sp, lineHeight = 24.sp),
    titleMedium = DefaultTypography.titleMedium.copy(fontSize = 15.sp, lineHeight = 21.sp),
    titleSmall = DefaultTypography.titleSmall.copy(fontSize = 13.sp, lineHeight = 18.sp),
    bodyLarge = DefaultTypography.bodyLarge.copy(fontSize = 14.sp, lineHeight = 21.sp),
    bodyMedium = DefaultTypography.bodyMedium.copy(fontSize = 12.sp, lineHeight = 18.sp),
    bodySmall = DefaultTypography.bodySmall.copy(fontSize = 10.sp, lineHeight = 15.sp),
    labelLarge = DefaultTypography.labelLarge.copy(fontSize = 12.sp, lineHeight = 16.sp),
    labelMedium = DefaultTypography.labelMedium.copy(fontSize = 10.sp, lineHeight = 14.sp),
    labelSmall = DefaultTypography.labelSmall.copy(fontSize = 9.sp, lineHeight = 12.sp),
)
private val SmallPhoneTypography = Typography(
    displayLarge = BaseTypography.displayLarge.copy(fontSize = 42.sp, lineHeight = 48.sp),
    displayMedium = BaseTypography.displayMedium.copy(fontSize = 34.sp, lineHeight = 40.sp),
    displaySmall = BaseTypography.displaySmall.copy(fontSize = 28.sp, lineHeight = 34.sp),
    headlineLarge = BaseTypography.headlineLarge.copy(fontSize = 24.sp, lineHeight = 30.sp),
    headlineMedium = BaseTypography.headlineMedium.copy(fontSize = 20.sp, lineHeight = 26.sp),
    headlineSmall = BaseTypography.headlineSmall.copy(fontSize = 18.sp, lineHeight = 23.sp),
    titleLarge = BaseTypography.titleLarge.copy(fontSize = 17.sp, lineHeight = 22.sp),
    titleMedium = BaseTypography.titleMedium.copy(fontSize = 14.sp, lineHeight = 19.sp),
    titleSmall = BaseTypography.titleSmall.copy(fontSize = 12.sp, lineHeight = 17.sp),
    bodyLarge = BaseTypography.bodyLarge.copy(fontSize = 13.sp, lineHeight = 19.sp),
    bodyMedium = BaseTypography.bodyMedium.copy(fontSize = 11.sp, lineHeight = 17.sp),
    bodySmall = BaseTypography.bodySmall.copy(fontSize = 10.sp, lineHeight = 14.sp),
    labelLarge = BaseTypography.labelLarge.copy(fontSize = 11.sp, lineHeight = 15.sp),
    labelMedium = BaseTypography.labelMedium.copy(fontSize = 10.sp, lineHeight = 14.sp),
    labelSmall = BaseTypography.labelSmall.copy(fontSize = 9.sp, lineHeight = 12.sp),
)

@Composable
fun SadarBudgetTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    val view = LocalView.current
    val smallPhone = sbDesignSystem().windowClass == SbWindowClass.Mini
    val colorScheme = if (darkTheme) Dark else Light

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = colorScheme.surfaceContainer.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = if (smallPhone) SmallPhoneTypography else BaseTypography,
        content = content,
    )
}
