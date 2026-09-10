package com.example.app_alimentador;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.HashMap;


public class MainActivity extends AppCompatActivity {

    private EditText editTextUsuario, editTextPass;
    private Button btnInicioSesion;
    private Button btnNuevo;
    public static HashMap<String, String> listaUsuarios;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        editTextUsuario = findViewById(R.id.Usuario_EditText);
        editTextPass = findViewById(R.id.Pass_EditText);
        btnInicioSesion = findViewById(R.id.btn_IniciarSesion);
        btnNuevo = findViewById(R.id.btnNuevo);


        // Solo se siembra la primera vez: volver a MainActivity crea una nueva
        // instancia, y reasignar el mapa aqui borraba los usuarios recien creados.
        if (listaUsuarios == null) {
            listaUsuarios = new HashMap<>();
            listaUsuarios.put("user","user");
            listaUsuarios.put("mhviteri","1726419417");
            listaUsuarios.put("admin","admin");
            listaUsuarios.put("feria","feria");
        }

        btnInicioSesion.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v){
                String usuario = editTextUsuario.getText().toString().trim();
                String pass = editTextPass.getText().toString().trim();

                if (listaUsuarios.containsKey(usuario)){
                    if (listaUsuarios.get(usuario).equals(pass)){
                        Toast.makeText(MainActivity.this, "Acceso exitoso", Toast.LENGTH_SHORT).show();

                        // Cada sesion arranca con la bitacora en blanco: el total
                        // de alimento que ve el usuario es el suyo, no lo que
                        // dispenso quien entro antes en este mismo proceso.
                        RegistroAlimentacion.get().limpiar();

                        Intent intent1 = new Intent(MainActivity.this, opcionesActivity.class);
                        startActivity(intent1);
                    }else {
                        Toast.makeText(MainActivity.this, "Contraseña incorrecta", Toast.LENGTH_SHORT).show();
                    }
                }else {
                    Toast.makeText(MainActivity.this, "Usuario no encontrado", Toast.LENGTH_SHORT).show();
                }
            }

        });
        btnNuevo.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent2 = new Intent(MainActivity.this, crearUsuarioActivity.class);
                startActivity(intent2);
            }
        });

    }
}