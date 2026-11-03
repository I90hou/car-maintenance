package com.carmaintenance.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/**
 * 品牌附加语义色（Material3 标准角色之外的补充）
 */
object AppColors {
    val BrandDeep = Color(0xFF06636D)
    val BrandSoft = Color(0xFFDDF2F1)
    val TextSecondary = Color(0xFF718083)
    val TextTertiary = Color(0xFF9AA7A9)
    val Border = Color(0xFFE1E8E9)
    val Success = Color(0xFF319B75)
    val Warning = Color(0xFFD99A38)
    val Danger = Color(0xFFD9534F)
}

// ===== 浅色方案（品牌青绿 #087F8C）=====
private val LightScheme = lightColorScheme(
    primary = Color(0xFF087F8C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDF2F1),
    onPrimaryContainer = Color(0xFF04424A),

    secondary = Color(0xFF3C7F86),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3F1F2),
    onSecondaryContainer = Color(0xFF14545C),

    tertiary = Color(0xFF2F8F86),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE0F2EE),
    onTertiaryContainer = Color(0xFF0F4A44),

    background = Color(0xFFF5F8FA),
    onBackground = Color(0xFF263438),

    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF263438),
    surfaceVariant = Color(0xFFF0F4F5),
    onSurfaceVariant = Color(0xFF718083),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAFCFD),
    surfaceContainer = Color(0xFFF0F4F5),
    surfaceContainerHigh = Color(0xFFFFFFFF),
    surfaceContainerHighest = Color(0xFFEDF2F4),

    error = Color(0xFFD9534F),
    onError = Color.White,
    errorContainer = Color(0xFFFBEAEA),
    onErrorContainer = Color(0xFF6B2422),

    outline = Color(0xFFC9D5D7),
    outlineVariant = Color(0xFFE1E8E9),
    scrim = Color(0xFF000000)
)

// ===== 深色方案（同色系压低明度）=====
private val DarkScheme = darkColorScheme(
    primary = Color(0xFF6FD3DB),
    onPrimary = Color(0xFF00363B),
    primaryContainer = Color(0xFF0B4C53),
    onPrimaryContainer = Color(0xFFD3F1F2),

    secondary = Color(0xFF9FCED3),
    onSecondary = Color(0xFF0A3539),
    secondaryContainer = Color(0xFF23494E),
    onSecondaryContainer = Color(0xFFE0F1F2),

    tertiary = Color(0xFF8FD6CB),
    onTertiary = Color(0xFF07352F),
    tertiaryContainer = Color(0xFF1E4A44),
    onTertiaryContainer = Color(0xFFDDF2EE),

    background = Color(0xFF101719),
    onBackground = Color(0xFFDCE5E7),

    surface = Color(0xFF161F22),
    onSurface = Color(0xFFDCE5E7),
    surfaceVariant = Color(0xFF232E31),
    onSurfaceVariant = Color(0xFFAEBCC0),
    surfaceContainerLowest = Color(0xFF0C1214),
    surfaceContainerLow = Color(0xFF131B1D),
    surfaceContainer = Color(0xFF1A2427),
    surfaceContainerHigh = Color(0xFF1F2B2E),
    surfaceContainerHighest = Color(0xFF263336),

    error = Color(0xFFE9A6A3),
    onError = Color(0xFF4C1210),
    errorContainer = Color(0xFF5C1F1D),
    onErrorContainer = Color(0xFFFAD9D8),

    outline = Color(0xFF7C8C90),
    outlineVariant = Color(0xFF334044)
)

// ===== 字体层级 =====
private val AppTypography = Typography(
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 29.sp,
        letterSpacing = (-0.2).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.1.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.2.sp
    )
)

// ===== 圆角规范 =====
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun CarMaintenanceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkScheme else LightScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
