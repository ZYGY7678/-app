package com.zygy.reader;

public final class ReaderDocument {
    public enum Type { TEXT, PDF }
    public final Type type;
    public final String title;
    public final String text;
    public ReaderDocument(Type type,String title,String text){this.type=type;this.title=title;this.text=text;}
    public static ReaderDocument text(String title,String text){return new ReaderDocument(Type.TEXT,title,text);}
    public static ReaderDocument pdf(String title){return new ReaderDocument(Type.PDF,title,null);}
}
