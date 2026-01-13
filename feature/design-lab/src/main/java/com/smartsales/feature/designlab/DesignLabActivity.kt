package com.smartsales.feature.designlab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.smartsales.feature.designlab.experiments.HistoryAnchoringExperiment
import com.smartsales.feature.designlab.galleries.ColorGallery

/**
 * The Design Lab Entry Point.
 * Only accessible in DEBUG builds.
 */
class DesignLabActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Register Galleries
        registerGalleries()

        setContent {
            var selectedGallery by remember { mutableStateOf<LabRegistry.Gallery?>(null) }

            if (selectedGallery == null) {
                DesignLabDashboard(
                    onGallerySelected = { selectedGallery = it }
                )
            } else {
                // Render content in a full-screen wrapper
                selectedGallery!!.content()
            }
        }
    }

    private fun registerGalleries() {
        // Core Galleries
        LabRegistry.register(
            id = "colors",
            title = "Color Gallery",
            description = "The complete palette (Light/Dark/Gradients).",
            content = { ColorGallery() }
        )

        // Migration: Register History Anchoring as a Gallery for now
        LabRegistry.register(
            id = "history_anchoring",
            title = "History Anchoring (Legacy)",
            description = "Calibration of shadows, blending, and typography.",
            content = { HistoryAnchoringExperiment() }
        )
    }
}
