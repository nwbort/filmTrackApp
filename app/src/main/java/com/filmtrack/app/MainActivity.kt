package com.filmtrack.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.filmtrack.app.ui.navigation.FilmTrackNavGraph
import com.filmtrack.app.ui.navigation.Routes
import com.filmtrack.app.ui.theme.FilmTrackTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val quickCapture = intent?.getBooleanExtra("quick_capture", false) == true
            || intent?.action == "com.filmtrack.app.QUICK_CAPTURE"

        setContent {
            FilmTrackTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    FilmTrackNavGraph(
                        navController = navController,
                        startDestination = if (quickCapture) Routes.CAMERA_QUICK else Routes.ROLL_LIST
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}
