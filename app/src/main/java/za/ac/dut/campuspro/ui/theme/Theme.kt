package za.ac.dut.campuspro.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

private val CampusColors = lightColorScheme(
    primary = Orange,
    onPrimary = Ink,
    primaryContainer = OrangeTint,
    onPrimaryContainer = OrangeDark,
    secondary = Ink,
    onSecondary = Color.White,
    background = Cream,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFF6F1EC),
    onSurfaceVariant = Grey,
    outline = Color(0xFFCCCCCC),
    outlineVariant = LineGrey,
    error = Red,
    onError = Color.White
)

private val base = Typography()
private val CampusTypography = base.copy(
    headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Black),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Bold)
)

/** Campus Pro brand theme: DUT-inspired orange and black on cream. */
@Composable
fun CampusProTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = CampusColors, typography = CampusTypography, content = content)
}
