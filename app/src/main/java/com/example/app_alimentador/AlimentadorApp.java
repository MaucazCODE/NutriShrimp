package com.example.app_alimentador;

import android.app.Application;

/**
 * Arranca al iniciar la app, no al abrir cada vista, los dos seguimientos que
 * tienen que sobrevivir a los cambios de pantalla:
 *
 * - el muestreo del historico, para que registroActivity encuentre datos ya
 *   acumulados cada vez que se abre;
 * - el registro de alimentacion, que pesa cada descarga por el tiempo que los
 *   motores estan encendidos y se perderia el final de casi todas si solo
 *   escuchase mientras su vista esta visible.
 */
public class AlimentadorApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        HistorialMuestras.get().iniciar();
        RegistroAlimentacion.get().iniciar();
    }
}
