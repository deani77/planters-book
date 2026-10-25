package com.selendang3.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
import kotlin.math.cos
import kotlin.math.sin

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
    MaterialTheme(colorScheme = estateColorScheme(dark)) {
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
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    textAlign = if (route == "home") TextAlign.Center else TextAlign.Start)
                NavHost(nav, startDestination = "home", modifier = Modifier.weight(1f)) {
                    composable("home") {
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { nav.navigate("dashboard") },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 88.dp).semantics { contentDescription = "Open Estate Dashboard" },
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF21523D), contentColor = Color.White)) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    EstateModuleIcon("dashboard", Modifier.size(28.dp), Color.White)
                                    Text("Estate Dashboard", style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold)
                                }
                            }
                            SectionGrid(mainSections) { nav.navigate(it) }
                        }
                    }
                    composable("information") {
                        Column {
                            OutlinedButton(onClick = { nav.navigate("estate-records") }) { Text("All Estate Records") }
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
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        sections.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { section ->
                    FilledTonalButton(onClick = { onOpen(section.route) },
                        modifier = Modifier.weight(1f).heightIn(min = 88.dp),
                        shape = RoundedCornerShape(18.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            EstateModuleIcon(section.route, Modifier.size(26.dp), MaterialTheme.colorScheme.primary)
                            Text(section.title, textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
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
    // Low-contrast vector palm canopy; the same semantic palette applies to every route.
    val base = MaterialTheme.colorScheme.background
    val accent = MaterialTheme.colorScheme.primary
    val soft = MaterialTheme.colorScheme.secondaryContainer
    Box(Modifier.fillMaxSize().background(base).background(Brush.verticalGradient(listOf(soft.copy(alpha = 0.45f), base)))) {
        Canvas(Modifier.fillMaxSize()) {
            for (row in 0..7) for (col in 0..4) {
                val center = Offset(size.width * (col + 0.2f + (row % 2) * 0.4f) / 4f, size.height * row / 7f)
                val radius = size.width * 0.11f
                for (leaf in 0..7) {
                    val angle = leaf * Math.PI / 4
                    drawLine(accent.copy(alpha = if (route == "home") 0.055f else 0.02f), center,
                        center + Offset((cos(angle) * radius).toFloat(), (sin(angle) * radius).toFloat()),
                        strokeWidth = 12f)
                }
                drawCircle(accent.copy(alpha = if (route == "home") 0.06f else 0.02f), radius * 0.18f, center)
            }
        }

    }
}

