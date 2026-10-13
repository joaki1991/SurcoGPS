# Revisión — SPRINT-07 (revisor independiente)
- Alcance: dep OSMDroid 6.1.20, `mapa/FuentesMapa.kt` (+`configPara`), `mapa/MapaCampo.kt`,
  `CampoViewModel` (modo), `CampoScreen` (mapa embebido), `FuentesMapaTest`.
- Aceptación: híbrido (Esri + referencia) / satélite / calles conmutables — CUMPLE
  (lógica verificada por tests). Atribución OSM+Esri siempre visible — CUMPLE.
  Aviso sin conexión + caché OSMDroid — CUMPLE (código; render pendiente).
- Reglas técnicas: fuentes autorizadas sin key, user-agent `SurcoGPS/1.0`, respeto
  documentado de la Tile Usage Policy — CUMPLE.
- Seguridad/privacidad: sin solicitar ubicación aún (Sprint 08); tiles = único
  tráfico tercero, documentado — OK. Sin secretos.
- Calidad: `MapView` con ciclo de vida (onResume/onPause/onDetach); overlay de
  referencia añadido/eliminado al cambiar de modo; firma del constructor verificada
  contra el artefacto real con `javap` — OK.
- Bloqueo externo (no impide PASS de implementación): verificación visual en
  pantalla imposible aquí — `adb devices` vacío y `Win32_Processor.
  VirtualizationFirmwareEnabled=False` (VT-x desactivado; el emulador no arranca).
  Desbloqueo: activar VT-x en BIOS o conectar un dispositivo y reejecutar
  instalación + captura en Sprint 12.
- Regresiones: `:core:test` no afectado (sin cambios en `:core`). Cambios ajenos: ninguno.
- Decisión: **PASS**.
