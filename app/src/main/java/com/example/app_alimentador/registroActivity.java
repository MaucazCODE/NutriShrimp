package com.example.app_alimentador;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.os.Bundle;
import android.widget.TextView;

import java.util.Locale;

/**
 * Historico de los tres parametros de monitoreo, mas la bitacora de alimento
 * dispensado en la sesion.
 *
 * No muestrea nada: el buffer lo mantiene {@link HistorialMuestras}, que corre
 * desde que arranca la app. Esta activity solo se suscribe mientras es visible
 * y dibuja lo que haya, asi que entrar y salir no reinicia el historico.
 *
 * El registro de alimentacion viene de otra fuente, {@link RegistroAlimentacion},
 * que se llena desde controlActivity y se vacia en cada inicio de sesion. Por
 * eso hay dos suscripciones distintas, con ciclos de vida distintos.
 */
public class registroActivity extends AppCompatActivity
        implements HistorialMuestras.Observador, RegistroAlimentacion.Observador {

    // Mismas escalas que monitoreoActivity, para que ambas vistas coincidan.
    private static final float TEMP_MAX = 50f;
    private static final float SONIDO_MAX = 100f;
    private static final float PH_MAX = 14f;

    private BarChartView graficoTemp;
    private BarChartView graficoSonido;
    private BarChartView graficoPh;

    private TablaMuestras tablaTemp;
    private TablaMuestras tablaSonido;
    private TablaMuestras tablaPh;

    private AlimentacionChartView graficoAlimentacion;
    private TextView totalAlimentacion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.registro);

        graficoTemp = findViewById(R.id.graficoTemperatura);
        graficoSonido = findViewById(R.id.graficoSonido);
        graficoPh = findViewById(R.id.graficoPh);

        // Una ranura categorica por parametro, en orden fijo.
        graficoTemp.configurar(ContextCompat.getColor(this, R.color.serie_temperatura),
                TEMP_MAX, 1, "°C");
        graficoSonido.configurar(ContextCompat.getColor(this, R.color.serie_sonido),
                SONIDO_MAX, 0, "%");
        graficoPh.configurar(ContextCompat.getColor(this, R.color.serie_ph),
                PH_MAX, 1, "");

        // El gemelo legible de cada grafico: mismos valores, mismo orden.
        tablaTemp = new TablaMuestras(findViewById(R.id.tablaTemperatura), "°C", 1);
        tablaSonido = new TablaMuestras(findViewById(R.id.tablaSonido), "%", 0);
        tablaPh = new TablaMuestras(findViewById(R.id.tablaPh), "pH", 1);

        graficoAlimentacion = findViewById(R.id.graficoAlimentacion);
        totalAlimentacion = findViewById(R.id.totalAlimentacion);
    }

    @Override
    protected void onStart() {
        super.onStart();
        HistorialMuestras.get().registrarObservador(this);
        RegistroAlimentacion.get().registrarObservador(this);
        // Pinta de inmediato lo ya acumulado, sin esperar la proxima muestra.
        refrescar();
        refrescarAlimentacion();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Solo deja de escuchar: el muestreo sigue corriendo en segundo plano.
        HistorialMuestras.get().quitarObservador(this);
        RegistroAlimentacion.get().quitarObservador(this);
    }

    @Override
    public void onHistorialActualizado() {
        refrescar();
    }

    @Override
    public void onRegistroActualizado() {
        refrescarAlimentacion();
    }

    /** Vuelca la bitacora de alimento: barras por descarga y suma de la sesion. */
    private void refrescarAlimentacion() {
        RegistroAlimentacion r = RegistroAlimentacion.get();
        graficoAlimentacion.setEventos(r.getEventos());
        totalAlimentacion.setText(getString(R.string.totalDispensado,
                String.format(Locale.getDefault(), "%.1f", r.getTotalKg())));
    }

    private void refrescar() {
        HistorialMuestras h = HistorialMuestras.get();

        graficoTemp.setMuestras(h.getTemperatura());
        tablaTemp.actualizar(h.getTemperatura());

        graficoSonido.setMuestras(h.getSonido());
        tablaSonido.actualizar(h.getSonido());

        graficoPh.setMuestras(h.getPh());
        tablaPh.actualizar(h.getPh());
    }
}
