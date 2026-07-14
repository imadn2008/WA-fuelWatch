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
