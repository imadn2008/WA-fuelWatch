package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.ui.ThemeColor

private val DarkColorScheme =
  darkColorScheme(
    primary = BluePrimaryDark,
    onPrimary = BlueOnPrimaryDark,
    primaryContainer = BluePrimaryContainerDark,
    onPrimaryContainer = BlueOnPrimaryContainerDark,
    secondary = BlueSecondaryDark,
    onSecondary = BlueOnSecondaryDark,
    secondaryContainer = BlueSecondaryContainerDark,
    onSecondaryContainer = BlueOnSecondaryContainerDark,
    background = PolishBackgroundDark,
    onBackground = PolishOnBackgroundDark,
    surface = PolishSurfaceDark,
    onSurface = PolishOnSurfaceDark,
    surfaceVariant = PolishSurfaceVariantDark,
    onSurfaceVariant = PolishOnSurfaceVariantDark,
    outline = PolishOutlineDark,
    outlineVariant = PolishOutlineVariantDark
  )

private val LightColorScheme =
  lightColorScheme(
    primary = BluePrimaryLight,
    onPrimary = BlueOnPrimaryLight,
    primaryContainer = BluePrimaryContainerLight,
    onPrimaryContainer = BlueOnPrimaryContainerLight,
    secondary = BlueSecondaryLight,
    onSecondary = BlueOnSecondaryLight,
    secondaryContainer = BlueSecondaryContainerLight,
    onSecondaryContainer = BlueOnSecondaryContainerLight,
    background = PolishBackgroundLight,
    onBackground = PolishOnBackgroundLight,
    surface = PolishSurfaceLight,
    onSurface = PolishOnSurfaceLight,
    surfaceVariant = PolishSurfaceVariantLight,
    onSurfaceVariant = PolishOnSurfaceVariantLight,
    outline = PolishOutlineLight,
    outlineVariant = PolishOutlineVariantLight
  )

