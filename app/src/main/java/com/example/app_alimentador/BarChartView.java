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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Grafico de barras para el historico de un parametro.
 *
 * Dibuja hasta {@link #MAX_MUESTRAS} valores, el mas reciente a la derecha.
 * La escala vertical es fija (la misma que usa monitoreoActivity) para que las
 * barras sean comparables entre lecturas y no se re-escalen solas.
 *
 * Una sola serie por grafico: un unico color y sin leyenda, el titulo ya dice
 * que se esta graficando.
 */
public class BarChartView extends View {

    /** Ranuras dibujadas; la ventana la define HistorialMuestras. */
    public static final int MAX_MUESTRAS = HistorialMuestras.MAX_MUESTRAS;

    private static final int SEGUNDOS_POR_MUESTRA =
            (int) (HistorialMuestras.PERIODO_MUESTREO_MS / 1000L);

    private final List<Float> valores = new ArrayList<>();

    private float escalaMax = 100f;
    private int decimales = 1;
    private String sufijo = "";

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

    public BarChartView(Context context) {
        this(context, null);
    }

    public BarChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        dp = context.getResources().getDisplayMetrics().density;
        sp = context.getResources().getDisplayMetrics().scaledDensity;

        barraPaint.setStyle(Paint.Style.FILL);
        barraPaint.setColor(ContextCompat.getColor(context, R.color.serie_temperatura));

        // Rejilla y eje: linea solida de 1px, un paso por encima del fondo.
        // Nunca punteada: el punteado se lee como "umbral" cuando solo es rejilla.
        rejillaPaint.setStyle(Paint.Style.STROKE);
        rejillaPaint.setStrokeWidth(Math.max(1f, dp));
        rejillaPaint.setColor(ContextCompat.getColor(context, R.color.grafico_rejilla));

        ejePaint.setStyle(Paint.Style.STROKE);
        ejePaint.setStrokeWidth(Math.max(1f, dp));
        ejePaint.setColor(ContextCompat.getColor(context, R.color.grafico_eje));

        // El texto nunca lleva el color de la serie: tinta neutra siempre.
        textoTenuePaint.setColor(ContextCompat.getColor(context, R.color.grafico_texto_tenue));
        textoTenuePaint.setTextSize(10 * sp);

        textoPaint.setColor(ContextCompat.getColor(context, R.color.grafico_texto));
        textoPaint.setTextSize(13 * sp);
        textoPaint.setFakeBoldText(true);
        textoPaint.setTextAlign(Paint.Align.CENTER);
    }

    /** Configura la serie: color del mark, tope de la escala y formato del valor. */
    public void configurar(int colorSerie, float escalaMax, int decimales, String sufijo) {
        this.barraPaint.setColor(colorSerie);
        this.escalaMax = escalaMax > 0 ? escalaMax : 100f;
        this.decimales = decimales;
        this.sufijo = sufijo == null ? "" : sufijo;
        invalidate();
    }

    /**
     * Reemplaza lo dibujado por las muestras dadas, de la mas vieja a la mas
     * nueva. La vista no acumula nada: el buffer vive en HistorialMuestras.
     */
    public void setMuestras(List<Float> muestras) {
        valores.clear();
        valores.addAll(muestras);
        while (valores.size() > MAX_MUESTRAS) {
            valores.remove(0);
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float padIzq = 34 * dp;   // espacio para las etiquetas del eje Y
        float padDer = 8 * dp;
        float padArriba = 24 * dp; // aire para la etiqueta del valor mas reciente
        float bandaX = 18 * dp;    // banda del eje X, para que no se recorte

        float plotIzq = padIzq;
        float plotDer = getWidth() - padDer;
        float plotArriba = padArriba;
        float plotAbajo = getHeight() - bandaX;

        if (plotDer <= plotIzq || plotAbajo <= plotArriba) {
            return;
        }

        float alto = plotAbajo - plotArriba;

        // --- Rejilla + etiquetas del eje Y (0 / mitad / tope) ---
        for (int i = 0; i <= 2; i++) {
            float frac = i / 2f;
            float y = plotAbajo - alto * frac;
            if (i > 0) {
                canvas.drawLine(plotIzq, y, plotDer, y, rejillaPaint);
            }
            String tick = formatearTick(escalaMax * frac);
            textoTenuePaint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(tick, plotIzq - 6 * dp, y + 3.5f * dp, textoTenuePaint);
        }

        // Linea base
        canvas.drawLine(plotIzq, plotAbajo, plotDer, plotAbajo, ejePaint);

        if (valores.isEmpty()) {
            textoTenuePaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("Esperando datos...", (plotIzq + plotDer) / 2f,
                    (plotArriba + plotAbajo) / 2f, textoTenuePaint);
            return;
        }

        // --- Barras ---
        // Siempre se reservan 12 ranuras para que el ancho no cambie al llenarse.
        float anchoRanura = (plotDer - plotIzq) / MAX_MUESTRAS;
        float separacion = 2 * dp;                 // el hueco separa, no un borde
        float anchoBarra = Math.min(anchoRanura - separacion, 24 * dp);
        float radio = 4 * dp;

        int n = valores.size();
        // Alineado a la derecha: la muestra mas nueva siempre ocupa la ultima
        // ranura, para que "ahora" apunte de verdad a la barra mas reciente
        // mientras el buffer todavia se esta llenando.
        int desplazamiento = MAX_MUESTRAS - n;
        for (int i = 0; i < n; i++) {
            float valor = valores.get(i);
            float frac = valor / escalaMax;
            if (frac < 0f) frac = 0f;
            if (frac > 1f) frac = 1f;

            float altoBarra = alto * frac;
            float centro = plotIzq + anchoRanura * (desplazamiento + i + 0.5f);
            float izq = centro - anchoBarra / 2f;
            float der = centro + anchoBarra / 2f;
            float arriba = plotAbajo - altoBarra;

            if (altoBarra < 1f) {
                continue; // un valor de 0 no dibuja una barra fantasma
            }

            // Extremo redondeado de 4dp arriba, cuadrado en la linea base.
            barraRect.set(izq, arriba, der, plotAbajo);
            radios[0] = radio; radios[1] = radio; // sup-izq
            radios[2] = radio; radios[3] = radio; // sup-der
            radios[4] = 0f;    radios[5] = 0f;    // inf-der
            radios[6] = 0f;    radios[7] = 0f;    // inf-izq
            barraPath.reset();
            barraPath.addRoundRect(barraRect, radios, Path.Direction.CW);
            canvas.drawPath(barraPath, barraPaint);
        }

        // --- Etiqueta directa solo en la muestra mas reciente ---
        // Etiquetar las 12 seria ruido; el eje Y carga el resto.
        float valorUltimo = valores.get(n - 1);
        float fracUltimo = Math.max(0f, Math.min(1f, valorUltimo / escalaMax));
        float centroUltimo = plotIzq + anchoRanura * (MAX_MUESTRAS - 0.5f);
        float arribaUltimo = plotAbajo - alto * fracUltimo;

        String etiqueta = formatearValor(valorUltimo);
        float mitadTexto = textoPaint.measureText(etiqueta) / 2f;
        // Se sujeta a los bordes para que el texto no se salga de la vista.
        float xEtiqueta = Math.max(plotIzq + mitadTexto,
                Math.min(centroUltimo, getWidth() - padDer - mitadTexto));
        canvas.drawText(etiqueta, xEtiqueta, arribaUltimo - 6 * dp, textoPaint);

        // --- Eje X: solo los extremos, el resto se sobreentiende ---
        // La etiqueta de la muestra mas vieja se ancla a su propia barra, no al
        // borde del plot, que estaria vacio mientras el buffer se llena.
        textoTenuePaint.setTextAlign(Paint.Align.LEFT);
        String masViejo = "-" + ((n - 1) * SEGUNDOS_POR_MUESTRA) + " s";
        float xViejo = plotIzq + anchoRanura * desplazamiento;
        canvas.drawText(masViejo, xViejo, plotAbajo + 13 * dp, textoTenuePaint);

        textoTenuePaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("ahora", plotDer, plotAbajo + 13 * dp, textoTenuePaint);
    }

    private String formatearTick(float v) {
        return String.format(Locale.getDefault(), "%.0f", v);
    }

    private String formatearValor(float v) {
        return String.format(Locale.getDefault(), "%." + decimales + "f", v) + sufijo;
    }
}
