package id.sadarbudget.mobile.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Central visual scale for SadarBudget.
 * All spacing, corner radius and control heights should originate here.
 */
enum class SbWindowClass { Mini, Medium, Large, Expanded }

@Immutable
data class SbSpacing(
    val xxs: Dp,
    val xs: Dp,
    val sm: Dp,
    val md: Dp,
    val lg: Dp,
    val xl: Dp,
    val pageHorizontal: Dp,
    val pageVertical: Dp,
    val section: Dp,
    val cardPadding: Dp,
)

@Immutable
data class SbRadius(
    val control: Dp,
    val card: Dp,
    val hero: Dp,
    val pill: Dp,
)

@Immutable
data class SbDesignSystem(
    val windowClass: SbWindowClass,
    val spacing: SbSpacing,
    val radius: SbRadius,
    val controlHeight: Dp,
    val navHeight: Dp,
    val railWidth: Dp,
)

@Composable
fun sbDesignSystem(): SbDesignSystem {
    val configuration = LocalConfiguration.current
    val width = configuration.screenWidthDp
    val windowClass = when {
        width >= 840 -> SbWindowClass.Expanded
        width >= 600 -> SbWindowClass.Large
        width >= 380 -> SbWindowClass.Medium
        else -> SbWindowClass.Mini
    }

    return when (windowClass) {
        SbWindowClass.Mini -> SbDesignSystem(
            windowClass = windowClass,
            spacing = SbSpacing(
                xxs = 3.dp, xs = 6.dp, sm = 9.dp, md = 12.dp, lg = 16.dp, xl = 22.dp,
                pageHorizontal = 12.dp, pageVertical = 10.dp, section = 10.dp, cardPadding = 11.dp,
            ),
            radius = SbRadius(control = 10.dp, card = 14.dp, hero = 16.dp, pill = 999.dp),
            controlHeight = 42.dp,
            navHeight = 56.dp,
            railWidth = 64.dp,
        )

        SbWindowClass.Medium -> SbDesignSystem(
            windowClass = windowClass,
            spacing = SbSpacing(
                xxs = 4.dp, xs = 7.dp, sm = 10.dp, md = 14.dp, lg = 18.dp, xl = 26.dp,
                pageHorizontal = 16.dp, pageVertical = 14.dp, section = 12.dp, cardPadding = 14.dp,
            ),
            radius = SbRadius(control = 12.dp, card = 16.dp, hero = 18.dp, pill = 999.dp),
            controlHeight = 46.dp,
            navHeight = 60.dp,
            railWidth = 68.dp,
        )

        SbWindowClass.Large -> SbDesignSystem(
            windowClass = windowClass,
            spacing = SbSpacing(
                xxs = 4.dp, xs = 8.dp, sm = 12.dp, md = 16.dp, lg = 20.dp, xl = 30.dp,
                pageHorizontal = 20.dp, pageVertical = 16.dp, section = 14.dp, cardPadding = 16.dp,
            ),
            radius = SbRadius(control = 12.dp, card = 16.dp, hero = 18.dp, pill = 999.dp),
            controlHeight = 50.dp,
            navHeight = 62.dp,
            railWidth = 72.dp,
        )

        SbWindowClass.Expanded -> SbDesignSystem(
            windowClass = windowClass,
            spacing = SbSpacing(
                xxs = 5.dp, xs = 9.dp, sm = 13.dp, md = 18.dp, lg = 24.dp, xl = 34.dp,
                pageHorizontal = 24.dp, pageVertical = 18.dp, section = 16.dp, cardPadding = 18.dp,
            ),
            radius = SbRadius(control = 12.dp, card = 18.dp, hero = 20.dp, pill = 999.dp),
            controlHeight = 52.dp,
            navHeight = 64.dp,
            railWidth = 76.dp,
        )
    }
}
