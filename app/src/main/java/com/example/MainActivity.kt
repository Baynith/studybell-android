package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ads.AdMobManager
import com.example.ui.navigation.AppDestination
import com.example.ui.navigation.StudyBellApp
import com.example.ui.theme.StudyBellTheme
import com.example.ui.viewmodel.NavTab
import com.example.ui.viewmodel.StudyBellViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as StudyBellApplication
        val repo = app.repository

        val viewModelFactory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return StudyBellViewModel(app, repo) as T
            }
        }

        setContent {
            val vm: StudyBellViewModel = viewModel(factory = viewModelFactory)
            val uiState by vm.uiState.collectAsState()

            val destinationExtra = intent?.getStringExtra("destination")
            val homeworkId = intent?.getLongExtra("homework_id", -1L) ?: -1L

            LaunchedEffect(destinationExtra) {
                if (destinationExtra == "TASKS") {
                    vm.setTab(NavTab.TASKS)
                }
            }

            val initialDest: AppDestination? = when (destinationExtra) {
                "ADD_HOMEWORK" -> AppDestination.AddEditHomework(null)
                "EDIT_HOMEWORK" -> if (homeworkId > 0) AppDestination.AddEditHomework(homeworkId) else null
                else -> null
            }

            StudyBellTheme(darkTheme = uiState.isDarkTheme) {
                StudyBellApp(viewModel = vm, initialDestination = initialDest)
            }
        }
    }

    private var isFirstResume = true

    override fun onResume() {
        super.onResume()
        if (isFirstResume) {
            isFirstResume = false
        } else {
            AdMobManager.showAppOpenAdIfAvailable(this)
        }
    }
}

@Composable
fun Greeting(name: String, modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier) {
    androidx.compose.material3.Text(text = "Hello $name!", modifier = modifier)
}
