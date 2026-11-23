# LEFTickr 📰

A lightweight, transparent, floating RSS news ticker overlay for Android (tested on Android 15), inspired by the classic **Tickr** application on Linux.

Built in Kotlin & Jetpack Compose on Tromjaro Linux as a first-time programming project.

---

## ✨ Features

* **Floating Transparent Bar:** Scrolls smoothly at the bottom of your screen over your launcher and other apps without blocking your view.
* **Clickable Headlines:** Tap any scrolling news headline to open the full article directly in your default browser.
* **Live Customization:** Tap the small gear icon (`⚙️`) on the left of the ticker to open the settings menu (the ticker automatically hides while typing):
  * Custom RSS feed URL + built-in presets (NU.nl, NOS, Omroep Brabant, Tweakers)
  * Adjustable scroll speed (`20` to `150 dp/sec`)
  * Adjustable font size (`8 sp` to `32 sp`)
  * Multiple text colors
  * Font family choices (Sans-Serif, Serif, Monospace) + toggleable **Bold** style
* **Auto-Refresh:** Automatically fetches fresh headlines in the background every 10 minutes.

---

## 📲 Installation (Android 15)

1. Go to the [**Releases**](../../releases) section on the right and download the latest `LEFTickr-v1.0.apk` file to your phone.
2. Open the APK to install it (allow your browser/file manager to install unknown apps if prompted).
3. **Important for Android 15 (Restricted Settings):**
   Because this app is sideloaded and uses a screen overlay (`SYSTEM_ALERT_WINDOW`), Android 15 may initially block the overlay toggle:
   * Open your phone's **Settings > Apps > See all apps > LEFTickr**.
   * Tap the **three dots (`⋮`)** in the top right corner and select **Allow restricted settings**.
4. Open **LEFTickr**, tap **Toestemming Instellen** (Grant Permission), enable *Display over other apps* for LEFTickr, go back, and tap **Opslaan & Balk Starten**.

---

## 🤝 Contributing & Maintenance Status

This is my **very first Android app**, built as a fun learning project. 
* It is shared **"as-is"** under the **GPL-3.0** license—I do not plan to actively maintain or moderate this repository.
* **Forks and Pull Requests are very welcome!** If you would like to improve the code, add more fonts, hide/auto-fade the settings gear icon (e.g., show it on long-press), or add English translations to the UI, feel free to fork this repo and build upon it.

---

## 📜 License

This project is licensed under the **GNU General Public License v3.0** — see the [LICENSE](LICENSE) file for details.
