package com.zygy.wallpapers;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;

public final class WallpaperGenerator {
    private WallpaperGenerator() {}

    public static Bitmap generate(int id, int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        float w = width;
        float h = height;

        int palette = id % 12;
        int c1 = Color.HSVToColor(new float[]{(palette * 31f) % 360f, 0.76f, 0.95f});
        int c2 = Color.HSVToColor(new float[]{(palette * 31f + 92f) % 360f, 0.72f, 0.78f});
        int c3 = Color.HSVToColor(new float[]{(palette * 31f + 190f) % 360f, 0.70f, 0.62f});

        int style = id % 6;
        switch (style) {
            case 0 -> drawAurora(canvas, p, w, h, c1, c2, c3);
            case 1 -> drawOrbits(canvas, p, w, h, c1, c2, c3);
            case 2 -> drawGrid(canvas, p, w, h, c1, c2, c3);
            case 3 -> drawSunset(canvas, p, w, h, c1, c2, c3);
            case 4 -> drawMinimal(canvas, p, w, h, c1, c2);
            default -> drawShapes(canvas, p, w, h, c1, c2, c3);
        }

        Paint vignette = new Paint(Paint.ANTI_ALIAS_FLAG);
        vignette.setShader(new RadialGradient(
                w * 0.50f, h * 0.42f, Math.max(w, h) * 0.82f,
                new int[]{0x00000000, 0x25000000},
                new float[]{0.55f, 1f},
                Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, vignette);
        return bitmap;
    }

    private static void drawAurora(Canvas c, Paint p, float w, float h, int a, int b, int d) {
        p.setShader(new LinearGradient(0, 0, w, h, new int[]{a, b, d}, null, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, p);
        p.setShader(new RadialGradient(w * 0.18f, h * 0.18f, w * 0.48f,
                0x99FFFFFF, 0x00000000, Shader.TileMode.CLAMP));
        c.drawCircle(w * 0.18f, h * 0.18f, w * 0.48f, p);
        p.setShader(new RadialGradient(w * 0.78f, h * 0.72f, w * 0.66f,
                0x557FFFFFF, 0x00000000, Shader.TileMode.CLAMP));
        c.drawCircle(w * 0.78f, h * 0.72f, w * 0.66f, p);
        p.setShader(null);
        p.setColor(0x22FFFFFF);
        for (int i = 0; i < 9; i++) {
            float y = h * (0.18f + i * 0.09f);
            c.drawOval(new RectF(-w * 0.18f + i * 18, y, w * 0.92f, y + h * 0.035f), p);
        }
    }

    private static void drawOrbits(Canvas c, Paint p, float w, float h, int a, int b, int d) {
        p.setShader(new LinearGradient(0, 0, 0, h, new int[]{0xFF080A14, a, 0xFF111827}, null, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, p);
        p.setShader(new RadialGradient(w * 0.5f, h * 0.42f, w * 0.62f,
                0xCCFFFFFF, 0x001FFFFFFF, Shader.TileMode.CLAMP));
        c.drawCircle(w * 0.5f, h * 0.42f, w * 0.62f, p);
        p.setShader(null);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(2f, w * 0.012f));
        p.setColor(0x66FFFFFF);
        for (int i = 1; i <= 5; i++) {
            float rw = w * (0.18f + i * 0.13f);
            float rh = h * (0.08f + i * 0.075f);
            c.drawOval(new RectF(w * 0.5f - rw, h * 0.42f - rh, w * 0.5f + rw, h * 0.42f + rh), p);
        }
        p.setStyle(Paint.Style.FILL);
        p.setColor(b);
        c.drawCircle(w * 0.5f, h * 0.42f, w * 0.11f, p);
        p.setColor(d);
        c.drawCircle(w * 0.22f, h * 0.22f, w * 0.035f, p);
        c.drawCircle(w * 0.80f, h * 0.64f, w * 0.025f, p);
    }

    private static void drawGrid(Canvas c, Paint p, float w, float h, int a, int b, int d) {
        p.setShader(new LinearGradient(0, 0, w, h, new int[]{0xFF10131D, a, 0xFF0C0F16}, null, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, p);
        p.setShader(null);
        p.setColor(0x22FFFFFF);
        p.setStrokeWidth(Math.max(1f, w * 0.004f));
        float step = w * 0.105f;
        for (float x = 0; x < w; x += step) c.drawLine(x, 0, x + h * 0.32f, h, p);
        for (float x = -h * 0.32f; x < w; x += step) c.drawLine(x, 0, x + h * 0.32f, h, p);
        p.setColor(b);
        c.drawCircle(w * 0.24f, h * 0.35f, w * 0.17f, p);
        p.setColor(d);
        c.drawCircle(w * 0.76f, h * 0.68f, w * 0.24f, p);
        p.setColor(0x20FFFFFF);
        c.drawCircle(w * 0.52f, h * 0.52f, w * 0.09f, p);
    }

    private static void drawSunset(Canvas c, Paint p, float w, float h, int a, int b, int d) {
        p.setShader(new LinearGradient(0, 0, 0, h,
                new int[]{a, b, 0xFF11131D}, null, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, p);
        p.setShader(new RadialGradient(w * 0.5f, h * 0.57f, w * 0.32f,
                0xFFFFF0B0, 0x00FFB37A, Shader.TileMode.CLAMP));
        c.drawCircle(w * 0.5f, h * 0.57f, w * 0.32f, p);
        p.setShader(null);
        p.setColor(0xD9000000);
        PathLike skyline = new PathLike();
        skyline.moveTo(0, h * 0.68f);
        skyline.lineTo(w * 0.16f, h * 0.62f);
        skyline.lineTo(w * 0.29f, h * 0.69f);
        skyline.lineTo(w * 0.43f, h * 0.60f);
        skyline.lineTo(w * 0.58f, h * 0.68f);
        skyline.lineTo(w * 0.74f, h * 0.63f);
        skyline.lineTo(w, h * 0.70f);
        skyline.lineTo(w, h);
        skyline.lineTo(0, h);
        skyline.close();
        c.drawPath(skyline.path, p);
    }

    private static void drawMinimal(Canvas c, Paint p, float w, float h, int a, int b) {
        p.setColor(0xFF0A0D13);
        c.drawRect(0, 0, w, h, p);
        p.setShader(new LinearGradient(0, 0, w, h, a, b, Shader.TileMode.CLAMP));
        p.setAlpha(230);
        c.drawRoundRect(new RectF(w * 0.12f, h * 0.21f, w * 0.88f, h * 0.79f),
                w * 0.11f, w * 0.11f, p);
        p.setShader(null);
        p.setAlpha(255);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(2f, w * 0.008f));
        p.setColor(0x66FFFFFF);
        c.drawRoundRect(new RectF(w * 0.19f, h * 0.28f, w * 0.81f, h * 0.72f),
                w * 0.09f, w * 0.09f, p);
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xAAFFFFFF);
        c.drawCircle(w * 0.5f, h * 0.5f, w * 0.035f, p);
    }

    private static void drawShapes(Canvas c, Paint p, float w, float h, int a, int b, int d) {
        p.setColor(0xFF0B0E15);
        c.drawRect(0, 0, w, h, p);
        p.setColor(a);
        c.drawCircle(w * 0.18f, h * 0.20f, w * 0.24f, p);
        p.setColor(b);
        c.drawCircle(w * 0.85f, h * 0.36f, w * 0.30f, p);
        p.setColor(d);
        c.drawCircle(w * 0.33f, h * 0.79f, w * 0.28f, p);
        p.setColor(0x22FFFFFF);
        c.drawCircle(w * 0.68f, h * 0.77f, w * 0.16f, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(2f, w * 0.01f));
        p.setColor(0x55FFFFFF);
        c.drawCircle(w * 0.50f, h * 0.52f, w * 0.20f, p);
        p.setStyle(Paint.Style.FILL);
    }

    private static final class PathLike {
        final android.graphics.Path path = new android.graphics.Path();
        void moveTo(float x, float y) { path.moveTo(x, y); }
        void lineTo(float x, float y) { path.lineTo(x, y); }
        void close() { path.close(); }
    }
}
