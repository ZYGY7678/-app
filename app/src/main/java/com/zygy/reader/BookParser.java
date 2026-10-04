package com.zygy.reader;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.text.Html;
import android.text.Spanned;

import androidx.documentfile.provider.DocumentFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class BookParser {
    private BookParser(){}

    public static ReaderDocument parse(Context context, Uri uri) throws Exception {
        DocumentFile file=DocumentFile.fromSingleUri(context,uri);
        String title=file!=null?file.getName():null;
        if(title==null||title.trim().isEmpty()) title="ספר";
        String low=title.toLowerCase(Locale.ROOT);
        if(low.endsWith(".pdf")) return ReaderDocument.pdf(title);
        if(low.endsWith(".epub")) return ReaderDocument.text(title,parseEpub(context,uri));
        if(low.endsWith(".fb2")) return ReaderDocument.text(title,parseTextLike(context,uri,true));
        if(low.endsWith(".html")||low.endsWith(".htm")) return ReaderDocument.text(title,parseTextLike(context,uri,false));
        return ReaderDocument.text(title,readUtf8(context.getContentResolver().openInputStream(uri)));
    }

    private static String parseEpub(Context context,Uri uri) throws IOException {
        InputStream in=context.getContentResolver().openInputStream(uri);
        if(in==null) throw new IOException("אין גישה לקובץ");
        ArrayList<String> pages=new ArrayList<>();
        try(ZipInputStream zin=new ZipInputStream(in)){
            ZipEntry e;
            while((e=zin.getNextEntry())!=null){
                if(e.isDirectory()) continue;
                String n=e.getName().toLowerCase(Locale.ROOT);
                if(n.endsWith(".xhtml")||n.endsWith(".html")||n.endsWith(".htm")){
                    String html=readAll(zin);
                    if(!html.trim().isEmpty()) pages.add(cleanMarkup(html));
                }
            }
        }
        if(pages.isEmpty()) throw new IOException("ה־EPUB לא מכיל פרקי טקסט מוכרים");
        StringBuilder out=new StringBuilder();
        for(String p:pages){String s=p.trim();if(!s.isEmpty()){out.append(s).append("\n\n\n");}}
        return normalize(out.toString());
    }

    private static String parseTextLike(Context context,Uri uri,boolean fb2) throws Exception {
        String raw=readUtf8(context.getContentResolver().openInputStream(uri));
        if(fb2){
            int a=raw.indexOf("<body"); int b=raw.indexOf("</body>");
            if(a>=0&&b>a) raw=raw.substring(a,b);
        }
        return normalize(cleanMarkup(raw));
    }

    private static String cleanMarkup(String raw){
        if(raw==null)return "";
        String s=raw.replaceAll("(?is)<script.*?</script>"," ")
                .replaceAll("(?is)<style.*?</style>"," ")
                .replaceAll("(?i)</?(br|p|div|section|article|h[1-6]|li|title|blockquote|tr)[^>]*>","\n")
                .replaceAll("(?s)<[^>]+>"," ");
        Spanned sp=Html.fromHtml(s,Html.FROM_HTML_MODE_LEGACY);
        return sp.toString().replace("\u00A0"," ");
    }

    private static String readUtf8(InputStream in)throws IOException{
        if(in==null)throw new IOException("אין גישה לקובץ");
        return normalize(readAll(in));
    }

    private static String readAll(InputStream in)throws IOException{
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        byte[] buf=new byte[8192]; int n;
        while((n=in.read(buf))!=-1) out.write(buf,0,n);
        return out.toString(java.nio.charset.StandardCharsets.UTF_8);
    }

    private static String normalize(String s){
        return s.replace("\r\n","\n").replace("\r","\n")
                .replaceAll("[ \\t]+"," ")
                .replaceAll("\\n{4,}","\n\n\n")
                .trim();
    }
}
