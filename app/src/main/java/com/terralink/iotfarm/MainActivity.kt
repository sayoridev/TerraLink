package com.terralink.iotfarm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.terralink.iotfarm.ui.dashboard.DashboardScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Inizializza lo Splash Screen nativo
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        // Opzionale: puoi bloccare lo splash screen finché i dati non sono caricati
        // splashScreen.setKeepOnScreenCondition { viewModel.isLoading.value }

        setContent {
            DashboardScreen()
        }
    }
}