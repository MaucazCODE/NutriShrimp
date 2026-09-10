package com.example.app_alimentador;

import android.content.Context;
import android.view.Gravity;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.List;
import java.util.Locale;

/**
 * Tabla compacta que acompana a cada BarChartView con las 12 muestras en texto.
 *
 * Es el equivalente legible del grafico: los colores de serie quedan por debajo
 * de 3:1 sobre el fondo rosado, asi que los valores tienen que poder leerse sin
 * depender del color de la barra.
 *
 * Las columnas se alinean a la derecha igual que las barras: la muestra mas
 * nueva siempre en la ultima columna, los huecos a la izquierda mientras el
 * buffer se llena.
 */
class TablaMuestras {

    private static final int COLUMNAS = HistorialMuestras.MAX_MUESTRAS;
    private static final int SEGUNDOS_POR_MUESTRA =
            (int) (HistorialMuestras.PERIODO_MUESTREO_MS / 1000L);
    private static final String VACIO = "–";

    private final TextView[] celdasValor = new TextView[COLUMNAS];
    private final int decimales;

    TablaMuestras(TableLayout contenedor, String unidad, int decimales) {
        this.decimales = decimales;

        Context ctx = contenedor.getContext();
        contenedor.setStretchAllColumns(true);

        TableRow filaTiempo = new TableRow(ctx);
        TableRow filaValor = new TableRow(ctx);

        // Columna 0: rotulo de cada fila, para que la tabla se explique sola.
        filaTiempo.addView(celda(ctx, "s", true));
        filaValor.addView(celda(ctx, unidad, true));

        for (int j = 0; j < COLUMNAS; j++) {
            int segundos = (COLUMNAS - 1 - j) * SEGUNDOS_POR_MUESTRA;
            filaTiempo.addView(celda(ctx, segundos == 0 ? "0" : "-" + segundos, true));

            TextView valor = celda(ctx, VACIO, false);
            celdasValor[j] = valor;
            filaValor.addView(valor);
        }

        contenedor.addView(filaTiempo);
        contenedor.addView(filaValor);
    }

    /** Vuelca las muestras en la fila de valores, alineadas a la derecha. */
    void actualizar(List<Float> valores) {
        int n = valores.size();
        int desplazamiento = COLUMNAS - n;
        for (int j = 0; j < COLUMNAS; j++) {
            if (j < desplazamiento) {
                celdasValor[j].setText(VACIO);
            } else {
                float v = valores.get(j - desplazamiento);
                celdasValor[j].setText(String.format(Locale.getDefault(),
                        "%." + decimales + "f", v));
            }
        }
    }

    private static TextView celda(Context ctx, String texto, boolean tenue) {
        TextView tv = new TextView(ctx);
        tv.setText(texto);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(1, 2, 1, 2);
        // Cifras de tabla: el texto nunca lleva el color de la serie.
        if (tenue) {
            tv.setTextSize(9f);
            tv.setTextColor(ContextCompat.getColor(ctx, R.color.grafico_texto_tenue));
        } else {
            tv.setTextSize(10f);
            tv.setTextColor(ContextCompat.getColor(ctx, R.color.grafico_texto));
        }
        return tv;
    }
}
