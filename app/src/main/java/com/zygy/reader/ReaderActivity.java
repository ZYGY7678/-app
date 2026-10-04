package com.zygy.reader;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class ReaderActivity extends Activity {
    public static final String EXTRA_URI = "book_uri";
    private static final String PREF = "bookreader";

    private Uri uri;
    private ReaderDocument document;
    private android.widget.ScrollView scroll;
    private TextView bookText;
    private SeekBar progressBar;
    private ProgressBar topProgress;
    private TextView percentLabel;
    private FrameLayout root;
    private SharedPreferences prefs;
    private PdfPageAdapter pdfAdapter;
    private RecyclerView pdfList;
    private android.graphics.pdf.PdfRenderer pdfRenderer;
    private android.os.ParcelFileDescriptor pdfFd;

    private int bgColor = Color.rgb(250,248,243);
    private int textColor = Color.rgb(36,34,31);
    private int accent = Color.rgb(124,92,252);
    private float textSize = 20f;
    private float lineSpacing = 1.32f;
    private int margin = 20;
    private boolean keepScreen = true;
    private String font = "serif";

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREF, MODE_PRIVATE);
        uri = Uri.parse(getIntent().getStringExtra(EXTRA_URI));
        loadPrefs();
        if (keepScreen) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        buildReaderShell();
        parseAsync();
    }

    private void buildReaderShell() {
        root = new FrameLayout(this);
        root.setBackgroundColor(bgColor);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(8), dp(8), dp(8), dp(4));
        top.setBackgroundColor(bgColor);

        ImageButton back = iconButton("‹");
        back.setOnClickListener(v -> finish());
        ImageButton search = iconButton("⌕");
        search.setOnClickListener(v -> searchInBook());
        ImageButton bookmark = iconButton("☆");
        bookmark.setOnClickListener(v -> addBookmark());
        ImageButton more = iconButton("⋮");
        more.setOnClickListener(v -> showReaderMenu());

        TextView title = new TextView(this);
        title.setText("פותח ספר…");
        title.setTextSize(16);
        title.setTextColor(textColor);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        top.addView(back, new LinearLayout.LayoutParams(dp(44), dp(44)));
        top.addView(title, new LinearLayout.LayoutParams(0, dp(44), 1f));
        top.addView(search, new LinearLayout.LayoutParams(dp(44), dp(44)));
        top.addView(bookmark, new LinearLayout.LayoutParams(dp(44), dp(44)));
        top.addView(more, new LinearLayout.LayoutParams(dp(44), dp(44)));

        topProgress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        topProgress.setMax(100);
        topProgress.setProgress(0);
        topProgress.setProgressTintList(android.content.res.ColorStateList.valueOf(accent));

        FrameLayout.LayoutParams topLp=new FrameLayout.LayoutParams(-1,dp(52),Gravity.TOP);
        root.addView(top,topLp);
        FrameLayout.LayoutParams progLp=new FrameLayout.LayoutParams(-1,dp(2),Gravity.TOP);
        progLp.topMargin=dp(52);
        root.addView(topProgress,progLp);

        percentLabel = new TextView(this);
        percentLabel.setText("0%");
        percentLabel.setTextSize(11);
        percentLabel.setTextColor(textColor);
        percentLabel.setGravity(Gravity.CENTER);

        progressBar = new SeekBar(this);
        progressBar.setMax(100);
        progressBar.setProgress(0);
        progressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(accent));
        progressBar.setThumbTintList(android.content.res.ColorStateList.valueOf(accent));
        progressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s,int p,boolean fromUser){ if(fromUser) jumpTo(p); updateLabel(p); }
            public void onStartTrackingTouch(SeekBar s){}
            public void onStopTrackingTouch(SeekBar s){}
        });

        LinearLayout bottom = new LinearLayout(this);
        bottom.setPadding(dp(18),dp(4),dp(18),dp(8));
        bottom.setGravity(Gravity.CENTER_VERTICAL);
        bottom.setBackgroundColor(bgColor);
        bottom.addView(progressBar,new LinearLayout.LayoutParams(0,dp(32),1f));
        bottom.addView(percentLabel,new LinearLayout.LayoutParams(dp(45),dp(32)));
        FrameLayout.LayoutParams bottomLp=new FrameLayout.LayoutParams(-1,dp(48),Gravity.BOTTOM);
        root.addView(bottom,bottomLp);
        setContentView(root);
    }

    private void parseAsync() {
        new Thread(() -> {
            try {
                document=BookParser.parse(this,uri);
                runOnUiThread(() -> showDocument());
            } catch(Exception e) {
                runOnUiThread(() -> showError(e));
            }
        }).start();
    }

    private void showDocument() {
        if (document.title != null) {
            TextView title=(TextView)((ViewGroup)root.getChildAt(0)).getChildAt(1);
            title.setText(document.title);
        }
        if (document.type == ReaderDocument.Type.PDF) {
            showPdf();
        } else {
            showText();
        }
    }

    private void showText() {
        scroll = new android.widget.ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(bgColor);
        scroll.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        bookText = new TextView(this);
        bookText.setText(document.text == null ? "" : document.text);
        bookText.setTextColor(textColor);
        bookText.setTextSize(textSize);
        bookText.setTypeface("sans".equals(font) ? Typeface.SANS_SERIF : Typeface.SERIF);
        bookText.setLineSpacing(0, lineSpacing);
        bookText.setGravity(isHebrew(document.text) ? Gravity.RIGHT : Gravity.LEFT);
        bookText.setTextIsSelectable(true);
        bookText.setPadding(dp(margin),dp(20),dp(margin),dp(30));

        scroll.addView(bookText,new android.widget.ScrollView.LayoutParams(-1,-2));
        FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(-1,-1);
        lp.topMargin=dp(54); lp.bottomMargin=dp(48);
        root.addView(scroll,lp);

        int saved=prefs.getInt(key("pos"),0);
        scroll.post(() -> {
            if (saved>0) scroll.scrollTo(0,saved);
            updateScrollProgress();
        });
        scroll.getViewTreeObserver().addOnScrollChangedListener(this::updateScrollProgress);
    }

    private void showPdf() {
        try {
            pdfFd=getContentResolver().openFileDescriptor(uri,"r");
            if(pdfFd==null) throw new IOException("לא ניתן לפתוח את הקובץ");
            pdfRenderer=new android.graphics.pdf.PdfRenderer(pdfFd);
            pdfList=new RecyclerView(this);
            pdfList.setLayoutManager(new LinearLayoutManager(this));
            pdfList.setBackgroundColor(Color.rgb(35,35,35));
            pdfAdapter=new PdfPageAdapter(pdfRenderer,bgColor);
            pdfList.setAdapter(pdfAdapter);
            FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(-1,-1);
            lp.topMargin=dp(54); lp.bottomMargin=dp(48);
            root.addView(pdfList,lp);
            int saved=prefs.getInt(key("page"),0);
            final int target=Math.min(saved,Math.max(0,pdfRenderer.getPageCount()-1));
            pdfList.post(() -> pdfList.scrollToPosition(target));
            pdfList.addOnScrollListener(new RecyclerView.OnScrollListener(){
                @Override public void onScrolled(RecyclerView rv,int dx,int dy){updatePdfProgress();}
            });
        } catch(Exception e){ showError(e); }
    }

    private void updateScrollProgress() {
        if(scroll==null || bookText==null) return;
        int range=bookText.getHeight()-scroll.getHeight();
        int pct=range<=0?0:(int)(scroll.getScrollY()*100f/range);
        progressBar.setProgress(pct); topProgress.setProgress(pct); updateLabel(pct);
    }

    private void updatePdfProgress() {
        if(pdfList==null || pdfRenderer==null) return;
        int page=((LinearLayoutManager)pdfList.getLayoutManager()).findFirstVisibleItemPosition();
        int total=pdfRenderer.getPageCount();
        int pct=total<=1?0:(int)(page*100f/(total-1));
        progressBar.setProgress(pct); topProgress.setProgress(pct); updateLabel(pct);
    }

    private void updateLabel(int p){ if(percentLabel!=null) percentLabel.setText(p+"%"); }

    private void jumpTo(int p) {
        if(document==null) return;
        if(document.type==ReaderDocument.Type.PDF && pdfList!=null && pdfRenderer!=null){
            int page=(int)Math.round((pdfRenderer.getPageCount()-1)*(p/100f));
            pdfList.scrollToPosition(page);
        } else if(scroll!=null && bookText!=null){
            int range=Math.max(0,bookText.getHeight()-scroll.getHeight());
            scroll.scrollTo(0,(int)(range*(p/100f)));
        }
    }

    private void searchInBook() {
        if(document==null || document.type==ReaderDocument.Type.PDF){
            Toast.makeText(this,"חיפוש בתוך PDF לא זמין בגרסה זו",Toast.LENGTH_SHORT).show(); return;
        }
        final EditText input=new EditText(this);
        input.setHint("מילה או משפט");
        input.setSingleLine(true);
        new AlertDialog.Builder(this).setTitle("חיפוש בספר").setView(input)
                .setNegativeButton("ביטול",null)
                .setPositiveButton("חפש",(d,w)->{
                    String q=input.getText().toString().trim();
                    if(q.isEmpty()) return;
                    String hay=document.text.toLowerCase(java.util.Locale.ROOT);
                    int at=hay.indexOf(q.toLowerCase(java.util.Locale.ROOT));
                    if(at<0){Toast.makeText(this,"לא נמצאה התאמה",Toast.LENGTH_SHORT).show();return;}
                    int range=Math.max(0,bookText.getHeight()-scroll.getHeight());
                    int pct=document.text.length()==0?0:(int)(at*100f/document.text.length());
                    scroll.post(()->scroll.scrollTo(0,(int)(range*(pct/100f))));
                    Toast.makeText(this,"נמצאה התאמה",Toast.LENGTH_SHORT).show();
                }).show();
    }

    private void addBookmark() {
        int pct=currentPercent();
        Set<String> set=new HashSet<>(prefs.getStringSet("bookmarks",new HashSet<>()));
        set.add(uri.toString()+"|"+pct);
        prefs.edit().putStringSet("bookmarks",set).apply();
        Toast.makeText(this,"נשמרה סימניה ב־"+pct+"%",Toast.LENGTH_SHORT).show();
    }

    private void showReaderMenu() {
        final String[] items={"אפשרויות קריאה","סימניות","מסך מלא","אודות"};
        new AlertDialog.Builder(this).setTitle("BookFlow").setItems(items,(d,which)->{
            if(which==0) showReadingSettings();
            else if(which==1) showBookmarks();
            else if(which==2) toggleImmersive();
            else new AlertDialog.Builder(this).setTitle("BookFlow Reader").setMessage("קורא ספרים מודרני עם שמירת התקדמות, סימניות, חיפוש, PDF, EPUB, TXT, HTML ו־FB2.").setPositiveButton("סגור",null).show();
        }).show();
    }

    private void showBookmarks() {
        Set<String> set=prefs.getStringSet("bookmarks",new HashSet<>());
        java.util.ArrayList<String> mine=new java.util.ArrayList<>();
        for(String s:set) if(s.startsWith(uri.toString()+"|")) mine.add("עמוד/מיקום "+s.substring(s.lastIndexOf('|')+1)+"%");
        if(mine.isEmpty()) mine.add("אין עדיין סימניות בספר הזה");
        final String[] arr=mine.toArray(new String[0]);
        new AlertDialog.Builder(this).setTitle("הסימניות שלי").setItems(arr,(d,w)->{
            if(mine.get(w).contains("%")) {
                try { jumpTo(Integer.parseInt(mine.get(w).replaceAll("\\D+",""))); } catch(Exception ignored){}
            }
        }).setPositiveButton("סגור",null).show();
    }

    private void showReadingSettings() {
        LinearLayout box=new LinearLayout(this);
        box.setPadding(dp(8),dp(4),dp(8),0);
        box.setOrientation(LinearLayout.VERTICAL);

        TextView size=text("גודל טקסט  •  "+Math.round(textSize),16,textColor,true);
        LinearLayout sizeRow=new LinearLayout(this);
        TextView minus=text("−",28,accent,true); minus.setGravity(Gravity.CENTER);
        TextView plus=text("+",28,accent,true); plus.setGravity(Gravity.CENTER);
        sizeRow.addView(minus,new LinearLayout.LayoutParams(0,dp(46),1f));
        sizeRow.addView(size,new LinearLayout.LayoutParams(0,dp(46),2f));
        size.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->size.setGravity(Gravity.CENTER));
        sizeRow.addView(plus,new LinearLayout.LayoutParams(0,dp(46),1f));
        box.addView(sizeRow);
        minus.setOnClickListener(v->{textSize=Math.max(14,textSize-1);prefs.edit().putFloat("size",textSize).apply();applyReadingStyle();size.setText("גודל טקסט  •  "+Math.round(textSize));});
        plus.setOnClickListener(v->{textSize=Math.min(34,textSize+1);prefs.edit().putFloat("size",textSize).apply();applyReadingStyle();size.setText("גודל טקסט  •  "+Math.round(textSize));});

        box.addView(text("ערכת צבעים",14,textColor,true),margin(0,10,0,5));
        LinearLayout themes=new LinearLayout(this);
        String[] names={"יום","קרם","לילה","AMOLED"};
        int[] colors={0xFFFAF8F3,0xFFF2E6C9,0xFF18202B,0xFF000000};
        for(int i=0;i<names.length;i++){
            TextView b=text(names[i],12,i<2?Color.DKGRAY:Color.WHITE,true);
            b.setGravity(Gravity.CENTER); b.setBackground(round(colors[i],dp(12)));
            final int idx=i; b.setOnClickListener(v->{setTheme(idx);});
            themes.addView(b,new LinearLayout.LayoutParams(0,dp(42),1f));
            if(i<names.length-1) themes.addView(new View(this),new LinearLayout.LayoutParams(dp(6),1));
        }
        box.addView(themes);

        box.addView(text("גופן",14,textColor,true),margin(0,10,0,5));
        LinearLayout fonts=new LinearLayout(this);
        TextView serif=text("Serif",13,textColor,true); serif.setGravity(Gravity.CENTER);
        TextView sans=text("Sans",13,textColor,true); sans.setGravity(Gravity.CENTER);
        serif.setOnClickListener(v->{font="serif";prefs.edit().putString("font",font).apply();applyReadingStyle();});
        sans.setOnClickListener(v->{font="sans";prefs.edit().putString("font",font).apply();applyReadingStyle();});
        fonts.addView(serif,new LinearLayout.LayoutParams(0,dp(42),1f));
        fonts.addView(sans,new LinearLayout.LayoutParams(0,dp(42),1f));
        box.addView(fonts);

        box.addView(text("מרווח שורות",14,textColor,true),margin(0,10,0,5));
        LinearLayout spacing=new LinearLayout(this);
        String[] sp={"צפוף","נוח","מרווח"};
        float[] values={1.12f,1.32f,1.52f};
        for(int i=0;i<sp.length;i++){
            TextView b=text(sp[i],12,textColor,true); b.setGravity(Gravity.CENTER);
            final float value=values[i]; b.setOnClickListener(v->{lineSpacing=value;prefs.edit().putFloat("spacing",value).apply();applyReadingStyle();});
            spacing.addView(b,new LinearLayout.LayoutParams(0,dp(40),1f));
        }
        box.addView(spacing);
        new AlertDialog.Builder(this).setTitle("אפשרויות קריאה").setView(box).setPositiveButton("סיום",null).show();
    }

    private void setTheme(int idx){
        if(idx==0){bgColor=0xFFFAF8F3;textColor=0xFF24221F;}
        if(idx==1){bgColor=0xFFF2E6C9;textColor=0xFF33291F;}
        if(idx==2){bgColor=0xFF18202B;textColor=0xFFE7EAF0;}
        if(idx==3){bgColor=Color.BLACK;textColor=0xFFF0F0F0;}
        prefs.edit().putInt("theme",idx).apply(); applyReadingStyle();
    }

    private void applyReadingStyle(){
        root.setBackgroundColor(bgColor);
        View top=root.getChildAt(0); top.setBackgroundColor(bgColor);
        if(scroll!=null) scroll.setBackgroundColor(bgColor);
        if(bookText!=null){
            bookText.setTextColor(textColor);
            bookText.setTextSize(textSize);
            bookText.setTypeface("sans".equals(font)?Typeface.SANS_SERIF:Typeface.SERIF);
            bookText.setLineSpacing(0,lineSpacing);
            bookText.setPadding(dp(margin),dp(20),dp(margin),dp(30));
        }
    }

    private void toggleImmersive(){
        int flags=View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_LOW_PROFILE;
        if((getWindow().getDecorView().getSystemUiVisibility()&View.SYSTEM_UI_FLAG_FULLSCREEN)!=0) getWindow().getDecorView().setSystemUiVisibility(0);
        else getWindow().getDecorView().setSystemUiVisibility(flags);
    }

    private void loadPrefs(){
        textSize=prefs.getFloat("size",20f);
        lineSpacing=prefs.getFloat("spacing",1.32f);
        font=prefs.getString("font","serif");
        int t=prefs.getInt("theme",1);
        if(t==0){bgColor=0xFFFAF8F3;textColor=0xFF24221F;}
        if(t==1){bgColor=0xFFF2E6C9;textColor=0xFF33291F;}
        if(t==2){bgColor=0xFF18202B;textColor=0xFFE7EAF0;}
        if(t==3){bgColor=Color.BLACK;textColor=0xFFF0F0F0;}
    }

    @Override protected void onPause(){
        super.onPause();
        if(uri!=null && document!=null){
            int pct=currentPercent();
            BookFlowProgress.save(this,uri,pct);
            if(scroll!=null) prefs.edit().putInt(key("pos"),scroll.getScrollY()).apply();
            if(pdfList!=null) prefs.edit().putInt(key("page"),((LinearLayoutManager)pdfList.getLayoutManager()).findFirstVisibleItemPosition()).apply();
        }
    }

    private int currentPercent(){
        if(document==null) return 0;
        if(document.type==ReaderDocument.Type.PDF){
            if(pdfList==null||pdfRenderer==null)return 0;
            int page=((LinearLayoutManager)pdfList.getLayoutManager()).findFirstVisibleItemPosition();
            return pdfRenderer.getPageCount()<=1?0:(int)(page*100f/(pdfRenderer.getPageCount()-1));
        }
        if(scroll==null||bookText==null)return 0;
        int range=bookText.getHeight()-scroll.getHeight();
        return range<=0?0:(int)(scroll.getScrollY()*100f/range);
    }

    private String key(String suffix){ return (uri.toString().hashCode())+"_"+suffix; }

    private boolean isHebrew(String s){
        if(s==null)return false; int h=0,l=0;
        for(int i=0;i<Math.min(s.length(),2500);i++){char c=s.charAt(i); if(c>0x0590&&c<0x05FF)h++; else if(Character.isLetter(c))l++;}
        return h>l;
    }

    private void showError(Exception e){
        new AlertDialog.Builder(this).setTitle("לא הצלחתי לפתוח את הספר")
                .setMessage("הקובץ לא נקרא או שהפורמט שלו אינו נתמך.\n\n"+(e.getMessage()==null?"":e.getMessage()))
                .setPositiveButton("חזרה",null).show();
    }

    private ImageButton iconButton(String symbol){
        ImageButton b=new ImageButton(this); b.setImageDrawable(null); b.setContentDescription(symbol);
        TextView t=text(symbol,28,textColor,true);
        final FrameLayout holder=new FrameLayout(this);
        holder.addView(t,new FrameLayout.LayoutParams(-1,-1));
        holder.setBackground(round(0x14000000,dp(14)));
        holder.setOnClickListener(v->b.performClick());
        b.setBackgroundColor(Color.TRANSPARENT); b.setTag(holder); return b;
    }

    private TextView text(String s,float size,int color,boolean bold){
        TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color);
        t.setTypeface(bold?Typeface.DEFAULT_BOLD:Typeface.DEFAULT); t.setGravity(Gravity.CENTER); return t;
    }

    private android.graphics.drawable.GradientDrawable round(int color,int radius){android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable();d.setColor(color);d.setCornerRadius(radius);return d;}
    private LinearLayout.LayoutParams margin(int l,int t,int r,int b){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(dp(l),dp(t),dp(r),dp(b));return p;}
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}

    @Override protected void onDestroy(){
        try{if(pdfRenderer!=null)pdfRenderer.close();}catch(Exception ignored){}
        try{if(pdfFd!=null)pdfFd.close();}catch(Exception ignored){}
        super.onDestroy();
    }

    static class BookFlowProgress{
        static void save(Activity a,Uri uri,int pct){
            SharedPreferences p=a.getSharedPreferences("bookflow",MODE_PRIVATE);
            p.edit().putString("progress_"+uri.toString().hashCode(),String.valueOf(pct)).apply();
            for(int i=0;i<8;i++) if(uri.toString().equals(p.getString("uri_"+i,null))) p.edit().putString("progress_"+i,String.valueOf(pct)).apply();
        }
    }
}
