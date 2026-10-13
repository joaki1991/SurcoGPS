package es.surcogps.app

import android.app.Application
import es.surcogps.app.motor.MotorCampo

/** Aplicación: alberga el [MotorCampo] para que el tracking sobreviva a la UI. */
class SurcoApp : Application() {
    val motor: MotorCampo by lazy { MotorCampo(this) }
}
