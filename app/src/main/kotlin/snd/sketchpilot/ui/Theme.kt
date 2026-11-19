package snd.sketchpilot.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFB0A4FF),
    onPrimary = Color(0xFF201A4A),
    primaryContainer = Color(0xFF342C68),
    onPrimaryContainer = Color(0xFFDAD3FF),
    secondary = Color(0xFFC4C0D4),
    onSecondary = Color(0xFF2C2938),
    surface = Color(0xFF141318),
    onSurface = Color(0xFFE6E1E9),
    surfaceVariant = Color(0xFF26242E),
    onSurfaceVariant = Color(0xFFC6C1CF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    outline = Color(0xFF8E8997),
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF5048A0),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE4DEFF),
    onPrimaryContainer = Color(0xFF12005E),
    secondary = Color(0xFF605B73),
    onSecondary = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1C1B20),
    surfaceVariant = Color(0xFFE7E1EC),
    onSurfaceVariant = Color(0xFF49454F),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    outline = Color(0xFF79747E),
)

private val AppTypography = Typography(
    headlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Medium),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun SketchPilotTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dark -> dynamicDarkColorScheme(context)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
        dark -> DarkScheme
        else -> LightScheme
    }
    MaterialTheme(colorScheme = scheme, typography = AppTypography, shapes = AppShapes, content = content)
}
