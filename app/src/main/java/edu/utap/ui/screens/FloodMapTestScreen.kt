package edu.utap.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun FloodMapTestScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Flood Map Test Screen",
            style = MaterialTheme.typography.headlineMedium
        )
        
        // TODO: Add map component here
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(8.dp)
        ) {
            Text("Map will be displayed here")
        }
        
        // Test controls
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { /* TODO: Toggle flood overlay */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Toggle Flood Overlay")
            }
            
            Button(
                onClick = { /* TODO: Change flood intensity */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Change Flood Intensity")
            }
        }
    }
} 