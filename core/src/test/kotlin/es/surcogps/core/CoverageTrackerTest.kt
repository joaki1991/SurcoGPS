package es.surcogps.core

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CoverageTrackerTest {

    private val ancla = LatLon(41.0, -4.0)
    private val ancho = 6.0

    /** Pasada recta de 100 m hacia el norte, muestreada cada 10 m. */
    private fun pasadaNorte(tracker: CoverageTracker, esteM: Double = 0.0, largoM: Double = 100.0): Int {
        var nuevas = 0
        var prev = GeoMath.fromEnu(Enu(esteM, 0.0), ancla)
        var d = 10.0
        while (d <= largoM + 1e-9) {
            val p = GeoMath.fromEnu(Enu(esteM, d), ancla)
            nuevas += tracker.addSection(prev, p, ancho)
            prev = p
            d += 10.0
        }
        return nuevas
    }

    @Test
    fun pasadaSimpleMideLargoPorAncho() {
        val t = CoverageTracker(ancla)
        pasadaNorte(t)
        // Teórico 600 m²; el raster de 1 m cuenta celdas tocadas (7 filas para
        // franja ±3 m alineada → 707). Tolerancia documentada en CoverageTracker.
        assertTrue(t.coveredAreaM2 in 600.0..780.0, "area=${t.coveredAreaM2}")
    }

    @Test
    fun segundaPasadaIdenticaCasiNoSuma() {
        val t = CoverageTracker(ancla)
        val primero = pasadaNorte(t)
        assertTrue(primero > 0)
        val segundo = pasadaNorte(t)
        assertTrue(segundo <= primero * 0.02 + 1, "nuevas=$segundo primero=$primero")
    }

    @Test
    fun pasadasAdyacentesSuman() {
        val t = CoverageTracker(ancla)
        pasadaNorte(t, esteM = 0.0)
        val area1 = t.coveredAreaM2
        pasadaNorte(t, esteM = ancho)
        val area2 = t.coveredAreaM2
        assertTrue(area2 > area1 * 1.8, "area1=$area1 area2=$area2")
        assertTrue(area2 < area1 * 2.2, "area1=$area1 area2=$area2")
    }

    @Test
    fun solapeParcialSoloSumaLoNuevo() {
        val t = CoverageTracker(ancla)
        pasadaNorte(t, esteM = 0.0)
        val area1 = t.coveredAreaM2
        // Solape del 50 % (desplazada 3 m con ancho 6).
        val nuevas = pasadaNorte(t, esteM = 3.0)
        val area2 = t.coveredAreaM2
        val incremento = area2 - area1
        assertTrue(incremento > area1 * 0.35 && incremento < area1 * 0.65,
            "area1=$area1 area2=$area2 nuevas=$nuevas")
    }

    @Test
    fun pasadaDiagonalMideIgual() {
        val t = CoverageTracker(ancla)
        val a = GeoMath.fromEnu(Enu(0.0, 0.0), ancla)
        val b = GeoMath.fromEnu(Enu(70.0, 70.0), ancla) // ~99 m
        t.addSection(a, b, ancho)
        // Diagonal no alineada a la cuadrícula → más celdas tocadas (medido 736).
        assertTrue(t.coveredAreaM2 in 600.0..820.0, "area=${t.coveredAreaM2}")
    }

    @Test
    fun puntoAisladoPintaLineaTransversal() {
        val t = CoverageTracker(ancla)
        val nuevas = t.addSection(ancla, ancla, ancho)
        // Tramo de longitud 0: solo el barrido transversal (±3 m → 7 celdas).
        // En la práctica los tramos tienen longitud; los extremos aportan medio
        // disco que aquí no se muestrea (efecto despreciable, documentado).
        assertTrue(nuevas in 6..8, "nuevas=$nuevas")
    }

    @Test
    fun tramoCortoTransversal() {
        val t = CoverageTracker(ancla)
        val a = GeoMath.fromEnu(Enu(0.0, 0.0), ancla)
        val b = GeoMath.fromEnu(Enu(10.0, 0.0), ancla)
        t.addSection(a, b, 2.0)
        assertTrue(t.coveredAreaM2 in 15.0..60.0, "area=${t.coveredAreaM2}")
    }

    @Test
    fun resetVacia() {
        val t = CoverageTracker(ancla)
        pasadaNorte(t)
        assertTrue(t.cellCount > 0)
        t.clear()
        assertEquals(0, t.cellCount)
        assertEquals(0.0, t.coveredAreaM2)
    }

    @Test
    fun snapshotRestoreConservaArea() {
        val t = CoverageTracker(ancla)
        pasadaNorte(t)
        val snap = t.snapshot()
        val t2 = CoverageTracker(ancla)
        t2.restore(snap)
        assertEquals(t.cellCount, t2.cellCount)
        assertEquals(t.coveredAreaM2, t2.coveredAreaM2)
    }

    @Test
    fun hectareas() {
        val t = CoverageTracker(ancla)
        pasadaNorte(t)
        assertTrue(abs(t.coveredHa - t.coveredAreaM2 / 10_000.0) < 1e-12)
    }

    @Test
    fun anchosExtremos() {
        val t = CoverageTracker(ancla)
        val a = GeoMath.fromEnu(Enu(0.0, 0.0), ancla)
        val b = GeoMath.fromEnu(Enu(0.0, 50.0), ancla)
        t.addSection(a, b, 0.5)
        val estrecha = t.coveredAreaM2
        t.clear()
        t.addSection(a, b, 30.0)
        val ancha = t.coveredAreaM2
        // Franja 0.5 m → 2 columnas de celdas (medido 100); 30 m → ~31×51.
        assertTrue(estrecha in 70.0..130.0, "estrecha=$estrecha")
        assertTrue(ancha > estrecha * 10, "ancha=$ancha estrecha=$estrecha")
    }
}
