package es.surcogps.core

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SessionStoreTest {

    private val ancla = LatLon(41.0, -4.0)

    private fun tmpDir(): File = Files.createTempDirectory("surco-test").toFile()

    private fun sesionMuestra(id: String = "s1"): WorkSession {
        val tracker = CoverageTracker(ancla)
        val track = mutableListOf<LatLon>()
        var prev = GeoMath.fromEnu(Enu(0.0, 0.0), ancla)
        track.add(prev)
        for (d in 10..100 step 10) {
            val p = GeoMath.fromEnu(Enu(0.0, d.toDouble()), ancla)
            tracker.addSection(prev, p, 6.0)
            track.add(p)
            prev = p
        }
        return WorkSession(
            id = id,
            name = "Parcela \"El Olivar\" \\ norte",
            anchor = ancla,
            headingDeg = 0.0,
            widthM = 6.0,
            lateralShiftM = 0.0,
            offset = ImplementOffset(0.0, -2.0),
            track = track,
            cells = tracker.snapshot(),
            startedMs = 1_700_000_000_000L,
            updatedMs = 1_700_000_100_000L,
            finished = false
        )
    }

    @Test
    fun roundTripConservaTodo() {
        val store = FileSessionStore(tmpDir())
        val s = sesionMuestra()
        store.save(s)
        val leida = assertNotNull(store.load("s1"))
        assertEquals(s.id, leida.id)
        assertEquals(s.name, leida.name)
        assertEquals(s.anchor, leida.anchor)
        assertEquals(s.headingDeg, leida.headingDeg)
        assertEquals(s.widthM, leida.widthM)
        assertEquals(s.offset, leida.offset)
        assertEquals(s.track, leida.track)
        assertEquals(s.cells, leida.cells)
        assertEquals(s.finished, leida.finished)
    }

    @Test
    fun nombreConCaracteresEspeciales() {
        val store = FileSessionStore(tmpDir())
        store.save(sesionMuestra())
        assertEquals("Parcela \"El Olivar\" \\ norte", assertNotNull(store.load("s1")).name)
    }

    @Test
    fun listaYBorrado() {
        val store = FileSessionStore(tmpDir())
        assertTrue(store.list().isEmpty())
        store.save(sesionMuestra("a"))
        store.save(sesionMuestra("b").copy(name = "Otra", updatedMs = 1_700_000_200_000L))
        val lista = store.list()
        assertEquals(2, lista.size)
        assertEquals("b", lista[0].id) // más reciente primero
        assertTrue(store.delete("a"))
        assertNull(store.load("a"))
        assertFalse(store.delete("a"))
        assertEquals(1, store.list().size)
    }

    @Test
    fun cargarInexistenteDevuelveNull() {
        val store = FileSessionStore(tmpDir())
        assertNull(store.load("no-existe"))
    }

    @Test
    fun ficheroCorruptoNoRompe() {
        val dir = tmpDir()
        File(dir, "rota.json").writeText("{esto no es json", Charsets.UTF_8)
        val store = FileSessionStore(dir)
        assertNull(store.load("rota"))
        assertTrue(store.list().isEmpty())
    }

    @Test
    fun sesionVacia() {
        val store = FileSessionStore(tmpDir())
        val s = WorkSession(id = "vacia", name = "Vacía", anchor = ancla, headingDeg = 90.0, widthM = 3.0)
        store.save(s)
        val leida = assertNotNull(store.load("vacia"))
        assertEquals(s, leida)
    }

    @Test
    fun interrumpidaSeReanudaYSuma() {
        val dir = tmpDir()
        val store = FileSessionStore(dir)
        val s = sesionMuestra()
        store.save(s)
        // Reanudación: restaurar celdas y seguir trabajando otros 100 m al este.
        val reanudada = assertNotNull(store.load("s1"))
        assertFalse(reanudada.finished)
        val tracker = CoverageTracker(reanudada.anchor)
        tracker.restore(reanudada.cells)
        val antes = tracker.coveredAreaM2
        var prev = GeoMath.fromEnu(Enu(6.0, 0.0), ancla)
        for (d in 10..100 step 10) {
            val p = GeoMath.fromEnu(Enu(6.0, d.toDouble()), ancla)
            tracker.addSection(prev, p, 6.0)
            prev = p
        }
        assertTrue(tracker.coveredAreaM2 > antes * 1.8, "antes=$antes ahora=${tracker.coveredAreaM2}")
        store.save(reanudada.copy(cells = tracker.snapshot(), updatedMs = 1_700_000_200_000L, finished = true))
        val final = assertNotNull(store.load("s1"))
        assertTrue(final.finished)
        assertEquals(tracker.cellCount, final.cells.size)
    }

    @Test
    fun compactacionAgrupaRangos() {
        val store = FileSessionStore(tmpDir())
        // Fila 0: ix 0,1,2,5,6 → rangos [0-2],[5-6].
        val cells = setOf(
            (0L shl 32) xor (0L and 0xFFFFFFFFL),
            (1L shl 32) xor (0L and 0xFFFFFFFFL),
            (2L shl 32) xor (0L and 0xFFFFFFFFL),
            (5L shl 32) xor (0L and 0xFFFFFFFFL),
            (6L shl 32) xor (0L and 0xFFFFFFFFL)
        )
        val rows = store.compactRows(cells)
        assertEquals(listOf(0 to 2, 5 to 6), rows[0])
        // Y el JSON hace round-trip de esas celdas.
        val s = WorkSession(id = "c", name = "c", anchor = ancla, headingDeg = 0.0,
            widthM = 6.0, cells = cells)
        store.save(s)
        assertEquals(cells, assertNotNull(store.load("c")).cells)
    }
}
