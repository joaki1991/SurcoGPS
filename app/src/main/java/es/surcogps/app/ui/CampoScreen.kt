package es.surcogps.app.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import es.surcogps.app.mapa.MapaCampo
import org.osmdroid.util.GeoPoint

/** Pantalla de campo: mapa híbrido + banderillero GPS en vivo. */
@Composable
fun CampoScreen(vm: CampoViewModel) {
    val a by vm.ajustes.collectAsState()
    val modo by vm.modoMapa.collectAsState()
    val g by vm.guiado.collectAsState()
    val seguir by vm.seguirPosicion.collectAsState()
    val destino by vm.destinoCamara.collectAsState()
    val ubicacion by vm.ubicacion.collectAsState()
    val guiaMotor by vm.guiaActual.collectAsState()
    val nmea by vm.nmeaSentencias.collectAsState()
    val ha by vm.areaHa.collectAsState()
    val tramos by vm.tramosDibujo.collectAsState()
    val registrando by vm.registrando.collectAsState()
    val informe by vm.informeCierre.collectAsState()

    val permisoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { concedidos ->
        if (concedidos[Manifest.permission.ACCESS_FINE_LOCATION] == true) vm.iniciarGps()
    }
    DisposableEffect(Unit) {
        if (vm.tienePermisoUbicacion()) vm.iniciarGps()
        onDispose { vm.detenerGps() }
    }

    Column(Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        informe?.let {
            AvisoCierre(
                texto = it,
                alCompartir = vm::descartarCierres,
                alDescartar = vm::descartarCierres
            )
        }
        MapaCampo(
            modo = modo,
            alCambiarModo = vm::cambiarModoMapa,
            modifier = Modifier.fillMaxWidth().weight(1f),
            seguir = seguir,
            posicionSeguir = if (seguir) g.posicionApero?.let { GeoPoint(it.lat, it.lon) } else null,
            destino = destino,
            alDestinoAplicado = vm::consumirDestino,
            alGestoManual = vm::alGestoManual,
            tramos = tramos,
            ubicacion = ubicacion,
            guia = guiaMotor?.let { es.surcogps.app.mapa.EstadoGuia(it, g.linea) },
            alFijarAncla = vm::fijarAnclaEn,
            puedeIrAUbicacion = g.posicionApero != null,
            alIrAUbicacion = vm::irAUbicacion
        )
        // HUD del banderillero: precisión SIEMPRE visible (GPS de móvil = métrico).
        // Estado de carga: permiso concedido pero aún sin ninguna ubicación.
        if (g.fix == null && vm.tienePermisoUbicacion()) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 3.dp)
                Text("Buscando GPS… el mapa se centrará solo.", style = MaterialTheme.typography.bodyMedium)
            }
        }
        Text(
            if (g.fix == null) "Sin posición: concede el permiso de ubicación."
            else "Precisión ±${"%.0f".format(g.fix!!.accuracyM)} m · " +
                "Vel ${if (g.fix!!.speedMps != null) "%.1f".format(g.fix!!.speedMps!! * 3.6) else "—"} km/h" +
                if (g.aceptado && g.xteM != null)
                    " · Línea ${g.linea} · XTE ${"%+.1f".format(g.xteM)} m ${if (g.xteM!! > 0.3) "→" else if (g.xteM!! < -0.3) "←" else "✓"}"
                else if (!g.aceptado) " · Fix rechazado (${g.motivoRechazo})"
                else " · Fija el ancla para guiar",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            "Rumbo ${"%.0f".format(a.rumboDeg)}° · Ancho ${"%.1f".format(a.anchoAperoM)} m · " +
                "Trabajado ${"%.2f".format(ha)} ha · NMEA: $nmea sentencias",
            style = MaterialTheme.typography.bodySmall
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!vm.tienePermisoUbicacion()) {
                Button(
                    onClick = {
                        permisoLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.POST_NOTIFICATIONS
                            )
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Permitir ubicación")
                }
            }
            OutlinedButton(
                onClick = vm::fijarAnclaAqui,
                enabled = g.posicionApero != null,
                modifier = Modifier.weight(1f)
            ) {
                Text(if (a.anclaLat == null) "Fijar ancla aquí" else "Mover ancla aquí")
            }
            OutlinedButton(onClick = vm::alternarSeguir, modifier = Modifier.weight(1f)) {
                Text(if (seguir) "No seguir" else "Seguir")
            }
            OutlinedButton(
                onClick = vm::alternarRegistro,
                enabled = g.posicionApero != null,
                modifier = Modifier.weight(1f)
            ) {
                Text(if (registrando) "Pausar" else "Registrar")
            }
        }
        Text("Ancho del apero: ${"%.1f".format(a.anchoAperoM)} m")
        Slider(
            value = a.anchoAperoM.toFloat(),
            onValueChange = { vm.guardarAncho(it.toDouble()) },
            valueRange = 0.5f..30f,
            steps = 58
        )
    }
}
