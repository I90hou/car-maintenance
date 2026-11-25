package com.carmaintenance.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontLoadingStrategy
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.carmaintenance.app.R

/**
 * 现代车座舱（B 版）色板
 *
 * 全暗底 + 近乎无彩 + 一条电光蓝强调色。
 * 所有颜色都从这里取，Composable 里不再硬编码。
 */
object Cockpit {
    /** 页面底色 */
    val Background = Color(0xFF0A0C0F)

    /** 一级容器（卡片） */
    val Surface1 = Color(0xFF12151A)

    /** 二级容器（卡内分块） */
    val Surface2 = Color(0xFF181C22)

    /** 三级容器（进度槽） */
    val Surface3 = Color(0xFF20252C)

    /** 发丝分隔线 */
    val Line = Color(0xFF242A32)

    /** 描边（输入框、标签栏） */
    val Line2 = Color(0xFF2F363F)

    /** 主文字 */
    val Text = Color(0xFFEAEEF2)

    /** 次级文字 */
    val Text2 = Color(0xFF8A919B)

    /** 弱化文字 */
    val Text3 = Color(0xFF5A616B)

    /** 强调色（电光蓝） */
    val Accent = Color(0xFF4C8DFF)

    /** 强调色上的文字 */
    val OnAccent = Color(0xFF06101F)

    /** 强调色弱底 */
    val AccentDim = Color(0x264C8DFF)

    /** 正常 */
    val Ok = Color(0xFF3DD68C)

    /** 临近到期 */
    val Warn = Color(0xFFF5A524)

    /** 超期 / 危险 */
    val Danger = Color(0xFFFF6B6B)
}

/**
 * Manrope 拉丁字体（可变字重）
 *
 * 只覆盖拉丁字母与数字，中文交给系统字体（思源黑体 / 苹方 / 微软雅黑）。
 */
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private val Manrope = FontFamily(
    Font(
        resId = R.font.manrope_variable,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400))
    ),
    Font(
        resId = R.font.manrope_variable,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500))
    ),
    Font(
        resId = R.font.manrope_variable,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600))
    ),
    Font(
        resId = R.font.manrope_variable,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700))
    ),
    Font(
        resId = R.font.manrope_variable,
        weight = FontWeight.ExtraBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(800))
    )
)

/**
 * 字体层级
 *
 * 除了 Material3 的标准槽位，额外提供几组本设计特有的角色：
 * 小标签（带大字距）、字段标签、里程读数、提示文字。
 */
object AppText {

    /** 车牌 / 英文小标签 */
    val Plate = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 2.4.sp
    )

    /** 分组小标题 */
    val GroupLabel = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.Bold,
        fontSize = 10.5.sp,
        lineHeight = 14.sp,
        letterSpacing = 2.3.sp
    )

    /** 输入项 / 条目上的小标签 */
    val FieldLabel = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.Bold,
        fontSize = 9.5.sp,
        lineHeight = 13.sp,
        letterSpacing = 1.9.sp
    )

    /** 超大里程读数 */
    val Odometer = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 54.sp,
        lineHeight = 56.sp,
        letterSpacing = (-1.6).sp
    )

    /** 次级里程读数（详情页） */
    val OdometerSmall = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 46.sp,
        lineHeight = 48.sp,
        letterSpacing = (-1.2).sp
    )

    /** 说明文字 */
    val Hint = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.Medium,
        fontSize = 11.5.sp,
        lineHeight = 18.sp
    )

    /** 条目副行 */
    val RowMeta = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 17.sp
    )
}

// ===== 暗色方案（唯一色调：现代车座舱）=====
private val CockpitScheme = darkColorScheme(
    primary = Cockpit.Accent,
    onPrimary = Cockpit.OnAccent,
    primaryContainer = Color(0xFF16233A),
    onPrimaryContainer = Color(0xFFAFC9FF),

    secondary = Cockpit.Text2,
    onSecondary = Cockpit.Background,
    secondaryContainer = Cockpit.Surface2,
    onSecondaryContainer = Cockpit.Text,

    tertiary = Cockpit.Ok,
    onTertiary = Color(0xFF04160E),
    tertiaryContainer = Color(0xFF12301F),
    onTertiaryContainer = Color(0xFFA9E9C8),

    background = Cockpit.Background,
    onBackground = Cockpit.Text,

    surface = Cockpit.Surface1,
    onSurface = Cockpit.Text,
    surfaceVariant = Cockpit.Surface2,
    onSurfaceVariant = Cockpit.Text2,
    surfaceContainerLowest = Cockpit.Background,
    surfaceContainerLow = Cockpit.Surface1,
    surfaceContainer = Cockpit.Surface2,
    surfaceContainerHigh = Cockpit.Surface3,
    surfaceContainerHighest = Cockpit.Surface3,

    error = Cockpit.Danger,
    onError = Color(0xFF2A0606),
    errorContainer = Color(0xFF3A1414),
    onErrorContainer = Color(0xFFFFC9C9),

    outline = Cockpit.Line2,
    outlineVariant = Cockpit.Line,
    scrim = Color(0xFF040609)
)

// ===== 字体层级 =====
private val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 54.sp,
        lineHeight = 56.sp,
        letterSpacing = (-1.6).sp
    ),
    displayMedium = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 46.sp,
        lineHeight = 48.sp,
        letterSpacing = (-1.2).sp
    ),
    displaySmall = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 36.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.8).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 29.sp,
        lineHeight = 35.sp,
        letterSpacing = (-0.3).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.2).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleLarge = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 22.sp,
        lineHeight = 27.sp,
        letterSpacing = (-0.3).sp
    ),
    titleMedium = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 21.sp
    ),
    titleSmall = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 17.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 23.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp
    ),
    bodySmall = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.Medium,
        fontSize = 11.5.sp,
        lineHeight = 17.sp
    ),
    labelLarge = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 13.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.4.sp
    ),
    labelMedium = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.Bold,
        fontSize = 10.5.sp,
        lineHeight = 14.sp,
        letterSpacing = 2.3.sp
    ),
    labelSmall = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.Bold,
        fontSize = 9.5.sp,
        lineHeight = 13.sp,
        letterSpacing = 1.9.sp
    )
)

// ===== 圆角规范：大圆角、发丝线、无阴影 =====
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/**
 * 应用主题
 *
 * B 版「现代车座舱」是单一暗色定稿，不再跟随系统深浅色切换。
 */
@Composable
fun CarMaintenanceTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = CockpitScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // API 35 起系统强制边到边，状态栏/导航栏颜色不再可写，这里只保留旧版本行为
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                @Suppress("DEPRECATION")
                window.statusBarColor = Cockpit.Background.toArgb()
                @Suppress("DEPRECATION")
                window.navigationBarColor = Cockpit.Background.toArgb()
            }
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
