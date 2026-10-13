# Revisión — SPRINT-11 (revisor independiente)
- Alcance: `motor/MotorCampo.kt` (nuevo propietario del tracking), `motor/Politicas.kt`,
  `servicio/TrackingService.kt`, `servicio/ArranqueReceiver.kt`, `SurcoApp`,
  `CampoViewModel` (fachada), manifest (app, servicio `location`, receptor boot),
  `AjustesRepository` (flags), `CampoScreen` (permiso notificaciones),
  `PoliticasTest`.
- Aceptación: tracking con pantalla apagada (FGS tipo `location` + `START_STICKY`,
  motor en ámbito de app) — CUMPLE en código. Notificación persistente con ha,
  XTE y Detener — CUMPLE. Permisos correctos (FINE + FGS + FGS_LOCATION +
  POST_NOTIFICATIONS + BOOT) — CUMPLE. Sin red no se pierde nada (todo local:
  ficheros + DataStore + caché de teselas) — CUMPLE por diseño. Reanudación tras
  muerte/reinicio (flag + permiso + sesión restaurada) — CUMPLE en código.
- Reglas técnicas: el servicio NUNCA reanuda sin permiso (`debeReanudarServicio`
  testeado); `onDestroy` detiene el tracker; sin tracking encubierto — CUMPLE.
- Seguridad/privacidad: notificación en curso (usuario informado); ubicación solo
  con permiso; receptor boot limitado a despertar el servicio que decide — OK.
- Calidad: pantallas intactas (misma API del VM); `MotorCampo` con ámbito propio
  y ficheros en IO; receptor con `goAsync` sin bloquear; intervalo 1 s + retardo
  máx. 2 s (batería documentada).
- Regresiones: 20/20 + 70/70 + APK. Cambios ajenos: ninguno.
- Decisión: **PASS**.
