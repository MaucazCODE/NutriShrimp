package com.example.app_alimentador;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class opcionesActivity extends AppCompatActivity {

    private Button btnMonitoreo, btnControl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.opciones);

        btnMonitoreo = findViewById(R.id.btnMonitoreo);
        btnControl = findViewById(R.id.btnControl);

        btnMonitoreo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent1 = new Intent(opcionesActivity.this, monitoreoActivity.class);
                startActivity(intent1);
            }
        });
        btnControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // La rama de control ahora pasa por su propia seleccion:
                // alimentacion o navegacion.
                Intent intent1 = new Intent(opcionesActivity.this, opcControlActivity.class);
                startActivity(intent1);
            }
        });
    }
}

