package com.narostudio.narolauncher

import android.os.Bundle
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.json.JSONArray
import org.json.JSONObject

private val Bg = Color(0xFF050B12)
private val Panel = Color(0xFF0C1723)
private val Panel2 = Color(0xFF102131)
private val Orange = Color(0xFFFF850F)
private val TextMain = Color(0xFFF3F6FA)
private val Muted = Color(0xFF91A4B7)
private val Green = Color(0xFF35D98A)
private val Blue = Color(0xFF43B8F5)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        enableImmersiveMode()
        val prefs = getSharedPreferences("naro_prefs", MODE_PRIVATE)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary=Orange, background=Bg, surface=Panel, onBackground=TextMain, onSurface=TextMain)) {
                NaroApp(
                    load = { key, fallback -> prefs.getString(key, fallback) ?: fallback },
                    save = { key, value -> prefs.edit().putString(key, value).apply() }
                )
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enableImmersiveMode()
    }

    @Suppress("DEPRECATION")
    private fun enableImmersiveMode() {
        window.decorView.systemUiVisibility = (
            android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        )
    }
}

private val navItems = listOf("Home", "Versions", "Modrinth", "Skins & Capes", "Accounts", "Settings", "Downloads")
private val versions = listOf("1.21.10", "1.21.8", "1.21.4", "1.21.1", "1.20.6", "1.20.4", "1.19.4", "1.18.2", "1.16.5", "1.12.2", "1.8.9", "1.7.10", "b1.7.3", "a1.2.6", "Snapshot 25w14a")
private data class ModrinthProject(
    val id: String,
    val slug: String,
    val title: String,
    val description: String,
    val downloads: Long
)

private suspend fun searchModrinth(query: String, type: String): List<ModrinthProject> =
    withContext(Dispatchers.IO) {
        val q = URLEncoder.encode(query.ifBlank { "minecraft" }, "UTF-8")
        val facets = URLEncoder.encode("[[\\"project_type:$type\\"]]", "UTF-8")
        val url = URL("https://api.modrinth.com/v2/search?query=$q&facets=$facets&limit=30")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10000
            readTimeout = 15000
            setRequestProperty("User-Agent", "NaroLauncher/1.6.0 (Android)")
            setRequestProperty("Accept", "application/json")
        }
        try {
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("Modrinth returned HTTP ${connection.responseCode}")
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val hits = JSONObject(body).getJSONArray("hits")
            (0 until hits.length()).map { i ->
                val item = hits.getJSONObject(i)
                ModrinthProject(
                    id = item.optString("project_id"),
                    slug = item.optString("slug"),
                    title = item.optString("title", "Untitled project"),
                    description = item.optString("description", ""),
                    downloads = item.optLong("downloads", 0)
                )
            }
        } finally {
            connection.disconnect()
        }
    }

