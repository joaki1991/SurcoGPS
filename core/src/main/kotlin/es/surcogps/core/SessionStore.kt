package es.surcogps.core

import java.io.File
import java.nio.file.StandardCopyOption

/**
 * Sesión de trabajo: todo lo necesario para reanudar sin perder progreso.
 * Las celdas se guardan compactadas por filas (rangos de ix por cada iy).
 */
data class WorkSession(
    val id: String,
    val name: String,
    val anchor: LatLon,
    val headingDeg: Double,
    val widthM: Double,
    val lateralShiftM: Double = 0.0,
    val offset: ImplementOffset = ImplementOffset(),
    val track: List<LatLon> = emptyList(),
    val cells: Set<Long> = emptySet(),
    val startedMs: Long = 0L,
    val updatedMs: Long = 0L,
    val finished: Boolean = false
)

/** Resumen para listados (sin track ni celdas). */
data class SessionSummary(
    val id: String,
    val name: String,
    val updatedMs: Long,
    val finished: Boolean,
    val cellCount: Int
)

/**
 * Contrato de persistencia. La app Android lo implementa con Room; aquí vive la
 * implementación JVM de referencia en fichero JSON (sin dependencias).
 */
interface SessionStore {
    fun save(session: WorkSession)
    fun load(id: String): WorkSession?
    fun list(): List<SessionSummary>
    fun delete(id: String): Boolean
}

/** Implementación en fichero JSON (`<dir>/<id>.json`). Sin dependencias externas. */
class FileSessionStore(private val dir: File) : SessionStore {

    init {
        dir.mkdirs()
    }

    private fun fileFor(id: String): File {
        val safe = id.replace(Regex("[^A-Za-z0-9_-]"), "_").ifEmpty { "sesion" }
        return File(dir, "$safe.json")
    }

    /** Índice diminuto de la sesión (nombre, instantes, celdas) para listar. */
    private fun metaFor(id: String): File {
        val safe = id.replace(Regex("[^A-Za-z0-9_-]"), "_").ifEmpty { "sesion" }
        return File(dir, "$safe.meta")
    }

    /**
     * Guardado atómico: se escribe a `.tmp` y se renombra. Si el proceso muere
     * a mitad, la sesión anterior sigue íntegra (antes quedaba un JSON truncado
     * y la sesión se perdía).
     */
    override fun save(session: WorkSession) {
        require(session.id.isNotBlank()) { "id de sesión vacío" }
        val destino = fileFor(session.id)
        val texto = toJson(session)
        val tmp = File(dir, destino.name + ".tmp")
        tmp.writeText(texto, Charsets.UTF_8)
        try {
            java.nio.file.Files.move(
                tmp.toPath(), destino.toPath(),
                StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE
            )
        } catch (_: Exception) {
            // Sistema sin renombrado atómico: se reemplaza igualmente.
            if (!tmp.renameTo(destino)) {
                destino.writeText(texto, Charsets.UTF_8)
                tmp.delete()
            }
        }
        escribirMeta(session)
    }

    /** Índice de listado: unas decenas de bytes, sin celdas ni recorrido. */
    private fun escribirMeta(s: WorkSession) = escribirMetaResumen(
        SessionSummary(s.id, s.name, s.updatedMs, s.finished, s.cells.size)
    )

    private fun escribirMetaResumen(s: SessionSummary) {
        val texto = buildString {
            append('{')
            str("id", s.id); append(',')
            str("name", s.name); append(',')
            num("updatedMs", s.updatedMs.toDouble()); append(',')
            append("\"finished\":").append(if (s.finished) "true" else "false").append(',')
            append("\"cellCount\":").append(s.cellCount)
            append('}')
        }
        val destino = metaFor(s.id)
        val tmp = File(dir, destino.name + ".tmp")
        try {
            tmp.writeText(texto, Charsets.UTF_8)
            try {
                java.nio.file.Files.move(
                    tmp.toPath(), destino.toPath(),
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE
                )
            } catch (_: Exception) {
                if (!tmp.renameTo(destino)) {
                    destino.writeText(texto, Charsets.UTF_8)
                    tmp.delete()
                }
            }
        } catch (_: Exception) {
            // Sin índice se sigue pudiendo listar (se reconstruye al vuelo).
            tmp.delete()
        }
    }

