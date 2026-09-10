package com.example.app_alimentador;

import android.content.Context;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

/**
 * Color de la lectura de temperatura en vivo.
 *
 * El color es un degradado continuo, no tres escalones: se interpola entre tres
 * anclas para que el cambio acompanie a la lectura en lugar de saltar.
 *
 * <pre>
 *   &lt;= 20 C      azul claro
 *      27.5 C     verde        (centro de la banda templada 20-35)
 *   &gt;= 35 C      rojo
 * </pre>
 *
 * Tine solo la barra de la lectura en vivo. La cifra conserva su color fijo, y
 * el historico tambien: alli el color identifica al parametro, no a su valor.
 */
final class Termometro {

    private static final double T_FRIO = 20d;
    private static final double T_TEMPLADO = 27.5d;
    private static final double T_CALIENTE = 35d;

    private Termometro() {
    }

    /**
     * Color pleno del degradado. Pensado para la barra, que es una mancha de
     * color grande y no necesita cumplir contraste de texto.
     */
    static int color(Context ctx, double celsius) {
        int frio = ContextCompat.getColor(ctx, R.color.temp_frio);
        int templado = ContextCompat.getColor(ctx, R.color.temp_templado);
        int caliente = ContextCompat.getColor(ctx, R.color.temp_caliente);

        if (celsius <= T_FRIO) {
            return frio;
        }
        if (celsius >= T_CALIENTE) {
            return caliente;
        }
        if (celsius <= T_TEMPLADO) {
            float t = (float) ((celsius - T_FRIO) / (T_TEMPLADO - T_FRIO));
            return ColorUtils.blendARGB(frio, templado, t);
        }
        float t = (float) ((celsius - T_TEMPLADO) / (T_CALIENTE - T_TEMPLADO));
        return ColorUtils.blendARGB(templado, caliente, t);
    }

    /** Color de la barra cuando no hay dato: gris neutro, fuera del degradado. */
    static int colorSinDato(Context ctx) {
        return ContextCompat.getColor(ctx, R.color.temp_sin_dato);
    }
}
