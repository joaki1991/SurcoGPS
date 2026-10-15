package es.surcogps.app.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch

/**
 * Diagnóstico autónomo (sin ADB): estado, contadores, caché, eventos y
 * comprobación manual limitada + exportar informe. No toca el mapa.
 */
@Composable
fun PantallaDiagnostico(vm: CampoViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val d by vm.diagnostico.collectAsState()

    LaunchedEffect(Unit) {
        vm.refrescarDiagnostico()
    }

    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Diagnóstico", style = MaterialTheme.typography.headlineSmall)
        if (d == null) {
            Text("Cargando…")
            return@Column
        }
        val e = d!!
        Text("Permiso ubicación: ${if (e.permisoUbicacion) "sí" else "no"}")
        Text("GPS del sistema: ${if (e.gpsActivo) "activo" else "apagado o sin permiso"}")
        Text("Último fix: ${e.ultimoFix ?: "sin datos"}")
        e.stats?.let { s ->
            Text("Descarga: ${s.erroresDescarga} errores")
            Text("Caché: ${s.aciertosCache} aciertos · ${s.fallosCache} fallos · ${s.erroresGuardado} errores de guardado")
            Text("Memoria: ${s.oom} OOM · Cola: ${if (s.cola < 0) "…" else s.cola}")
        } ?: Text("Contadores: sin lectura (abrir Campo antes).")
        e.cache?.let {
            Text("Caché de teselas: ${it.ficheros} ficheros · ${it.bytes / 1024} KB · libre ${it.libreMB} MB")
            Text("Sonda directa: ${it.sondaDirecta ?: "sin ejecutar"}")
            Text("Sonda SQL: ${it.sondaSql ?: "sin ejecutar (botón Ejecutar diagnóstico)"}")
            if (it.top.isNotEmpty()) {
                Text("Ficheros: ${it.top.take(5).joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
            }
        }
        Text(
            "Cómo leerlo: HTTP ok + errores de descarga altos = el servidor limita; " +
                "errores de guardado altos = almacenamiento; OOM = memoria; " +
                "cola que no baja = reintentos continuos.",
            style = MaterialTheme.typography.bodySmall
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { vm.ejecutarDiagnostico() }, modifier = Modifier.weight(1f)) {
                Text("Ejecutar diagnóstico")
            }
            OutlinedButton(onClick = { vm.refrescarDiagnostico() }, modifier = Modifier.weight(1f)) {
                Text("Actualizar")
            }
        }
        if (e.chequeos.isNotEmpty()) {
            Text("Comprobaciones:", style = MaterialTheme.typography.titleSmall)
            e.chequeos.forEach {
                Text(
                    "• ${it.nombre}: ${if (it.ok) "OK" else "FALLO"} " +
                        "(HTTP ${it.codigo ?: "?"}, ${it.ms} ms${it.error?.let { x -> ", $x" } ?: ""})",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        if (e.cierres > 0) {
            Text("Cierres anteriores guardados: ${e.cierres} (ver Campo si es el último).")
        }
        Text("Eventos (${e.eventos.size}):", style = MaterialTheme.typography.titleSmall)
        Column(
            Modifier.fillMaxWidth().heightIn(max = 220.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (e.eventos.isEmpty()) Text("Sin eventos.", style = MaterialTheme.typography.bodySmall)
            e.eventos.takeLast(30).reversed().forEach {
                Text(
                    "${it.tag}: ${it.mensaje.take(160)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    scope.launch {
                        val f = vm.exportarInforme() ?: return@launch
                        val uri = FileProvider.getUriForFile(
                            context, "${context.packageName}.ficheros", f
                        )
                        context.startActivity(
                            Intent.createChooser(
                                Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                },
                                "Exportar informe"
                            )
                        )
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Exportar informe")
            }
            OutlinedButton(
                onClick = { vm.limpiarRegistros() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Borrar registros")
            }
        }
        Text(
            "Borrar registros no toca parcelas, sesiones ni ajustes.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
