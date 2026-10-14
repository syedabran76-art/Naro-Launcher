package com.narostudio.narolauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val NaroBackground = Color(0xFF101114)
private val NaroSurface = Color(0xFF1B1D22)
private val NaroOrange = Color(0xFFFF7628)
private val NaroText = Color(0xFFF4F4F5)
private val NaroMuted = Color(0xFFA4A6AE)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NaroApp() }
    }
}

private data class Destination(val label: String, val icon: ImageVector)

private val destinations = listOf(
    Destination("Home", Icons.Filled.Home),
    Destination("Versions", Icons.Filled.ViewList),
    Destination("Modrinth", Icons.Filled.Extension),
    Destination("Skins", Icons.Filled.Person),
    Destination("Settings", Icons.Filled.Settings)
)

@Composable
private fun NaroApp() {
    var selected by remember { mutableIntStateOf(0) }

    MaterialTheme {
        Surface(Modifier.fillMaxSize(), color = NaroBackground, contentColor = NaroText) {
            Scaffold(
                containerColor = NaroBackground,
                bottomBar = {
                    NavigationBar(containerColor = NaroSurface, contentColor = NaroText) {
                        destinations.forEachIndexed { index, destination ->
                            NavigationBarItem(
                                selected = selected == index,
                                onClick = { selected = index },
                                icon = { Icon(destination.icon, contentDescription = destination.label) },
                                label = { Text(destination.label, fontSize = 10.sp) }
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Column(
                    Modifier.fillMaxSize().padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("NARO", color = NaroOrange, fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
                            Text(destinations[selected].label, color = NaroText, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        }
                        Surface(shape = RoundedCornerShape(50), color = NaroSurface) {
                            Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Person, contentDescription = null, tint = NaroOrange)
                                Spacer(Modifier.width(6.dp))
                                Text("Local profile", color = NaroMuted, fontSize = 12.sp)
                            }
                        }
                    }
                    when (selected) {
                        0 -> HomeScreen()
                        1 -> PlaceholderScreen("Versions", "Version discovery and installation will connect to official Minecraft metadata in upcoming milestones.")
                        2 -> PlaceholderScreen("Modrinth", "Browse and install compatible mods, modpacks, resource packs, and shaders after the engine and profile system are integrated.")
                        3 -> PlaceholderScreen("Skins & Capes", "Skin previews and local customization are planned. Official account skin synchronization requires supported authentication.")
                        else -> PlaceholderScreen("Settings", "Runtime selection, graphics, controls, orientation, downloads, and diagnostics will be added with their underlying functionality.")
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeScreen() {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = NaroSurface)) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("YOUR NEXT WORLD", color = NaroOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Text("A new way to play.", color = NaroText, fontSize = 27.sp, fontWeight = FontWeight.Bold)
            Text("A fresh launcher, built from the ground up for Android.", color = NaroMuted, fontSize = 14.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusPill("Engine", "Not integrated")
                StatusPill("Build", "Milestone 1")
            }
            Button(
                onClick = {},
                enabled = false,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    disabledContainerColor = NaroOrange.copy(alpha = 0.35f),
                    disabledContentColor = NaroText.copy(alpha = 0.7f)
                )
            ) { Text("PLAY  ·  ENGINE NOT READY", fontWeight = FontWeight.Bold) }
            Text("Play stays disabled until a real Android Java engine and launch pipeline are integrated and verified.", color = NaroMuted, fontSize = 12.sp)
        }
    }
    Text("QUICK ACCESS", color = NaroMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        QuickCard("Versions", Icons.Filled.ViewList, Modifier.weight(1f))
        QuickCard("Downloads", Icons.Filled.Download, Modifier.weight(1f))
    }
    QuickCard("Modrinth", Icons.Filled.Extension, Modifier.fillMaxWidth())
    Text("PROJECT STATUS", color = NaroMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
    Text("The baseline UI is in place. Next: validate engine source, native build requirements, Android runtimes, and licensing before integration.", color = NaroMuted, fontSize = 13.sp)
}

@Composable
private fun StatusPill(label: String, value: String) {
    Surface(shape = RoundedCornerShape(12.dp), color = NaroBackground) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
            Text(label, color = NaroMuted, fontSize = 10.sp)
            Text(value, color = NaroText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun QuickCard(title: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = NaroSurface)) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(Modifier.background(NaroOrange.copy(alpha = 0.14f), RoundedCornerShape(12.dp)).padding(10.dp)) {
                Icon(icon, contentDescription = null, tint = NaroOrange)
            }
            Text(title, color = NaroText, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String, description: String) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = NaroSurface)) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, color = NaroOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Text("Coming in its implementation milestone", color = NaroText, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(description, color = NaroMuted, fontSize = 14.sp)
        }
    }
}
