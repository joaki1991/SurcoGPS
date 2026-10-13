package es.surcogps.core

import java.io.File

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

    override fun save(session: WorkSession) {
        require(session.id.isNotBlank()) { "id de sesión vacío" }
        fileFor(session.id).writeText(toJson(session), Charsets.UTF_8)
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
        return dir.listFiles { f -> f.extension == "json" }
            ?.mapNotNull { f ->
                try {
                    fromJson(f.readText(Charsets.UTF_8))
                } catch (_: Exception) {
                    null
                }
            }
            ?.sortedByDescending { it.updatedMs }
            ?.map { SessionSummary(it.id, it.name, it.updatedMs, it.finished, it.cells.size) }
            ?: emptyList()
    }

    override fun delete(id: String): Boolean = fileFor(id).delete()

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

    /** Inversa de [compact]. */
    fun expand(rows: Map<Int, List<Pair<Int, Int>>>): Set<Long> {
        val cells = mutableSetOf<Long>()
        for ((iy, ranges) in rows) {
            for ((x0, x1) in ranges) {
                for (ix in x0..x1) {
                    cells.add((ix.toLong() shl 32) xor (iy.toLong() and 0xFFFFFFFFL))
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
