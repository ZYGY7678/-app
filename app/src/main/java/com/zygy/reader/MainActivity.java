package com.zygy.reader;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int PICK_BOOK = 700;
    private static final int MAX_RECENTS = 8;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("bookflow", MODE_PRIVATE);
        getWindow().setStatusBarColor(Color.rgb(8, 10, 17));
        getWindow().setNavigationBarColor(Color.rgb(8, 10, 17));
        buildHome();

        if (Intent.ACTION_VIEW.equals(getIntent().getAction()) && getIntent().getData() != null) {
            openReader(getIntent().getData());
        }
    }

    private void buildHome() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(8, 10, 17));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(30));
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        scroll.addView(root);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text("BookFlow", 30, Color.WHITE, true);
        TextView logo = text("▰", 30, Color.rgb(124, 92, 252), true);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1f));
        header.addView(logo, new LinearLayout.LayoutParams(dp(50), dp(50)));
        root.addView(header);

        TextView subtitle = text("ספרייה אישית. קריאה רגועה. שליטה מלאה.", 14, Color.rgb(158, 168, 187), false);
        subtitle.setPadding(0, 0, 0, dp(14));
        root.addView(subtitle);

        MaterialCardView hero = card(Color.rgb(24, 22, 43));
        LinearLayout heroBody = vertical();
        heroBody.setPadding(dp(20), dp(20), dp(20), dp(18));

        TextView eyebrow = text("הקורא החדש שלך", 13, Color.rgb(167, 149, 255), true);
        TextView heroTitle = text("פתח ספר\nוהיכנס ישר לעולם שלו", 28, Color.WHITE, true);
        heroTitle.setLineSpacing(0, 1.02f);
        TextView heroText = text("PDF, EPUB, TXT, HTML ו-FB2 — עם המשך קריאה אוטומטי ועיצוב שנועד לשכוח מהמסך.", 14, Color.rgb(189, 194, 207), false);
        heroText.setLineSpacing(0, 1.18f);
        MaterialButton open = new MaterialButton(this);
        open.setText("  פתיחת ספר  ");
        open.setTextSize(15);
        open.setTextColor(Color.WHITE);
        open.setCornerRadius(dp(18));
        open.setBackgroundColor(Color.rgb(124, 92, 252));
        open.setOnClickListener(v -> chooseBook());

        heroBody.addView(eyebrow);
        heroBody.addView(heroTitle, marginParams(0, 8, 0, 10));
        heroBody.addView(heroText);
        heroBody.addView(open, marginParams(0, 18, 0, 0));
        hero.addView(heroBody);
        root.addView(hero);

        LinearLayout featureRow = horizontal();
        String[][] features = {
                {"◷", "המשך קריאה", "נשמר אוטומטית"},
                {"☆", "סימניות", "מקומות חשובים"},
                {"Aa", "עיצוב אישי", "גופן, גודל ומרווח"}
        };
        for (String[] f : features) {
            MaterialCardView c = card(Color.rgb(17, 20, 29));
            LinearLayout body = vertical();
            body.setGravity(Gravity.CENTER);
            body.setPadding(dp(12), dp(14), dp(12), dp(14));
            TextView icon = text(f[0], 23, Color.rgb(124, 92, 252), true);
            icon.setGravity(Gravity.CENTER);
            TextView a = text(f[1], 12, Color.WHITE, true);
            a.setGravity(Gravity.CENTER);
            TextView b = text(f[2], 10, Color.rgb(130, 139, 157), false);
            b.setGravity(Gravity.CENTER);
            body.addView(icon);
            body.addView(a, marginParams(0, 7, 0, 0));
            body.addView(b, marginParams(0, 2, 0, 0));
            c.addView(body);
            featureRow.addView(c, new LinearLayout.LayoutParams(0, dp(102), 1f));
            if (f != features[features.length - 1]) {
                View gap = new View(this);
                featureRow.addView(gap, new LinearLayout.LayoutParams(dp(7), 1));
            }
        }
        root.addView(featureRow, marginParams(0, 14, 0, 14));

        TextView section = text("הספרים האחרונים", 20, Color.WHITE, true);
        root.addView(section);

        List<String[]> recent = loadRecents();
        if (recent.isEmpty()) {
            MaterialCardView empty = card(Color.rgb(15, 18, 26));
            LinearLayout ebody = vertical();
            ebody.setGravity(Gravity.CENTER);
            ebody.setPadding(dp(20), dp(26), dp(20), dp(26));
            TextView ei = text("✦", 28, Color.rgb(124, 92, 252), true);
            TextView et = text("הספרייה שלך מוכנה", 18, Color.WHITE, true);
            TextView es = text("פתח ספר ראשון והקורא יזכור איפה עצרת.", 13, Color.rgb(139, 147, 162), false);
            ebody.addView(ei);
            ebody.addView(et, marginParams(0, 7, 0, 0));
            ebody.addView(es, marginParams(0, 4, 0, 0));
            empty.addView(ebody);
            root.addView(empty, marginParams(0, 12, 0, 0));
        } else {
            for (String[] item : recent) root.addView(recentCard(item), marginParams(0, 9, 0, 0));
        }

        TextView advanced = text("עוד", 20, Color.WHITE, true);
        root.addView(advanced, marginParams(0, 24, 0, 8));

        MaterialCardView settings = card(Color.rgb(15, 18, 26));
        LinearLayout sbody = horizontal();
        sbody.setGravity(Gravity.CENTER_VERTICAL);
        sbody.setPadding(dp(16), dp(14), dp(16), dp(14));
        TextView st = text("העדפות קריאה", 15, Color.WHITE, true);
        TextView ss = text("מצב לילה, גופן, גודל טקסט ועוד", 12, Color.rgb(139, 147, 162), false);
        LinearLayout stack = vertical();
        stack.addView(st);
        stack.addView(ss, marginParams(0, 3, 0, 0));
        TextView gear = text("⚙", 24, Color.rgb(124, 92, 252), true);
        sbody.addView(stack, new LinearLayout.LayoutParams(0, -2, 1f));
        sbody.addView(gear, new LinearLayout.LayoutParams(dp(36), dp(36)));
        settings.addView(sbody);
        settings.setOnClickListener(v -> showAppTips());
        root.addView(settings);

        MaterialCardView about = card(Color.rgb(12, 15, 22));
        LinearLayout ab = vertical();
        ab.setPadding(dp(16), dp(14), dp(16), dp(14));
        ab.addView(text("BookFlow  •  גרסה 1.0", 12, Color.rgb(124, 132, 149), true));
        ab.addView(text("נבנה לקריאה פשוטה, יפה ומהירה.", 11, Color.rgb(105, 114, 130), false), marginParams(0, 4, 0, 0));
        about.addView(ab);
        root.addView(about, marginParams(0, 12, 0, 0));

        setContentView(scroll);
    }

    private MaterialCardView recentCard(String[] item) {
        String title = item[0];
        String uri = item[1];
        int progress = safeInt(item[2]);
        MaterialCardView card = card(Color.rgb(15, 18, 26));
        LinearLayout body = horizontal();
        body.setGravity(Gravity.CENTER_VERTICAL);
        body.setPadding(dp(14), dp(13), dp(14), dp(13));

        TextView cover = text(fileBadge(title), 23, Color.WHITE, true);
        cover.setGravity(Gravity.CENTER);
        cover.setBackground(round(Color.rgb(124, 92, 252), dp(14)));
        body.addView(cover, new LinearLayout.LayoutParams(dp(52), dp(62)));

        LinearLayout stack = vertical();
        TextView t = text(title, 16, Color.WHITE, true);
        t.setMaxLines(2);
        TextView p = text(progress == 0 ? "עדיין לא התחלת" : "המשך מ־" + progress + "%", 12, Color.rgb(139, 147, 162), false);
        stack.addView(t, new LinearLayout.LayoutParams(-1, -2));
        stack.addView(p, marginParams(0, 4, 0, 7));
        android.widget.ProgressBar bar = new android.widget.ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        bar.setProgress(progress);
        stack.addView(bar, new LinearLayout.LayoutParams(-1, dp(4)));
        body.addView(stack, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView go = text("‹", 30, Color.rgb(124, 92, 252), true);
        body.addView(go, new LinearLayout.LayoutParams(dp(30), -1));

        card.addView(body);
        card.setOnClickListener(v -> openReader(Uri.parse(uri)));
        card.setOnLongClickListener(v -> {
            prefs.edit().remove("uri_" + indexOfRecent(uri)).apply();
            buildHome();
            return true;
        });
        return card;
    }

    private String fileBadge(String title) {
        String n = title.toLowerCase(Locale.ROOT);
        if (n.endsWith(".pdf")) return "PDF";
        if (n.endsWith(".epub")) return "E";
        return "TXT";
    }

    private void chooseBook() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(i, PICK_BOOK);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_BOOK || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            getContentResolver().takePersistableUriPermission(uri, data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) {}
        openReader(uri);
    }

    private void openReader(Uri uri) {
        Intent i = new Intent(this, ReaderActivity.class);
        i.putExtra(ReaderActivity.EXTRA_URI, uri.toString());
        startActivity(i);
    }

    static void rememberBook(Activity activity, Uri uri, String title) {
        SharedPreferences p = activity.getSharedPreferences("bookflow", MODE_PRIVATE);
        ArrayList<String> uris = new ArrayList<>();
        ArrayList<String> titles = new ArrayList<>();
        ArrayList<String> progress = new ArrayList<>();
        for (int j = 0; j < MAX_RECENTS; j++) {
            String u = p.getString("uri_" + j, null);
            if (u != null && !u.equals(uri.toString())) {
                uris.add(u);
                titles.add(p.getString("title_" + j, "ספר"));
                progress.add(p.getString("progress_" + j, "0"));
            }
        }
        uris.add(0, uri.toString());
        titles.add(0, title);
        progress.add(0, p.getString("progress_" + uri.toString().hashCode(), "0"));
        SharedPreferences.Editor e = p.edit();
        for (int j=0; j<MAX_RECENTS; j++) e.remove("uri_"+j).remove("title_"+j).remove("progress_"+j);
        for (int j=0; j<Math.min(MAX_RECENTS, uris.size()); j++) {
            e.putString("uri_"+j, uris.get(j));
            e.putString("title_"+j, titles.get(j));
            e.putString("progress_"+j, progress.get(j));
        }
        e.apply();
    }

    static void saveProgress(Activity activity, Uri uri, int percent) {
        SharedPreferences p = activity.getSharedPreferences("bookflow", MODE_PRIVATE);
        p.edit().putString("progress_"+uri.toString().hashCode(), String.valueOf(percent)).apply();
        for (int j=0;j<MAX_RECENTS;j++) if (uri.toString().equals(p.getString("uri_"+j,null))) {
            p.edit().putString("progress_"+j, String.valueOf(percent)).apply();
        }
    }

    private List<String[]> loadRecents() {
        List<String[]> out = new ArrayList<>();
        for (int j=0; j<MAX_RECENTS; j++) {
            String uri = prefs.getString("uri_"+j, null);
            if (uri != null) out.add(new String[]{
                    prefs.getString("title_"+j, "ספר"),
                    uri,
                    prefs.getString("progress_"+j, "0")
            });
        }
        return out;
    }

    private int indexOfRecent(String uri) {
        for (int j=0;j<MAX_RECENTS;j++) if (uri.equals(prefs.getString("uri_"+j,null))) return j;
        return 0;
    }

    private void showAppTips() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("העדפות קריאה")
                .setMessage("את רוב העדפות הקריאה אפשר לשנות מתוך מסך הקריאה עצמו: מצב יום/קרם/לילה/AMOLED, גודל טקסט, מרווח שורות, שוליים, גופן, כיוון טקסט, חיפוש וסימניות.\n\nלחיצה ארוכה על ספר אחרון מסירה אותו מהמסך הראשי.")
                .setPositiveButton("מעולה", null)
                .show();
    }

    private MaterialCardView card(int color) {
        MaterialCardView c = new MaterialCardView(this);
        c.setCardBackgroundColor(color);
        c.setRadius(dp(20));
        c.setStrokeWidth(dp(1));
        c.setStrokeColor(Color.rgb(35, 39, 50));
        return c;
    }

    private LinearLayout vertical() { LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout horizontal() { LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); return l; }
    private TextView text(String s,float size,int color,boolean bold){
        TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color);
        t.setTypeface(bold?Typeface.DEFAULT_BOLD:Typeface.DEFAULT); t.setGravity(Gravity.RIGHT); return t;
    }
    private LinearLayout.LayoutParams marginParams(int l,int t,int r,int b){ LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.setMargins(dp(l),dp(t),dp(r),dp(b)); return p; }
    private android.graphics.drawable.GradientDrawable round(int color,int radius){ android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d; }
    private int dp(int x){ return (int)(x*getResources().getDisplayMetrics().density+0.5f); }
    private int safeInt(String s){ try{return Integer.parseInt(s);}catch(Exception e){return 0;} }
}
