package com.example.app_alimentador;

import android.util.Log;

import com.google.firebase.database.DataSnapshot;

/** Utilidades compartidas para leer valores de sensores desde Firebase. */
final class Lecturas {

    private static final String TAG = "Lecturas";

    private Lecturas() {
    }

    /**
     * Lee el valor del snapshot sin asumir el tipo. El firmware puede escribir
     * enteros (Long), decimales (Double) o incluso cadenas; getValue(Integer.class)
     * lanzaba DatabaseException en los dos ultimos casos.
     *
     * @return el valor como Double, o null si el nodo no existe o no es numerico.
     */
    static Double leerNumero(DataSnapshot snapshot) {
        Object raw = snapshot.getValue();
        if (raw instanceof Number) {
            return ((Number) raw).doubleValue();
        }
        if (raw instanceof String) {
            try {
                return Double.parseDouble(((String) raw).trim());
            } catch (NumberFormatException e) {
                Log.w(TAG, "Valor no numerico en " + snapshot.getKey() + ": " + raw);
            }
        }
        return null;
    }

    /**
     * Lee una bandera sin asumir el tipo, por el mismo motivo que
     * {@link #leerNumero}: el firmware puede escribir un booleano, un 0/1 o la
     * cadena "true".
     *
     * @return el valor como Boolean, o null si el nodo no existe o no se
     *         reconoce como bandera.
     */
    static Boolean leerBooleano(DataSnapshot snapshot) {
        Object raw = snapshot.getValue();
        if (raw instanceof Boolean) {
            return (Boolean) raw;
        }
        if (raw instanceof Number) {
            return ((Number) raw).doubleValue() != 0d;
        }
        if (raw instanceof String) {
            String texto = ((String) raw).trim();
            if ("true".equalsIgnoreCase(texto) || "1".equals(texto)) {
                return Boolean.TRUE;
            }
            if ("false".equalsIgnoreCase(texto) || "0".equals(texto)) {
                return Boolean.FALSE;
            }
            Log.w(TAG, "Bandera no reconocida en " + snapshot.getKey() + ": " + raw);
        }
        return null;
    }

    /** Redondea y recorta al rango admitido por una ProgressBar. */
    static int aProgreso(double valor, int max) {
        long redondeado = Math.round(valor);
        if (redondeado < 0) return 0;
        if (redondeado > max) return max;
        return (int) redondeado;
    }
}
