# SPRINT-14 — Icono personalizado del APK
- Objetivo: icono propio de SurcoGPS (fuente del propietario) en lugar del genérico.
- Alcance: `assets/icono-fuente.png` (fuente 1254×1254), `mipmap-{mdpi…xxxhdpi}/ic_launcher.png`
  (48/72/96/144/192), `drawable-nodpi/ic_launcher_foreground.png` (432),
  `mipmap-anydpi-v26/ic_launcher.xml` + `ic_launcher_round.xml` (fondo blanco),
  `values/colors.xml`, `android:icon` + `android:roundIcon` en manifest.
- Tareas: copiar fuente, generar PNGs (System.Drawing, bicúbico), XMLs, manifest, compilar, aapt.
- Aceptación: `aapt dump badging` muestra `application icon=.../ic_launcher.xml`
  en todas las densidades + `application-label:'SurcoGPS'` — VERIFICADO.
- Pruebas: `assembleDebug` OK; badging real. Render del icono en lanzador: pendiente
  de dispositivo (se declara).
- Formato recomendado futuro: PNG cuadrado 1024×1024 con alfa (sustituir `assets/icono-fuente.png`
  y regenerar con el mismo script).
