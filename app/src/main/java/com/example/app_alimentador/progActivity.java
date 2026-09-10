package com.example.app_alimentador;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Programacion de ciclos de alimentacion.
 *
 * MAQUETA: recoge cantidad, intervalo y hora de inicio, pero no escribe nada en
 * Firebase ni arma temporizador alguno. "Programar" solo confirma por Toast.
 * Cuando la programacion se implemente de verdad, es aqui donde entra la
 * escritura, junto al resto de la rama de control.
 */
public class progActivity extends AppCompatActivity {

    private Button btnProgramar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.prog);

        btnProgramar = findViewById(R.id.btnProgramar);

        btnProgramar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Sin validacion ni escritura: la vista es una maqueta y
                // confirma siempre, sea cual sea lo que se haya escrito.
                Toast.makeText(progActivity.this,
                        R.string.cicloProgramado, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