    override fun load(id: String): WorkSession? {
        val f = fileFor(id)
        if (!f.isFile) return null
        return try {
            fromJson(f.readText(Charsets.UTF_8))
        } catch (_: Exception) {
            null // Fichero corrupto: no se rompe, se informa como ausente.
        }
    }

    override fun list(): List<SessionSummary> {
        val ficheros = dir.listFiles { f -> f.extension == "json" } ?: return emptyList()
        return ficheros.mapNotNull { f ->
            try {
                // Camino normal: índice de decenas de bytes (no se lee el cuerpo).
                val meta = metaFor(f.nameWithoutExtension)
                val desdeIndice = if (meta.isFile) leerMeta(meta.readText(Charsets.UTF_8)) else null
                if (desdeIndice != null) return@mapNotNull desdeIndice
                // Sesiones anteriores al índice (o índice ilegible): se resume
                // el cuerpo en streaming y se deja el índice escrito.
                val resumen = SessionResumen.leer(f.readText(Charsets.UTF_8))
                if (resumen != null) escribirMetaResumen(resumen)
                resumen
            } catch (_: Exception) {
                null
            }
        }.sortedByDescending { it.updatedMs }
    }

    private fun leerMeta(texto: String): SessionSummary? = try {
        val m = MiniJson.parseObject(texto)
        SessionSummary(
            id = m["id"] as String,
            name = m["name"] as String,
            updatedMs = (m["updatedMs"] as Number).toLong(),
            finished = m["finished"] as Boolean,
            cellCount = (m["cellCount"] as Number).toInt()
        )
    } catch (_: Exception) {
        null
    }

    override fun delete(id: String): Boolean {
        metaFor(id).delete()
        return fileFor(id).delete()
    }

    // ---- JSON manual (escritura) ----

    internal fun toJson(s: WorkSession): String = buildString {
        append('{')
        str("id", s.id); append(',')
        str("name", s.name); append(',')
        num("anchorLat", s.anchor.lat); append(',')
        num("anchorLon", s.anchor.lon); append(',')
        num("headingDeg", s.headingDeg); append(',')
        num("widthM", s.widthM); append(',')
        num("shiftM", s.lateralShiftM); append(',')
        num("offsetLat", s.offset.lateralM); append(',')
        num("offsetLon", s.offset.longitudinalM); append(',')
        append("\"track\":")
        append(SessionJson.encodeTrack(s.track))
        append(",")
        append("\"cells\":")
        append(SessionJson.encodeCells(s.cells))
        append(",")
        num("startedMs", s.startedMs.toDouble()); append(',')
        num("updatedMs", s.updatedMs.toDouble()); append(',')
        append("\"finished\":").append(if (s.finished) "true" else "false")
        append('}')
    }

    private fun StringBuilder.str(key: String, value: String) {
        append('"').append(key).append("\":\"").append(escape(value)).append('"')
    }

    private fun StringBuilder.num(key: String, value: Double) {
        append('"').append(key).append("\":").append(value)
    }

    internal fun escape(s: String): String = buildString {
        for (c in s) when (c) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> if (c < ' ') append("\\u%04x".format(c.code)) else append(c)
        }
    }

    /** Agrupa celdas por fila iy en rangos contiguos de ix. */
    internal fun compactRows(cells: Set<Long>): Map<Int, List<Pair<Int, Int>>> =
        CellCodec.compact(cells)

    // ---- JSON mínimo (lectura del esquema propio) ----

    internal fun fromJson(json: String): WorkSession {
        val root = MiniJson.parseObject(json)
        fun dbl(k: String): Double = (root[k] as Number).toDouble()
        @Suppress("UNCHECKED_CAST")
        val trackRaw = root["track"] as List<Any?>
        @Suppress("UNCHECKED_CAST")
        val cellsRaw = root["cells"] as Map<String, Any?>
        val track = SessionJson.decodeTrackList(trackRaw)
        val cells = SessionJson.decodeCellsMap(cellsRaw)
        return WorkSession(
            id = root["id"] as String,
            name = root["name"] as String,
            anchor = LatLon(dbl("anchorLat"), dbl("anchorLon")),
            headingDeg = dbl("headingDeg"),
            widthM = dbl("widthM"),
            lateralShiftM = dbl("shiftM"),
            offset = ImplementOffset(dbl("offsetLat"), dbl("offsetLon")),
            track = track,
            cells = cells,
            startedMs = dbl("startedMs").toLong(),
            updatedMs = dbl("updatedMs").toLong(),
            finished = root["finished"] as Boolean
        )
    }
}

