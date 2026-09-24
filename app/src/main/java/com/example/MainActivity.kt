package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.AppDatabase
import com.example.data.repository.MemoryRepository
import com.example.ui.AnchorViewModel
import com.example.ui.AnchorViewModelFactory
import com.example.ui.screens.AnchorHomeScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext)
        val repository = MemoryRepository(database.memoryDao())
        val viewModelFactory = AnchorViewModelFactory(repository)

        setContent {
            val anchorViewModel: AnchorViewModel = viewModel(factory = viewModelFactory)
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
                ) { _ ->
                    AnchorHomeScreen(viewModel = anchorViewModel)
                }
            }
        }
    }
}
