package com.zygy.wallpapers;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends Activity {
    private static final int WALLPAPER_COUNT = 48;
    private final String[] names = {
            "Aurora", "Ocean Mist", "Purple Dream", "Night Pulse", "Sunset Glow", "Midnight",
            "Crystal Sky", "Neon Flow", "Deep Space", "Soft Horizon", "Blue Orbit", "Rose Light",
            "Forest Fade", "Golden Hour", "Violet Glass", "Carbon", "Arctic", "Cosmic",
            "Calm Lines", "Electric", "Moonlight", "Skyline", "Amber", "Indigo",
            "Minimal One", "Minimal Two", "Minimal Three", "Minimal Four", "Minimal Five", "Minimal Six",
            "Prism", "Wave", "Galaxy", "Lava", "Mint", "Cobalt",
            "Pearl", "Aqua", "Twilight", "Firefly", "Rain", "Silver",
            "Dream Grid", "Soft Focus", "Dark Bloom", "Solar", "Blue Flame", "Zen"
    };
    private final String[] categories = {"הכול", "צבעוני", "כהה", "בהיר", "מינימליסטי"};
    private LinearLayout grid;
    private EditText search;
    private String category = "הכול";
    private boolean favoritesOnly = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(8, 11, 16));
        getWindow().setNavigationBarColor(Color.rgb(8, 11, 16));
        buildUi();
        renderGrid();
    }

    private void buildUi() {
        ScrollView rootScroll = new ScrollView(this);
        rootScroll.setFillViewport(true);
        rootScroll.setBackgroundColor(Color.rgb(8, 11, 16));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(12), dp(18), dp(28));
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        rootScroll.addView(root);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, dp(4), 0, dp(10));

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, -2, 1f);

        TextView title = new TextView(this);
        title.setText("רקעים");
        title.setTextColor(Color.WHITE);
        title.setTextSize(29);
        title.setTypeface(Typeface.DEFAULT_BOLD);

        TextView subtitle = new TextView(this);
        subtitle.setText("בחר רקע יפה, ותן למסך שלך מראה חדש");
        subtitle.setTextColor(Color.rgb(157, 166, 181));
        subtitle.setTextSize(14);
        subtitle.setPadding(0, dp(3), 0, 0);

        titles.addView(title);
        titles.addView(subtitle);
        header.addView(titles, titleParams);

        TextView fav = actionChip("★");
        fav.setOnClickListener(v -> {
            favoritesOnly = !favoritesOnly;
            fav.setText(favoritesOnly ? "★" : "☆");
            renderGrid();
        });
        header.addView(fav, new LinearLayout.LayoutParams(dp(48), dp(48)));
        root.addView(header);

        search = new EditText(this);
        search.setSingleLine(true);
        search.setHint("חפש רקע...");
        search.setTextColor(Color.WHITE);
        search.setHintTextColor(Color.rgb(130, 138, 153));
        search.setTextSize(15);
        search.setPadding(dp(16), 0, dp(16), 0);
        search.setBackground(round(Color.rgb(20, 25, 34), dp(18)));
        root.addView(search, new LinearLayout.LayoutParams(-1, dp(52)));
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { renderGrid(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        HorizontalScrollView categoriesScroll = new HorizontalScrollView(this);
        categoriesScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.setPadding(0, dp(14), 0, dp(14));
        for (String c : categories) {
            TextView chip = actionChip(c);
            chip.setTag(c);
            chip.setOnClickListener(v -> {
                category = (String) v.getTag();
                refreshChipStyles(chips);
                renderGrid();
            });
            chips.addView(chip, new LinearLayout.LayoutParams(-2, dp(42)));
        }
        categoriesScroll.addView(chips);
        root.addView(categoriesScroll);

        TextView count = new TextView(this);
        count.setText("רקעים");
        count.setTextColor(Color.rgb(198, 203, 212));
        count.setTextSize(15);
        count.setTypeface(Typeface.DEFAULT_BOLD);
        count.setPadding(0, 0, 0, dp(10));
        root.addView(count);

        grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        root.addView(grid);
        setContentView(rootScroll);
        refreshChipStyles(chips);
    }

    private void refreshChipStyles(LinearLayout chips) {
        for (int i = 0; i < chips.getChildCount(); i++) {
            TextView chip = (TextView) chips.getChildAt(i);
            String value = (String) chip.getTag();
            boolean active = value.equals(category);
            chip.setTextColor(active ? Color.WHITE : Color.rgb(190, 196, 206));
            chip.setBackground(round(active ? Color.rgb(124, 92, 252) : Color.rgb(25, 30, 40), dp(18)));
        }
    }

    private void renderGrid() {
        if (grid == null) return;
        grid.removeAllViews();
        String q = search == null ? "" : search.getText().toString().trim().toLowerCase(Locale.ROOT);

        LinearLayout row = null;
        int shown = 0;
        for (int id = 0; id < WALLPAPER_COUNT; id++) {
            if (!matchesCategory(id) || !names[id].toLowerCase(Locale.ROOT).contains(q) || (favoritesOnly && !isFavorite(id))) {
                continue;
            }
            if (shown % 2 == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
                grid.addView(row, new LinearLayout.LayoutParams(-1, dp(280)));
            }
            row.addView(createCard(id), new LinearLayout.LayoutParams(0, dp(268), 1f));
            if (shown % 2 == 0) {
                View gap = spacer(8);
                row.addView(gap, new LinearLayout.LayoutParams(dp(8), dp(268)));
            }
            shown++;
        }

        if (shown == 0) {
            TextView empty = new TextView(this);
            empty.setText("לא נמצאו רקעים מתאימים");
            empty.setTextColor(Color.rgb(150, 158, 172));
            empty.setGravity(Gravity.CENTER);
            empty.setTextSize(16);
            grid.addView(empty, new LinearLayout.LayoutParams(-1, dp(180)));
        }
    }

    private boolean matchesCategory(int id) {
        if ("הכול".equals(category)) return true;
        int style = id % 6;
        if ("מינימליסטי".equals(category)) return style == 4;
        if ("כהה".equals(category)) return style == 1 || style == 4;
        if ("בהיר".equals(category)) return style == 0 || style == 3;
        return style == 2 || style == 5;
    }

    private View createCard(int id) {
        FrameLayout card = new FrameLayout(this);
        card.setBackground(round(Color.rgb(19, 24, 33), dp(20)));
        card.setClipToOutline(true);
        card.setElevation(dp(3));

        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setImageBitmap(WallpaperGenerator.generate(id, dp(220), dp(330)));
        card.addView(image, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setGravity(Gravity.CENTER_VERTICAL);
        bottom.setPadding(dp(12), dp(8), dp(8), dp(8));
        bottom.setBackgroundColor(0xB3090C12);
        FrameLayout.LayoutParams bottomParams = new FrameLayout.LayoutParams(-1, dp(52), Gravity.BOTTOM);
        card.addView(bottom, bottomParams);

        TextView label = new TextView(this);
        label.setText(names[id]);
        label.setTextColor(Color.WHITE);
        label.setTextSize(13);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setSingleLine(true);
        bottom.addView(label, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView star = actionChip(isFavorite(id) ? "★" : "☆");
        star.setTextSize(18);
        star.setBackgroundColor(Color.TRANSPARENT);
        star.setOnClickListener(v -> {
            toggleFavorite(id);
            star.setText(isFavorite(id) ? "★" : "☆");
            Toast.makeText(this, isFavorite(id) ? "נוסף למועדפים" : "הוסר מהמועדפים", Toast.LENGTH_SHORT).show();
        });
        bottom.addView(star, new LinearLayout.LayoutParams(dp(42), dp(42)));

        TextView set = actionChip("הגדר");
        set.setTextSize(12);
        set.setTextColor(Color.WHITE);
        set.setBackground(round(Color.rgb(124, 92, 252), dp(15)));
        set.setOnClickListener(v -> setWallpaper(id));
        bottom.addView(set, new LinearLayout.LayoutParams(dp(64), dp(40)));

        card.setOnClickListener(v -> setWallpaper(id));
        return card;
    }

    private void setWallpaper(int id) {
        try {
            Bitmap bitmap = WallpaperGenerator.generate(id, 1080, 1920);
            WallpaperManager manager = WallpaperManager.getInstance(this);
            manager.setBitmap(bitmap);
            bitmap.recycle();
            Toast.makeText(this, "הטפט הוגדר בהצלחה", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "לא ניתן להגדיר את הטפט במכשיר הזה", Toast.LENGTH_LONG).show();
        }
    }

    private boolean isFavorite(int id) {
        return getPreferences(Context.MODE_PRIVATE).getBoolean("fav_" + id, false);
    }

    private void toggleFavorite(int id) {
        getPreferences(Context.MODE_PRIVATE).edit().putBoolean("fav_" + id, !isFavorite(id)).apply();
    }

    private TextView actionChip(String text) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setGravity(Gravity.CENTER);
        v.setTextSize(14);
        v.setTypeface(Typeface.DEFAULT_BOLD);
        v.setTextColor(Color.WHITE);
        v.setBackground(round(Color.rgb(25, 30, 40), dp(18)));
        return v;
    }

    private GradientDrawable round(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        return d;
    }

    private View spacer(int width) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(width, 1));
        return v;
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
