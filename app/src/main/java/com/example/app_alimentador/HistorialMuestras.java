package com.example.app_alimentador;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Historico compartido de los tres parametros de monitoreo.
 *
 * Vive fuera de las activities a proposito: el muestreo arranca junto con la
 * app (ver {@link AlimentadorApp}) y no se detiene al salir de
 * registroActivity, de modo que el historico sigue creciendo aunque la vista
 * este cerrada. registroActivity solo lo lee y lo dibuja.
 *
 * La base de datos guarda unicamente el valor actual de cada nodo, no una
 * serie; la serie se arma aqui muestreando ese valor cada 5 s (0.2 Hz) hacia
 * un buffer circular de 12 posiciones.
 */
final class HistorialMuestras {

    /** Tamano de la ventana: 12 muestras a 1 cada 5 s = 60 s. */
    static final int MAX_MUESTRAS = 12;

    /** 0.2 Hz. */
    static final long PERIODO_MUESTREO_MS = 5000L;

    private static final String TAG = "HistorialMuestras";

    private static HistorialMuestras instancia;

    static synchronized HistorialMuestras get() {
        if (instancia == null) {
            instancia = new HistorialMuestras();
        }
        return instancia;
    }

    /** Aviso de que llego una muestra nueva, para que la vista se redibuje. */
    interface Observador {
        void onHistorialActualizado();
    }

    private final List<Float> temperatura = new ArrayList<>();
    private final List<Float> sonido = new ArrayList<>();
    private final List<Float> ph = new ArrayList<>();

    // Ultimo valor recibido de cada nodo; null mientras no llegue nada.
    private Double ultimaTemp;
    private Double ultimoSonido;
    private Double ultimoPh;

    private final DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
    private final DatabaseReference tempRef = ref.child("temperatura");
    private final DatabaseReference sonidoRef = ref.child("nivelSonido");
    private final DatabaseReference phRef = ref.child("ph");

    private ValueEventListener tempListener;
    private ValueEventListener sonidoListener;
    private ValueEventListener phListener;

    private final Handler muestreador = new Handler(Looper.getMainLooper());
    private final List<Observador> observadores = new ArrayList<>();
    private boolean activo;

    private HistorialMuestras() {
    }

    private final Runnable tomarMuestra = new Runnable() {
        @Override
        public void run() {
            if (ultimaTemp != null) {
                empujar(temperatura, ultimaTemp.floatValue());
            }
            if (ultimoSonido != null) {
                empujar(sonido, ultimoSonido.floatValue());
            }
            if (ultimoPh != null) {
                empujar(ph, ultimoPh.floatValue());
            }
            // Copia defensiva: un observador puede darse de baja al recibir el aviso.
            for (Observador o : new ArrayList<>(observadores)) {
                o.onHistorialActualizado();
            }
            muestreador.postDelayed(this, PERIODO_MUESTREO_MS);
        }
    };

    /** Arranca el muestreo. Idempotente: llamarlo dos veces no duplica nada. */
    synchronized void iniciar() {
        if (activo) {
            return;
        }
        activo = true;

        tempListener = nuevoListener("temperatura", valor -> ultimaTemp = valor);
        tempRef.addValueEventListener(tempListener);

        sonidoListener = nuevoListener("nivelSonido", valor -> ultimoSonido = valor);
        sonidoRef.addValueEventListener(sonidoListener);

        phListener = nuevoListener("ph", valor -> ultimoPh = valor);
        phRef.addValueEventListener(phListener);

        muestreador.postDelayed(tomarMuestra, PERIODO_MUESTREO_MS);
    }

    /**
     * Detiene el muestreo y suelta los listeners. Hoy no se llama: el historico
     * corre mientras viva el proceso. Existe para poder pararlo si mas adelante
     * se quiere limitar al primer plano.
     */
    synchronized void detener() {
        if (!activo) {
            return;
        }
        activo = false;

        muestreador.removeCallbacks(tomarMuestra);

        if (tempListener != null) {
            tempRef.removeEventListener(tempListener);
            tempListener = null;
        }
        if (sonidoListener != null) {
            sonidoRef.removeEventListener(sonidoListener);
            sonidoListener = null;
        }
        if (phListener != null) {
            phRef.removeEventListener(phListener);
            phListener = null;
        }
    }

    void registrarObservador(Observador o) {
        if (!observadores.contains(o)) {
            observadores.add(o);
        }
    }

    void quitarObservador(Observador o) {
        observadores.remove(o);
    }

    List<Float> getTemperatura() {
        return Collections.unmodifiableList(temperatura);
    }

    List<Float> getSonido() {
        return Collections.unmodifiableList(sonido);
    }

    List<Float> getPh() {
        return Collections.unmodifiableList(ph);
    }

    private static void empujar(List<Float> buffer, float valor) {
        buffer.add(valor);
        while (buffer.size() > MAX_MUESTRAS) {
            buffer.remove(0);
        }
    }

    private interface AlLeer {
        void valor(Double valor);
    }

    private ValueEventListener nuevoListener(final String nodo, final AlLeer destino) {
        return new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                destino.valor(Lecturas.leerNumero(snapshot));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Fallo al leer " + nodo + ".", error.toException());
                destino.valor(null);
            }
        };
    }
}
