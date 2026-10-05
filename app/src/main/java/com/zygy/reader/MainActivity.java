package com.zygy.reader;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class MainActivity extends Activity {
    private RailRunnerView game;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        w.setStatusBarColor(Color.rgb(7, 10, 18));
        w.setNavigationBarColor(Color.rgb(7, 10, 18));
        game = new RailRunnerView(this);
        setContentView(game);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (game != null) game.resumeLoop();
    }

    @Override
    protected void onPause() {
        if (game != null) game.pauseForLifecycle();
        super.onPause();
    }

    private static final class RailRunnerView extends View {
        private static final int MENU = 0;
        private static final int RUNNING = 1;
        private static final int PAUSED = 2;
        private static final int GAME_OVER = 3;

        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Random random = new Random();
        private final SharedPreferences prefs;

        private final ArrayList<Train> trains = new ArrayList<>();
        private final ArrayList<Coin> coins = new ArrayList<>();
        private final ArrayList<Particle> particles = new ArrayList<>();

        private int state = MENU;
        private int lane = 5;
        private float lanePosition = 5f;
        private int score;
        private int collected;
        private int best;
        private int multiplier = 1;
        private boolean shield;
        private long shieldUntil;
        private long runStart;
        private long lastFrame;
        private long lastTrainSpawn;
        private long lastCoinSpawn;
        private long flashUntil;
        private float pulse;
        private float touchDownX;
        private float touchDownY;
        private boolean initialized;

        private float W;
        private float H;
        private float top;
        private float gap;
        private float centerX;

        private final Typeface bold = Typeface.create("sans-serif-rounded", Typeface.BOLD);
        private final Typeface normal = Typeface.create("sans-serif", Typeface.NORMAL);

        RailRunnerView(Context context) {
            super(context);
            setFocusable(true);
            p.setTypeface(normal);
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeCap(Paint.Cap.ROUND);
            prefs = context.getSharedPreferences("rail_runner", Context.MODE_PRIVATE);
            best = prefs.getInt("best", 0);
        }

        void resumeLoop() {
            if (state == PAUSED) {
                state = RUNNING;
                lastFrame = SystemClock.elapsedRealtime();
            }
            postInvalidateOnAnimation();
        }

        void pauseForLifecycle() {
            if (state == RUNNING) state = PAUSED;
        }

        @Override
        protected void onDraw(Canvas c) {
            super.onDraw(c);
            W = getWidth();
            H = getHeight();
            centerX = W * 0.5f;
            gap = Math.min(H * 0.105f, 92f);
            top = H * 0.20f;

            drawBackground(c);

            if (!initialized) {
                initialized = true;
                lastFrame = SystemClock.elapsedRealtime();
            }

            if (state == RUNNING) update();

            drawTracks(c);
            drawCoins(c);
            drawTrains(c);
            drawPlayer(c);
            drawParticles(c);
            drawHud(c);

            if (state == MENU) drawMenu(c);
            else if (state == PAUSED) drawPause(c);
            else if (state == GAME_OVER) drawGameOver(c);

            if (state == RUNNING) postInvalidateOnAnimation();
        }

        private void startGame() {
            state = RUNNING;
            lane = 5;
            lanePosition = 5f;
            score = 0;
            collected = 0;
            multiplier = 1;
            shield = false;
            shieldUntil = 0;
            trains.clear();
            coins.clear();
            particles.clear();
            runStart = SystemClock.elapsedRealtime();
            lastFrame = runStart;
            lastTrainSpawn = runStart - 700;
            lastCoinSpawn = runStart - 350;
            flashUntil = 0;
            pulse = 0;
            performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            postInvalidateOnAnimation();
        }

        private void update() {
            long now = SystemClock.elapsedRealtime();
            float dt = Math.min(0.035f, (now - lastFrame) / 1000f);
            lastFrame = now;

            pulse += dt;
            lanePosition += (lane - lanePosition) * Math.min(1f, dt * 12f);

            long elapsed = now - runStart;
            score = (int) (elapsed / 700L) + collected * 10 * multiplier;
            multiplier = Math.min(5, 1 + collected / 8);

            float difficulty = Math.min(1.9f, 1f + elapsed / 28000f);
            float trainInterval = Math.max(460f, 940f - elapsed / 45f);
            if (now - lastTrainSpawn >= (long) trainInterval) {
                spawnTrain(difficulty);
                lastTrainSpawn = now;
                if (random.nextFloat() < 0.18f) spawnTrain(difficulty * 0.98f);
            }

            if (now - lastCoinSpawn >= 1100L) {
                spawnCoin();
                lastCoinSpawn = now;
            }

            Iterator<Train> ti = trains.iterator();
            while (ti.hasNext()) {
                Train t = ti.next();
                t.x += t.dir * t.speed * difficulty * dt;
                if (t.x < -t.w - 80 || t.x > W + 80) {
                    ti.remove();
                    continue;
                }

                float trainCenter = t.x + t.w * 0.5f;
                float y = laneY(t.lane);
                if (!t.hit && Math.abs(trainCenter - centerX) < t.w * 0.5f + 25f
                        && Math.abs(y - laneY(lanePosition)) < gap * 0.34f) {
                    t.hit = true;
                    if (shield) {
                        shield = false;
                        shieldUntil = 0;
                        flashUntil = now + 220;
                        burst(centerX, y);
                        ti.remove();
                        score += 35;
                    } else {
                        gameOver();
                        return;
                    }
                }
            }

            Iterator<Coin> ci = coins.iterator();
            while (ci.hasNext()) {
                Coin coin = ci.next();
                coin.phase += dt * 5f;
                coin.x += coin.drift * dt;
                float y = laneY(coin.lane);
                if (Math.abs(coin.x - centerX) < 42f && Math.abs(y - laneY(lanePosition)) < gap * 0.37f) {
                    collected++;
                    if (coin.shield) {
                        shield = true;
                        shieldUntil = now + 6500L;
                    }
                    burst(coin.x, y);
                    ci.remove();
                    score += coin.shield ? 45 : 10 * multiplier;
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    continue;
                }
                if (coin.x < -60 || coin.x > W + 60) ci.remove();
            }

            if (shield && now > shieldUntil) {
                shield = false;
                shieldUntil = 0;
            }

            Iterator<Particle> pi = particles.iterator();
            while (pi.hasNext()) {
                Particle q = pi.next();
                q.life -= dt;
                q.x += q.vx * dt;
                q.y += q.vy * dt;
                q.vy += 280f * dt;
                if (q.life <= 0) pi.remove();
            }
        }

        private void spawnTrain(float difficulty) {
            int l = random.nextInt(6);
            float w = 150f + random.nextInt(95);
            float speed = 230f + random.nextFloat() * 125f;
            int dir = random.nextBoolean() ? 1 : -1;
            float x = dir > 0 ? -w - 15 : W + 15;
            trains.add(new Train(l, x, w, 62f, dir, speed));
        }

        private void spawnCoin() {
            Coin coin = new Coin(random.nextInt(6), random.nextBoolean(), random);
            coin.x = random.nextBoolean() ? 70f : Math.max(70f, W - 70f);
            coin.drift = random.nextBoolean() ? 7f : -7f;
            coins.add(coin);
        }

        private void move(int delta) {
            int next = Math.max(0, Math.min(5, lane + delta));
            if (next != lane) {
                lane = next;
                performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            }
        }

        private void gameOver() {
            state = GAME_OVER;
            best = Math.max(best, score);
            prefs.edit().putInt("best", best).apply();
            burst(centerX, laneY(lanePosition));
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        }

        private float laneY(float l) {
            return top + l * gap;
        }

        private void drawBackground(Canvas c) {
            p.setStyle(Paint.Style.FILL);
            p.setShader(new LinearGradient(0, 0, 0, H,
                    Color.rgb(7, 10, 18), Color.rgb(18, 24, 38), Shader.TileMode.CLAMP));
            c.drawRect(0, 0, W, H, p);
            p.setShader(null);

            p.setColor(Color.argb(35, 120, 180, 255));
            for (int i = 0; i < 20; i++) {
                float sx = (i * 97f) % W;
                float sy = 36f + (i * 53f) % Math.max(80f, top - 40f);
                c.drawCircle(sx, sy, 1.5f + (i % 3), p);
            }

            p.setColor(Color.argb(24, 100, 130, 180));
            for (int i = 0; i < 7; i++) {
                float x = (i + 1) * W / 8f;
                c.drawRoundRect(x, 78, x + 3, top + 50, 3, 3, p);
            }

            p.setShader(new LinearGradient(0, top - 50, 0, top + gap * 6,
                    Color.argb(0, 70, 220, 255), Color.argb(65, 70, 220, 255),
                    Shader.TileMode.CLAMP));
            c.drawRect(0, top - 40, W, H, p);
            p.setShader(null);

            if (SystemClock.elapsedRealtime() < flashUntil) {
                p.setColor(Color.argb(85, 255, 255, 255));
                c.drawRect(0, 0, W, H, p);
            }
        }

        private void drawTracks(Canvas c) {
            for (int i = 0; i < 6; i++) {
                float y = laneY(i);
                boolean active = i == lane;
                p.setColor(active ? Color.rgb(28, 43, 62) : Color.rgb(22, 30, 43));
                c.drawRoundRect(14, y - gap * 0.41f, W - 14, y + gap * 0.41f, 20, 20, p);

                p.setColor(Color.rgb(45, 55, 70));
                float sleeperGap = 38f;
                for (float x = 24; x < W - 10; x += sleeperGap) {
                    c.drawRoundRect(x, y - 25, x + 10, y + 25, 5, 5, p);
                }

                stroke.setStrokeWidth(5);
                stroke.setColor(Color.rgb(92, 108, 126));
                c.drawLine(10, y - 16, W - 10, y - 16, stroke);
                c.drawLine(10, y + 16, W - 10, y + 16, stroke);

                stroke.setStrokeWidth(2);
                stroke.setColor(active ? Color.rgb(74, 203, 255) : Color.rgb(57, 72, 88));
                c.drawLine(10, y - 18, W - 10, y - 18, stroke);
                c.drawLine(10, y + 18, W - 10, y + 18, stroke);
            }

            p.setColor(Color.argb(45, 120, 214, 255));
            c.drawRoundRect(centerX - 3, top - gap * 0.55f, centerX + 3,
                    top + gap * 5.55f, 3, 3, p);
        }

        private void drawTrains(Canvas c) {
            for (Train t : trains) {
                float y = laneY(t.lane);
                float left = t.x;
                float right = t.x + t.w;

                p.setColor(Color.rgb(16, 21, 30));
                c.drawRoundRect(left, y - t.h * 0.5f, right, y + t.h * 0.5f, 13, 13, p);

                p.setColor(t.dir > 0 ? Color.rgb(245, 101, 101) : Color.rgb(255, 174, 74));
                c.drawRoundRect(left + 6, y - t.h * 0.5f + 7, right - 6,
                        y + t.h * 0.5f - 7, 10, 10, p);

                p.setColor(Color.argb(220, 225, 240, 250));
                float windowW = Math.max(22f, (t.w - 54f) / 3f);
                for (int i = 0; i < 3; i++) {
                    float wx = left + 22 + i * (windowW + 6);
                    c.drawRoundRect(wx, y - 17, wx + windowW, y + 2, 5, 5, p);
                }

                p.setColor(Color.rgb(11, 15, 22));
                c.drawCircle(left + 33, y + 25, 9, p);
                c.drawCircle(right - 33, y + 25, 9, p);

                p.setColor(Color.rgb(255, 246, 183));
                float front = t.dir > 0 ? right - 13 : left + 13;
                c.drawCircle(front, y - 19, 5, p);
            }
        }

        private void drawCoins(Canvas c) {
            for (Coin coin : coins) {
                float y = laneY(coin.lane) + (float) Math.sin(coin.phase) * 4f;
                if (coin.shield) {
                    stroke.setStyle(Paint.Style.STROKE);
                    stroke.setStrokeWidth(4);
                    stroke.setColor(Color.rgb(86, 226, 255));
                    c.drawCircle(coin.x, y, 18, stroke);
                    stroke.setStyle(Paint.Style.STROKE);
                    stroke.setStrokeWidth(2);
                    stroke.setColor(Color.rgb(185, 247, 255));
                    c.drawCircle(coin.x, y, 10, stroke);
                } else {
                    p.setColor(Color.rgb(255, 202, 72));
                    c.drawCircle(coin.x, y, 16, p);
                    p.setColor(Color.rgb(255, 236, 145));
                    c.drawCircle(coin.x, y, 10, p);
                    p.setColor(Color.rgb(194, 142, 18));
                    p.setTextAlign(Paint.Align.CENTER);
                    p.setTypeface(bold);
                    p.setTextSize(13);
                    c.drawText("+", coin.x, y + 5, p);
                }
            }
        }

        private void drawPlayer(Canvas c) {
            float y = laneY(lanePosition);
            float bob = (float) Math.sin(pulse * 7f) * 2.5f;
            y += bob;

            if (shield) {
                int alpha = 95 + (int) (45f * ((float) Math.sin(pulse * 5f) + 1f) * 0.5f);
                stroke.setStyle(Paint.Style.STROKE);
                stroke.setStrokeWidth(4);
                stroke.setColor(Color.argb(alpha, 82, 226, 255));
                c.drawCircle(centerX, y, 40, stroke);
            }

            p.setColor(Color.rgb(237, 244, 250));
            c.drawCircle(centerX, y - 27, 18, p);

            p.setColor(Color.rgb(73, 108, 150));
            c.drawRoundRect(centerX - 27, y - 43, centerX + 27, y - 27, 8, 8, p);

            p.setColor(Color.rgb(55, 193, 214));
            c.drawRoundRect(centerX - 24, y - 15, centerX + 24, y + 27, 12, 12, p);

            p.setColor(Color.rgb(20, 28, 40));
            c.drawCircle(centerX - 6, y - 30, 2.6f, p);
            c.drawCircle(centerX + 6, y - 30, 2.6f, p);

            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(6);
            stroke.setColor(Color.rgb(237, 244, 250));
            c.drawLine(centerX - 12, y + 26, centerX - 19, y + 40, stroke);
            c.drawLine(centerX + 12, y + 26, centerX + 19, y + 40, stroke);
            c.drawLine(centerX - 22, y - 2, centerX - 34, y + 12, stroke);
            c.drawLine(centerX + 22, y - 2, centerX + 34, y + 12, stroke);
        }

        private void drawParticles(Canvas c) {
            for (Particle q : particles) {
                p.setColor(Color.argb((int) (255 * Math.max(0, q.life / q.maxLife)), 100, 222, 255));
                c.drawCircle(q.x, q.y, q.size, p);
            }
        }

        private void drawHud(Canvas c) {
            if (state == MENU) return;

            drawPill(c, 18, 22, 152, 70, Color.argb(180, 15, 21, 31));
            p.setTypeface(bold);
            p.setTextAlign(Paint.Align.LEFT);
            p.setTextSize(12);
            p.setColor(Color.rgb(126, 151, 179));
            c.drawText("ניקוד", 34, 44, p);
            p.setTextSize(26);
            p.setColor(Color.WHITE);
            c.drawText(String.valueOf(score), 34, 69, p);

            drawPill(c, W - 176, 22, W - 18, 70, Color.argb(180, 15, 21, 31));
            p.setTextAlign(Paint.Align.RIGHT);
            p.setTextSize(12);
            p.setColor(Color.rgb(126, 151, 179));
            c.drawText("שיא " + best, W - 36, 44, p);
            p.setTextSize(16);
            p.setColor(Color.rgb(255, 202, 72));
            c.drawText("×" + multiplier, W - 36, 66, p);

            if (shield) {
                drawPill(c, 18, 82, 146, 118, Color.argb(190, 21, 56, 72));
                p.setTextAlign(Paint.Align.LEFT);
                p.setTypeface(bold);
                p.setTextSize(13);
                p.setColor(Color.rgb(130, 235, 255));
                c.drawText("מגן פעיל", 31, 106, p);
            }

            drawPill(c, W - 74, 82, W - 18, 138, Color.argb(190, 15, 21, 31));
            p.setTextAlign(Paint.Align.CENTER);
            p.setTextSize(22);
            p.setColor(Color.WHITE);
            c.drawText("Ⅱ", W - 46, 118, p);

            float by = H - 74;
            drawPill(c, 26, by, 124, H - 20, Color.rgb(31, 43, 58));
            drawPill(c, W - 124, by, W - 26, H - 20, Color.rgb(31, 43, 58));
            p.setTypeface(bold);
            p.setTextSize(24);
            p.setColor(Color.WHITE);
            p.setTextAlign(Paint.Align.CENTER);
            c.drawText("▲", 75, by + 34, p);
            c.drawText("▼", W - 75, by + 34, p);
        }

        private void drawMenu(Canvas c) {
            drawOverlay(c);

            p.setTypeface(bold);
            p.setTextAlign(Paint.Align.CENTER);
            p.setColor(Color.WHITE);
            p.setTextSize(Math.min(48, W * 0.105f));
            c.drawText("מסילת לילה", centerX, H * 0.19f, p);

            p.setTypeface(normal);
            p.setTextSize(17);
            p.setColor(Color.rgb(152, 176, 201));
            c.drawText("חוצים את המסילות. מתחמקים מהרכבות.", centerX, H * 0.235f, p);

            drawLogo(c, centerX, H * 0.39f);

            drawButton(c, centerX - 150, H * 0.60f, centerX + 150, H * 0.70f,
                    "התחל משחק", true);

            p.setTextSize(14);
            p.setColor(Color.rgb(133, 154, 180));
            c.drawText("החלקה למעלה / למטה • או השתמש בכפתורים", centerX, H * 0.765f, p);
            p.setColor(Color.rgb(102, 126, 154));
            c.drawText("אסוף מטבעות • קבל מגן • שבור את השיא", centerX, H * 0.80f, p);

            drawPill(c, centerX - 92, H * 0.865f, centerX + 92, H * 0.915f,
                    Color.argb(110, 40, 55, 75));
            p.setTypeface(bold);
            p.setTextSize(13);
            p.setColor(Color.rgb(160, 184, 210));
            c.drawText("השיא שלך: " + best, centerX, H * 0.897f, p);
        }

        private void drawPause(Canvas c) {
            drawOverlay(c);
            p.setTypeface(bold);
            p.setTextAlign(Paint.Align.CENTER);
            p.setTextSize(38);
            p.setColor(Color.WHITE);
            c.drawText("המשחק מושהה", centerX, H * 0.37f, p);
            drawButton(c, centerX - 145, H * 0.48f, centerX + 145, H * 0.58f,
                    "המשך", true);
            drawButton(c, centerX - 145, H * 0.615f, centerX + 145, H * 0.715f,
                    "משחק חדש", false);
        }

        private void drawGameOver(Canvas c) {
            drawOverlay(c);
            p.setTypeface(bold);
            p.setTextAlign(Paint.Align.CENTER);
            p.setColor(Color.WHITE);
            p.setTextSize(38);
            c.drawText("הריצה נגמרה", centerX, H * 0.33f, p);

            p.setTypeface(normal);
            p.setTextSize(16);
            p.setColor(Color.rgb(145, 170, 198));
            c.drawText("רכבת אחת הייתה מהירה מדי.", centerX, H * 0.375f, p);

            p.setTypeface(bold);
            p.setTextSize(54);
            p.setColor(Color.rgb(87, 218, 250));
            c.drawText(String.valueOf(score), centerX, H * 0.49f, p);

            p.setTypeface(normal);
            p.setTextSize(14);
            p.setColor(Color.rgb(135, 157, 183));
            c.drawText("ניקוד", centerX, H * 0.525f, p);
            c.drawText("שיא: " + best + " • מטבעות: " + collected, centerX, H * 0.57f, p);

            drawButton(c, centerX - 150, H * 0.64f, centerX + 150, H * 0.74f,
                    "נסה שוב", true);
        }

        private void drawLogo(Canvas c, float x, float y) {
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(5);
            stroke.setColor(Color.rgb(77, 208, 241));
            c.drawLine(x - 80, y + 38, x + 80, y + 38, stroke);
            c.drawLine(x - 70, y + 56, x + 70, y + 56, stroke);
            for (int i = -3; i <= 3; i++) {
                p.setColor(Color.rgb(43, 57, 74));
                c.drawRoundRect(x + i * 22 - 5, y + 30, x + i * 22 + 5, y + 64, 4, 4, p);
            }

            p.setColor(Color.rgb(237, 244, 250));
            c.drawCircle(x, y - 31, 22, p);
            p.setColor(Color.rgb(73, 108, 150));
            c.drawRoundRect(x - 32, y - 52, x + 32, y - 33, 9, 9, p);
            p.setColor(Color.rgb(55, 193, 214));
            c.drawRoundRect(x - 27, y - 12, x + 27, y + 34, 13, 13, p);
        }

        private void drawOverlay(Canvas c) {
            p.setColor(Color.argb(190, 5, 8, 15));
            c.drawRect(0, 0, W, H, p);
            p.setShader(new LinearGradient(0, H * 0.18f, 0, H * 0.78f,
                    Color.argb(10, 86, 226, 255), Color.argb(75, 86, 226, 255),
                    Shader.TileMode.CLAMP));
            c.drawRect(0, H * 0.10f, W, H * 0.86f, p);
            p.setShader(null);
        }

        private void drawButton(Canvas c, float l, float t, float r, float b, String label, boolean primary) {
            float radius = (b - t) * 0.5f;
            p.setColor(primary ? Color.rgb(58, 193, 224) : Color.rgb(37, 49, 65));
            c.drawRoundRect(l, t, r, b, radius, radius, p);
            if (primary) {
                p.setColor(Color.argb(45, 255, 255, 255));
                c.drawRoundRect(l + 4, t + 4, r - 4, t + 9, 4, 4, p);
            }
            p.setTypeface(bold);
            p.setTextAlign(Paint.Align.CENTER);
            p.setTextSize(17);
            p.setColor(primary ? Color.rgb(6, 17, 24) : Color.WHITE);
            c.drawText(label, (l + r) / 2f, (t + b) / 2f + 6, p);
        }

        private void drawPill(Canvas c, float l, float t, float r, float b, int color) {
            p.setColor(color);
            c.drawRoundRect(l, t, r, b, 20, 20, p);
        }

        private void burst(float x, float y) {
            for (int i = 0; i < 18; i++) {
                double a = random.nextDouble() * Math.PI * 2;
                float speed = 70f + random.nextFloat() * 170f;
                particles.add(new Particle(
                        x, y,
                        (float) Math.cos(a) * speed,
                        (float) Math.sin(a) * speed - 50f,
                        3f + random.nextFloat() * 4f));
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            float x = e.getX();
            float y = e.getY();

            if (e.getAction() == MotionEvent.ACTION_DOWN) {
                touchDownX = x;
                touchDownY = y;
                return true;
            }

            if (e.getAction() == MotionEvent.ACTION_UP) {
                float dx = x - touchDownX;
                float dy = y - touchDownY;

                if (state == MENU) {
                    startGame();
                    return true;
                }

                if (state == GAME_OVER) {
                    startGame();
                    return true;
                }

                if (state == PAUSED) {
                    state = RUNNING;
                    lastFrame = SystemClock.elapsedRealtime();
                    postInvalidateOnAnimation();
                    return true;
                }

                if (state == RUNNING) {
                    if (touchDownX > W - 95 && touchDownY < 160) {
                        state = PAUSED;
                        return true;
                    }

                    if (Math.abs(dy) > 45 && Math.abs(dy) > Math.abs(dx)) {
                        move(dy < 0 ? -1 : 1);
                        return true;
                    }

                    if (touchDownY > H - 125) {
                        if (touchDownX < W * 0.5f) move(-1);
                        else move(1);
                        return true;
                    }

                    if (Math.abs(y - laneY(lanePosition)) > 28) {
                        move(y < laneY(lanePosition) ? -1 : 1);
                    }
                    return true;
                }
            }
            return true;
        }

        private static final class Train {
            final int lane;
            float x, w, h, speed;
            final int dir;
            boolean hit;

            Train(int lane, float x, float w, float h, int dir, float speed) {
                this.lane = lane;
                this.x = x;
                this.w = w;
                this.h = h;
                this.dir = dir;
                this.speed = speed;
            }
        }

        private static final class Coin {
            final int lane;
            final boolean shield;
            float x;
            float drift;
            float phase;

            Coin(int lane, boolean canBeShield, Random random) {
                this.lane = lane;
                this.shield = canBeShield && random.nextFloat() < 0.25f;
            }
        }

        private static final class Particle {
            float x, y, vx, vy, size, life = 0.75f, maxLife = 0.75f;

            Particle(float x, float y, float vx, float vy, float size) {
                this.x = x;
                this.y = y;
                this.vx = vx;
                this.vy = vy;
                this.size = size;
            }
        }
    }
}
