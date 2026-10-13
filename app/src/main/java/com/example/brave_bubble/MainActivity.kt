package com.example.brave_bubble

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

val UbuntuFont = FontFamily(Font(R.font.ubuntu))

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) checkOverlayAndStart()
    }
    private val infoLocationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            if (grants.values.any { it }) {
                getSharedPreferences("BubblePrefs", Context.MODE_PRIVATE)
                    .edit().putInt("bubble_mode", 4).apply()
            } else {
                Toast.makeText(this, "Location permission is needed to show altitude.", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val prefs = getSharedPreferences("BubblePrefs", Context.MODE_PRIVATE)
            var isDarkMode by remember { mutableStateOf(prefs.getBoolean("is_dark_mode", true)) }

            MaterialTheme(
                colorScheme = if (isDarkMode) darkColorScheme() else lightColorScheme(),
                typography = Typography(
                    headlineMedium = MaterialTheme.typography.headlineMedium.copy(fontFamily = UbuntuFont),
                    titleLarge = MaterialTheme.typography.titleLarge.copy(fontFamily = UbuntuFont),
                    titleMedium = MaterialTheme.typography.titleMedium.copy(fontFamily = UbuntuFont),
                    bodyMedium = MaterialTheme.typography.bodyMedium.copy(fontFamily = UbuntuFont),
                    labelLarge = MaterialTheme.typography.labelLarge.copy(fontFamily = UbuntuFont)
                )
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    BubbleSettingsScreen(
                        isDarkMode = isDarkMode,
                        onThemeToggle = { 
                            isDarkMode = it
                            prefs.edit().putBoolean("is_dark_mode", it).apply()
                        },
                        onLaunchClick = { checkAllPermissionsAndStart() },
                        onInfoModeSelected = { selectInfoMode() },
                        context = this
                    )
                }
            }
        }
    }

    private fun selectInfoMode() {
        getSharedPreferences("BubblePrefs", Context.MODE_PRIVATE).edit().putInt("bubble_mode", 4).apply()
        val hasLocationPermission =
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (hasLocationPermission) {
            getSharedPreferences("BubblePrefs", Context.MODE_PRIVATE).edit().putInt("bubble_mode", 4).apply()
        } else {
            infoLocationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
            )
        }
    }

    private fun checkAllPermissionsAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        checkOverlayAndStart()
    }

    private fun checkOverlayAndStart() {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        } else {
            try {
                startForegroundService(Intent(this, BubbleService::class.java))
            } catch (e: Exception) {
                Toast.makeText(this, "Launch error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

@Composable
fun BubbleSettingsScreen(
    isDarkMode: Boolean,
    onThemeToggle: (Boolean) -> Unit,
    onLaunchClick: () -> Unit,
    onInfoModeSelected: () -> Unit,
    context: Context
) {
    val prefs = context.getSharedPreferences("BubblePrefs", Context.MODE_PRIVATE)
    var currentMode by remember { mutableIntStateOf(prefs.getInt("bubble_mode", 0)) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Brave Bubble", style = MaterialTheme.typography.headlineMedium)
            IconButton(onClick = { onThemeToggle(!isDarkMode) }) {
                Icon(if (isDarkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode, contentDescription = "Toggle Theme")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onLaunchClick, modifier = Modifier.fillMaxWidth().height(55.dp)) {
            Text("Launch / Restart Bubble", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(modifier = Modifier.height(24.dp))

        // --- STACKED MODE ACCORDIONS ---
        Text("Modules", style = MaterialTheme.typography.titleLarge, modifier = Modifier.align(Alignment.Start))
        Spacer(modifier = Modifier.height(8.dp))

        // 1. Data Monitor
        ExpandableModeCard(
            title = "Data Monitor",
            icon = Icons.Filled.Wifi,
            isActive = currentMode == 0,
            onActivate = { currentMode = 0; prefs.edit().putInt("bubble_mode", 0).apply() }
        ) { DataMonitorSettings(prefs, context) }

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Timezone Clock
        ExpandableModeCard(
            title = "World Clock",
            icon = Icons.Filled.Schedule,
            isActive = currentMode == 1,
            onActivate = { currentMode = 1; prefs.edit().putInt("bubble_mode", 1).apply() }
        ) { TimezoneSettings(prefs) }

        Spacer(modifier = Modifier.height(8.dp))

        // 3. Smart Battery
        ExpandableModeCard(
            title = "Smart Battery",
            icon = Icons.Filled.BatteryChargingFull,
            isActive = currentMode == 2,
            onActivate = { currentMode = 2; prefs.edit().putInt("bubble_mode", 2).apply() }
        ) { Text("Battery settings: Currently auto-tracks live percentage.", modifier = Modifier.padding(16.dp)) }

        Spacer(modifier = Modifier.height(8.dp))

        // 4. Weather Station
        ExpandableModeCard(
            title = "Weather Station",
            icon = Icons.Filled.Cloud,
            isActive = currentMode == 3,
            onActivate = { currentMode = 3; prefs.edit().putInt("bubble_mode", 3).apply() }
        ) { OpenWeatherSettings() }

        Spacer(modifier = Modifier.height(8.dp))

        ExpandableModeCard(
            title = "Info Mode",
            icon = Icons.Filled.Info,
            isActive = currentMode == 4,
            onActivate = {
                onInfoModeSelected()
                currentMode = 4
            }
        ) { InfoModeSettings(prefs) }

        Spacer(modifier = Modifier.height(24.dp))

        // --- GLOBAL APPEARANCE SETTINGS ---
        GlobalAppearanceSettings(prefs)
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun ExpandableModeCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, isActive: Boolean, onActivate: () -> Unit, content: @Composable () -> Unit) {
    var expanded by remember { mutableStateOf(isActive) }
    
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(12.dp)) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = isActive, onClick = onActivate)
                Icon(icon, contentDescription = null, modifier = Modifier.padding(horizontal = 8.dp), tint = MaterialTheme.colorScheme.primary)
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Icon(if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, contentDescription = "Expand")
            }
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                    content()
                }
            }
        }
    }
}

