package es.surcogps.core

import java.io.File

/** Parcela agrícola: marco de trabajo estable (ancla + rumbo + apero). */
data class Parcela(
    val id: String,
    val nombre: String,
    val ancla: LatLon,
    val rumbo: Double,
    val ancho: Double,
    val shift: Double = 0.0,
    val offset: ImplementOffset = ImplementOffset(),
    val creadaMs: Long = 0L
)

/** Contrato de parcelas (la app lo usa en fichero local; sin nube). */
interface ParcelaStore {
    fun guardar(p: Parcela)
    fun porId(id: String): Parcela?
    fun listar(): List<Parcela>
    fun borrar(id: String): Boolean
}

/**
 * Parcelas en un único fichero JSON (`parcelas.json`). Sin dependencias.
 * Esquema: `{"<id>": {"nombre":..,"anclaLat":..,"anclaLon":..,"rumbo":..,
 * "ancho":..,"shift":..,"offLat":..,"offLon":..,"creadaMs":..}}`.
 */
class FileParcelaStore(private val dir: File) : ParcelaStore {

    init {
        dir.mkdirs()
    }

    private fun archivo(): File = File(dir, "parcelas.json")

    private fun leer(): MutableMap<String, Parcela> {
        val f = archivo()
        if (!f.isFile) return mutableMapOf()
        return try {
            val root = MiniJson.parseObject(f.readText(Charsets.UTF_8))
            root.mapValuesTo(mutableMapOf()) { (id, v) ->
                @Suppress("UNCHECKED_CAST")
                val m = v as Map<String, Any?>
                fun dbl(k: String): Double = (m[k] as Number).toDouble()
                Parcela(
                    id = id,
                    nombre = m["nombre"] as String,
                    ancla = LatLon(dbl("anclaLat"), dbl("anclaLon")),
                    rumbo = SurcoCore.normalizeHeading(dbl("rumbo")),
                    ancho = dbl("ancho"),
                    shift = dbl("shift"),
                    offset = ImplementOffset(dbl("offLat"), dbl("offLon")),
                    creadaMs = dbl("creadaMs").toLong()
                )
            }
        } catch (_: Exception) {
            mutableMapOf() // Corrupto: se parte de vacío, sin romper.
        }
    }

    private fun escribir(todas: Map<String, Parcela>) {
        val sb = StringBuilder("{")
        todas.entries.sortedBy { it.value.creadaMs }.forEachIndexed { i, (id, p) ->
            if (i > 0) sb.append(',')
            sb.append('"').append(escapeId(id)).append("\":{")
            sb.append("\"nombre\":\"").append(escapeNombre(p.nombre)).append("\",")
            sb.append("\"anclaLat\":").append(p.ancla.lat).append(',')
            sb.append("\"anclaLon\":").append(p.ancla.lon).append(',')
            sb.append("\"rumbo\":").append(p.rumbo).append(',')
            sb.append("\"ancho\":").append(p.ancho).append(',')
            sb.append("\"shift\":").append(p.shift).append(',')
            sb.append("\"offLat\":").append(p.offset.lateralM).append(',')
            sb.append("\"offLon\":").append(p.offset.longitudinalM).append(',')
            sb.append("\"creadaMs\":").append(p.creadaMs)
            sb.append('}')
        }
        sb.append('}')
        archivo().writeText(sb.toString(), Charsets.UTF_8)
    }

    override fun guardar(p: Parcela) {
        require(p.id.isNotBlank() && p.nombre.isNotBlank()) { "parcela inválida" }
        require(p.ancho in GuidanceEngine.MIN_WIDTH_M..GuidanceEngine.MAX_WIDTH_M) {
            "ancho fuera de rango"
        }
        val todas = leer()
        todas[p.id] = p.copy(rumbo = SurcoCore.normalizeHeading(p.rumbo))
        escribir(todas)
    }

    override fun porId(id: String): Parcela? = leer()[id]

    override fun listar(): List<Parcela> =
        leer().values.sortedByDescending { it.creadaMs }

    override fun borrar(id: String): Boolean {
        val todas = leer()
        if (todas.remove(id) == null) return false
        escribir(todas)
        return true
    }

    private fun escapeId(s: String): String = s.replace(Regex("[^A-Za-z0-9_-]"), "_")

    private fun escapeNombre(s: String): String = buildString {
        for (c in s) when (c) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\n' -> append("\\n")
            else -> append(c)
        }
    }
}