/**
 * Lector de resumen en streaming.
 *
 * `list()` solo necesita id, nombre, instantes, estado y nº de celdas. La
 * versión anterior parseaba el JSON completo y **expandía** las celdas a un
 * `HashSet<Long>` (una `Long` boxeada por m²) y reconstruía el track entero
 * (un `LatLon` por punto). Con datos acumulados eso son cientos de miles de
 * objetos por llamada, y se ejecutaba en cada autoguardado y en cada entrada a
 * Campo: GC, CPU y ralentización al reabrir la app.
 *
 * Este lector recorre el texto una vez, salta `track` y cuenta las celdas
 * sumando los rangos ya compactados, sin expandirlos.
 */
object SessionResumen {

    fun leer(json: String): SessionSummary? = try {
        Escaner(json).resumen()
    } catch (_: Exception) {
        null
    }

    private class Escaner(private val s: String) {
        private var i = 0

        fun resumen(): SessionSummary {
            val campos = HashMap<String, Any>(8)
            objeto(campos)
            val id = campos["id"] as? String ?: throw IllegalArgumentException("sin id")
            val nombre = campos["name"] as? String ?: ""
            val actualizado = (campos["updatedMs"] as? Double)?.toLong() ?: 0L
            val terminada = campos["finished"] as? Boolean ?: false
            val celdas = (campos["cellCount"] as? Int) ?: 0
            return SessionSummary(id, nombre, actualizado, terminada, celdas)
        }

        private fun objeto(destino: MutableMap<String, Any>) {
            espacio()
            esperar('{')
            espacio()
            if (ver('}')) return
            while (true) {
                espacio()
                val clave = cadena()
                espacio()
                esperar(':')
                espacio()
                when (clave) {
                    "cells" -> destino["cellCount"] = contarCeldas()
                    "track" -> saltarValor()
                    "id", "name" -> destino[clave] = cadena()
                    "updatedMs", "startedMs" -> destino[clave] = numero()
                    "finished" -> destino[clave] = booleano()
                    else -> saltarValor()
                }
                espacio()
                if (ver(',')) continue
                esperar('}')
                return
            }
        }

        /** Suma rangos `[[a,b],...]` de cada fila, sin materializar celdas. */
        private fun contarCeldas(): Int {
            espacio()
            esperar('{')
            var total = 0L
            espacio()
            if (ver('}')) return 0
            while (true) {
                cadena() // clave: fila iy
                espacio()
                esperar(':')
                espacio()
                esperar('[')
                espacio()
                if (!ver(']')) {
                    while (true) {
                        espacio()
                        esperar('[')
                        val desde = entero()
                        espacio()
                        esperar(',')
                        val hasta = entero()
                        espacio()
                        esperar(']')
                        if (hasta >= desde) total += hasta - desde + 1
                        espacio()
                        if (ver(',')) continue
                        esperar(']')
                        break
                    }
                }
                espacio()
                if (ver(',')) continue
                esperar('}')
                break
            }
            return if (total > Int.MAX_VALUE) Int.MAX_VALUE else total.toInt()
        }

        /** Salta un valor completo (objeto, array, cadena, número o literal). */
        private fun saltarValor() {
            espacio()
            if (i >= s.length) throw IllegalArgumentException("fin inesperado")
            when (s[i]) {
                '"' -> {
                    cadena()
                    return
                }
                '{', '[' -> {
                    var profundidad = 0
                    while (i < s.length) {
                        when (s[i]) {
                            '"' -> {
                                cadena()
                                continue
                            }
                            '{', '[' -> profundidad++
                            '}', ']' -> {
                                profundidad--
                                i++
                                if (profundidad == 0) return
                                continue
                            }
                        }
                        i++
                    }
                    throw IllegalArgumentException("valor sin cerrar")
                }
                else -> while (i < s.length && s[i] !in ",}] \t\r\n") i++
            }
        }

        private fun cadena(): String {
            espacio()
            esperar('"')
            val sb = StringBuilder()
            while (true) {
                if (i >= s.length) throw IllegalArgumentException("cadena sin cerrar")
                val c = s[i++]
                if (c == '"') return sb.toString()
                if (c != '\\') {
                    sb.append(c)
                    continue
                }
                if (i >= s.length) throw IllegalArgumentException("escape incompleto")
                when (val e = s[i++]) {
                    '"' -> sb.append('"')
                    '\\' -> sb.append('\\')
                    '/' -> sb.append('/')
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append('\t')
                    'u' -> {
                        if (i + 4 > s.length) throw IllegalArgumentException("unicode incompleto")
                        sb.append(s.substring(i, i + 4).toInt(16).toChar())
                        i += 4
                    }
                    else -> throw IllegalArgumentException("escape desconocido: $e")
                }
            }
        }

