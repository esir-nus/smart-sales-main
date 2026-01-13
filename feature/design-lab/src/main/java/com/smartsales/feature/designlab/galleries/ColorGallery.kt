package com.smartsales.feature.designlab.galleries

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartsales.feature.designlab.theme.LabColors

@Composable
fun ColorGallery() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LabColors.LightBackground)
            .padding(16.dp)
    ) {
        Text(
            text = "🎨 Color Gallery",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 24.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 140.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Light Mode
            item { SectionHeader("Light Mode") }
            items(getLightColors()) { (name, color) -> ColorSwatch(name, color) }

            // Dark Mode
            item { SectionHeader("Dark Mode") }
            items(getDarkColors()) { (name, color) -> ColorSwatch(name, color) }

            // Gradients
            item { SectionHeader("Gradients") }
            items(getGradients()) { (name, brush) -> GradientSwatch(name, brush) }

            // Effects
            item { SectionHeader("Effects") }
            items(getEffects()) { (name, color) -> ColorSwatch(name, color) }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    )
}

@Composable
private fun ColorSwatch(name: String, color: Color) {
    Column(
        modifier = Modifier
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(color, RoundedCornerShape(8.dp))
                .border(1.dp, Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
        )
        
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = "#${Integer.toHexString(color.toArgb()).uppercase()}",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, color = Color.Gray)
        )
    }
}

@Composable
private fun GradientSwatch(name: String, brush: Brush) {
    Column(
        modifier = Modifier
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(brush, RoundedCornerShape(8.dp))
        )
        
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = "Gradient",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, color = Color.Gray)
        )
    }
}

// Data Providers
private fun getLightColors() = listOf(
    "Background" to LabColors.LightBackground,
    "Surface Card" to LabColors.LightSurfaceCard,
    "Accent Pri" to LabColors.LightAccentPrimary,
    "Text Pri" to LabColors.LightTextPrimary,
    "Danger" to LabColors.LightDangerText
)

private fun getDarkColors() = listOf(
    "Background" to LabColors.DarkBackground,
    "Surface Card" to LabColors.DarkSurfaceCard,
    "Accent Pri" to LabColors.DarkAccentPrimary,
    "Text Pri" to LabColors.DarkTextPrimary
)

private fun getGradients() = listOf(
    "Wave Idle" to LabColors.WaveIdle,
    "Wave List." to LabColors.WaveListening,
    "Wave Think." to LabColors.WaveThinking,
    "Wave Error" to LabColors.WaveError,
    "Bub. Grad." to LabColors.BubbleGradient
)

private fun getEffects() = listOf(
    "Aurora TL" to LabColors.AuroraTopLeft,
    "Aurora CR" to LabColors.AuroraCenterRight,
    "Glass Shdw" to LabColors.GlassShadow,
    "Bub. Shdw" to LabColors.BubbleShadow,
    "Crys. Card" to LabColors.CrystalCardBg
)
