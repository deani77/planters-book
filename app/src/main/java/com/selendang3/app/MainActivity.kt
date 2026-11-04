package com.selendang3.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.selendang3.app.data.EstateDatabase
import com.selendang3.app.data.EstatePackages
import java.text.DateFormat
import java.util.Date

data class Section(val route: String, val title: String, val description: String)

val estateSections = listOf(
    Section("overview", "Estate Overview", "Estate profile, location, cultivation and maturity status."),
    Section("fields", "Field Details", "Field codes, hectarage, palm population and density."),
    Section("ages", "Palm Age Profile", "Palm ages by field and age-category distribution."),
    Section("soil", "Soil & Topography", "Soil composition, terrain and corresponding areas."),
    Section("rainfall", "Rainfall", "Monthly rainfall, rainy days and historical comparisons."),
    Section("abw", "Average Bunch Weight", "Current-period and cumulative ABW in kg/bunch, with year-on-year comparison. Field-level figures are pending the supporting Excel file."),
    Section("roads", "Roads & Infrastructure", "Main, secondary and subsidiary road specifications."),
)

val mainSections = listOf(
    Section("information", "Estate Details", "Estate metrics, rainfall, workforce and costs"),
    Section("production", "Field Details", "Field dashboard and records by field."),
    Section("upkeep", "Upkeep and Cultivation", "Estate work dashboard, excluding crop production."),
    Section("reports", "Crop Production", "Field production, daily drafts and period totals."),
    Section("settings", "Settings", "Local data import and preferences"),
    Section("tools", "Planter Tools", "Offline plantation calculators."),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { EstateApp() }
    }
}

@Composable
fun EstateApp() {
    val estateName = rememberEstateName()
    val context = LocalContext.current.applicationContext
    val preferences = remember(context) { context.getSharedPreferences("user_settings", 0) }
    var themeChoice by remember { mutableStateOf(preferences.getString("theme", "system") ?: "system") }
    val dark = when (themeChoice) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    val dao = remember(context) { EstateDatabase.get(context).estateDao() }
    val snapshot by remember(dao) { dao.observeSnapshot() }.collectAsStateWithLifecycle(null)
    val estateUpdate = remember(snapshot?.content) { snapshot?.let { EstatePackages.parse(it.content) } }
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: "home"
    val allSections = mainSections + estateSections
    val title = when (route) {
        "home" -> "Planters Book"
        "dashboard" -> "Estate Dashboard"
        "estate-records" -> "All Estate Records"
        else -> allSections.firstOrNull { it.route == route }?.title ?: "Estate Details"
    }
    MaterialTheme(colorScheme = estateColorScheme(dark, bookAccent(route))) {
        Box(Modifier.fillMaxSize()) {
            PlantationBackground(route)
            Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 20.dp)) {
                if (route != "home") {
                    TextButton(onClick = { nav.popBackStack() }) { Text("‹  Back") }
                } else {
                    Spacer(Modifier.height(20.dp))
                    Text(estateName, modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center)
                }
                Text(title, style = if (route == "home") MaterialTheme.typography.headlineLarge else MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold, color = Color.White,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                        .background(Brush.linearGradient(listOf(bookAccent(route).start, bookAccent(route).end)), RoundedCornerShape(24.dp))
                        .padding(20.dp),
                    textAlign = if (route == "home") TextAlign.Center else TextAlign.Start)
                NavHost(nav, startDestination = "home", modifier = Modifier.weight(1f)) {
                    composable("home") {
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Spacer(Modifier.height(8.dp))
                            SectionGrid(listOf(Section("dashboard", "Estate Dashboard", "")) + mainSections) { nav.navigate(it) }
                        }
                    }
                    composable("information") {
                        Column {
                            OutlinedButton(onClick = { nav.navigate("estate-records") }, modifier = Modifier.fillMaxWidth().heightIn(min = EstateControlHeight)) { Text("All Estate Records") }
                            Box(Modifier.weight(1f)) { EstateDashboard(estateUpdate) }
                        }
                    }
                    composable("estate-records") {
                        Column(Modifier.verticalScroll(rememberScrollState())) {
                            SectionGrid(estateSections) { nav.navigate(it) }
                        }
                    }
                    estateSections.forEach { section ->
                        composable(section.route) { EstateSectionData(section, estateUpdate) }
                    }
                    composable("production") { FieldDetailsScreen(estateUpdate) }
                    composable("upkeep") { UpkeepCultivationScreen(estateUpdate) }
                    composable("dashboard") { OperationalDashboardScreen(estateUpdate) }
                    composable("reports") { ProductionScreen() }
                    composable("tools") { PlanterToolsScreen() }
                    composable("settings") {
                        UserSettingsScreen(themeChoice) {
                            themeChoice = it
                            preferences.edit().putString("theme", it).apply()
                        }
                    }
                    mainSections.filter { it.route != "information" && it.route != "settings" && it.route != "reports" && it.route != "production" && it.route != "upkeep" && it.route != "tools" }.forEach { section ->
                        composable(section.route) {
                            val message = when(section.route) {
                                "settings" -> "Manage your estate information with local file imports and on-device records."
                                else -> section.description
                            }
                            InfoCard(section.title, message)
                        }
                    }
                }
                if (route == "home") {
                    Text("Planters Book", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp), textAlign = TextAlign.Center)
                }
                Text("v${BuildConfig.VERSION_NAME}  •  Local storage\nLast update: ${snapshot?.let { DateFormat.getDateTimeInstance().format(Date(it.updatedAt)) + " · " + it.source } ?: "No data yet"}",
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun SectionGrid(sections: List<Section>, onOpen: (String) -> Unit) {
    EstatePillGrid(sections.size, maxColumns = 2) { index ->
        val section = sections[index]
        val accent = bookAccent(section.route)
        Card(onClick = { onOpen(section.route) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 112.dp).semantics {
                if (section.route == "dashboard") contentDescription = "Open Estate Dashboard"
            },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent, contentColor = Color.White)) {
            Column(Modifier.fillMaxWidth()
                .background(Brush.linearGradient(listOf(accent.start, accent.end)))
                .padding(horizontal = 12.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                EstateModuleIcon(section.route, Modifier.size(28.dp), Color.White)
                Text(section.title, textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(body, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun PlantationBackground(route: String) {
    val base = MaterialTheme.colorScheme.background
    val soft = MaterialTheme.colorScheme.primaryContainer
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
        listOf(soft, base, base))))
}
