package com.lefandroid.leftickr

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.*
import java.net.URL

data class NieuwsItem(val titel: String, val link: String)

class TickrOverlayService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    private lateinit var windowManager: WindowManager
    private var composeView: ComposeView? = null
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val nieuwsLijstState = mutableStateListOf<NieuwsItem>()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        startMelding()
        toonZwevendeBalk()
        startNieuwsOphaler()
    }

    private fun startNieuwsOphaler() {
        val prefs = getSharedPreferences("tickr_prefs", Context.MODE_PRIVATE)
        val rssUrl = prefs.getString("rss_url", "https://nu.nl")?.trim()
            ?.ifEmpty { "https://nu.nl" } ?: "https://nu.nl"

        serviceScope.launch {
            while (isActive) {
                val opgehaald = haalNieuwsOp(rssUrl)
                nieuwsLijstState.clear()
                nieuwsLijstState.addAll(opgehaald)
                delay(10 * 60 * 1000L)
            }
        }
    }

    private fun startMelding() {
        val kanaalId = "tickr_kanaal"
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val kanaal = NotificationChannel(kanaalId, "LEFTickr Balk", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(kanaal)
            Notification.Builder(this, kanaalId)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        val melding = builder
            .setContentTitle("LEFTickr is actief")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
        startForeground(1, melding)
    }

    @OptIn(ExperimentalFoundationApi::class)
    private fun toonZwevendeBalk() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val prefs = getSharedPreferences("tickr_prefs", Context.MODE_PRIVATE)
        val snelheid = prefs.getFloat("snelheid", 60f)
        val grootte = prefs.getFloat("grootte", 18f)
        val tekstKleur = Color(prefs.getInt("kleur", Color.White.toArgb()))
        val font = when (prefs.getString("font", "Standaard")) {
            "Schreef" -> FontFamily.Serif
            "Typemachine" -> FontFamily.Monospace
            else -> FontFamily.SansSerif
        }
        val gewicht = if (prefs.getBoolean("bold", false)) FontWeight.Bold else FontWeight.Normal

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.START
            y = 20
        }

        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@TickrOverlayService)
            setViewTreeViewModelStoreOwner(this@TickrOverlayService)
            setViewTreeSavedStateRegistryOwner(this@TickrOverlayService)

            setContent {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Transparent)
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 6.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .clickable {
                                val intent = Intent(this@TickrOverlayService, MainActivity::class.java).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    putExtra("OPEN_INSTELLINGEN", true)
                                }
                                startActivity(intent)
                            }
                            .padding(6.dp)
                    ) { Text("⚙️", fontSize = 14.sp) }

                    Box(modifier = Modifier.weight(1f)) {
                        if (nieuwsLijstState.isEmpty()) {
                            Text(
                                text = "Nieuws ophalen...",
                                color = tekstKleur,
                                fontSize = grootte.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .basicMarquee(iterations = Int.MAX_VALUE, velocity = snelheid.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                nieuwsLijstState.forEach { item ->
                                    Text(
                                        text = item.titel,
                                        color = tekstKleur,
                                        fontSize = grootte.sp,
                                        fontFamily = font,
                                        fontWeight = gewicht,
                                        maxLines = 1,
                                        modifier = Modifier
                                            .clickable {
                                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(item.link)).apply {
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                startActivity(browserIntent)
                                            }
                                            .padding(horizontal = 8.dp)
                                    )
                                    Text(" +++ ", color = Color.Red, fontSize = grootte.sp, fontWeight = gewicht)
                                }
                            }
                        }
                    }
                }
            }
        }
        windowManager.addView(composeView, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        composeView?.let { windowManager.removeView(it) }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

suspend fun haalNieuwsOp(feedUrl: String): List<NieuwsItem> = withContext(Dispatchers.IO) {
    val resultaten = mutableListOf<NieuwsItem>()
    try {
        val tekst = URL(feedUrl).readText()
        val ruweItems = tekst.split("<item>").drop(1).take(12)
        for (blok in ruweItems) {
            val titel = blok.substringAfter("<title>", "").substringBefore("</title>", "")
                .replace("<![CDATA[", "").replace("]]>", "")
                .replace("&amp;", "&").replace("&quot;", "\"").replace("&#039;", "'").trim()
            val link = blok.substringAfter("<link>", "").substringBefore("</link>", "")
                .replace("<![CDATA[", "").replace("]]>", "").trim()
            if (titel.isNotEmpty()) resultaten.add(NieuwsItem(titel, link.ifEmpty { feedUrl }))
        }
        if (resultaten.isEmpty()) {
            resultaten.add(NieuwsItem("Geen items gevonden op $feedUrl", feedUrl))
        }
    } catch (e: Exception) {
        resultaten.add(NieuwsItem("Fout bij feed: ${e.localizedMessage}", feedUrl))
    }
    resultaten
}
