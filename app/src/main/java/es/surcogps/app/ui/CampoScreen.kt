package es.surcogps.app.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.Surface
import android.view.View
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import es.surcogps.app.gps.EstadoGuiado
import es.surcogps.app.gps.FuenteGps
import es.surcogps.app.mapa.MapaSimple
import es.surcogps.app.mapa.ModoMapa
import kotlin.math.roundToInt

/**
 * Pantalla única de SurcoGPS: mapa a pantalla completa + panel de control.
 *
 * - El mapa ocupa todo el lienzo (también en horizontal). Los controles flotan
 *   encima y se pueden ocultar con «Ocultar».
 * - En vertical el panel va abajo; en horizontal, a la derecha.
 * - No hay navegación inferior ni pantallas secundarias.
 */
@Composable
fun CampoScreen(vm: CampoViewModel) {
    val g by vm.guiado.collectAsState()
    val ancho by vm.ancho.collectAsState()
    val rumbo by vm.rumbo.collectAsState()
    val areaHa by vm.areaHa.collectAsState()
    val tramos by vm.tramos.collectAsState()
    val registrando by vm.registrando.collectAsState()
    val gpsActivo by vm.gpsActivo.collectAsState()
    val fuente by vm.fuenteGps.collectAsState()
    val modo by vm.modoMapa.collectAsState()
    val seguir by vm.seguirPosicion.collectAsState()
    val destino by vm.destinoCamara.collectAsState()
    val ubicacion by vm.ubicacion.collectAsState()
    val guia by vm.guia.collectAsState()
    val ancla by vm.ancla.collectAsState()

    val context = LocalContext.current
    val vista = LocalView.current
    val ciclo = LocalLifecycleOwner.current
    var permiso by remember { mutableStateOf(vm.tienePermisoUbicacion()) }
    var panelVisible by rememberSaveable { mutableStateOf(true) }
    var confirmarNuevo by remember { mutableStateOf(false) }

    val lanzadorPermiso = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { concedidos ->
        permiso = concedidos[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            vm.tienePermisoUbicacion()
        if (permiso) vm.iniciarGps()
    }

    // GPS: se enciende al entrar y sigue vivo al girar la pantalla. Solo se
    // apaga al cerrar la app o desde la notificación del servicio.
    DisposableEffect(ciclo) {
        val observador = LifecycleEventObserver { _, evento ->
            when (evento) {
                Lifecycle.Event.ON_START -> {
                    permiso = vm.tienePermisoUbicacion()
                    if (permiso) vm.iniciarGps()
                    vm.iniciarBrujula()
                }
                Lifecycle.Event.ON_RESUME -> {
                    permiso = vm.tienePermisoUbicacion()
                    if (permiso) vm.iniciarGps()
                }
                Lifecycle.Event.ON_STOP -> vm.detenerBrujula()
                else -> Unit
            }
        }
        ciclo.lifecycle.addObserver(observador)
        onDispose {
            ciclo.lifecycle.removeObserver(observador)
            vm.detenerBrujula()
        }
    }

    val rotacion = remember { rotacionPantalla(vista) }
    LaunchedEffect(rotacion) { vm.actualizarRotacionPantalla(rotacion) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val horizontal = maxWidth > maxHeight
        MapaSimple(
            retenido = vm.mapa,
            modo = modo,
            seguir = seguir,
            posicion = ubicacion?.let { es.surcogps.core.LatLon(it.lat, it.lon) },
            destino = destino,
            alDestinoAplicado = vm::consumirDestino,
            alGestoManual = vm::alGestoManual,
            tramos = tramos,
            ubicacion = ubicacion,
            guia = guia,
            // En vertical el panel ocupa la parte baja; en horizontal, la
            // derecha. Se compensa la cámara para no tapar el punto azul.
            offsetX = if (panelVisible && horizontal) -0.20f else 0f,
            offsetY = if (panelVisible && !horizontal) -0.26f else 0f,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            Modifier
                .align(Alignment.TopStart)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EstadoPill(
                g = g,
                gpsActivo = gpsActivo,
                fuente = fuente,
                registrando = registrando,
                areaHa = areaHa
            )
        }

        Row(
            Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                // En vertical los botones bajan para no tapar la píldora de
                // estado en pantallas estrechas.
                .padding(start = 10.dp, end = 10.dp, top = if (horizontal) 10.dp else 84.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(
                onClick = vm::alternarModoMapa,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(if (modo == ModoMapa.CALLES) "Calles" else "Satélite")
            }
            if (!panelVisible) {
                FilledTonalButton(onClick = { panelVisible = true }) {
                    Text("Controles")
                }
            }
        }

        if (!permiso) {
            PermisoCard(
                modifier = Modifier
                    .align(Alignment.Center)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(20.dp),
                alPedir = {
                    lanzadorPermiso.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.POST_NOTIFICATIONS
                        )
                    )
                },
                alAjustes = { abrirAjustesApp(context) }
            )
        }

        if (panelVisible) {
            PanelControl(
                modifier = if (horizontal) {
                    Modifier
                        .align(Alignment.CenterEnd)
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        // Deja libre la fila superior (satélite/controles).
                        .padding(start = 10.dp, end = 10.dp, top = 64.dp, bottom = 10.dp)
                        .widthIn(max = 360.dp)
                        .fillMaxHeight()
                } else {
                    Modifier
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(10.dp)
                        .fillMaxWidth()
                        .heightIn(max = maxHeight * 0.68f)
                },
                ancho = ancho,
                rumbo = rumbo,
                areaHa = areaHa,
                tramos = tramos.size,
                registrando = registrando,
                seguir = seguir,
                brujulaDisponible = vm.brujulaDisponible,
                puedeAnclar = g.fix != null,
                tieneAncla = ancla != null,
                alCambiarAncho = vm::actualizarAncho,
                alCambiarRumbo = vm::actualizarRumbo,
                alUsarBrujula = { vm.usarRumboBrujula() },
                alAnclar = vm::fijarAnclaAqui,
                alAlternarRegistro = vm::alternarRegistro,
                alCentrar = vm::irAUbicacion,
                alAlternarSeguir = vm::alternarSeguir,
                alNuevo = { confirmarNuevo = true },
                alOcultar = { panelVisible = false }
            )
        }
    }

    if (confirmarNuevo) {
        AlertDialog(
            onDismissRequest = { confirmarNuevo = false },
            title = { Text("¿Empezar un recorrido nuevo?") },
            text = {
                Text(
                    "Se borrará solo lo pintado en esta pantalla y la plantilla se " +
                        "recolocará en tu posición actual. Las sesiones y parcelas " +
                        "guardadas en el móvil NO se tocan."
                )
            },
            confirmButton = {
                Button(onClick = {
                    vm.reiniciarRecorrido()
                    confirmarNuevo = false
                }) { Text("Sí, borrar lo pintado") }
            },
            dismissButton = {
                TextButton(onClick = { confirmarNuevo = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun EstadoPill(
    g: EstadoGuiado,
    gpsActivo: Boolean,
    fuente: FuenteGps?,
    registrando: Boolean,
    areaHa: Double
) {
    val fix = g.fix
    val (texto, color) = when {
        !gpsActivo -> "GPS detenido" to Color(0xFFEF9A9A)
        fix == null -> "Buscando GPS…" to Color(0xFFFFE082)
        !g.aceptado -> {
            val detalle = if (fix.accuracyM.isFinite()) {
                "Señal insuficiente ±${fix.accuracyM.roundToInt()} m"
            } else {
                "Señal insuficiente"
            }
            detalle to Color(0xFFFFB74D)
        }
        else -> {
            val vel = fix.speedMps?.let { " · ${(it * 3.6f).roundToInt()} km/h" } ?: ""
            "±${fix.accuracyM.roundToInt()} m$vel · ${"%.2f".format(areaHa)} ha" to
                Color(0xFFA5D6A7)
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.93f),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 3.dp
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (gpsActivo && fix == null) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp), strokeWidth = 2.dp
                    )
                }
                Text(
                    texto,
                    style = MaterialTheme.typography.labelLarge,
                    color = color,
                    fontWeight = FontWeight.SemiBold
                )
            }
            val segunda = buildString {
                append(if (registrando) "Pintando" else "Pintura en pausa")
                if (gpsActivo) {
                    append(" · ")
                    append(
                        when (fuente) {
                            FuenteGps.FUSED -> "GPS Google"
                            FuenteGps.PLATAFORMA -> "GPS dispositivo"
                            null -> "GPS"
                        }
                    )
                }
            }
            Text(
                segunda,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PermisoCard(
    modifier: Modifier,
    alPedir: () -> Unit,
    alAjustes: () -> Unit
) {
    Surface(
        modifier = modifier.widthIn(max = 420.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 8.dp
    ) {
        Column(
            Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Activa el GPS",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "SurcoGPS necesita tu ubicación precisa para mostrar el punto azul, " +
                    "pintar el recorrido y colocar las líneas de trabajo. No se envía " +
                    "a ningún servidor.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = alPedir,
                modifier = Modifier.fillMaxWidth().height(54.dp)
            ) { Text("Permitir ubicación") }
            TextButton(onClick = alAjustes) { Text("Abrir ajustes de la app") }
        }
    }
}

@Composable
private fun PanelControl(
    modifier: Modifier,
    ancho: Double,
    rumbo: Double,
    areaHa: Double,
    tramos: Int,
    registrando: Boolean,
    seguir: Boolean,
    brujulaDisponible: Boolean,
    puedeAnclar: Boolean,
    tieneAncla: Boolean,
    alCambiarAncho: (Double) -> Unit,
    alCambiarRumbo: (Double) -> Unit,
    alUsarBrujula: () -> Unit,
    alAnclar: () -> Unit,
    alAlternarRegistro: () -> Unit,
    alCentrar: () -> Unit,
    alAlternarSeguir: () -> Unit,
    alNuevo: () -> Unit,
    alOcultar: () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 6.dp
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Ancho del apero",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "${"%.1f".format(ancho)} m · ${"%.2f".format(areaHa)} ha · " +
                            "$tramos tramos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = alOcultar) { Text("Ocultar") }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BotonPaso("−", enabled = ancho > PasosAjuste.ANCHO_MIN_M) {
                    alCambiarAncho(ancho - PasosAjuste.ANCHO_PASO_M)
                }
                Slider(
                    value = ancho.toFloat(),
                    onValueChange = { alCambiarAncho(it.toDouble()) },
                    valueRange = PasosAjuste.ANCHO_MIN_M..PasosAjuste.ANCHO_MAX_M,
                    steps = PasosAjuste.pasosSlider(
                        PasosAjuste.ANCHO_MIN_M,
                        PasosAjuste.ANCHO_MAX_M,
                        PasosAjuste.ANCHO_PASO_M
                    ),
                    modifier = Modifier.weight(1f)
                )
                BotonPaso("+", enabled = ancho < PasosAjuste.ANCHO_MAX_M) {
                    alCambiarAncho(ancho + PasosAjuste.ANCHO_PASO_M)
                }
            }

            HorizontalDivider()

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Orientación de las líneas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${"%.0f".format(rumbo)}° ${cardinal(rumbo)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Slider(
                value = rumbo.toFloat().coerceIn(0f, 359f),
                onValueChange = { alCambiarRumbo(it.toDouble()) },
                valueRange = 0f..359f,
                steps = 358
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Preset("N 0°", rumbo, 0.0, Modifier.weight(1f), alCambiarRumbo)
                Preset("E 90°", rumbo, 90.0, Modifier.weight(1f), alCambiarRumbo)
                Preset("S 180°", rumbo, 180.0, Modifier.weight(1f), alCambiarRumbo)
                Preset("O 270°", rumbo, 270.0, Modifier.weight(1f), alCambiarRumbo)
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = alUsarBrujula,
                    enabled = brujulaDisponible,
                    modifier = Modifier.weight(1f).height(48.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) { Text("Usar brújula", textAlign = TextAlign.Center) }
                OutlinedButton(
                    onClick = alAnclar,
                    enabled = puedeAnclar,
                    modifier = Modifier.weight(1f).height(48.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) { Text(if (tieneAncla) "Ancla aquí" else "Poner ancla", textAlign = TextAlign.Center) }
            }

            HorizontalDivider()

            Button(
                onClick = alAlternarRegistro,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(
                    if (registrando) "Pausar pintura" else "Reanudar pintura",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = alCentrar,
                    modifier = Modifier.weight(1f).height(50.dp)
                ) { Text("Centrar") }
                OutlinedButton(
                    onClick = alAlternarSeguir,
                    modifier = Modifier.weight(1f).height(50.dp)
                ) { Text(if (seguir) "Seguir: sí" else "Seguir: no") }
            }
            TextButton(
                onClick = alNuevo,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Nuevo recorrido (borra solo lo pintado)") }
        }
    }
}

@Composable
private fun BotonPaso(texto: String, enabled: Boolean, alPulsar: () -> Unit) {
    OutlinedButton(
        onClick = alPulsar,
        enabled = enabled,
        modifier = Modifier.size(54.dp),
        contentPadding = PaddingValues(0.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Text(texto, fontSize = 28.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Preset(
    etiqueta: String,
    rumboActual: Double,
    valor: Double,
    modifier: Modifier = Modifier,
    alElegir: (Double) -> Unit
) {
    FilterChip(
        selected = kotlin.math.abs(rumboActual - valor) < 0.5,
        onClick = { alElegir(valor) },
        label = { Text(etiqueta, maxLines = 1) },
        modifier = modifier
    )
}

/** Rumbo en texto cardinal (N, NE, E, SE, S, SO, O, NO). */
internal fun cardinal(grados: Double): String = when {
    grados < 22.5 || grados >= 337.5 -> "N"
    grados < 67.5 -> "NE"
    grados < 112.5 -> "E"
    grados < 157.5 -> "SE"
    grados < 202.5 -> "S"
    grados < 247.5 -> "SO"
    grados < 292.5 -> "O"
    else -> "NO"
}

@Suppress("DEPRECATION")
private fun rotacionPantalla(vista: View): Int {
    val rotacion = if (Build.VERSION.SDK_INT >= 30) {
        vista.display?.rotation ?: Surface.ROTATION_0
    } else {
        (vista.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
            .defaultDisplay.rotation
    }
    return when (rotacion) {
        Surface.ROTATION_90 -> 90
        Surface.ROTATION_180 -> 180
        Surface.ROTATION_270 -> 270
        else -> 0
    }
}

private fun abrirAjustesApp(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}
