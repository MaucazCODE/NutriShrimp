package com.example.app_alimentador;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

/**
 * Navegacion teledirigida: mando direccional de cuatro sectores mas un PARO.
 *
 * Cada boton del mando pone en true su bandera de {@code nav} solo mientras se
 * mantiene pulsado. Las cuatro banderas se escriben siempre juntas con
 * updateChildren, asi que por construccion nunca puede haber dos en true a la
 * vez: la orden nueva y el apagado de las demas viajan en la misma escritura.
 */
public class controlNavActivity extends AppCompatActivity {

    private static final String TAG = "controlNavActivity";

    private static final String NAV_ADELANTE = "nav/adelante";
    private static final String NAV_ATRAS = "nav/atras";
    private static final String NAV_HORARIO = "nav/ho";
    private static final String NAV_ANTIHORARIO = "nav/aho";
    private static final String MOTORES = "motores";
    private static final String PWM = "pwm";
    private static final String PARO = "paro";

    private DPadView mando;
    private Button btnParo;

    private final DatabaseReference raiz = FirebaseDatabase.getInstance().getReference();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.control_nav);

        mando = findViewById(R.id.mando);
        btnParo = findViewById(R.id.btnParoNav);

        mando.setOnDireccionListener(this::enviarDireccion);

        btnParo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pararTodo();
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Red de seguridad: si la vista deja de verse (back, home, pantalla
        // apagada) no puede quedarse una orden viva moviendo el equipo.
        mando.soltarSinAvisar();
        enviarDireccion(DPadView.NINGUNA);
    }

    /**
     * Escribe el estado completo de {@code nav}: la direccion dada en true y el
     * resto en false. Con {@link DPadView#NINGUNA} las cuatro quedan en false.
     */
    private void enviarDireccion(int direccion) {
        Map<String, Object> estado = new HashMap<>();
        estado.put(NAV_ADELANTE, direccion == DPadView.ARRIBA);
        estado.put(NAV_ATRAS, direccion == DPadView.ABAJO);
        estado.put(NAV_HORARIO, direccion == DPadView.IZQUIERDA);
        estado.put(NAV_ANTIHORARIO, direccion == DPadView.DERECHA);

        // Pulsar una direccion es una orden de movimiento nueva, y como tal
        // levanta el paro. Al soltar (NINGUNA) no se toca: soltar el mando no
        // es una orden, y reponer paro=false ahi borraria un PARO reciente.
        if (direccion != DPadView.NINGUNA) {
            estado.put(PARO, false);
        }

        raiz.updateChildren(estado)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "nav actualizado: " + estado))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Fallo al actualizar nav.", e);
                    Toast.makeText(controlNavActivity.this,
                            "Error de conexión: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * PARO: detiene todo lo que se mueve, no solo la navegacion. Apaga las
     * cuatro direcciones y ademas los motores de alimentacion, todo en una
     * unica escritura para que no quede nada a medias.
     */
    private void pararTodo() {
        mando.soltarSinAvisar();

        Map<String, Object> paro = new HashMap<>();
        paro.put(NAV_ADELANTE, false);
        paro.put(NAV_ATRAS, false);
        paro.put(NAV_HORARIO, false);
        paro.put(NAV_ANTIHORARIO, false);
        paro.put(MOTORES, false);
        paro.put(PWM, 0);
        paro.put(PARO, true);

        raiz.updateChildren(paro)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "PARO aplicado.");
                    Toast.makeText(controlNavActivity.this,
                            "Motores detenidos.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Fallo al aplicar el PARO.", e);
                    Toast.makeText(controlNavActivity.this,
                            "Error al detener: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }
}
