package com.example.app_alimentador;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

/**
 * Mando direccional: un anillo partido por las diagonales en cuatro botones
 * (arriba, abajo, izquierda y derecha).
 *
 * Las cuatro zonas son sectores de corona circular, no rectangulos, asi que no
 * se pueden armar con Buttons normales: se dibujan y se detecta el toque por
 * angulo y distancia al centro.
 *
 * Las flechas rectas se dibujan con Path; las de giro usan el recurso
 * {@code R.drawable.arrow}, tal cual para el antihorario y reflejado en
 * horizontal para el horario.
 */
public class DPadView extends View {

    public static final int NINGUNA = -1;
    public static final int ARRIBA = 0;
    public static final int DERECHA = 1;
    public static final int ABAJO = 2;
    public static final int IZQUIERDA = 3;

    /** Hueco angular en las diagonales, en grados: separa sin dibujar bordes. */
    private static final float HUECO_GRADOS = 2.5f;

    /** Radio interior como fraccion del exterior: deja los botones como banda. */
    private static final float FACTOR_RADIO_INTERNO = 0.62f;

    /** Altura de la flecha de giro como fraccion del radio del mando. */
    private static final float ALTO_FLECHA_GIRO = 0.42f;

    /**
     * Avisa del sector activo cada vez que cambia. Se llama tambien al soltar,
     * con {@link #NINGUNA}, para que quien escuche pueda apagar la orden: el
     * mando solo manda mientras se mantiene pulsado.
     */
    public interface OnDireccionListener {
        void onDireccion(int direccion);
    }

    private final Paint sectorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint flechaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bitmapPaint =
            new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

    private final RectF ovalo = new RectF();
    private final RectF ovaloInterno = new RectF();
    private final RectF destinoFlecha = new RectF();
    private final Path sectorPath = new Path();
    private final Path flechaPath = new Path();

    private Bitmap flechaGiro;

    private int colorNormal;
    private int colorPresionado;

    private int sectorPresionado = NINGUNA;
    private OnDireccionListener listener;

    private float dp;

    public DPadView(Context context) {
        this(context, null);
    }

    public DPadView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        dp = context.getResources().getDisplayMetrics().density;

        colorNormal = ContextCompat.getColor(context, R.color.logo);
        colorPresionado = ContextCompat.getColor(context, R.color.logo_presionado);

        sectorPaint.setStyle(Paint.Style.FILL);

        flechaPaint.setStyle(Paint.Style.STROKE);
        flechaPaint.setStrokeWidth(7 * dp);
        flechaPaint.setStrokeCap(Paint.Cap.ROUND);
        flechaPaint.setStrokeJoin(Paint.Join.ROUND);
        flechaPaint.setColor(ContextCompat.getColor(context, R.color.black));

