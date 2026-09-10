package com.example.app_alimentador;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Grafico del registro de alimentacion: una barra por descarga, rotulada con
 * los kilos arriba y con la hora abajo.
 *
 * Se parece a {@link BarChartView} pero no puede ser el mismo: alli el eje X es
 * tiempo regular (una muestra cada 5 s) y la escala vertical es fija porque el
 * rango del sensor se conoce de antemano. Aqui cada ranura es un evento suelto,
 * las descargas no estan espaciadas de forma regular, y el tope depende de lo
 * que pida el usuario, asi que la escala se ajusta a lo dispensado.
 *
 * Cada barra lleva su cifra encima: los colores de serie quedan por debajo de
 * 3:1 sobre el fondo rosado, asi que el valor tiene que poder leerse sin
 * depender del color de la barra.
 */
public class AlimentacionChartView extends View {

    /** Descargas dibujadas a la vez; con mas, las horas se encimarian. */
    private static final int MAX_BARRAS = 8;

    private final List<RegistroAlimentacion.Evento> eventos = new ArrayList<>();
    private final SimpleDateFormat formatoHora =
            new SimpleDateFormat("HH:mm", Locale.getDefault());

    private final Paint barraPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rejillaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ejePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textoTenuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textoPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Path barraPath = new Path();
    private final RectF barraRect = new RectF();
    private final float[] radios = new float[8];

    private float dp;
    private float sp;

    public AlimentacionChartView(Context context) {
        this(context, null);
    }

    public AlimentacionChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        dp = context.getResources().getDisplayMetrics().density;
        sp = context.getResources().getDisplayMetrics().scaledDensity;

        barraPaint.setStyle(Paint.Style.FILL);
        barraPaint.setColor(ContextCompat.getColor(context, R.color.serie_alimento));

        // Mismo cromo que el historico de sensores: rejilla y eje recesivos,
        // linea solida de 1px, nunca punteada.
        rejillaPaint.setStyle(Paint.Style.STROKE);
        rejillaPaint.setStrokeWidth(Math.max(1f, dp));
        rejillaPaint.setColor(ContextCompat.getColor(context, R.color.grafico_rejilla));

        ejePaint.setStyle(Paint.Style.STROKE);
        ejePaint.setStrokeWidth(Math.max(1f, dp));
        ejePaint.setColor(ContextCompat.getColor(context, R.color.grafico_eje));

        textoTenuePaint.setColor(ContextCompat.getColor(context, R.color.grafico_texto_tenue));
        textoTenuePaint.setTextSize(10 * sp);

        textoPaint.setColor(ContextCompat.getColor(context, R.color.grafico_texto));
        textoPaint.setTextSize(11 * sp);
        textoPaint.setFakeBoldText(true);
        textoPaint.setTextAlign(Paint.Align.CENTER);
    }

    /**
     * Reemplaza lo dibujado por las descargas dadas, de la mas vieja a la mas
     * nueva. La vista no acumula nada: la bitacora vive en RegistroAlimentacion.
     */
    public void setEventos(List<RegistroAlimentacion.Evento> nuevos) {
        eventos.clear();
        eventos.addAll(nuevos);
        // Solo caben MAX_BARRAS; se conservan las mas recientes.
        while (eventos.size() > MAX_BARRAS) {
            eventos.remove(0);
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float padIzq = 34 * dp;    // espacio para las etiquetas del eje Y
        float padDer = 10 * dp;
        float padArriba = 22 * dp; // aire para la cifra encima de las barras
        float bandaX = 18 * dp;    // banda de las horas, para que no se recorten

        float plotIzq = padIzq;
        float plotDer = getWidth() - padDer;
        float plotArriba = padArriba;
        float plotAbajo = getHeight() - bandaX;

        if (plotDer <= plotIzq || plotAbajo <= plotArriba) {
            return;
        }

        float alto = plotAbajo - plotArriba;
        float escalaMax = calcularEscala();

        // --- Rejilla + etiquetas del eje Y (0 / mitad / tope) ---
        for (int i = 0; i <= 2; i++) {
            float frac = i / 2f;
            float y = plotAbajo - alto * frac;
            if (i > 0) {
                canvas.drawLine(plotIzq, y, plotDer, y, rejillaPaint);
            }
            String tick = String.format(Locale.getDefault(), "%.1f", escalaMax * frac);
            textoTenuePaint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(tick, plotIzq - 6 * dp, y + 3.5f * dp, textoTenuePaint);
        }

        canvas.drawLine(plotIzq, plotAbajo, plotDer, plotAbajo, ejePaint);

        if (eventos.isEmpty()) {
            textoTenuePaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("Sin descargas en las últimas 24 h",
                    (plotIzq + plotDer) / 2f, (plotArriba + plotAbajo) / 2f, textoTenuePaint);
            return;
        }

        // --- Barras ---
        // Siempre se reservan MAX_BARRAS ranuras para que el ancho no cambie a
        // medida que se acumulan descargas.
        float anchoRanura = (plotDer - plotIzq) / MAX_BARRAS;
        float separacion = 3 * dp;
        float anchoBarra = Math.min(anchoRanura - separacion, 28 * dp);
        float radio = 4 * dp;

        int n = eventos.size();
        // Alineado a la izquierda, al reves que el historico de sensores: alli
        // "ahora" esta fijo a la derecha, pero una bitacora se lee como una
        // lista que crece, y la primera descarga de la sesion es la primera.
        for (int i = 0; i < n; i++) {
            RegistroAlimentacion.Evento e = eventos.get(i);

            float frac = (float) (e.kg / escalaMax);
            if (frac < 0f) frac = 0f;
            if (frac > 1f) frac = 1f;

            float altoBarra = alto * frac;
            float centro = plotIzq + anchoRanura * (i + 0.5f);

            if (altoBarra >= 1f) {
                float arriba = plotAbajo - altoBarra;
                barraRect.set(centro - anchoBarra / 2f, arriba,
                        centro + anchoBarra / 2f, plotAbajo);
                // Extremo redondeado arriba, cuadrado en la linea base.
                radios[0] = radio; radios[1] = radio; // sup-izq
                radios[2] = radio; radios[3] = radio; // sup-der
                radios[4] = 0f;    radios[5] = 0f;    // inf-der
                radios[6] = 0f;    radios[7] = 0f;    // inf-izq
                barraPath.reset();
                barraPath.addRoundRect(barraRect, radios, Path.Direction.CW);
                canvas.drawPath(barraPath, barraPaint);
            }

            // Cifra encima de la barra: cuanto se dispenso en esa descarga.
            String kg = String.format(Locale.getDefault(), "%.1f", e.kg);
            canvas.drawText(kg, centro, plotAbajo - altoBarra - 5 * dp, textoPaint);

            // Hora de la descarga, debajo de su propia barra.
            textoTenuePaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(formatoHora.format(new Date(e.instante)),
                    centro, plotAbajo + 13 * dp, textoTenuePaint);
        }
    }

    /**
     * Tope del eje Y: el siguiente kilo entero por encima de la descarga mayor,
     * con un minimo de 1 kg para que una bitacora vacia o de valores muy chicos
     * no colapse la escala.
     */
    private float calcularEscala() {
        double mayor = 0d;
        for (RegistroAlimentacion.Evento e : eventos) {
            if (e.kg > mayor) {
                mayor = e.kg;
            }
        }
        return (float) Math.max(1d, Math.ceil(mayor));
    }
}