        private fun numero(): Double {
            espacio()
            val inicio = i
            while (i < s.length && s[i] !in ",}] \t\r\n") i++
            return s.substring(inicio, i).toDouble()
        }

        private fun entero(): Long {
            espacio()
            val inicio = i
            if (i < s.length && (s[i] == '-' || s[i] == '+')) i++
            while (i < s.length && s[i].isDigit()) i++
            return s.substring(inicio, i).toLong()
        }

        private fun booleano(): Boolean {
            espacio()
            return when {
                s.startsWith("true", i) -> {
                    i += 4
                    true
                }
                s.startsWith("false", i) -> {
                    i += 5
                    false
                }
                else -> throw IllegalArgumentException("booleano inválido")
            }
        }

        private fun espacio() {
            while (i < s.length && s[i].isWhitespace()) i++
        }

        private fun ver(c: Char): Boolean {
            if (i < s.length && s[i] == c) {
                i++
                return true
            }
            return false
        }

        private fun esperar(c: Char) {
            if (i >= s.length || s[i] != c) throw IllegalArgumentException("se esperaba '$c'")
            i++
        }
    }
}

/** Analizador JSON mínimo, suficiente para el esquema de [FileSessionStore]. */
object MiniJson {
    fun parseObject(s: String): Map<String, Any?> {
        val p = Parser(s)
        p.ws()
        val v = p.value()
        p.ws()
        require(p.eof()) { "contenido tras el objeto" }
        @Suppress("UNCHECKED_CAST")
        return v as? Map<String, Any?> ?: throw IllegalArgumentException("no es objeto")
    }

    private class Parser(val s: String) {
        var i = 0
        fun eof(): Boolean = i >= s.length
        fun ws() {
            while (!eof() && s[i].isWhitespace()) i++
        }

        fun value(): Any? {
            ws()
            require(!eof()) { "fin inesperado" }
            return when (s[i]) {
                '{' -> obj()
                '[' -> arr()
                '"' -> str()
                't' -> lit("true", true)
                'f' -> lit("false", false)
                'n' -> lit("null", null)
                else -> num()
            }
        }

        fun obj(): Map<String, Any?> {
            i++ // {
            val m = linkedMapOf<String, Any?>()
            ws()
            if (!eof() && s[i] == '}') {
                i++
                return m
            }
            while (true) {
                ws()
                val k = str()
                ws()
                require(!eof() && s[i] == ':') { "se esperaba ':'" }
                i++
                m[k] = value()
                ws()
                require(!eof()) { "fin inesperado en objeto" }
                if (s[i] == '}') {
                    i++
                    return m
                }
                require(s[i] == ',') { "se esperaba ','" }
                i++
            }
        }

        fun arr(): List<Any?> {
            i++ // [
            val l = mutableListOf<Any?>()
            ws()
            if (!eof() && s[i] == ']') {
                i++
                return l
            }
            while (true) {
                l.add(value())
                ws()
                require(!eof()) { "fin inesperado en array" }
                if (s[i] == ']') {
                    i++
                    return l
                }
                require(s[i] == ',') { "se esperaba ','" }
                i++
            }
        }

        fun str(): String {
            require(s[i] == '"')
            i++
            val sb = StringBuilder()
            while (true) {
                require(i < s.length) { "cadena sin cerrar" }
                val c = s[i++]
                if (c == '"') return sb.toString()
                if (c == '\\') {
                    require(i < s.length) { "escape incompleto" }
                    when (val e = s[i++]) {
                        '"' -> sb.append('"')
                        '\\' -> sb.append('\\')
                        '/' -> sb.append('/')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'u' -> {
                            require(i + 4 <= s.length) { "escape unicode incompleto" }
                            sb.append(s.substring(i, i + 4).toInt(16).toChar())
                            i += 4
                        }
                        else -> throw IllegalArgumentException("escape desconocido: $e")
                    }
                } else sb.append(c)
            }
        }

        fun lit(word: String, v: Any?): Any? {
            require(s.startsWith(word, i)) { "literal inválido" }
            i += word.length
            return v
        }

