package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.GreatVoiceRoomTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    val appContainer = (application as GreatVoiceRoomApp).container
    
    setContent {
      GreatVoiceRoomTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          androidx.compose.foundation.layout.Box(modifier = Modifier.padding(innerPadding)) {
            AppNavigation(appContainer = appContainer)
          }
        }
      }
    }
  }
}

