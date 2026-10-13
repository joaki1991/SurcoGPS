# SPRINT-11 — Segundo plano y robustez
- Objetivo: tracking con pantalla apagada y recuperación ante fallos.
- Alcance: ForegroundService tipo `location` + notificación (ha, XTE, parar),
  permisos FGS, gestión batería (intervalos), buffer sin red, reintento tras reinicio
  (BOOT_COMPLETED), manejo de pérdida de señal.
- Aceptación: 5 min con pantalla apagada registra sin huecos; reinicio documentado.
- Pruebas: prueba manual cronometrada + test de buffer.
