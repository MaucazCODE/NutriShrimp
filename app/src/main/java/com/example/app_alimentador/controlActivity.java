package com.example.app_alimentador;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Control de alimentacion: cantidad de comida y alcance de aspersion.
 *
 * La vista habla en kilogramos y en metros porque es lo que entiende el
 * usuario; la base de datos guarda gramos en {@code pwm} y el nivel 0-10 en
 * {@code aspersor}. Las dos conversiones se hacen aqui, justo antes de escribir.
 */
public class controlActivity extends AppCompatActivity {

    private static final String TAG = "controlActivity";

    // Nodos de Firebase que toca esta vista.
    private static final String MOTORES = "motores";
    private static final String PWM = "pwm";
    private static final String PARO = "paro";
    private static final String ASPERSOR = "aspersor";

    private static final int GRAMOS_POR_KG = 1000;

    // Presets de alimento, en kilogramos (los rotulos del layout).
    private static final double KG_OPCION_1 = 2.5d;
    private static final double KG_OPCION_2 = 4.5d;
    private static final double KG_OPCION_3 = 5.5d;
    private static final double KG_OPCION_4 = 6d;

    /** Tope del campo libre: el mayor de los presets. */
    private static final double KG_MAX = KG_OPCION_4;

    // Presets de aspersion, en metros.
    private static final double METROS_OPCION_1 = 5d;
    private static final double METROS_OPCION_2 = 10d;
    private static final double METROS_OPCION_3 = 13d;

    private Button btnMotores;
    private Button btnParo;
    private Button btnProgramar;

    private RadioGroup opciones;
    private RadioButton alimento1;
    private RadioButton alimento2;
    private RadioButton alimento3;
    private RadioButton alimento4;
    private EditText otroValor;

    private RadioGroup opcionesAspersion;
    private RadioButton aspersion1;
    private RadioButton aspersion2;
    private RadioButton aspersion3;

    private final DatabaseReference raiz = FirebaseDatabase.getInstance().getReference();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.control);

        btnMotores = findViewById(R.id.btnMotores);
        btnParo = findViewById(R.id.btnParo);
        btnProgramar = findViewById(R.id.btnProgramarCiclos);

        opciones = findViewById(R.id.opciones);
        alimento1 = findViewById(R.id.rbAlimento2_5);
        alimento2 = findViewById(R.id.rbAlimento4_5);
        alimento3 = findViewById(R.id.rbAlimento5_5);
        alimento4 = findViewById(R.id.rbAlimento6);
        otroValor = findViewById(R.id.editTextOtro);

        opcionesAspersion = findViewById(R.id.opcionesAspersion);
        aspersion1 = findViewById(R.id.rbAspersion5);
        aspersion2 = findViewById(R.id.rbAspersion10);
        aspersion3 = findViewById(R.id.rbAspersion13);

        setupClickListeners();
    }

    private void setupClickListeners() {
        btnMotores.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                encender();
            }
        });

        btnParo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                parar();
            }
        });

        btnProgramar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(controlActivity.this, progActivity.class));
            }
        });
    }

    /**
     * Manda la orden completa en una sola escritura: cantidad, alcance y
     * arranque viajan juntos, asi que el firmware nunca ve los motores
     * encendidos con una cantidad que todavia es la de la orden anterior.
     */
    private void encender() {
        Double kg = kilosPedidos();
        if (kg == null) {
            return; // kilosPedidos ya explico al usuario que falta o que sobra
        }
        int gramos = (int) Math.round(kg * GRAMOS_POR_KG);

        Map<String, Object> orden = new HashMap<>();
        orden.put(PWM, gramos);
        orden.put(MOTORES, true);
        // Una orden nueva levanta el paro: si no, la parada quedaria enganchada
        // y el equipo ignoraria todo lo que se mandase despues.
        orden.put(PARO, false);

        Double metros = metrosPedidos();
        if (metros != null) {
            orden.put(ASPERSOR, Aspersion.aNivel(metros));
        }

        btnMotores.setEnabled(false);
        raiz.updateChildren(orden)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Orden enviada: " + orden);
                    // La bitacora no se toca aqui: pesa cada descarga por el
                    // tiempo que los motores esten encendidos, y de eso se
                    // encarga RegistroAlimentacion siguiendo el nodo /motores.
                    btnMotores.setEnabled(true);
                    Toast.makeText(controlActivity.this,
                            mensajeDeOrden(kg, metros), Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Fallo al enviar la orden.", e);
                    btnMotores.setEnabled(true);
                    Toast.makeText(controlActivity.this,
                            "Error al enviar la orden: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * PARO: apaga los motores, deja la cantidad en cero y levanta la bandera
     * {@code paro}, todo en una unica escritura para que no quede a medias.
     * El alcance de aspersion no se toca: es un ajuste, no algo que se mueva.
     */
    private void parar() {
        Map<String, Object> paro = new HashMap<>();
        paro.put(MOTORES, false);
        paro.put(PWM, 0);
        paro.put(PARO, true);

        raiz.updateChildren(paro)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "PARO aplicado.");
                    Toast.makeText(controlActivity.this,
                            "Motores detenidos.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Fallo al aplicar el PARO.", e);
                    Toast.makeText(controlActivity.this,
                            "Error al detener motores: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Kilos que pide la vista: manda el campo libre si tiene algo escrito y, si
     * no, el preset marcado. Devuelve null (y avisa) si no hay nada valido.
     */
    private Double kilosPedidos() {
        String escrito = otroValor.getText().toString().trim();
        if (!escrito.isEmpty()) {
            double kg;
            try {
                // El teclado decimal usa coma en los locales que la llevan.
                kg = Double.parseDouble(escrito.replace(',', '.'));
            } catch (NumberFormatException e) {
                aviso("Por favor, ingrese un valor numérico válido");
                return null;
            }
            if (kg <= 0 || kg > KG_MAX) {
                aviso(String.format(Locale.getDefault(),
                        "Por favor, ingrese un valor válido (%.1f kg máximo)", KG_MAX));
                return null;
            }
            return kg;
        }

        int marcado = opciones.getCheckedRadioButtonId();
        if (marcado == alimento1.getId()) {
            return KG_OPCION_1;
        }
        if (marcado == alimento2.getId()) {
            return KG_OPCION_2;
        }
        if (marcado == alimento3.getId()) {
            return KG_OPCION_3;
        }
        if (marcado == alimento4.getId()) {
            return KG_OPCION_4;
        }
        aviso("Por favor, seleccione una cantidad de comida");
        return null;
    }

    /**
     * Metros de aspersion marcados, o null si el usuario no eligio ninguno.
     * Es opcional a proposito: sin seleccion no se toca {@code aspersor} y se
     * respeta el alcance que ya tuviera configurado el equipo.
     */
    private Double metrosPedidos() {
        int marcado = opcionesAspersion.getCheckedRadioButtonId();
        if (marcado == aspersion1.getId()) {
            return METROS_OPCION_1;
        }
        if (marcado == aspersion2.getId()) {
            return METROS_OPCION_2;
        }
        if (marcado == aspersion3.getId()) {
            return METROS_OPCION_3;
        }
        return null;
    }

    private static String mensajeDeOrden(double kg, Double metros) {
        if (metros == null) {
            return String.format(Locale.getDefault(), "Motores activados. %.1f kg", kg);
        }
        return String.format(Locale.getDefault(),
                "Motores activados. %.1f kg, aspersión %.0f m", kg, metros);
    }

    private void aviso(String texto) {
        Toast.makeText(this, texto, Toast.LENGTH_SHORT).show();
    }
}