fun getThemeColors(themeColor: ThemeColor, darkTheme: Boolean): Pair<Color, Color> {
    return when (themeColor) {
        ThemeColor.BLUE -> {
            if (darkTheme) Pair(Color(0xFF9ECAFF), Color(0xFF00497D))
            else Pair(Color(0xFF0061A4), Color(0xFFD1E4FF))
        }
        ThemeColor.GREEN -> {
            if (darkTheme) Pair(Color(0xFF81C784), Color(0xFF1B5E20))
            else Pair(Color(0xFF2E7D32), Color(0xFFC8E6C9))
        }
        ThemeColor.ORANGE -> {
            if (darkTheme) Pair(Color(0xFFFFB74D), Color(0xFF5D2500))
            else Pair(Color(0xFFD84315), Color(0xFFFFDDBB))
        }
        ThemeColor.PURPLE -> {
            if (darkTheme) Pair(Color(0xFFD0BCFF), Color(0xFF4F378B))
            else Pair(Color(0xFF6750A4), Color(0xFFEADDFF))
        }
        ThemeColor.RED -> {
            if (darkTheme) Pair(Color(0xFFFFB4AB), Color(0xFF93000A))
            else Pair(Color(0xFFBA1A1A), Color(0xFFFFDAD6))
        }
        ThemeColor.TEAL -> {
            if (darkTheme) Pair(Color(0xFF80CBC4), Color(0xFF004D40))
            else Pair(Color(0xFF00796B), Color(0xFFE0F2F1))
        }
        ThemeColor.AMBER -> {
            if (darkTheme) Pair(Color(0xFFFFD54F), Color(0xFF5D4037))
            else Pair(Color(0xFFF57C00), Color(0xFFFFF8E1))
        }
        ThemeColor.ROSE -> {
            if (darkTheme) Pair(Color(0xFFF48FB1), Color(0xFF880E4F))
            else Pair(Color(0xFFD81B60), Color(0xFFFCE4EC))
        }
        ThemeColor.SLATE -> {
            if (darkTheme) Pair(Color(0xFFCFD8DC), Color(0xFF37474F))
            else Pair(Color(0xFF455A64), Color(0xFFECEFF1))
        }
        ThemeColor.CYAN -> {
            if (darkTheme) Pair(Color(0xFF80DEEA), Color(0xFF006064))
            else Pair(Color(0xFF00ACC1), Color(0xFFE0F7FA))
        }
        ThemeColor.INDIGO -> {
            if (darkTheme) Pair(Color(0xFF9FA8DA), Color(0xFF1A237E))
            else Pair(Color(0xFF3F51B5), Color(0xFFE8EAF6))
        }
        ThemeColor.GOLD -> {
            if (darkTheme) Pair(Color(0xFFFFD700), Color(0xFF4A3C00))
            else Pair(Color(0xFFB8860B), Color(0xFFFFFDF0))
        }
        ThemeColor.EMERALD -> {
            if (darkTheme) Pair(Color(0xFF66BB6A), Color(0xFF004D40))
            else Pair(Color(0xFF2E7D32), Color(0xFFE8F5E9))
        }
        ThemeColor.VIOLET -> {
            if (darkTheme) Pair(Color(0xFFE040FB), Color(0xFF4A148C))
            else Pair(Color(0xFF8E24AA), Color(0xFFF3E5F5))
        }
        ThemeColor.DEEP_ORANGE -> {
            if (darkTheme) Pair(Color(0xFFFF8A65), Color(0xFFBF360C))
            else Pair(Color(0xFFE64A19), Color(0xFFFBE9E7))
        }
        ThemeColor.CRIMSON -> {
            if (darkTheme) Pair(Color(0xFFFF8A80), Color(0xFF880E4F))
            else Pair(Color(0xFFC2185B), Color(0xFFFCE4EC))
        }
        ThemeColor.MINT -> {
            if (darkTheme) Pair(Color(0xFF80EEB0), Color(0xFF004D40))
            else Pair(Color(0xFF00A86B), Color(0xFFE8F8F0))
        }
        ThemeColor.SKY_BLUE -> {
            if (darkTheme) Pair(Color(0xFF80D8FF), Color(0xFF006064))
            else Pair(Color(0xFF0288D1), Color(0xFFE1F5FE))
        }
        ThemeColor.COSMIC_BLACK -> {
            if (darkTheme) Pair(Color(0xFFE0E0E0), Color(0xFF000000))
            else Pair(Color(0xFF212121), Color(0xFFF5F5F5))
        }
        ThemeColor.PEACH -> {
            if (darkTheme) Pair(Color(0xFFFFCC80), Color(0xFFE65100))
            else Pair(Color(0xFFFFB74D), Color(0xFFFFF3E0))
        }
        ThemeColor.SAKURA_PINK -> {
            if (darkTheme) Pair(Color(0xFFFFB7C5), Color(0xFF4A121A))
            else Pair(Color(0xFFF48FB1), Color(0xFFFFF0F3))
        }
        ThemeColor.FOREST_GREEN -> {
            if (darkTheme) Pair(Color(0xFFA5D6A7), Color(0xFF1B5E20))
            else Pair(Color(0xFF1B5E20), Color(0xFFE8F5E9))
        }
        ThemeColor.LAVENDER -> {
            if (darkTheme) Pair(Color(0xFFB39DDB), Color(0xFF311B92))
            else Pair(Color(0xFF673AB7), Color(0xFFEDE7F6))
        }
    }
}

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  themeColor: ThemeColor = ThemeColor.BLUE,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val baseColorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  val (primaryColor, containerColor) = getThemeColors(themeColor, darkTheme)
  val colorScheme = baseColorScheme.copy(
      primary = primaryColor,
      primaryContainer = containerColor,
      secondary = primaryColor.copy(alpha = 0.85f),
      secondaryContainer = containerColor.copy(alpha = 0.6f)
  )

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
