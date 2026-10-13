# Revisión — SPRINT-12 (revisor independiente)
- Alcance: suites completas, `assembleDebug`+`assembleRelease` (+firma local),
  verificación `aapt`/`apksigner`, revisión de seguridad, README de instalación.
- Aceptación: ambos APK existen con rutas exactas — CUMPLE. Badging
  `es.surcogps.app` — CUMPLE. Firma válida y trazable al keystore local — CUMPLE.
  Revisión de seguridad PASS — CUMPLE. Docs de instalación/uso — CUMPLE.
- Reglas del proyecto: sin secretos (escaneo limpio), sin TODOs funcionales,
  sin botones muertos, package id estable, mapa con atribución, precisión
  siempre visible, superficie sin duplicar — CUMPLE.
- Historial: 1 commit por sprint aprobado (11 + este), `git push` confirmado por
  sprint — CUMPLE.
- Deuda honesta: prueba en hardware pendiente (bloqueo externo: sin dispositivo
  ni VT-x). No se simula: el informe declara lo NO ejecutado y el README explica
  cómo ejecutarlo.
- Decisión: **PASS**.
