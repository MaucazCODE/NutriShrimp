package com.example.app_alimentador;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.ValueEventListener;

import java.util.Locale;


public class monitoreoActivity  extends AppCompatActivity {

    private static final String TAG = "monitoreoActivity";

    private static final int TEMP_MAX = 50;
    private static final int SONIDO_MAX = 100;
    // La escala de pH es 0-14. La ProgressBar solo admite enteros, asi que se
    // trabaja en decimas (0-140) para no perder el decimal en la barra.
    private static final int PH_MAX = 14;
    private static final int PH_DECIMAS = PH_MAX * 10;

    private ProgressBar temperatura;
    private TextView tempTextView;

    private ProgressBar sonido;
    private TextView sonidoTextView;

    private ProgressBar ph;
    private TextView phTextView;

    DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
    DatabaseReference tempRef = ref.child("temperatura");
    DatabaseReference sonidoRef = ref.child("nivelSonido");
    DatabaseReference phRef = ref.child("ph");

    // Se guardan para poder desregistrarlos en onStop()
    private ValueEventListener tempListener;
    private ValueEventListener sonidoListener;
    private ValueEventListener phListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.monitoreo);
        tempTextView = findViewById(R.id.num);

        temperatura = findViewById(R.id.progressBar);
        sonido=findViewById(R.id.progressBar2);
        sonidoTextView = findViewById(R.id.sonido);
        ph = findViewById(R.id.progressBar3);
        phTextView = findViewById(R.id.ph);
        temperatura.setMax(TEMP_MAX);
        sonido.setMax(SONIDO_MAX);
        ph.setMax(PH_DECIMAS);

        findViewById(R.id.btnHistorico).setOnClickListener(v ->
                startActivity(new Intent(monitoreoActivity.this, registroActivity.class)));
    }

    @Override
    protected void onStart(){
        super.onStart();

        tempListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Double valor = Lecturas.leerNumero(snapshot);
                if (valor == null) {
                    sinTemperatura();
                    return;
                }
                temperatura.setProgress(Lecturas.aProgreso(valor, TEMP_MAX));
                tempTextView.setText(String.format(Locale.getDefault(), "%.1f°C", valor));
                pintarTemperatura(valor);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Fallo al leer temperatura.", error.toException());
                sinTemperatura();
            }
        };
        tempRef.addValueEventListener(tempListener);

        sonidoListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Double valor = Lecturas.leerNumero(snapshot);
                if (valor == null) {
                    sonido.setProgress(0);
                    sonidoTextView.setText("--%");
                    return;
                }
                sonido.setProgress(Lecturas.aProgreso(valor, SONIDO_MAX));
                sonidoTextView.setText(String.format(Locale.getDefault(), "%.0f%%", valor));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Fallo al leer nivel de sonido.", error.toException());
                sonido.setProgress(0);
                sonidoTextView.setText("--%");
            }
        };
        sonidoRef.addValueEventListener(sonidoListener);

        phListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Double valor = Lecturas.leerNumero(snapshot);
                if (valor == null) {
                    ph.setProgress(0);
                    phTextView.setText("--");
                    return;
                }
                ph.setProgress(Lecturas.aProgreso(valor * 10, PH_DECIMAS));
                phTextView.setText(String.format(Locale.getDefault(), "%.1f", valor));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Fallo al leer pH.", error.toException());
                ph.setProgress(0);
                phTextView.setText("--");
            }
        };
        phRef.addValueEventListener(phListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
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

    /**
     * Tine la barra de temperatura segun el valor leido: es lo que convierte la
     * lectura en vivo en un degradado azul-verde-rojo. Solo la barra, la cifra
     * conserva siempre su color. El historico tampoco se tine, alli el color
     * identifica al parametro.
     */
    private void pintarTemperatura(double celsius) {
        temperatura.setProgressTintList(
                ColorStateList.valueOf(Termometro.color(this, celsius)));
    }

    /** Estado sin lectura: barra a cero en gris neutro y cifra en guiones. */
    private void sinTemperatura() {
        temperatura.setProgress(0);
        tempTextView.setText("--°C");
        temperatura.setProgressTintList(
                ColorStateList.valueOf(Termometro.colorSinDato(this)));
    }
}
