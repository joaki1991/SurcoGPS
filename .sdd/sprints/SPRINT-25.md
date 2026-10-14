# SPRINT-25 — Informe de cierre + arranque a prueba de fallos
- Objetivo: obtener la traza real del cierre en v1.3.0 y no tumbar el arranque.
- Alcance: `soporte/CrashReporter.kt` (guardar/rotar/leer/listar, handler en
  `SurcoApp` encadenado al del sistema), `ui/AvisoCierre.kt` (ver + compartir
  vía FileProvider + descartar), VM (`informeCierre`, `descartarCierres`),
  `CampoScreen` (tarjeta), manifest provider + `res/xml/ficheros.xml`,
  `MapaCampo` (fallback a `MapView` estándar si el proveedor vigilado falla,
  con nota en el informe), `CrashReporterTest`.
- Aceptación: cierre con traza compartible; arranque degradado antes que muerte;
  suite verde.
- Pruebas: `CrashReporterTest` 4/4 + suite 49/49 + `assembleDebug`.