        flechaGiro = BitmapFactory.decodeResource(context.getResources(), R.drawable.arrow);
    }

    public void setOnDireccionListener(OnDireccionListener l) {
        this.listener = l;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        // Siempre cuadrado: el mando es un circulo.
        int lado = Math.min(getMeasuredWidth(), getMeasuredHeight());
        setMeasuredDimension(lado, lado);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float r = Math.min(cx, cy) - 2 * dp;
        if (r <= 0) {
            return;
        }

        float rInterno = r * FACTOR_RADIO_INTERNO;
        ovalo.set(cx - r, cy - r, cx + r, cy + r);
        ovaloInterno.set(cx - rInterno, cy - rInterno, cx + rInterno, cy + rInterno);

        // En Canvas 0 grados apunta al este y los angulos crecen en sentido
        // horario, asi que el sector de arriba va de 225 a 315.
        dibujarSector(canvas, 225f, ARRIBA);
        dibujarSector(canvas, 315f, DERECHA);
        dibujarSector(canvas, 45f, ABAJO);
        dibujarSector(canvas, 135f, IZQUIERDA);

        // Las flechas van centradas en la banda, no a media pantalla.
        float d = (r + rInterno) / 2f;
        dibujarFlechaRecta(canvas, cx, cy - d, r, true);   // arriba
        dibujarFlechaRecta(canvas, cx, cy + d, r, false);  // abajo
        // Segun lo pedido: derecha = antihorario, izquierda = horario.
        // El PNG tal cual describe el giro horario (sube por la izquierda), asi
        // que va en el sector izquierdo; el reflejo es el antihorario.
        dibujarFlechaGiro(canvas, cx - d, cy, r, false);   // izquierda: horario
        dibujarFlechaGiro(canvas, cx + d, cy, r, true);    // derecha: antihorario
    }

    private void dibujarSector(Canvas canvas, float anguloInicio, int sector) {
        sectorPaint.setColor(sector == sectorPresionado ? colorPresionado : colorNormal);

        float ini = anguloInicio + HUECO_GRADOS;
        float barrido = 90f - 2 * HUECO_GRADOS;

        // Banda anular: se recorre el arco exterior y se vuelve por el interior.
        sectorPath.reset();
        sectorPath.arcTo(ovalo, ini, barrido);
        sectorPath.arcTo(ovaloInterno, ini + barrido, -barrido);
        sectorPath.close();
        canvas.drawPath(sectorPath, sectorPaint);
    }

    /** Flecha recta arriba/abajo. */
    private void dibujarFlechaRecta(Canvas canvas, float x, float y, float r, boolean haciaArriba) {
        float h = r * 0.12f;          // media longitud del asta
        float w = r * 0.09f;          // media anchura de la cabeza
        float signo = haciaArriba ? -1f : 1f;
        float puntaY = y + signo * h;

        flechaPath.reset();
        flechaPath.moveTo(x, y - signo * h);
        flechaPath.lineTo(x, puntaY);
        flechaPath.moveTo(x - w, puntaY - signo * w);
        flechaPath.lineTo(x, puntaY);
        flechaPath.lineTo(x + w, puntaY - signo * w);
        canvas.drawPath(flechaPath, flechaPaint);
    }

    /**
     * Flecha de giro a partir de {@code R.drawable.arrow}. El parametro solo
     * decide si se refleja en horizontal: la imagen tal cual es el giro horario
     * y su espejo el antihorario.
     */
    private void dibujarFlechaGiro(Canvas canvas, float x, float y, float r, boolean espejo) {
        if (flechaGiro == null || flechaGiro.getHeight() == 0) {
            return;
        }

        float alto = r * ALTO_FLECHA_GIRO;
        float ancho = alto * flechaGiro.getWidth() / (float) flechaGiro.getHeight();

        // En los sectores laterales el ancho cae en sentido radial: se limita al
        // grosor de la banda para que la imagen no se salga del boton.
        float maxAncho = r * (1f - FACTOR_RADIO_INTERNO) * 0.78f;
        if (ancho > maxAncho) {
            alto = alto * maxAncho / ancho;
            ancho = maxAncho;
        }

        destinoFlecha.set(x - ancho / 2f, y - alto / 2f, x + ancho / 2f, y + alto / 2f);

        if (espejo) {
            canvas.save();
            canvas.scale(-1f, 1f, x, y);   // espejo respecto al eje vertical
            canvas.drawBitmap(flechaGiro, null, destinoFlecha, bitmapPaint);
            canvas.restore();
        } else {
            canvas.drawBitmap(flechaGiro, null, destinoFlecha, bitmapPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                int sector = sectorEn(event.getX(), event.getY());
                if (sector == NINGUNA) {
                    return false; // fuera de la banda: no se captura el gesto
                }
                cambiarSector(sector);
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                // Al arrastrar fuera del sector se suelta, como haria un boton.
                if (sectorEn(event.getX(), event.getY()) != sectorPresionado) {
                    cambiarSector(NINGUNA);
                }
                return true;
            }
            case MotionEvent.ACTION_UP:
                if (sectorPresionado != NINGUNA) {
                    performClick();
                }
                cambiarSector(NINGUNA);
                return true;
            case MotionEvent.ACTION_CANCEL:
                cambiarSector(NINGUNA);
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    /** Cambia el sector activo y avisa solo si de verdad cambio. */
    private void cambiarSector(int nuevo) {
        if (nuevo == sectorPresionado) {
            return;
        }
        sectorPresionado = nuevo;
        invalidate();
        if (listener != null) {
            listener.onDireccion(nuevo);
        }
    }

    /** Suelta el mando sin avisar; para cuando la vista deja de estar visible. */
    public void soltarSinAvisar() {
        if (sectorPresionado != NINGUNA) {
            sectorPresionado = NINGUNA;
            invalidate();
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    /** Sector bajo el punto dado, o {@link #NINGUNA} si cae fuera de la banda. */
    private int sectorEn(float x, float y) {
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float r = Math.min(cx, cy) - 2 * dp;

        float rInterno = r * FACTOR_RADIO_INTERNO;
        float dx = x - cx;
        float dy = y - cy;
        float dist2 = dx * dx + dy * dy;
        // La zona activa es la banda: ni fuera del circulo ni el hueco central.
        if (dist2 > r * r || dist2 < rInterno * rInterno) {
            return NINGUNA;
        }

        double grados = Math.toDegrees(Math.atan2(dy, dx));
        if (grados < 0) {
            grados += 360;
        }
        if (grados >= 225 && grados < 315) return ARRIBA;
        if (grados >= 45 && grados < 135) return ABAJO;
        if (grados >= 135 && grados < 225) return IZQUIERDA;
        return DERECHA;
    }
}