        fun num(): Double {
            val start = i
            while (!eof() && s[i] in "-+0123456789.eE") i++
            return s.substring(start, i).toDouble()
        }
    }
}

/** Compactación de celdas raster por filas (compartida fichero/Room). */
object CellCodec {

    /** Agrupa celdas por fila iy en rangos contiguos de ix. Vacío → mapa vacío. */
    fun compact(cells: Set<Long>): Map<Int, List<Pair<Int, Int>>> {
        val byRow = mutableMapOf<Int, MutableList<Int>>()
        for (key in cells) {
            val ix = (key shr 32).toInt()
            val iy = (key and 0xFFFFFFFFL).toInt()
            byRow.getOrPut(iy) { mutableListOf() }.add(ix)
        }
        return byRow.mapValues { (_, xs) ->
            val sorted = xs.sorted()
            val ranges = mutableListOf<Pair<Int, Int>>()
            var start = sorted[0]
            var prev = sorted[0]
            for (i in 1 until sorted.size) {
                if (sorted[i] == prev + 1) {
                    prev = sorted[i]
                } else {
                    ranges.add(start to prev)
                    start = sorted[i]
                    prev = sorted[i]
                }
            }
            ranges.add(start to prev)
            ranges
        }
    }

    /** Inversa de [compact]. Reserva el tamaño exacto (evita rehashing masivo). */
    fun expand(rows: Map<Int, List<Pair<Int, Int>>>): Set<Long> {
        var total = 0L
        for (ranges in rows.values) {
            for ((x0, x1) in ranges) if (x1 >= x0) total += (x1 - x0 + 1)
        }
        val capacidad = if (total > Int.MAX_VALUE / 2) Int.MAX_VALUE / 2 else (total * 4 / 3 + 1).toInt()
        val cells = HashSet<Long>(capacidad)
        for ((iy, ranges) in rows) {
            val fila = iy.toLong() and 0xFFFFFFFFL
            for ((x0, x1) in ranges) {
                for (ix in x0..x1) {
                    cells.add((ix.toLong() shl 32) xor fila)
                }
            }
        }
        return cells
    }
}

/**
 * Códec JSON público para track y celdas (mismo esquema en fichero JVM y Room).
 * Sin dependencias externas.
 */
object SessionJson {

    fun encodeCells(cells: Set<Long>): String = buildString {
        append('{')
        CellCodec.compact(cells).entries.sortedBy { it.key }.forEachIndexed { i, (iy, ranges) ->
            if (i > 0) append(',')
            append('"').append(iy).append("\":[")
            ranges.forEachIndexed { j, r ->
                if (j > 0) append(',')
                append('[').append(r.first).append(',').append(r.second).append(']')
            }
            append(']')
        }
        append('}')
    }

    fun decodeCells(json: String): Set<Long> {
        @Suppress("UNCHECKED_CAST")
        val raw = MiniJson.parseObject(json) as Map<String, Any?>
        return decodeCellsMap(raw)
    }

    internal fun decodeCellsMap(raw: Map<String, Any?>): Set<Long> {
        val rows = mutableMapOf<Int, List<Pair<Int, Int>>>()
        for ((iyStr, rangesAny) in raw) {
            val iy = iyStr.toInt()
            @Suppress("UNCHECKED_CAST")
            val ranges = rangesAny as List<Any?>
            rows[iy] = ranges.map {
                @Suppress("UNCHECKED_CAST")
                val pair = it as List<Any?>
                (pair[0] as Number).toInt() to (pair[1] as Number).toInt()
            }
        }
        return CellCodec.expand(rows)
    }

    fun encodeTrack(track: List<LatLon>): String = buildString {
        append('[')
        track.forEachIndexed { i, p ->
            if (i > 0) append(',')
            append('[').append(p.lat).append(',').append(p.lon).append(']')
        }
        append(']')
    }

    fun decodeTrack(json: String): List<LatLon> {
        val parsed = MiniJson.parseObject("{\"t\":$json}")["t"]
        @Suppress("UNCHECKED_CAST")
        return decodeTrackList(parsed as List<Any?>)
    }

    internal fun decodeTrackList(raw: List<Any?>): List<LatLon> = raw.map {
        @Suppress("UNCHECKED_CAST")
        val p = it as List<Any?>
        LatLon((p[0] as Number).toDouble(), (p[1] as Number).toDouble())
    }
}
