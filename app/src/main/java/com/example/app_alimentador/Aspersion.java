package com.example.app_alimentador;

/**
 * Conversion entre el alcance de aspersion en metros, que es lo que elige el
 * usuario, y el nivel 0-10 que guarda el nodo {@code /aspersor} de Firebase.
 *
 * La relacion es lineal y esta anclada en los dos extremos: nivel 0 = 1 m y
 * nivel 10 = 13 m. De ahi:
 *
 * <pre>
 *   metros = 1 + 1.2 * nivel
 *   nivel  = (metros - 1) / 1.2
 * </pre>
 *
 * El nivel se escribe siempre como entero: la parte decimal se redondea hacia
 * abajo si es menor que .5 y hacia arriba en caso contrario. De los tres
 * presets de la vista de control, 5 m -> 3.33 -> 3, 10 m -> 7.5 -> 8 y
 * 13 m -> 10 -> 10.
 */
final class Aspersion {

    /** Alcance con el nivel al minimo. */
    static final double METROS_EN_NIVEL_MIN = 1d;

    /** Alcance con el nivel al maximo. */
    static final double METROS_EN_NIVEL_MAX = 13d;

    static final double NIVEL_MIN = 0d;
    static final double NIVEL_MAX = 10d;

    /** Metros que suma cada unidad de nivel: (13 - 1) / 10. */
    private static final double METROS_POR_NIVEL =
            (METROS_EN_NIVEL_MAX - METROS_EN_NIVEL_MIN) / (NIVEL_MAX - NIVEL_MIN);

    private Aspersion() {
    }

    /**
     * Nivel entero 0-10 que corresponde a un alcance dado, recortado al rango.
     * Redondea al entero mas cercano: por debajo de .5 hacia abajo, de .5 en
     * adelante hacia arriba.
     */
    static int aNivel(double metros) {
        double nivel = (metros - METROS_EN_NIVEL_MIN) / METROS_POR_NIVEL;
        if (nivel < NIVEL_MIN) {
            nivel = NIVEL_MIN;
        } else if (nivel > NIVEL_MAX) {
            nivel = NIVEL_MAX;
        }
        return (int) Math.round(nivel);
    }

    /** Inversa de {@link #aNivel(double)}: alcance en metros de un nivel dado. */
    static double aMetros(double nivel) {
        return METROS_EN_NIVEL_MIN + nivel * METROS_POR_NIVEL;
    }
}
