# SPRINT-08 — GPS real + banderillero
- Objetivo: posición/velocidad/precisión reales y guía visual por XTE.
- Alcance: FusedLocationProvider alta precisión, GnssStatus/NMEA diagnóstico,
  permisos + rationale, HUD (velocidad, precisión, XTE, línea activa, flechas),
  compatibilidad GNSS externo documentada.
- Aceptación: precisión visible siempre; XTE coincide con core; mock-location verificado.
- Pruebas: mock route en emulador + test integrado guidance+filter.