@Composable
private fun NaroApp(load: (String,String)->String, save:(String,String)->Unit) {
    var page by remember { mutableStateOf("Home") }
    var profile by remember { mutableStateOf(load("profile", "PlayerOne")) }
    var selectedVersion by remember {
        mutableStateOf(load("profileVersion:$profile", load("selectedVersion", "1.20.4")))
    }
    var account by remember { mutableStateOf(load("account", "Offline account")) }
    var orientation by remember { mutableStateOf(load("orientation", "Landscape")) }
    var theme by remember { mutableStateOf(load("theme", "Dark (Naro Orange)")) }
    var memory by remember { mutableFloatStateOf(load("memory","4").toFloatOrNull() ?: 4f) }
    var fps by remember { mutableStateOf(load("fps", "Unlimited")) }
    var graphics by remember { mutableStateOf(load("graphics", "OpenGL ES (engine pending)")) }
    var controls by remember { mutableStateOf(load("controls", "Default")) }
    var search by remember { mutableStateOf("") }
    var modType by remember { mutableStateOf("Mods") }
    var modResults by remember { mutableStateOf<List<ModrinthProject>>(emptyList()) }
    var modLoading by remember { mutableStateOf(false) }
    var modError by remember { mutableStateOf("") }
    val context = LocalContext.current

    LaunchedEffect(search, modType) {
        delay(350)
        modLoading = true
        modError = ""
        try {
            val type = when (modType) {
                "Modpacks" -> "modpack"
                "Resource Packs" -> "resourcepack"
                else -> "mod"
            }
            modResults = searchModrinth(search.trim(), type)
        } catch (e: Exception) {
            modError = e.message ?: "Could not connect to Modrinth."
            modResults = emptyList()
        } finally {
            modLoading = false
        }
    }
    var versionFilter by remember { mutableStateOf("All") }
    var toast by remember { mutableStateOf("") }
    var tasks by remember { mutableStateOf(load("tasks", "Sodium|Queued").split("\n").filter { it.contains("|") }) }
    var skinNames by remember { mutableStateOf(load("skins", "Default Steve\nAlex").split("\n").filter { it.isNotBlank() }) }
    var profileNames by remember { mutableStateOf(load("profiles", "Default\nPerformance\nVanilla").split("\n").filter { it.isNotBlank() }) }
    var newName by remember { mutableStateOf("") }
    var showAdd by remember { mutableStateOf(false) }
    var showPlayInfo by remember { mutableStateOf(false) }

    fun persist(key:String, value:String) = save(key,value)
    fun addTask(name:String) {
        tasks = (tasks + "$name|Queued").distinct()
        persist("tasks", tasks.joinToString("\n"))
        toast = "$name added to download queue"
    }
    Row(Modifier.fillMaxSize().background(Bg)) {
        Column(Modifier.width(176.dp).fillMaxHeight().background(Color(0xFF07111B)).padding(12.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(8.dp), modifier=Modifier.padding(bottom=18.dp)) {
                Box(Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(Orange), contentAlignment=Alignment.Center) {
                    Text("N", fontWeight=FontWeight.Black, color=Bg, fontSize=25.sp)
                }
                Column {
                    Text("NARO", fontSize=20.sp, fontWeight=FontWeight.Black, letterSpacing=2.sp)
                    Text("LAUNCHER", fontSize=9.sp, color=Muted, letterSpacing=2.sp)
                }
            }
            navItems.forEach { item ->
                val icon = when(item) { "Home"->Icons.Default.Home; "Versions"->Icons.Default.ViewList; "Modrinth"->Icons.Default.Search; "Skins & Capes"->Icons.Default.Person; "Accounts"->Icons.Default.AccountCircle; "Settings"->Icons.Default.Settings; else->Icons.Default.Download }
                Row(Modifier.fillMaxWidth().padding(vertical=3.dp).clip(RoundedCornerShape(9.dp)).background(if(page==item) Color(0xFF422812) else Color.Transparent).clickable { page=item }.padding(horizontal=10.dp, vertical=10.dp), verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    Icon(icon, null, tint=if(page==item) Orange else Muted, modifier=Modifier.size(18.dp))
                    Text(item, color=if(page==item) TextMain else Muted, fontSize=12.sp, fontWeight=if(page==item) FontWeight.SemiBold else FontWeight.Normal, maxLines=1, overflow=TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.weight(1f))
            Card(colors=CardDefaults.cardColors(containerColor=Panel), shape=RoundedCornerShape(10.dp)) {
                Column(Modifier.padding(10.dp)) {
                    Text("DOWNLOADS", color=Muted, fontSize=9.sp, letterSpacing=1.sp)
                    Text("${tasks.size} queued tasks", fontSize=12.sp, fontWeight=FontWeight.SemiBold)
                    LinearProgressIndicator(progress={ if(tasks.isEmpty()) 0f else .32f }, color=Orange, trackColor=Panel2, modifier=Modifier.fillMaxWidth().padding(top=7.dp))
                }
            }
            Text("v1.5.0 • MILESTONE", color=Muted, fontSize=9.sp, modifier=Modifier.padding(top=10.dp))
        }
        Column(Modifier.weight(1f).fillMaxHeight().padding(horizontal=18.dp, vertical=14.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically, modifier=Modifier.fillMaxWidth().padding(bottom=12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(page.uppercase(), color=TextMain, fontSize=22.sp, fontWeight=FontWeight.Bold, letterSpacing=1.sp)
                    Text(when(page) { "Home"->"Play Minecraft. Your Way."; "Versions"->"Explore releases, snapshots and historical versions"; "Modrinth"->"Mods, modpacks and resource packs"; "Skins & Capes"->"Make your character yours"; "Accounts"->"Manage local profiles and sign-in status"; "Settings"->"Tune your launcher experience"; else->"Track queued downloads and installation tasks" }, color=Muted, fontSize=11.sp)
                }
                Surface(color=Panel, shape=RoundedCornerShape(10.dp)) {
                    Row(Modifier.padding(horizontal=12.dp, vertical=8.dp), verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(Green))
                        Text(profile, fontSize=12.sp, fontWeight=FontWeight.SemiBold)
                        Icon(Icons.Default.ExpandMore, null, tint=Muted, modifier=Modifier.size(16.dp))
                    }
                }
            }
            when(page) {
                "Home" -> HomePage(profile, account, selectedVersion, tasks.size, onVersion={page="Versions"}, onMods={page="Modrinth"}, onSkins={page="Skins & Capes"}, onSettings={page="Settings"}, onPlay={showPlayInfo=true})
                "Versions" -> {
                    Row(verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(8.dp), modifier=Modifier.fillMaxWidth().padding(bottom=10.dp)) {
                        OutlinedTextField(search,{search=it},modifier=Modifier.weight(1f).height(52.dp),singleLine=true,placeholder={Text("Search versions…",fontSize=12.sp)},leadingIcon={Icon(Icons.Default.Search,null)},shape=RoundedCornerShape(10.dp))
                        listOf("All","Release","Snapshot","Old").forEach { f -> FilterChip(selected=versionFilter==f,onClick={versionFilter=f},label={Text(f,fontSize=11.sp)},colors=FilterChipDefaults.filterChipColors(selectedContainerColor=Orange,selectedLabelColor=Bg)) }
                    }
                    LazyColumn(verticalArrangement=Arrangement.spacedBy(7.dp)) {
                        items(versions.filter { it.contains(search,true) && (versionFilter=="All" || (versionFilter=="Release" && it.firstOrNull()?.isDigit()==true) || (versionFilter=="Snapshot" && it.startsWith("Snapshot")) || (versionFilter=="Old" && (it.startsWith("b")||it.startsWith("a")||it=="1.7.10"||it=="1.8.9")) ) }) { v ->
                            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Panel).padding(horizontal=12.dp, vertical=8.dp), verticalAlignment=Alignment.CenterVertically) {
                                Box(Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(Panel2),contentAlignment=Alignment.Center){Icon(Icons.Default.Inventory2,null,tint=Orange)}
                                Column(Modifier.weight(1f).padding(start=10.dp)) { Text(v,fontWeight=FontWeight.SemiBold,fontSize=13.sp); Text(if(v.startsWith("Snapshot")) "Snapshot • Mojang" else if(v.startsWith("a")||v.startsWith("b")) "Historical alpha/beta" else "Release • Mojang",fontSize=10.sp,color=Muted) }
                                if(selectedVersion==v) Text("SELECTED",fontSize=9.sp,color=Green,modifier=Modifier.padding(end=8.dp))
                                Button(onClick={selectedVersion=v;persist("selectedVersion",v);persist("profileVersion:$profile",v);addTask("Minecraft $v")},colors=ButtonDefaults.buttonColors(containerColor=Orange,contentColor=Bg),contentPadding=PaddingValues(horizontal=16.dp,vertical=6.dp),modifier=Modifier.height(34.dp)){Text(if(selectedVersion==v) "Install" else "Select",fontSize=11.sp)}
                            }
                        }
                    }
                }
                "Modrinth" -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        OutlinedTextField(
                            value = search,
                            onValueChange = { search = it },
                            modifier = Modifier.weight(1f).height(52.dp),
                            singleLine = true,
                            placeholder = { Text("Search real Modrinth projects…", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            shape = RoundedCornerShape(10.dp)
                        )
                        listOf("Mods", "Modpacks", "Resource Packs").forEach { f ->
                            FilterChip(
                                selected = modType == f,
                                onClick = { modType = f },
                                label = { Text(f, fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Orange,
                                    selectedLabelColor = Bg
                                )
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
                    ) {
                        Text("MODRINTH PROJECTS", fontSize = 10.sp, color = Muted, letterSpacing = 1.sp,
                            modifier = Modifier.weight(1f))
                        if (modLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Orange,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("${modResults.size} results", fontSize = 10.sp, color = Muted)
                        }
                    }

                    if (modError.isNotBlank()) {
                        Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
                            Column(Modifier.fillMaxWidth().padding(14.dp)) {
                                Text("Couldn't load Modrinth", color = Orange, fontWeight = FontWeight.Bold)
                                Text(modError, color = Muted, fontSize = 11.sp,
                                    modifier = Modifier.padding(top = 5.dp))
                                TextButton(onClick = {
                                    search = search.trim()
                                    modType = modType
                                }) { Text("Retry", color = Orange) }
                            }
                        }
                    } else if (!modLoading && modResults.isEmpty()) {
                        EmptyState(
                            "No projects found",
                            "Try another search or project category.",
                            Icons.Default.Search
                        )
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            items(modResults, key = { it.id }) { project ->
                                Row(
                                    Modifier.fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Panel)
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        Modifier.size(42.dp).clip(RoundedCornerShape(10.dp))
                                            .background(Panel2),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Extension, null, tint = Orange)
                                    }
                                    Column(
                                        Modifier.weight(1f).padding(horizontal = 12.dp)
                                    ) {
                                        Text(project.title, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(project.description, fontSize = 10.sp, color = Muted,
                                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        Text(
                                            "%,d downloads".format(project.downloads),
                                            fontSize = 9.sp, color = Blue,
                                            modifier = Modifier.padding(top = 3.dp)
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            val typePath = when (modType) {
                                                "Modpacks" -> "modpack"
                                                "Resource Packs" -> "resourcepack"
                                                else -> "mod"
                                            }
                                            context.startActivity(
                                                Intent(
                                                    Intent.ACTION_VIEW,
                                                    Uri.parse("https://modrinth.com/$typePath/${project.slug}")
                                                )
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Orange, contentColor = Bg
                                        ),
                                        modifier = Modifier.height(34.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp)
                                    ) { Text("View", fontSize = 11.sp) }
                                }
                            }
                        }
                    }
                    Text(
                        "Live search is connected. Project downloads and loader/version compatibility are the next step.",
                        fontSize = 10.sp, color = Muted,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                "Skins & Capes" -> {
                    Row(horizontalArrangement=Arrangement.spacedBy(12.dp),modifier=Modifier.fillMaxSize()) {
                        Card(Modifier.weight(1f).fillMaxHeight(),colors=CardDefaults.cardColors(containerColor=Panel),shape=RoundedCornerShape(14.dp)) {
                            Column(Modifier.fillMaxSize().padding(16.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                                Text("SKIN PREVIEW",color=Muted,fontSize=10.sp,letterSpacing=1.sp)
                                Spacer(Modifier.height(14.dp))
                                Box(Modifier.size(width=105.dp,height=150.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF18364A)),contentAlignment=Alignment.Center) {
                                    Column(horizontalAlignment=Alignment.CenterHorizontally) {
                                        Box(Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFC58A61)),contentAlignment=Alignment.Center){Text("☺",fontSize=30.sp,color=Color(0xFF533322))}
                                        Row(horizontalArrangement=Arrangement.spacedBy(3.dp)){Box(Modifier.width(27.dp).height(56.dp).background(Color(0xFF1D9C9C)));Box(Modifier.width(27.dp).height(56.dp).background(Color(0xFF1D9C9C)))}
                                        Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){Box(Modifier.width(23.dp).height(32.dp).background(Color(0xFF303E9B)));Box(Modifier.width(23.dp).height(32.dp).background(Color(0xFF303E9B)))}
                                    }
                                }
                                Text("Local preview placeholder",fontSize=10.sp,color=Muted,modifier=Modifier.padding(top=8.dp))
                                Button(onClick={showAdd=true;newName=""},colors=ButtonDefaults.buttonColors(containerColor=Orange,contentColor=Bg),modifier=Modifier.fillMaxWidth().padding(top=10.dp)){Text("＋ Add skin entry")}
                            }
                        }
                        Card(Modifier.weight(1f).fillMaxHeight(),colors=CardDefaults.cardColors(containerColor=Panel),shape=RoundedCornerShape(14.dp)) {
                            Column(Modifier.padding(14.dp)) {
                                Text("MY SKINS",fontWeight=FontWeight.Bold,fontSize=14.sp)
                                Text("Local skin names and selection",color=Muted,fontSize=10.sp,modifier=Modifier.padding(bottom=8.dp))
                                skinNames.forEach { s -> Row(Modifier.fillMaxWidth().padding(vertical=4.dp).clip(RoundedCornerShape(8.dp)).background(Panel2).clickable { toast="$s selected" }.padding(9.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.Person,null,tint=Orange);Text(s,Modifier.weight(1f).padding(start=8.dp),fontSize=12.sp);IconButton(onClick={skinNames=skinNames-s;persist("skins",(skinNames-s).joinToString("\n"))},modifier=Modifier.size(28.dp)){Icon(Icons.Default.Close,null,tint=Muted,modifier=Modifier.size(16.dp))}} }
                                Text("Capes",fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=10.dp))
                                Text("Capes require a connected Microsoft/Mojang account and are not uploaded by this local manager.",fontSize=10.sp,color=Muted)
                            }
                        }
                    }
                }
                "Accounts" -> {
                    Row(horizontalArrangement=Arrangement.spacedBy(12.dp),modifier=Modifier.fillMaxSize()) {
                        Card(Modifier.weight(1f).fillMaxHeight(),colors=CardDefaults.cardColors(containerColor=Panel)) { Column(Modifier.padding(16.dp)) {
                            Icon(Icons.Default.AccountCircle,null,tint=Orange,modifier=Modifier.size(42.dp))
                            Text("CURRENT ACCOUNT",fontSize=10.sp,color=Muted,modifier=Modifier.padding(top=10.dp))
                            Text(account,fontSize=19.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(vertical=5.dp))
                            Text("Offline profiles work for local testing only. Online-mode servers require a valid Microsoft-authenticated Minecraft account.",fontSize=11.sp,color=Muted)
                            Button(onClick={showAdd=true;newName="";toast="Enter an offline profile name"},modifier=Modifier.fillMaxWidth().padding(top=12.dp),colors=ButtonDefaults.buttonColors(containerColor=Orange,contentColor=Bg)){Text("＋ Add offline profile")}
                            OutlinedButton(onClick={toast="Microsoft sign-in needs a registered OAuth client and secure callback configuration; not enabled yet."},modifier=Modifier.fillMaxWidth()){Text("Connect Microsoft (setup needed)",fontSize=11.sp)}
                        } }
                        Card(Modifier.weight(1f).fillMaxHeight(),colors=CardDefaults.cardColors(containerColor=Panel)) { Column(Modifier.padding(16.dp)) {
                            Text("PROFILES",fontSize=14.sp,fontWeight=FontWeight.Bold)
                            profileNames.forEach { p ->
                                Row(
                                    Modifier.fillMaxWidth().padding(top=7.dp)
                                        .clip(RoundedCornerShape(8.dp)).background(Panel2)
                                        .clickable {
                                            profile = p
                                            persist("profile", p)
                                            selectedVersion = load("profileVersion:$p", load("selectedVersion", "1.20.4"))
                                            persist("selectedVersion", selectedVersion)
                                            toast = "Switched to $p"
                                        }.padding(start=10.dp, end=4.dp, top=6.dp, bottom=6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Person, null, tint = if (profile == p) Orange else Muted)
                                    Column(Modifier.weight(1f).padding(start=8.dp)) {
                                        Text(p, fontSize=12.sp, fontWeight=FontWeight.SemiBold)
                                        Text("Minecraft $selectedVersion", fontSize=9.sp, color=Muted)
                                    }
                                    if (profile == p) Icon(Icons.Default.CheckCircle, null, tint=Green)
                                    if (profileNames.size > 1) {
                                        IconButton(onClick = {
                                            profileNames = profileNames.filterNot { it == p }
                                            persist("profiles", profileNames.joinToString("\\n"))
                                            if (profile == p) {
                                                profile = profileNames.first()
                                                persist("profile", profile)
                                                selectedVersion = load("profileVersion:$profile", "1.20.4")
                                                persist("selectedVersion", selectedVersion)
                                            }
                                            toast = "Profile removed"
                                        }, modifier=Modifier.size(32.dp)) {
                                            Icon(Icons.Default.DeleteOutline, null, tint=Muted, modifier=Modifier.size(17.dp))
                                        }
                                    }
                                }
                            }
                            OutlinedButton(onClick={showAdd=true;newName=""},modifier=Modifier.padding(top=10.dp)){Text("＋ New profile")}
                        } }
                    }
                }
                "Settings" -> {
                    Row(horizontalArrangement=Arrangement.spacedBy(12.dp),modifier=Modifier.fillMaxSize()) {
                        LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            item { SettingCard("APPEARANCE") {
                                SettingRow("Theme","Naro Dark / Orange")
                                SettingRow("Orientation",orientation) { orientation=if(orientation=="Landscape") "Auto" else "Landscape";persist("orientation",orientation);toast="Orientation preference saved (landscape-first)" }
                                SettingRow("Animations","Smooth") { toast="Animation preference: Smooth" }
                                SettingRow("Background","Minecraft landscape") { toast="Background style selected" }
                            } }
                            item { SettingCard("GAME") {
                                SettingRow("Default version",selectedVersion){page="Versions"}
                                SettingRow("Default profile",profile){page="Accounts"}
                                SettingRow("Controls layout",controls){controls=if(controls=="Default") "Compact" else if(controls=="Compact") "Custom" else "Default";persist("controls",controls)}
                            } }
                        }
                        LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            item { SettingCard("PERFORMANCE & GRAPHICS") {
                                Text("Memory allocation: ${memory.toInt()} GB",fontSize=12.sp)
                                Slider(value=memory,onValueChange={memory=it},valueRange=1f..8f,steps=6,onValueChangeFinished={persist("memory",memory.toInt().toString())},colors=SliderDefaults.colors(thumbColor=Orange,activeTrackColor=Orange))
                                SettingRow("Graphics backend",graphics){graphics=if(graphics.startsWith("OpenGL")) "Vulkan (engine pending)" else "OpenGL ES (engine pending)";persist("graphics",graphics)}
                                SettingRow("FPS limit",fps){fps=when(fps){"Unlimited"->"120";"120"->"60";"60"->"30";else->"Unlimited"};persist("fps",fps)}
                                Text("These are saved preferences; actual renderer/runtime options become active after Pojav engine integration.",fontSize=10.sp,color=Muted)
                            } }
                            item { SettingCard("LAUNCHER") {
                                SettingRow("Download location","App storage"){toast="Using app-private storage"}
                                SettingRow("Clear cache","Not available") { toast="No cached game files to clear yet" }
                                SettingRow("Reset settings","") { showAdd=true;newName="RESET_CONFIRM" }
                            } }
                        }
                    }
                }
                "Downloads" -> {
                    Column(Modifier.fillMaxSize()) {
                        Row(verticalAlignment=Alignment.CenterVertically,modifier=Modifier.padding(bottom=10.dp)) { Text("${tasks.size} TASKS",color=Muted,fontSize=11.sp,modifier=Modifier.weight(1f)); TextButton(onClick={tasks= emptyList();persist("tasks","");toast="Queue cleared"}){Text("Clear queue",color=Orange)} }
                        if(tasks.isEmpty()) EmptyState("All caught up","Queued version and mod items will appear here.",Icons.Default.Download)
                        else LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            items(tasks) { task ->
                                val parts=task.split("|")
                                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Panel).padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
                                    Icon(Icons.Default.Downloading,null,tint=Orange,modifier=Modifier.size(28.dp))
                                    Column(Modifier.weight(1f).padding(horizontal=12.dp)){Text(parts[0],fontWeight=FontWeight.SemiBold,fontSize=13.sp);Text(parts.getOrElse(1){"Queued"},fontSize=10.sp,color=Muted);LinearProgressIndicator(progress={0f},color=Orange,trackColor=Panel2,modifier=Modifier.fillMaxWidth().padding(top=6.dp))}
                                    TextButton(onClick={tasks=tasks.filterNot{it==task};persist("tasks",tasks.joinToString("\n"))}){Text("Remove",color=Orange)}
                                }
                            }
                        }
                        Text("Queue tracking is local. Actual downloads will be wired to Mojang/Modrinth services in the next milestone.",fontSize=10.sp,color=Muted,modifier=Modifier.padding(top=8.dp))
                    }
                }
            }
        }
    }
    if(toast.isNotBlank()) {
        LaunchedEffect(toast) { kotlinx.coroutines.delay(2600); toast="" }
        Surface(Modifier.padding(18.dp),color=Color(0xFF263544),shape=RoundedCornerShape(12.dp),shadowElevation=8.dp) {
            Row(Modifier.padding(horizontal=14.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){Icon(Icons.Default.Info,null,tint=Orange);Text(toast,fontSize=12.sp);IconButton(onClick={toast=""},modifier=Modifier.size(24.dp)){Icon(Icons.Default.Close,null,tint=Muted)}} 
        }
    }
    if(showAdd) {
        AlertDialog(onDismissRequest={showAdd=false},title={Text(if(newName=="RESET_CONFIRM") "Reset launcher settings?" else "Add to Naro Launcher")},text={
            if(newName=="RESET_CONFIRM") Text("Reset saved settings to their defaults? Existing profile and skin names will remain.")
            else Column { Text("Create a local entry. Microsoft authentication and actual skin upload need separate integration.",fontSize=12.sp,color=Muted);Spacer(Modifier.height(8.dp));OutlinedTextField(newName,{newName=it},label={Text("Name")},singleLine=true) }
        },confirmButton={TextButton(onClick={
            if(newName=="RESET_CONFIRM") { memory=4f;fps="Unlimited";graphics="OpenGL ES (engine pending)";orientation="Landscape";controls="Default";persist("memory","4");persist("fps",fps);persist("graphics",graphics);persist("orientation",orientation);persist("controls",controls) }
            else if(newName.isNotBlank()) {
                if(page=="Skins & Capes") { skinNames=(skinNames+newName).distinct();persist("skins",skinNames.joinToString("\n")) }
                else if(page=="Accounts") { account=newName;profile=newName;persist("account",account);persist("profile",profile);profileNames=(profileNames+newName).distinct();persist("profiles",profileNames.joinToString("\n")) }
                else { profileNames=(profileNames+newName).distinct();persist("profiles",profileNames.joinToString("\n"));profile=newName;persist("profile",profile) }
            }
            showAdd=false;newName=""
        }) { Text("Save", color=Orange) } }, dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } })
    }
    if(showPlayInfo) AlertDialog(onDismissRequest={showPlayInfo=false},icon={Icon(Icons.Default.SportsEsports,null,tint=Orange)},title={Text("Game engine integration pending")},text={Text("The launcher interface and local management features are in place. Real Minecraft gameplay requires integrating PojavLauncher’s runtime, version installer, libraries, authentication, and renderer. Play is disabled rather than pretending the game launched.")},confirmButton={TextButton(onClick={showPlayInfo=false}){Text("Got it",color=Orange)}})
}

