package es.surcogps.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import es.surcogps.app.soporte.RegistroDiag
import es.surcogps.app.ui.CampoScreen
import es.surcogps.app.ui.CampoViewModel

/**
 * Una sola Activity, una sola pantalla. Sin navegación inferior ni destinos
 * secundarios: el mapa ocupa todo el lienzo y los controles flotan encima.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // En campo la pantalla debe quedar encendida mientras se trabaja.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        RegistroDiag.registrar(filesDir, "ciclo", "MainActivity.onCreate (v2)")
        setContent {
            SurcoGPSApp()
        }
    }

    override fun onStart() {
        super.onStart()
        RegistroDiag.registrar(filesDir, "ciclo", "MainActivity.onStart")
    }

    override fun onResume() {
        super.onResume()
        RegistroDiag.registrar(filesDir, "ciclo", "MainActivity.onResume")
    }

    override fun onPause() {
        RegistroDiag.registrar(filesDir, "ciclo", "MainActivity.onPause")
        super.onPause()
    }

    override fun onStop() {
        RegistroDiag.registrar(filesDir, "ciclo", "MainActivity.onStop")
        super.onStop()
    }

    override fun onDestroy() {
        RegistroDiag.registrar(filesDir, "ciclo", "MainActivity.onDestroy")
        super.onDestroy()
    }
}

@Composable
fun SurcoGPSApp() {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF9CCC65),
            onPrimary = Color(0xFF1B2E00),
            primaryContainer = Color(0xFF33691E),
            onPrimaryContainer = Color(0xFFE8F5E9),
            secondary = Color(0xFFFFD54F),
            surface = Color(0xFF1C1D20),
            onSurface = Color(0xFFF1F1F1),
            surfaceVariant = Color(0xFF2A2C30),
            onSurfaceVariant = Color(0xFFC7C7C7),
            background = Color(0xFF000000),
            onBackground = Color(0xFFF1F1F1),
            error = Color(0xFFEF9A9A)
        )
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            val vm: CampoViewModel = viewModel()
            CampoScreen(vm)
        }
    }
}
