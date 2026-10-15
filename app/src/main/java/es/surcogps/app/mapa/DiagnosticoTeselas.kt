package es.surcogps.app.mapa

import java.net.HttpURLConnection
import java.net.URL

/**
 * Diagnóstico de red hacia las teselas (E/S, sin tests unitarios: necesita red).
 * Distingue «no hay red hacia Esri» de «fallo de renderizado».
 */
object DiagnosticoTeselas {

    /** Resultado detallado de una comprobación (código, error y duración). */
    data class ResultadoHttp(val ok: Boolean, val codigo: Int?, val error: String?, val ms: Long)

    /** Comprueba una URL de tesela (true = responde 2xx/3xx). */
    fun comprobar(url: String, timeoutMs: Int = 8000): Boolean =
        comprobarDetalle(url, timeoutMs).ok

    fun comprobarDetalle(url: String, timeoutMs: Int = 8000): ResultadoHttp {
        val t0 = System.currentTimeMillis()
        return try {
            val c = URL(url).openConnection() as HttpURLConnection
            c.connectTimeout = timeoutMs
            c.readTimeout = timeoutMs
            c.setRequestProperty("User-Agent", "SurcoGPS/1.1")
            c.connect()
            val code = c.responseCode
            c.disconnect()
            val ok = code in 200..399
            ResultadoHttp(ok, code, if (ok) null else "HTTP $code", System.currentTimeMillis() - t0)
        } catch (e: Exception) {
            ResultadoHttp(false, null, e.javaClass.simpleName, System.currentTimeMillis() - t0)
        }
    }
}
