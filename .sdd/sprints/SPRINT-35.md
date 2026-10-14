# SPRINT-35 — Quitar topes de descarga
- Causa probable (cadena de evidencia): la píldora mostró miles de fallos con
  `HTTP ok`; nada tapa las teselas (ningún overlay pinta gris a pantalla
  completa: es el fondo de «tesela vacía» → NO CARGAN, no se ocultan). Los topes
  manuales (`tileDownloadThreads=4`, `tileDownloadMaxQueueSize=40`, Sprint 29)
  son la única interferencia restante en la ruta de descarga frente a la época
  que renderizaba: al llenarse la cola, osmdroid falla el exceso
  (`…ExceedsMaxQueueSize`), se reintenta, la cola no drena y el móvil arde.
- Alcance: vuelta a los valores de serie (se eliminan los dos setters) + nota.
  Se conserva todo lo demás (guardia por modo, cámara, overlays, watchdog, cola).
- Aceptación: suite verde; build verde. Verificación en campo: la cola debe
  drenar y el terreno aparecer.
- Pruebas: suite + `assembleDebug` (sin lógica nueva que unit-testear).