@Composable
fun GlobalAppearanceSettings(prefs: android.content.SharedPreferences) {
    var size by remember { mutableFloatStateOf(prefs.getInt("bubble_size", 220).toFloat()) }
    var opacity by remember { mutableFloatStateOf(prefs.getFloat("bubble_opacity", 0.8f).coerceIn(0f, 1f)) }
    var fontSizePercent by remember { mutableFloatStateOf(prefs.getFloat("bubble_font_scale_percent", 100f).coerceIn(50f, 200f)) }
    var fontOpacity by remember { mutableFloatStateOf(prefs.getFloat("bubble_font_opacity", 1f).coerceIn(0f, 1f)) }
    var shape by remember { mutableIntStateOf(prefs.getInt("bubble_shape", 0).coerceIn(0, 2)) }
    var showBorder by remember { mutableStateOf(prefs.getBoolean("show_border", true)) }
    var bgColor by remember { mutableStateOf(Color(prefs.getInt("bg_color_int", android.graphics.Color.parseColor("#000080")))) }
    var borderColor by remember { mutableStateOf(Color(prefs.getInt("border_color_int", android.graphics.Color.parseColor("#50C878")))) }

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Global Appearance", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                Text("Show Border Ring", modifier = Modifier.weight(1f), fontFamily = UbuntuFont)
                Switch(checked = showBorder, onCheckedChange = { showBorder = it; prefs.edit().putBoolean("show_border", it).apply() })
            }

            Text("Size: ${size.toInt()}px")
            Slider(value = size, onValueChange = { size = it; prefs.edit().putInt("bubble_size", it.toInt()).apply() }, valueRange = 100f..400f)

            Text("Background opacity: ${(opacity * 100).toInt()}%")
            Slider(value = opacity, onValueChange = { opacity = it; prefs.edit().putFloat("bubble_opacity", it).apply() }, valueRange = 0f..1f)

            Text("Font size: ${fontSizePercent.toInt()}%")
            Slider(
                value = fontSizePercent,
                onValueChange = { fontSizePercent = it; prefs.edit().putFloat("bubble_font_scale_percent", it).apply() },
                valueRange = 50f..200f
            )

            Text("Font opacity: ${(fontOpacity * 100).toInt()}%")
            Slider(
                value = fontOpacity,
                onValueChange = { fontOpacity = it; prefs.edit().putFloat("bubble_font_opacity", it).apply() },
                valueRange = 0f..1f
            )

            Text("Bubble shape")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf("Circle", "Rounded", "Square").forEachIndexed { index, label ->
                    SegmentedButton(
                        selected = shape == index,
                        onClick = { shape = index; prefs.edit().putInt("bubble_shape", index).apply() },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = 3)
                    ) {
                        Text(label)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            RainbowColorPicker("Background Color") { newColor -> bgColor = newColor; prefs.edit().putInt("bg_color_int", newColor.toArgb()).apply() }

            if (showBorder) {
                Spacer(modifier = Modifier.height(16.dp))
                RainbowColorPicker("Ring Color") { newColor -> borderColor = newColor; prefs.edit().putInt("border_color_int", newColor.toArgb()).apply() }
            }
        }
    }
}

