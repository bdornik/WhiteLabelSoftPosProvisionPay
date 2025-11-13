package com.payten.whitelabel.dto;

public class PrintingText {
    private String tekst;
    boolean bold = false;
    boolean underline = false;
    char alignment = 'L';
    int fontSize = 0;

    public PrintingText(String tekst) {
        this.tekst = tekst;
    }

    public PrintingText(String tekst, boolean bold, boolean underline, char alignment, int fontSize) {
        this.tekst = tekst;
        this.bold = bold;
        this.underline = underline;
        this.alignment = alignment;
        this.fontSize = fontSize;
    }

    public PrintingText(PrintingText stampaniTekst) {
        this.tekst = stampaniTekst.getTekst();
        this.bold = stampaniTekst.isBold();
        this.underline = stampaniTekst.isUnderline();
        this.alignment = stampaniTekst.getAlignment();
        this.fontSize = stampaniTekst.getFontSize();
    }

    public void update() {
        if (bold) {
            this.tekst = "<b>" + this.tekst + "</b>";
        }
        if (underline) {
            this.tekst = "<u>" + this.tekst + "</u>";
        }
        switch (fontSize) {
            case 0:
                this.tekst = "<font size='normal'>" + this.tekst + "</font>";
                break;
            case 1:
                this.tekst = "<font size='big'>" + this.tekst + "</font>";
                break;
            case 2:
                this.tekst = "<font size='big-2'>" + this.tekst + "</font>";
                break;
            case 3:
                this.tekst = "<font size='big-3'>" + this.tekst + "</font>";
                break;
            case 4:
                this.tekst = "<font size='big-4'>" + this.tekst + "</font>";
                break;
            case 5:
                this.tekst = "<font size='big-5'>" + this.tekst + "</font>";
                break;
            case 6:
                this.tekst = "<font size='big-6'>" + this.tekst + "</font>";
                break;
            case 7:
                this.tekst = "<font size='wide'>" + this.tekst + "</font>";
                break;
            case 8:
                this.tekst = "<font size='tall'>" + this.tekst + "</font>";
                break;
        }
        switch (alignment) {
            case 'L':
                this.tekst = "[L]" + this.tekst;
                break;
            case 'C':
                this.tekst = "[C]" + this.tekst;
                break;
            case 'R':
                this.tekst = "[R]" + this.tekst;
                break;
        }
    }
    public String getTekst() {
        return tekst;
    }

    public boolean isBold() {
        return bold;
    }

    public boolean isUnderline() {
        return underline;
    }

    public char getAlignment() {
        return alignment;
    }

    public int getFontSize() {
        return fontSize;
    }

    public void setTekst(String tekst) {
        this.tekst = tekst;
    }

    public void setBold(boolean bold) {
        this.bold = bold;
    }

    public void setUnderline(boolean underline) {
        this.underline = underline;
    }

    public void setAlignment(char alignment) {
        this.alignment = alignment;
    }

    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
    }
}
