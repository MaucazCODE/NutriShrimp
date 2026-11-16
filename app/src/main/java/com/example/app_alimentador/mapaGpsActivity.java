package com.example.app_alimentador;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class mapaGpsActivity extends AppCompatActivity {

    private static final String TAG = "mapaGpsActivity";

    private final DatabaseReference raiz = FirebaseDatabase.getInstance().getReference();
    private final List<GeoPoint> puntosRuta = new ArrayList<>();
    private final List<Overlay> overlaysRuta = new ArrayList<>();

    private MapView mapa;
    private TextView coordenadaActual;
    private ValueEventListener actualListener;
    private Marker marcadorActual;
    private boolean centradoEnActual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.mapa_gps);

        mapa = findViewById(R.id.mapa);
        coordenadaActual = findViewById(R.id.coordenadaActual);
        Button limpiarRuta = findViewById(R.id.btnLimpiarRuta);
        Button activarAutomatico = findViewById(R.id.btnActivarAutomatico);

        mapa.setTileSource(TileSourceFactory.MAPNIK);
        mapa.setMultiTouchControls(true);
        mapa.getController().setZoom(3.0);
        mapa.getController().setCenter(new GeoPoint(0.0, 0.0));
        mapa.getOverlays().add(new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint punto) {
                agregarPunto(punto);
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint punto) {
                return false;
            }
        }));

        limpiarRuta.setOnClickListener(v -> {
            puntosRuta.clear();
            dibujarRuta();
            Toast.makeText(this, R.string.ruta_limpiada, Toast.LENGTH_SHORT).show();
        });
        activarAutomatico.setOnClickListener(v -> activarModoAutomatico());
    }

    @Override
    protected void onStart() {
        super.onStart();
        DatabaseReference actualRef = raiz.child("actual");
        actualListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                mostrarActual(snapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "No se pudo leer actual desde Firebase.", error.toException());
                Toast.makeText(mapaGpsActivity.this,
                        getString(R.string.error_cargar_actual, error.getMessage()),
                        Toast.LENGTH_LONG).show();
            }
        };
        actualRef.addValueEventListener(actualListener);
    }

    @Override
    protected void onStop() {
        if (actualListener != null) {
            raiz.child("actual").removeEventListener(actualListener);
            actualListener = null;
        }
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapa.onResume();
    }

    @Override
    protected void onPause() {
        mapa.onPause();
        super.onPause();
    }

    private void agregarPunto(GeoPoint punto) {
        puntosRuta.add(punto);
        dibujarRuta();
        Toast.makeText(this, getString(R.string.punto_agregado, puntosRuta.size()),
                Toast.LENGTH_SHORT).show();
    }

    private void dibujarRuta() {
        mapa.getOverlays().removeAll(overlaysRuta);
        overlaysRuta.clear();

        if (puntosRuta.size() > 1) {
            Polyline linea = new Polyline();
            linea.setPoints(new ArrayList<>(puntosRuta));
            linea.setColor(Color.BLUE);
            linea.setWidth(8.0f);
            mapa.getOverlays().add(linea);
            overlaysRuta.add(linea);
        }

        for (int i = 0; i < puntosRuta.size(); i++) {
            Marker marcador = new Marker(mapa);
            marcador.setPosition(puntosRuta.get(i));
            marcador.setTitle(getString(R.string.punto_ruta, i + 1));
            marcador.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            mapa.getOverlays().add(marcador);
            overlaysRuta.add(marcador);
        }
        mapa.invalidate();
    }

    private void mostrarActual(DataSnapshot snapshot) {
        Double latitud = Lecturas.leerNumero(snapshot.child("latitud"));
        Double longitud = Lecturas.leerNumero(snapshot.child("longitud"));
        if (!coordenadaValida(latitud, longitud)) {
            coordenadaActual.setText(R.string.actual_sin_dato);
            if (snapshot.exists()) {
                Log.w(TAG, "La coordenada publicada en actual no es válida.");
                Toast.makeText(this, R.string.coordenada_invalida, Toast.LENGTH_SHORT).show();
            }
            if (marcadorActual != null) {
                mapa.getOverlays().remove(marcadorActual);
                marcadorActual = null;
                mapa.invalidate();
            }
            return;
        }

        coordenadaActual.setText(getString(R.string.actual_coordenada,
                String.format(Locale.US, "%.6f", latitud),
                String.format(Locale.US, "%.6f", longitud)));
        GeoPoint posicion = new GeoPoint(latitud, longitud);
        if (marcadorActual == null) {
            marcadorActual = new Marker(mapa);
            marcadorActual.setTitle("Actual");
            marcadorActual.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            mapa.getOverlays().add(marcadorActual);
        }
        marcadorActual.setPosition(posicion);

        if (!centradoEnActual) {
            mapa.getController().setZoom(17.0);
            mapa.getController().animateTo(posicion);
            centradoEnActual = true;
        }
        mapa.invalidate();
    }

    private boolean coordenadaValida(Double latitud, Double longitud) {
        return latitud != null && longitud != null
                && Double.isFinite(latitud) && Double.isFinite(longitud)
                && latitud >= -90.0 && latitud <= 90.0
                && longitud >= -180.0 && longitud <= 180.0;
    }

    private void activarModoAutomatico() {
        if (puntosRuta.isEmpty()) {
            Toast.makeText(this, R.string.ruta_vacia, Toast.LENGTH_LONG).show();
            return;
        }

        List<Map<String, Object>> ruta = new ArrayList<>();
        for (GeoPoint punto : puntosRuta) {
            Map<String, Object> coordenadas = new HashMap<>();
            coordenadas.put("latitud", punto.getLatitude());
            coordenadas.put("longitud", punto.getLongitude());
            ruta.add(coordenadas);
        }

        Map<String, Object> actualizacion = new HashMap<>();
        actualizacion.put("ruta", ruta);
        actualizacion.put("automatico", true);
        raiz.updateChildren(actualizacion)
                .addOnSuccessListener(unused -> Toast.makeText(this,
                        R.string.ruta_enviada, Toast.LENGTH_SHORT).show())
                .addOnFailureListener(error -> {
                    Log.e(TAG, "No se pudo enviar la ruta ni activar el modo automático.", error);
                    Toast.makeText(this,
                            getString(R.string.error_enviar_ruta, error.getMessage()),
                            Toast.LENGTH_LONG).show();
                });
    }
}
