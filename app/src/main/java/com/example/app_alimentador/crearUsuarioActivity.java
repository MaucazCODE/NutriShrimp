package com.example.app_alimentador;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class crearUsuarioActivity extends AppCompatActivity {
    private EditText editTextUsuario, editTextPass, editTextMail;
    private Button btnCrearUsuario;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.crear_usuario);


        editTextUsuario = findViewById(R.id.cUsuario);
        editTextPass = findViewById(R.id.cContra);
        editTextMail = findViewById(R.id.cMail);
        btnCrearUsuario = findViewById(R.id.btnCrear);

        btnCrearUsuario.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String usuario = editTextUsuario.getText().toString().trim();
                String pass = editTextPass.getText().toString().trim();
                String mail = editTextMail.getText().toString().trim();

                if (usuario.isEmpty() || pass.isEmpty() || mail.isEmpty()) {
                    Toast.makeText(crearUsuarioActivity.this, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (MainActivity.listaUsuarios.containsKey(usuario)) {
                    Toast.makeText(crearUsuarioActivity.this, "Ese usuario ya existe", Toast.LENGTH_SHORT).show();
                    return;
                }
                MainActivity.listaUsuarios.put(usuario, pass);
                Toast.makeText(crearUsuarioActivity.this, "Usuario creado exitosamente", Toast.LENGTH_SHORT).show();
                // Basta con cerrar: debajo esta la MainActivity que nos abrio.
                // Lanzar otra apilaba una instancia nueva en cada alta.
                finish();
            }
        });
    }
}