@Composable
private fun HomePage(profile:String, account:String, version:String, tasks:Int, onVersion:()->Unit, onMods:()->Unit, onSkins:()->Unit, onSettings:()->Unit, onPlay:()->Unit) {
    Row(Modifier.fillMaxSize(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1.25f),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Card(Modifier.fillMaxWidth().height(122.dp),colors=CardDefaults.cardColors(containerColor=Color(0xFF132638)),shape=RoundedCornerShape(14.dp)) {
                Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF17374B),Color(0xFF2E281F),Color(0xFF101C28)))).padding(16.dp)) {
                    Column(Modifier.align(Alignment.CenterStart)) {
                        Text("GOOD TO SEE YOU",fontSize=10.sp,color=Orange,letterSpacing=1.sp)
                        Text(profile,fontSize=22.sp,fontWeight=FontWeight.Bold)
                        Text("$account  •  Offline-ready profile",fontSize=11.sp,color=Muted)
                    }
                    Icon(Icons.Default.Landscape,null,tint=Color(0xFF627D89),modifier=Modifier.size(90.dp).align(Alignment.CenterEnd))
                }
            }
            Card(Modifier.fillMaxWidth().weight(1f),colors=CardDefaults.cardColors(containerColor=Panel),shape=RoundedCornerShape(14.dp)) {
                Column(Modifier.fillMaxSize().padding(14.dp)) {
                    Row(verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF31583C)),contentAlignment=Alignment.Center){Icon(Icons.Default.Inventory2,null,tint=Color(0xFF9CE06B),modifier=Modifier.size(30.dp))};Column(Modifier.weight(1f).padding(start=10.dp)){Text("Minecraft $version",fontSize=15.sp,fontWeight=FontWeight.Bold);Text("Selected version • Runtime not configured",fontSize=10.sp,color=Muted)};IconButton(onClick=onVersion){Icon(Icons.Default.ExpandMore,null,tint=Muted)}}
                    Spacer(Modifier.height(10.dp))
                    Button(onClick=onPlay,modifier=Modifier.fillMaxWidth().height(44.dp),colors=ButtonDefaults.buttonColors(containerColor=Orange,contentColor=Bg),shape=RoundedCornerShape(9.dp)){Icon(Icons.Default.PlayArrow,null);Text("  PLAY",fontWeight=FontWeight.Black,letterSpacing=1.sp)}
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.padding(top=10.dp)) {
                        MiniStat("Java runtime","Pojav integration pending",Icons.Default.Memory,Modifier.weight(1f))
                        MiniStat("Game files","Not installed",Icons.Default.Folder,Modifier.weight(1f))
                    }
                }
            }
        }
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text("QUICK ACCESS",fontSize=10.sp,color=Muted,letterSpacing=1.sp)
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { QuickTile("Versions",Icons.Default.ViewList,Modifier.weight(1f),onVersion);QuickTile("Modrinth",Icons.Default.Extension,Modifier.weight(1f),onMods) }
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { QuickTile("Skins",Icons.Default.Person,Modifier.weight(1f),onSkins);QuickTile("Settings",Icons.Default.Settings,Modifier.weight(1f),onSettings) }
            Card(Modifier.fillMaxWidth().weight(1f),colors=CardDefaults.cardColors(containerColor=Panel),shape=RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment=Alignment.CenterVertically){Text("RECENT ACTIVITY",fontSize=10.sp,color=Muted,letterSpacing=1.sp,modifier=Modifier.weight(1f));Text("$tasks queued",fontSize=10.sp,color=Orange)}
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.CheckCircle,null,tint=Green);Column(Modifier.padding(start=9.dp)){Text("Launcher initialized",fontSize=12.sp,fontWeight=FontWeight.SemiBold);Text("Settings and profiles save locally",fontSize=10.sp,color=Muted)}}
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.Info,null,tint=Orange);Column(Modifier.padding(start=9.dp)){Text("Pojav engine adapter",fontSize=12.sp,fontWeight=FontWeight.SemiBold);Text("Needed for actual gameplay",fontSize=10.sp,color=Muted)}}
                }
            }
        }
    }
}

