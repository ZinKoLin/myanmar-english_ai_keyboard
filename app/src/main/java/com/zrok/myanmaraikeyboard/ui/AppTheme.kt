package com.zrok.myanmaraikeyboard.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.zrok.myanmaraikeyboard.R

@Composable
fun MyanmarAiTheme(content: @Composable () -> Unit) {
    val pyidaungsu = FontFamily(Font(R.font.pyidaungsu_regular))
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF6750A4),
            secondary = Color(0xFF625B71),
            surface = Color(0xFFFFFBFE)
        ),
        typography = Typography().copy(
            bodyLarge = Typography().bodyLarge.copy(fontFamily = pyidaungsu),
            bodyMedium = Typography().bodyMedium.copy(fontFamily = pyidaungsu),
            bodySmall = Typography().bodySmall.copy(fontFamily = pyidaungsu),
            labelLarge = Typography().labelLarge.copy(fontFamily = pyidaungsu),
            titleMedium = Typography().titleMedium.copy(fontFamily = pyidaungsu)
        ),
        content = content
    )
}
