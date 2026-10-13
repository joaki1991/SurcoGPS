# Revisión — SPRINT-08 (revisor independiente)
- Alcance: dep play-services-location, `gps/LocationTracker.kt`, `gps/GuiadoPipeline.kt`
  (+`EstadoGuiado`), `CampoViewModel` (guiado, ancla, seguir, NMEA), `CampoScreen`
  (HUD + permiso + ancla), `MapaCampo` (seguir), ancla en DataStore, `GuiadoPipelineTest`.
- Aceptación: velocidad/precisión visibles SIEMPRE (HUD) — CUMPLE. XTE + línea + flecha
  con fix válido — CUMPLE (pipeline testeado). Puerta configurable — CUMPLE.
  Compatibilidad GNSS externo documentada (mock provider + NMEA) — CUMPLE.
- Reglas técnicas: precisión estimada obligatoria en `Fix`; filtro antes que guía;
  offset del apero aplicado antes del XTE; heading del vehículo por desplazamiento
  con fallback al rumbo — CUMPLE.
- Seguridad/privacidad: permiso FINE solicitado con rationale implícito (botón
  explícito, GPS parado hasta conceder); `MissingPermission` suprimido solo tras
  verificación en el llamador; NMEA tras try/catch; `onCleared` detiene — OK.
- Calidad: `LocationTracker` con start/stop idempotentes; `GuiadoPipeline` puro y
  testeado; DataStore valida rangos; sin botones muertos (permiso/ancla/seguir
  funcionales; ancla deshabilitada sin posición con motivo).
- Regresiones: suite app 10/10; `:core` intacto. Cambios ajenos: ninguno.
- Decisión: **PASS**.
