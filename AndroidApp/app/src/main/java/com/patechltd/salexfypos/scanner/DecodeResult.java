package com.patechltd.salexfypos.scanner;

public class DecodeResult {
    public final String text;
    public final String format;

    public DecodeResult(String text, String format) {
        this.text = text;
        this.format = format;
    }
}
