package com.patechltd.salexfypos.print;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;

/**
 * Minimal ESC/POS command builder for 80mm / 58mm thermal receipt printers.
 */
public final class EscPos {

    public static final int ALIGN_LEFT = 0;
    public static final int ALIGN_CENTER = 1;
    public static final int ALIGN_RIGHT = 2;

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();

    public byte[] toByteArray() {
        return out.toByteArray();
    }

    public EscPos init() {
        out.write(0x1B);
        out.write(0x40);
        return this;
    }

    public EscPos feed(int lines) {
        out.write(0x1B);
        out.write(0x64);
        out.write(lines & 0xFF);
        return this;
    }

    public EscPos cut() {
        out.write(0x1D);
        out.write(0x56);
        out.write(0x41);
        out.write(0x00);
        return this;
    }

    public EscPos align(int align) {
        out.write(0x1B);
        out.write(0x61);
        out.write(align & 0xFF);
        return this;
    }

    public EscPos bold(boolean on) {
        out.write(0x1B);
        out.write(0x45);
        out.write(on ? 1 : 0);
        return this;
    }

    public EscPos size(int width, int height) {
        out.write(0x1D);
        out.write(0x21);
        int v = ((width - 1) & 0x0F) | (((height - 1) & 0x0F) << 4);
        out.write(v);
        return this;
    }

    public EscPos normalSize() {
        return size(1, 1);
    }

    public EscPos text(String text) {
        text(text, Charset.forName("ISO-8859-1"));
        return this;
    }

    public EscPos text(String text, Charset charset) {
        try {
            out.write(text.getBytes(charset));
        } catch (Exception ignored) {
            for (int i = 0; i < text.length(); i++) {
                out.write(text.charAt(i) & 0xFF);
            }
        }
        return this;
    }

    public EscPos line(String text) {
        return line(text, ALIGN_LEFT, false);
    }

    public EscPos line(String text, int align, boolean bold) {
        this.align(align).bold(bold).text(text).text("\n");
        return this;
    }

    public EscPos divider(char ch, int width) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < width; i++) sb.append(ch);
        return line(sb.toString());
    }

    public EscPos blank() {
        return line("");
    }
}
