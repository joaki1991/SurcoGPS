package es.surcogps.app.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider

/**
 * Aviso de cierre anterior (o nota de arranque degradado): muestra el informe
 * guardado y permite compartirlo para diagnóstico. Nada sale del móvil sin
 * que el usuario lo envíe.
 */
@Composable
fun AvisoCierre(
    texto: String,
    alCompartir: () -> Unit,
    alDescartar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "La app se cerró de forma inesperada (o arrancó degradada). " +
                    "Comparte este informe para diagnosticarlo:",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                texto.take(2000),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 160.dp)
                    .verticalScroll(rememberScrollState())
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    val archivo = context.cacheDir.resolve("cierre-para-enviar.txt").apply {
                        writeText(texto)
                    }
                    val uri = FileProvider.getUriForFile(
                        context, "${context.packageName}.ficheros", archivo
                    )
                    context.startActivity(
                        Intent.createChooser(
                            Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            },
                            "Compartir informe"
                        )
                    )
                    alCompartir()
                }) {
                    Text("Compartir")
                }
                TextButton(onClick = alDescartar) {
                    Text("Descartar")
                }
            }
        }
    }
}