@Composable
fun OpenWeatherSettings() {
    val secretPrefs = androidx.compose.ui.platform.LocalContext.current
        .getSharedPreferences("OpenWeatherSecrets", Context.MODE_PRIVATE)
    var apiKey by remember { mutableStateOf(secretPrefs.getString("openweather_api_key", "") ?: "") }
    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = apiKey,
            onValueChange = {
                apiKey = it
                secretPrefs.edit().putString("openweather_api_key", it.trim()).apply()
            },
            label = { Text("OpenWeather API key") },
            singleLine = true,
            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Text("Saved on this device for OpenWeather integration.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun InfoModeSettings(prefs: android.content.SharedPreferences) {
    val infoItems = listOf(
        "Altitude" to "info_show_altitude",
        "Lunar phase" to "info_show_lunar_phase",
        "Sun-Earth distance" to "info_show_sun_distance",
        "Moon-Earth distance" to "info_show_moon_distance",
        "Sunrise/sunset and daylight" to "info_show_sunrise_sunset"
    )
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        infoItems.forEach { (label, key) ->
            var checked by remember { mutableStateOf(prefs.getBoolean(key, true)) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = { checked = it; prefs.edit().putBoolean(key, it).apply() }
                )
                Text(label)
            }
        }
    }
}

@Composable
fun DataMonitorSettings(prefs: android.content.SharedPreferences, context: Context) {
    var dataLimitInput by remember { mutableStateOf(prefs.getFloat("data_limit_gb", 5.0f).toString()) }
    var totalBytesUsed by remember { mutableLongStateOf(prefs.getLong("total_bytes_used", 0L)) }
    val usedGb = totalBytesUsed.toDouble() / (1024 * 1024 * 1024)

    Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = dataLimitInput,
                onValueChange = { dataLimitInput = it },
                label = { Text("Limit (GB)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                dataLimitInput.toFloatOrNull()?.let {
                    prefs.edit().putFloat("data_limit_gb", it).apply()
                    Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                }
            }, modifier = Modifier.padding(top = 6.dp)) { Text("Save") }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Used: ${String.format("%.2f", usedGb)} GB")
            Button(onClick = { totalBytesUsed = 0L; prefs.edit().putLong("total_bytes_used", 0L).apply() }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Reset") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimezoneSettings(prefs: android.content.SharedPreferences) {
    val timezones = listOf("Africa/Conakry", "Asia/Kathmandu", "Asia/Dubai", "UTC", "Europe/London", "America/New_York", "Asia/Tokyo")
    var selectedTz by remember { mutableStateOf(prefs.getString("clock_timezone", "Africa/Conakry") ?: "Africa/Conakry") }
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(16.dp)) {
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(selectedTz)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                timezones.forEach { tz ->
                    DropdownMenuItem(
                        text = { Text(tz) },
                        onClick = { selectedTz = tz; prefs.edit().putString("clock_timezone", tz).apply(); expanded = false }
                    )
                }
            }
        }
    }
}

@Composable
fun RainbowColorPicker(label: String, onColorSelected: (Color) -> Unit) {
    val rainbowColors = listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
    val brush = Brush.horizontalGradient(rainbowColors)

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(45.dp)
                .background(brush, RoundedCornerShape(8.dp))
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val x = change.position.x.coerceIn(0f, size.width.toFloat())
                        val hue = (x / size.width) * 360f
                        onColorSelected(Color.hsv(hue, 1f, 1f))
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val x = offset.x.coerceIn(0f, size.width.toFloat())
                        val hue = (x / size.width) * 360f
                        onColorSelected(Color.hsv(hue, 1f, 1f))
                    }
                }
        )
    }
}