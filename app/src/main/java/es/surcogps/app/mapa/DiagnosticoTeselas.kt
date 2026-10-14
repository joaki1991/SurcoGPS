package es.surcogps.app.mapa

import java.net.HttpURLConnection
import java.net.URL

/**
 * Diagnóstico de red hacia las teselas (E/S, sin tests unitarios: necesita red).
 * Distingue «no hay red hacia Esri» de «fallo de renderizado».
 */
object DiagnosticoTeselas {

    /** Comprueba una URL de tesela (true = responde 2xx/3xx). */
    fun comprobar(url: String, timeoutMs: Int = 8000): Boolean {
        return try {
            val c = URL(url).openConnection() as HttpURLConnection
            c.connectTimeout = timeoutMs
            c.readTimeout = timeoutMs
            c.setRequestProperty("User-Agent", "SurcoGPS/1.1")
            c.connect()
            val ok = c.responseCode in 200..399
            c.disconnect()
            ok
        } catch (_: Exception) {
            false
        }
    }
}
