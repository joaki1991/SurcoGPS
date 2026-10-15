# SPRINT-40 — Sonda de almacenamiento + informe
- Objetivo: distinguir fallo de escritura, base corrupta y disco lleno, en el móvil.
- Alcance: `soporte/SondaAlmacen.kt` (inspección + escritura directa, JVM),
  `EstadoCache` extendido (libreMB, top, sondas), sonda SQL real
  (`SqlTileWriter.save/exists/remove` en `ejecutarDiagnostico`, con limpieza),
  pantalla con sección de almacenamiento, informe con la sección,
  `SondaAlmacenTest`.
- Nada se borra (solo `sonda.tmp` temporal y la tesela de juguete z0, eliminada).
- Aceptación: suite verde; build verde.
- Pruebas: `SondaAlmacenTest` 4/4 + suite 72/72 + `assembleDebug`.
