package com.example.app_alimentador;

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
 * Bitacora de las descargas de alimento de las ultimas 24 h.
 *
 * Lo que se apunta no es lo que se pidio sino lo que de verdad salio: el peso
 * sale del tiempo que los motores estuvieron encendidos, a razon de
 * {@link #GRAMOS_POR_SEGUNDO}. Pedir 6 kg y cortar con el PARO a la mitad tiene
 * que anotar 3 kg, no 6.
 *
 * Por eso el registro no lo alimenta controlActivity al mandar la orden, sino
 * el propio nodo {@code /motores}: una descarga se abre cuando pasa a true y se
 * cierra cuando vuelve a false, sin importar quien lo apago (el firmware al
 * terminar, el PARO de alimentacion o el de navegacion).
 *
 * Corre desde que arranca la app (ver {@link AlimentadorApp}) y no desde que se
 * abre una vista: si solo escuchase mientras registroActivity esta visible se
 * perderia el final de casi todas las descargas.
 */
final class RegistroAlimentacion {

    /** Caudal del dosificador: lo que sale por cada segundo de motor. */
    static final double GRAMOS_POR_SEGUNDO = 32d;

    /** Ventana que cubre el registro. */
    static final long VENTANA_MS = 24L * 60L * 60L * 1000L;

    private static final String TAG = "RegistroAlimentacion";
    private static final String MOTORES = "motores";
    private static final int GRAMOS_POR_KG = 1000;

    /** Una descarga ya terminada. */
    static final class Evento {
        /** Instante en que se apagaron los motores. */
        final long instante;
        final double kg;

        Evento(long instante, double kg) {
            this.instante = instante;
            this.kg = kg;
        }
    }

    /** Aviso de que el registro cambio, para que la vista se redibuje. */
    interface Observador {
        void onRegistroActualizado();
    }

    private static RegistroAlimentacion instancia;

    static synchronized RegistroAlimentacion get() {
        if (instancia == null) {
            instancia = new RegistroAlimentacion();
        }
        return instancia;
    }

    private final List<Evento> eventos = new ArrayList<>();
    private final List<Observador> observadores = new ArrayList<>();

    private final DatabaseReference motoresRef =
            FirebaseDatabase.getInstance().getReference().child(MOTORES);
    private ValueEventListener motoresListener;
    private boolean activo;

    /** Instante en que arrancaron los motores; 0 si no hay descarga abierta. */
    private long inicioDescarga;

    private RegistroAlimentacion() {
    }

    /** Empieza a seguir el nodo /motores. Idempotente. */
    synchronized void iniciar() {
        if (activo) {
            return;
        }
        activo = true;

        motoresListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean encendido = Lecturas.leerBooleano(snapshot);
                if (encendido == null) {
                    return;
                }
                if (encendido) {
                    abrirDescarga();
                } else {
                    cerrarDescarga();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Fallo al seguir " + MOTORES + ".", error.toException());
            }
        };
        motoresRef.addValueEventListener(motoresListener);
    }

    /**
     * Deja de seguir el nodo. Hoy no se llama: el registro corre mientras viva
     * el proceso, igual que {@link HistorialMuestras}.
     */
    synchronized void detener() {
        if (!activo) {
            return;
        }
        activo = false;
        if (motoresListener != null) {
            motoresRef.removeEventListener(motoresListener);
            motoresListener = null;
        }
    }

    /**
     * Arranque de los motores. Solo se toma la primera transicion: el listener
     * puede volver a recibir true sin que haya habido un false en medio (por
     * ejemplo si se reescribe la orden), y eso no es una descarga nueva.
     */
    private void abrirDescarga() {
        if (inicioDescarga != 0L) {
            return;
        }
        inicioDescarga = System.currentTimeMillis();
    }

    /** Parada de los motores: se cierra la descarga y se pesa por tiempo. */
    private void cerrarDescarga() {
        if (inicioDescarga == 0L) {
            return; // no habia nada abierto: un false sobre otro false
        }
        long fin = System.currentTimeMillis();
        long duracionMs = fin - inicioDescarga;
        inicioDescarga = 0L;

        if (duracionMs <= 0L) {
            return;
        }
        double kg = GRAMOS_POR_SEGUNDO * (duracionMs / 1000d) / GRAMOS_POR_KG;

        eventos.add(new Evento(fin, kg));
        podar();
        avisar();
    }

    /**
     * Vacia la bitacora: se llama al iniciar sesion.
     *
     * Si hay una descarga en curso no se descarta, se le pone el reloj a cero:
     * el alimento que salio antes de este inicio de sesion no es de esta sesion,
     * pero lo que salga a partir de ahora si.
     */
    void limpiar() {
        boolean habia = !eventos.isEmpty();
        eventos.clear();
        if (inicioDescarga != 0L) {
            inicioDescarga = System.currentTimeMillis();
        }
        if (habia) {
            avisar();
        }
    }

    /** Descargas dentro de la ventana, de la mas vieja a la mas nueva. */
    List<Evento> getEventos() {
        podar();
        return Collections.unmodifiableList(eventos);
    }

    /** Suma de lo dispensado dentro de la ventana, en kilogramos. */
    double getTotalKg() {
        podar();
        double total = 0d;
        for (Evento e : eventos) {
            total += e.kg;
        }
        return total;
    }

    void registrarObservador(Observador o) {
        if (!observadores.contains(o)) {
            observadores.add(o);
        }
    }

    void quitarObservador(Observador o) {
        observadores.remove(o);
    }

    /**
     * Descarta lo que ya cayo fuera de la ventana. La lista esta en orden
     * cronologico, asi que basta con ir soltando por delante.
     */
    private void podar() {
        long limite = System.currentTimeMillis() - VENTANA_MS;
        while (!eventos.isEmpty() && eventos.get(0).instante < limite) {
            eventos.remove(0);
        }
    }

    private void avisar() {
        // Copia defensiva: un observador puede darse de baja al recibir el aviso.
        for (Observador o : new ArrayList<>(observadores)) {
            o.onRegistroActualizado();
        }
    }
}
