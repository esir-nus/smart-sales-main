package com.smartsales.prism

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Prism Clean Room Application
 * 
 * This is a standalone application for testing the Prism architecture
 * in complete isolation from legacy code.
 */
@HiltAndroidApp
class PrismApplication : Application()
