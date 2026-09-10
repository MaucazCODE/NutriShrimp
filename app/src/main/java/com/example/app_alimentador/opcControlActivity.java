package com.example.app_alimentador;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Bifurcacion de la rama de control: alimentacion o navegacion.
 * Se interpone entre opcionesActivity y controlActivity.
 */
public class opcControlActivity extends AppCompatActivity {

    private Button btnAlimentacion, btnNavegacion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.opc_control);

        btnAlimentacion = findViewById(R.id.btnAlimentacion);
        btnNavegacion = findViewById(R.id.btnNavegacion);

        btnAlimentacion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(opcControlActivity.this, controlActivity.class));
            }
        });

        btnNavegacion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(opcControlActivity.this, controlNavActivity.class));
            }
        });
    }
}
