# Informe de pruebas — SPRINT-10
- Fecha: 2026-10-13 · Ejecutado por: sesión principal OpenCode.
- Comandos y resultados reales:
  1. `.\gradlew.bat :core:test` → **BUILD SUCCESSFUL**; 70/70 (10 nuevos:
     `ParcelaStoreTest` 6/6 + `SessionJsonTest` 4/4; `SessionStoreTest` 8/8 tras
     la refactorización al códec compartido).
  2. `.\gradlew.bat :app:testDebugUnitTest` → **BUILD SUCCESSFUL**; 17/17
     (7 `CoberturaEngineTest` incl. `snapshotRestoreParaReanudar` nuevo).
  3. `.\gradlew.bat :app:assembleDebug` → **BUILD SUCCESSFUL**.
- Incidencias:
  - Room descartado (ver revisión/ADR-05): con KSP 2.0.20-1.0.25 el procesado
    generaba los `_Impl` sin error, pero `compileDebugKotlin` no resolvía las
    clases anotadas (`ParcelaEntity`, `SurcoDb`, …) sin reportar error alguno en
    sus ficheros. Limpieza total (`:app:clean`), reinicio del daemon y
    compilación `--no-daemon` no lo resolvieron. Decisión: persistencia en
    ficheros JSON vía `:core` (contrato ya probado), sin pérdida de requisitos.
  - `ParcelaScreen` con línea colapsada (`)` + `Row`) tras una edición —
    detectado por el compilador, corregido, verde.
- Casos: parcelas guardar/listar/borrar con nombre difícil y rumbo normalizado;
  sesiones crear/guardar/reanudar/terminar con cuadrícula intacta (vía
  `CoberturaEngine.restaurar` testeado); autoguardado cada 15 s + al salir;
  fichero corrupto → vacío sin romper.
- No ejecutado (sin dispositivo): matar la app y reabrir (proceso real).
  Prueba manual: Sprint 12 si hay dispositivo.
- Decisión: PASS.
