package com.jh.carvideo;

/**
 * Reconoce los gestos a ciegas de un solo dedo que comparten JH LAUNCHER, JH MUSIC y JH VIDEO:
 * - Deslizar a la derecha / izquierda: siguiente / anterior.
 * - Dibujar un círculo (basta la mitad del círculo): abrir / cerrar la biblioteca (el volumen vertical lo resuelve cada app).
 *
 * Solo guarda puntos y los evalúa al levantar el dedo; no toca ninguna vista. Es una copia idéntica en las
 * tres apps (cada una es un proyecto aparte): si se cambia aquí, cambiar también en las otras dos.
 */
final class BlindGestureTracker {

    static final int NONE = 0;
    static final int SWIPE_NEXT = 1;
    static final int SWIPE_PREV = -1;

    private static final int MAX_POINTS = 240;
    private static final long CIRCLE_MAX_MS = 3500;
    private static final long SWIPE_MAX_MS = 3000;

    private final float minSwipePx;
    private final float minCirclePx;
    private final float maxCirclePx;

    private final float[] xs = new float[MAX_POINTS];
    private final float[] ys = new float[MAX_POINTS];
    private int count;
    private long startMs;
    private long lastMs;
    private boolean active;

    BlindGestureTracker(float density) {
        minSwipePx = 90 * density;
        minCirclePx = 80 * density;
        maxCirclePx = 420 * density;
    }

    void start(float x, float y, long nowMs) {
        count = 0;
        active = true;
        startMs = nowMs;
        lastMs = nowMs;
        add(x, y, nowMs);
    }

    void add(float x, float y, long nowMs) {
        if (!active) return;
        lastMs = nowMs;
        if (count >= MAX_POINTS) {
            // Trazo larguísimo: se descarta (no es un gesto a ciegas)
            active = false;
            return;
        }
        // Se ignoran los puntos casi repetidos para no llenar el buffer
        if (count > 0) {
            float dx = x - xs[count - 1];
            float dy = y - ys[count - 1];
            if (dx * dx + dy * dy < 4f) return;
        }
        xs[count] = x;
        ys[count] = y;
        count++;
    }

    void cancel() {
        active = false;
        count = 0;
    }

    boolean isActive() {
        return active;
    }

    /** Termina el trazo: devuelve true si fue un círculo (basta la mitad del círculo, no hace falta cerrarlo). */
    boolean finishIsCircle() {
        boolean ok = active && evaluateCircle();
        // Si no fue círculo el trazo sigue vigente: todavía puede ser un deslizamiento (finishSwipe)
        if (ok) active = false;
        return ok;
    }

    /** Termina el trazo: NONE, SWIPE_NEXT o SWIPE_PREV. Llamar solo si no fue un círculo. */
    int finishSwipe() {
        int result = active ? evaluateSwipe() : NONE;
        active = false;
        return result;
    }

    private int evaluateSwipe() {
        if (count < 2 || lastMs - startMs > SWIPE_MAX_MS) return NONE;
        float dx = xs[count - 1] - xs[0];
        float dy = ys[count - 1] - ys[0];
        if (Math.abs(dx) < minSwipePx || Math.abs(dx) < Math.abs(dy) * 2f) return NONE;
        // Debe ser casi recto: un trazo curvo no es un deslizamiento
        float path = 0;
        for (int i = 1; i < count; i++) {
            path += (float) Math.hypot(xs[i] - xs[i - 1], ys[i] - ys[i - 1]);
        }
        if (path > Math.abs(dx) * 1.5f) return NONE;
        return dx > 0 ? SWIPE_NEXT : SWIPE_PREV;
    }

    private boolean evaluateCircle() {
        if (count < 10 || lastMs - startMs > CIRCLE_MAX_MS) return false;

        float minX = xs[0], maxX = xs[0], minY = ys[0], maxY = ys[0];
        for (int i = 1; i < count; i++) {
            minX = Math.min(minX, xs[i]);
            maxX = Math.max(maxX, xs[i]);
            minY = Math.min(minY, ys[i]);
            maxY = Math.max(maxY, ys[i]);
        }
        float w = maxX - minX;
        float h = maxY - minY;
        // Una media luna es el doble de ancha que alta: el lado corto puede ser la mitad del largo
        if (Math.max(w, h) < minCirclePx || Math.min(w, h) < minCirclePx * 0.5f
                || w > maxCirclePx || h > maxCirclePx) return false;
        float aspect = w / h;
        if (aspect < 0.4f || aspect > 2.5f) return false;

        // Círculo que mejor se ajusta a los puntos (Kåsa). El centro del cuadro que envuelve el trazo no sirve:
        // en un arco incompleto queda corrido y el giro sale inflado (una media luna parecería 230°)
        double mx = 0, my = 0;
        for (int i = 0; i < count; i++) {
            mx += xs[i];
            my += ys[i];
        }
        mx /= count;
        my /= count;
        double sxx = 0, syy = 0, sxy = 0, sxz = 0, syz = 0, sz = 0;
        for (int i = 0; i < count; i++) {
            double x = xs[i] - mx;
            double y = ys[i] - my;
            double z = x * x + y * y;
            sxx += x * x;
            syy += y * y;
            sxy += x * y;
            sxz += x * z;
            syz += y * z;
            sz += z;
        }
        // Sistema 3x3 (las sumas de x e y valen 0 al estar centrados): [sxx sxy 0; sxy syy 0; 0 0 n]·[A B C] = -[sxz syz sz]
        double det = sxx * syy - sxy * sxy;
        if (Math.abs(det) < 1e-6) return false; // casi una línea recta
        double fa = (-sxz * syy + syz * sxy) / det;
        double fb = (-syz * sxx + sxz * sxy) / det;
        double fc = -sz / count;
        double ccx = -fa / 2;
        double ccy = -fb / 2;
        double r2 = ccx * ccx + ccy * ccy - fc;
        if (r2 <= 0) return false;
        double radius = Math.sqrt(r2);
        if (radius * 2 < minCirclePx || radius * 2 > maxCirclePx) return false;
        double cx = ccx + mx;
        double cy = ccy + my;

        // Casi todos los puntos cerca de la circunferencia (no una "S" ni una línea doblada)
        int near = 0;
        for (int i = 0; i < count; i++) {
            double d = Math.hypot(xs[i] - cx, ys[i] - cy);
            if (Math.abs(d - radius) <= radius * 0.3) near++;
        }
        if (near < count * 0.85) return false;

        // Giro acumulado alrededor del centro: basta con la mitad del círculo (170° da margen al dibujarlo a
        // mano) en un solo sentido, no hace falta cerrarlo
        double total = 0;
        double prev = Math.atan2(ys[0] - cy, xs[0] - cx);
        for (int i = 1; i < count; i++) {
            double a = Math.atan2(ys[i] - cy, xs[i] - cx);
            double d = a - prev;
            while (d > Math.PI) d -= 2 * Math.PI;
            while (d < -Math.PI) d += 2 * Math.PI;
            total += d;
            prev = a;
        }
        double turns = Math.abs(total);
        return turns >= Math.toRadians(170) && turns <= Math.toRadians(620);
    }
}
