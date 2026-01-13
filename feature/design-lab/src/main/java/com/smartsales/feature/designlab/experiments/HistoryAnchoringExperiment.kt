package com.smartsales.feature.designlab.experiments

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartsales.feature.designlab.theme.LabColors

/**
 * Platform Anchoring Harness for History Drawer visuals.
 * Used to calibrate shadows, footer blending, and typography.
 */
@Composable
fun HistoryAnchoringExperiment() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LabColors.LightBackground)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        CalibrationHeader("1. Active Row Shadow (Glow)")
        ShadowCalibrationRow()

        CalibrationHeader("2. Footer Blend (Separation)")
        FooterCalibrationRow()

        CalibrationHeader("3. Typography (Optical Weight)")
        TypographyCalibrationRow()
    }
}

@Composable
private fun CalibrationHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
    )
}

@Composable
private fun ShadowCalibrationRow() {
    val alphas = listOf(0.10f, 0.15f, 0.20f, 0.25f, 0.30f)
    val elevations = listOf(0.dp, 2.dp, 4.dp, 6.dp)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        elevations.forEach { elevation ->
            Text("Elevation: $elevation", style = MaterialTheme.typography.labelSmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                alphas.forEach { alpha ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .shadow(
                                elevation = elevation,
                                shape = RoundedCornerShape(12.dp),
                                spotColor = LabColors.BubbleShadow.copy(alpha = alpha),
                                ambientColor = Color.Transparent
                            )
                            .background(Color.Transparent)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "${(alpha * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FooterCalibrationRow() {
    val alphas = listOf(0.0f, 0.01f, 0.02f, 0.05f)
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        alphas.forEach { alpha ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    color = Color.Black.copy(alpha = alpha),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(60.dp).fillMaxWidth(0.22f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("α $alpha", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun TypographyCalibrationRow() {
    val configs = listOf(
        Triple(14.sp, FontWeight.Medium, "14sp Medium"),
        Triple(15.sp, FontWeight.Medium, "15sp Medium (Curr)"),
        Triple(15.sp, FontWeight.SemiBold, "15sp SemiBold"),
        Triple(16.sp, FontWeight.Medium, "16sp Medium")
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        configs.forEach { (size, weight, label) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Session Title Example",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = size,
                        fontWeight = weight,
                        letterSpacing = (-0.02).sp
                    )
                )
                Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }
    }
}