@Composable private fun MiniStat(title:String,value:String,icon:androidx.compose.ui.graphics.vector.ImageVector,modifier:Modifier=Modifier) {
    Row(modifier.clip(RoundedCornerShape(9.dp)).background(Panel2).padding(9.dp),verticalAlignment=Alignment.CenterVertically) {
        Icon(icon,null,tint=Orange,modifier=Modifier.size(20.dp));Column(Modifier.padding(start=7.dp)){Text(title,fontSize=10.sp,fontWeight=FontWeight.Bold);Text(value,fontSize=8.sp,color=Muted,maxLines=1,overflow=TextOverflow.Ellipsis)}
    }
}
@Composable private fun QuickTile(title:String,icon:androidx.compose.ui.graphics.vector.ImageVector,modifier:Modifier=Modifier,action:()->Unit) {
    Column(modifier.clip(RoundedCornerShape(10.dp)).background(Panel).clickable(onClick=action).padding(12.dp),horizontalAlignment=Alignment.CenterHorizontally) {
        Icon(icon,null,tint=Orange,modifier=Modifier.size(25.dp));Text(title,fontSize=11.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.padding(top=6.dp))
    }
}
@Composable private fun SettingCard(title:String,content:@Composable ColumnScope.()->Unit) {
    Card(colors=CardDefaults.cardColors(containerColor=Panel),shape=RoundedCornerShape(12.dp)) {
        Column(Modifier.fillMaxWidth().padding(13.dp)) { Text(title,fontSize=10.sp,color=Orange,letterSpacing=1.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(bottom=8.dp));content() }
    }
}
@Composable private fun SettingRow(label:String,value:String,onClick:()->Unit={}) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(7.dp)).clickable(onClick=onClick).padding(vertical=8.dp),verticalAlignment=Alignment.CenterVertically) {
        Text(label,fontSize=12.sp,modifier=Modifier.weight(1f))
        Text(value,fontSize=10.sp,color=Muted,maxLines=1,overflow=TextOverflow.Ellipsis)
        Icon(Icons.Default.ChevronRight,null,tint=Muted,modifier=Modifier.size(17.dp))
    }
}
@Composable private fun EmptyState(title:String,description:String,icon:androidx.compose.ui.graphics.vector.ImageVector) {
    Column(Modifier.fillMaxSize(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
        Icon(icon,null,tint=Orange,modifier=Modifier.size(42.dp));Text(title,fontSize=18.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=10.dp));Text(description,fontSize=12.sp,color=Muted)
    }
}
