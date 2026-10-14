package es.surcogps.app.mapa

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * Verificación LOCAL de la pila HTTP de teselas (exigida antes del APK):
 * un servidor real en localhost demuestra que la formación de URLs y la
 * descarga funcionan; el dispositivo solo puede fallar por su red.
 */
class DiagnosticoTeselasTest {

    private var servidor: ServerSocket? = null
    private val activo = AtomicBoolean(false)

    @After
    fun cerrar() {
        activo.set(false)
        try {
            servidor?.close()
        } catch (_: Exception) {
        }
        servidor = null
    }

    /** Mini-servidor HTTP: 200 con 4 bytes en /bien, 404 en el resto. */
    private fun arrancar(): Int {
        val s = ServerSocket(0)
        servidor = s
        activo.set(true)
        thread(isDaemon = true) {
            while (activo.get()) {
                try {
                    val cli = s.accept()
                    cli.use { c ->
                        val entrada = c.getInputStream()
                        val buf = ByteArrayOutputStream()
                        var b = entrada.read()
                        // Lee la cabecera hasta línea en blanco (límite 4 KB).
                        while (b != -1 && buf.size() < 4096) {
                            buf.write(b)
                            val n = buf.size()
                            if (n >= 4) {
                                val t = buf.toByteArray()
                                if (t[n - 4] == '\r'.code.toByte() && t[n - 3] == '\n'.code.toByte() &&
                                    t[n - 2] == '\r'.code.toByte() && t[n - 1] == '\n'.code.toByte()
                                ) break
                            }
                            b = entrada.read()
                        }
                        val peticion = buf.toString(Charsets.US_ASCII)
                        val salida = c.getOutputStream()
                        if (peticion.startsWith("GET /bien")) {
                            val cuerpo = byteArrayOf(1, 2, 3, 4)
                            salida.write("HTTP/1.1 200 OK\r\nContent-Length: 4\r\nConnection: close\r\n\r\n".toByteArray())
                            salida.write(cuerpo)
                        } else {
                            salida.write("HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".toByteArray())
                        }
                        salida.flush()
                    }
                } catch (_: Exception) {
                    // Cierre del servidor o cliente roto: termina el hilo.
                    if (!activo.get()) break
                }
            }
        }
        return s.localPort
    }

    @Test
    fun urlEsriBienFormada() {
        // La misma URL que pide el mapa (verificada contra el servidor real).
        assertEquals(
            "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/10/395/500",
            FuentesMapa.urlMuestraEsri()
        )
    }

    @Test
    fun comprobar200Local() {
        val puerto = arrancar()
        assertTrue(DiagnosticoTeselas.comprobar("http://127.0.0.1:$puerto/bien"))
    }

    @Test
    fun comprobar404Local() {
        val puerto = arrancar()
        assertFalse(DiagnosticoTeselas.comprobar("http://127.0.0.1:$puerto/roto"))
    }

    @Test
    fun comprobarPuertoCerrado() {
        // Conexión rechazada al instante (sin colgar el test).
        assertFalse(DiagnosticoTeselas.comprobar("http://127.0.0.1:9/tile", 2000))
    }
}

class TextoColaTest {

    @Test
    fun formato() {
        assertEquals("Cola: 0 · HTTP ok", textoCola(0L, true))
        assertEquals("Cola: 12 · HTTP fallo", textoCola(12L, false))
        assertEquals("Cola: 3 · HTTP…", textoCola(3L, null))
        assertEquals("Teselas … · HTTP ok", textoCola(-1L, true))
    }
}
