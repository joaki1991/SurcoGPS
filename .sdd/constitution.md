# Constitución SDD — SurcoGPS

## Principios
1. **Evidencia sobre afirmación.** Nada está "probado", "compilado" o "publicado"
   sin comando ejecutado, salida real y artefacto verificable registrados en
   `.sdd/test-reports/` y `.sdd/reviews/`.
2. **Dominio agrícola real.** El tractor trabaja metros sobre el terreno, no grados.
   Toda geometría productiva vive en un marco métrico local (ENU tangente).
3. **Honestidad metrológica.** El GPS de móvil es métrico, no centimétrico.
   La precisión estimada siempre es visible; el filtrado es explícito y configurable.
4. **Trabajo = recorrido válido.** Solo las ubicaciones filtradas generan cobertura.
   La proximidad a una guía no produce trabajo.
5. **Superficie sin duplicados.** La hectárea facturable se cuenta una vez aunque se
   pise dos veces (raster de celdas).
6. **Local primero.** Sin cuentas, sin nube, sin telemetría. Parcela y sesión viven
   en el dispositivo y sobreviven a cierres y reinicios.
7. **Mapa legal.** Solo fuentes con licencia que permita el uso (OSM + Esri),
   con atribución y respeto a la Tile Usage Policy.
8. **Trazabilidad SDD.** Cada cambio de código cuelga de un sprint con spec,
   tests, review y decisión PASS/BLOCKED/FAIL.

## Reglas de calidad (puertas)
- Un sprint solo es PASS si: implementa todo su alcance, los tests definidos pasan
  en máquina local, el diff ha sido revisado entero, no hay secretos ni código
  muerto funcional, y la documentación está sincronizada.
- BLOCKED solo es válido ante causa externa verificable (p. ej. sin SDK, sin red,
  permiso denegado por el SO) con evidencia del error y plan de desbloqueo.
- FAIL obliga a corregir y repetir pruebas antes de commitear.
- Prohibido: restar lat/lon en grados para distancias, colorear sin fix válido,
  prometer precisión centimétrica, commitear keystores o API keys, reescribir
  historia con `--force` / `reset --hard`.
