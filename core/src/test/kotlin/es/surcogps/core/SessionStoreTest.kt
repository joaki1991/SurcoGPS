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
    fun resumenCoincideConLaCargaCompleta() {
        val dir = tmpDir()
        val store = FileSessionStore(dir)
        val s = sesionMuestra("r1")
        store.save(s)
        val r = assertNotNull(SessionResumen.leer(File(dir, "r1.json").readText(Charsets.UTF_8)))
        assertEquals(s.id, r.id)
        assertEquals(s.name, r.name)
        assertEquals(s.updatedMs, r.updatedMs)
        assertEquals(s.finished, r.finished)
        assertEquals(s.cells.size, r.cellCount)
        // El listado debe usar el resumen y coincidir con la carga real.
        val resumenListado = store.list().single()
        assertEquals(s.cells.size, resumenListado.cellCount)
    }

    @Test
    fun resumenConCeldasVaciasYNegativas() {
        val dir = tmpDir()
        val store = FileSessionStore(dir)
        val cells = setOf(
            (0L shl 32) xor (-5L and 0xFFFFFFFFL),
            (1L shl 32) xor (-5L and 0xFFFFFFFFL),
            (-7L shl 32) xor (3L and 0xFFFFFFFFL)
        )
        store.save(WorkSession(id = "neg", name = "Neg", anchor = ancla, headingDeg = 0.0, widthM = 6.0, cells = cells))
        store.save(WorkSession(id = "vac", name = "Vacía", anchor = ancla, headingDeg = 0.0, widthM = 6.0))
        val porId = store.list().associateBy { it.id }
        assertEquals(3, assertNotNull(porId["neg"]).cellCount)
        assertEquals(0, assertNotNull(porId["vac"]).cellCount)
    }

    @Test
    fun resumenDeFicheroCorruptoEsNull() {
        assertNull(SessionResumen.leer("{esto no es json"))
        assertNull(SessionResumen.leer(""))
        assertNull(SessionResumen.leer("{\"name\":\"sin id\"}"))
    }

    @Test
    fun guardadoNoDejaTemporales() {
        val dir = tmpDir()
        val store = FileSessionStore(dir)
        store.save(sesionMuestra("t1"))
        store.save(sesionMuestra("t1").copy(name = "Segunda"))
        assertEquals("Segunda", assertNotNull(store.load("t1")).name)
        assertTrue(dir.listFiles()!!.none { it.name.endsWith(".tmp") })
    }

    @Test
    fun indiceEvitaLeerElCuerpo() {
        val dir = tmpDir()
        val store = FileSessionStore(dir)
        store.save(sesionMuestra("i1"))
        val indice = File(dir, "i1.meta")
        assertTrue(indice.isFile)
        assertTrue(indice.length() < 300, "índice de ${indice.length()} B")
        // El listado sigue siendo correcto leyendo solo el índice.
        val r = store.list().single()
        assertEquals("i1", r.id)
        assertEquals(sesionMuestra("i1").cells.size, r.cellCount)
    }

    @Test
    fun sesionAntiguaSinIndiceSeMigra() {
        val dir = tmpDir()
        val store = FileSessionStore(dir)
        store.save(sesionMuestra("legacy"))
        File(dir, "legacy.meta").delete() // simula sesión de versión anterior
        val r = store.list().single()
        assertEquals("legacy", r.id)
        assertEquals(sesionMuestra("legacy").cells.size, r.cellCount)
        assertTrue(File(dir, "legacy.meta").isFile, "el índice debe quedar escrito")
    }

    @Test
    fun indiceIlegibleSeReconstruye() {
        val dir = tmpDir()
        val store = FileSessionStore(dir)
        store.save(sesionMuestra("roto"))
        File(dir, "roto.meta").writeText("{no es json", Charsets.UTF_8)
        val r = store.list().single()
        assertEquals("roto", r.id)
        assertEquals(sesionMuestra("roto").cells.size, r.cellCount)
    }

    @Test
    fun borrarEliminaTambienElIndice() {
        val dir = tmpDir()
        val store = FileSessionStore(dir)
        store.save(sesionMuestra("b1"))
        assertTrue(store.delete("b1"))
        assertFalse(File(dir, "b1.meta").exists())
        assertTrue(store.list().isEmpty())
    }

    @Test
    fun compactacionAgrupaRangos() {        val store = FileSessionStore(tmpDir())
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

class SessionJsonTest {

    private fun celda(ix: Int, iy: Int): Long =
        (ix.toLong() shl 32) xor (iy.toLong() and 0xFFFFFFFFL)

    @Test
    fun celdasRoundTrip() {
        val cells = setOf(
            celda(0, 0), celda(1, 0), celda(2, 0), celda(5, 0),
            celda(-3, -7), celda(2_000_000, 1_000_000)
        )
        assertEquals(cells, SessionJson.decodeCells(SessionJson.encodeCells(cells)))
    }

    @Test
    fun celdasVacias() {
        assertEquals("{}", SessionJson.encodeCells(emptySet()))
        assertEquals(emptySet(), SessionJson.decodeCells("{}"))
    }

    @Test
    fun trackRoundTrip() {
        val track = listOf(LatLon(41.0, -4.0), LatLon(41.5, -4.25), LatLon(-12.34, 179.9))
        assertEquals(track, SessionJson.decodeTrack(SessionJson.encodeTrack(track)))
        assertEquals(emptyList(), SessionJson.decodeTrack(SessionJson.encodeTrack(emptyList())))
    }

    @Test
    fun codecExpandCompact() {
        val rows = mapOf(0 to listOf(0 to 2, 5 to 6), -1 to listOf(10 to 10))
        val cells = CellCodec.expand(rows)
        assertEquals(6, cells.size)
        assertEquals(rows, CellCodec.compact(cells))
        assertEquals(emptyMap(), CellCodec.compact(emptySet()))
    }
}
