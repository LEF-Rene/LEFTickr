package com.lefandroid.leftickr

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val vanTandwiel = intent.getBooleanExtra("OPEN_INSTELLINGEN", false)
        if (vanTandwiel) {
            stopService(Intent(this, TickrOverlayService::class.java))
        } else if (Settings.canDrawOverlays(this)) {
            startTickrService()
            finish()
            return
        }

        setContent {
            InstellingenScherm(
                onStartOfUpdate = {
                    if (Settings.canDrawOverlays(this)) {
                        stopService(Intent(this, TickrOverlayService::class.java))
                        startTickrService()
                        finish()
                    } else {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName")
                        )
                        startActivity(intent)
                    }
                },
                onStopBalk = {
                    stopService(Intent(this, TickrOverlayService::class.java))
                    finish()
                }
            )
        }
    }

    private fun startTickrService() {
        val serviceIntent = Intent(this, TickrOverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }
}

@Composable
fun InstellingenScherm(onStartOfUpdate: () -> Unit, onStopBalk: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("tickr_prefs", Context.MODE_PRIVATE) }

    var snelheid by remember { mutableFloatStateOf(prefs.getFloat("snelheid", 60f)) }
    var tekstGrootte by remember { mutableFloatStateOf(prefs.getFloat("grootte", 18f)) }
    var rssUrl by remember { mutableStateOf(prefs.getString("rss_url", "https://nu.nl") ?: "") }
    var gekozenKleur by remember { mutableIntStateOf(prefs.getInt("kleur", Color.White.toArgb())) }
    var gekozenFont by remember { mutableStateOf(prefs.getString("font", "Standaard") ?: "Standaard") }
    var isVetgedrukt by remember { mutableStateOf(prefs.getBoolean("bold", false)) } // Standaard NIET bold!
    var heeftToestemming by remember { mutableStateOf(Settings.canDrawOverlays(context)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF181818))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("LEFTickr Instellingen", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)

        if (!heeftToestemming) {
            Text("Geef eerst toestemming om over andere apps te tekenen.", color = Color(0xFFFFB74D), fontSize = 14.sp)
        }

        Text("Kies een nieuwsbron (of typ eigen URL):", color = Color.LightGray, fontSize = 13.sp)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val presets = listOf(
                "NU.nl" to "https://nu.nl",
                "NOS" to "https://nos.nl",
                "Brabant" to "https://omroepbrabant.nl",
                "Tweakers" to "https://feedburner.com"
            )
            presets.forEach { (naam, url) ->
                FilterChip(
                    selected = rssUrl == url,
                    onClick = { rssUrl = url },
                    label = { Text(naam) }
                )
            }
        }

        OutlinedTextField(
            value = rssUrl,
            onValueChange = { rssUrl = it },
            label = { Text("RSS Feed URL", color = Color.LightGray) },
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Text("Scrollsnelheid: ${snelheid.toInt()}", color = Color.White)
        Slider(value = snelheid, onValueChange = { snelheid = it }, valueRange = 20f..150f)

        // Lettergrootte nu vanaf 8 sp!
        Text("Lettergrootte: ${tekstGrootte.toInt()} sp", color = Color.White)
        Slider(value = tekstGrootte, onValueChange = { tekstGrootte = it }, valueRange = 8f..32f)

        Text("Tekstkleur:", color = Color.White)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(Color.White, Color.Yellow, Color.Cyan, Color.Green, Color(0xFFFF5252)).forEach { kleur ->
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(kleur, CircleShape)
                        .clickable { gekozenKleur = kleur.toArgb() }
                )
            }
        }

        Text("Lettertype & Stijl:", color = Color.White)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Standaard", "Schreef", "Typemachine").forEach { naam ->
                FilterChip(selected = gekozenFont == naam, onClick = { gekozenFont = naam }, label = { Text(naam) })
            }
            FilterChip(
                selected = isVetgedrukt,
                onClick = { isVetgedrukt = !isVetgedrukt },
                label = { Text("Vet (Bold)") }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                prefs.edit()
                    .putFloat("snelheid", snelheid)
                    .putFloat("grootte", tekstGrootte)
                    .putString("rss_url", rssUrl.trim())
                    .putInt("kleur", gekozenKleur)
                    .putString("font", gekozenFont)
                    .putBoolean("bold", isVetgedrukt)
                    .apply()
                heeftToestemming = Settings.canDrawOverlays(context)
                onStartOfUpdate()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (heeftToestemming) "Opslaan & Balk Starten" else "Toestemming Instellen")
        }


        OutlinedButton(onClick = onStopBalk, modifier = Modifier.fillMaxWidth()) {
            Text("Zwevende Balk Sluiten", color = Color.Red)
        }
    }
}
