package com.patechltd.salexfypos.scanner;

import android.media.Image;

public final class YuvUtils {

    private YuvUtils() {
    }

    public static class Nv21 {
        public byte[] data;
        public int width;
        public int height;
    }

    public static Nv21 convert(Image image, int rotationDegrees) {
        Image.Plane[] planes = image.getPlanes();
        Image.Plane yPlane = planes[0];
        Image.Plane uPlane = planes[1];
        Image.Plane vPlane = planes[2];

        int width = image.getWidth();
        int height = image.getHeight();
        int ySize = width * height;
        int uvSize = ySize / 2;

        byte[] nv21 = new byte[ySize + uvSize];

        copyPlane(nv21, 0, yPlane, width, height, width, 1);

        int uvWidth = width / 2;
        int uvHeight = height / 2;
        int uvOffset = ySize;

        byte[] rowU = new byte[uvWidth];
        byte[] rowV = new byte[uvWidth];
        for (int y = 0; y < uvHeight; y++) {
            yPlane.getBuffer().position(y * yPlane.getRowStride());
            uPlane.getBuffer().position(y * uPlane.getRowStride());
            vPlane.getBuffer().position(y * vPlane.getRowStride());
            uPlane.getBuffer().get(rowU, 0, uvWidth);
            vPlane.getBuffer().get(rowV, 0, uvWidth);
            for (int x = 0; x < uvWidth; x++) {
                nv21[uvOffset + (y * uvWidth + x) * 2] = rowV[x];
                nv21[uvOffset + (y * uvWidth + x) * 2 + 1] = rowU[x];
            }
        }

        Nv21 result = new Nv21();
        result.width = width;
        result.height = height;
        if (rotationDegrees == 0) {
            result.data = nv21;
            return result;
        }

        byte[] rotated = rotateNv21(nv21, width, height, rotationDegrees);
        result.data = rotated;
        if (rotationDegrees == 90 || rotationDegrees == 270) {
            result.width = height;
            result.height = width;
        }
        return result;
    }

    private static void copyPlane(byte[] dst, int dstOffset, Image.Plane plane, int width, int height,
                                  int stride, int pixelStride) {
        java.nio.ByteBuffer buffer = plane.getBuffer();
        int rowStride = plane.getRowStride();
        byte[] row = new byte[stride];
        for (int y = 0; y < height; y++) {
            buffer.position(y * rowStride);
            buffer.get(row, 0, stride);
            for (int x = 0; x < width; x++) {
                dst[dstOffset + y * width + x] = row[x * pixelStride];
            }
        }
    }

    public static byte[] rotateNv21(byte[] src, int width, int height, int rotation) {
        int ySize = width * height;
        byte[] out = new byte[src.length];

        if (rotation == 180) {
            for (int i = 0; i < ySize; i++) {
                out[ySize - 1 - i] = src[i];
            }
            int uvCount = ySize / 4;
            int uvStart = ySize;
            for (int i = 0; i < uvCount; i++) {
                int idx = uvStart + i * 2;
                int rev = uvStart + (uvCount - 1 - i) * 2;
                out[rev] = src[idx + 1];
                out[rev + 1] = src[idx];
            }
            return out;
        }

        int outW = (rotation == 90 || rotation == 270) ? height : width;
        int outH = (rotation == 90 || rotation == 270) ? width : height;
        int outYSize = outW * outH;

        if (rotation == 90) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int srcIdx = y * width + x;
                    int dstX = outW - 1 - y;
                    int dstY = x;
                    out[dstY * outW + dstX] = src[srcIdx];
                }
            }
            int uvW = width / 2;
            int uvH = height / 2;
            for (int y = 0; y < uvH; y++) {
                for (int x = 0; x < uvW; x++) {
                    int srcIdx = ySize + (y * uvW + x) * 2;
                    int dstX = (outW / 2) - 1 - y;
                    int dstY = x;
                    int dstIdx = outYSize + (dstY * (outW / 2) + dstX) * 2;
                    out[dstIdx] = src[srcIdx];
                    out[dstIdx + 1] = src[srcIdx + 1];
                }
            }
            return out;
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int srcIdx = y * width + x;
                int dstX = y;
                int dstY = outH - 1 - x;
                out[dstY * outW + dstX] = src[srcIdx];
            }
        }
        int uvW = width / 2;
        int uvH = height / 2;
        for (int y = 0; y < uvH; y++) {
            for (int x = 0; x < uvW; x++) {
                int srcIdx = ySize + (y * uvW + x) * 2;
                int dstX = y;
                int dstY = (outH / 2) - 1 - x;
                int dstIdx = outYSize + (dstY * (outW / 2) + dstX) * 2;
                out[dstIdx] = src[srcIdx];
                out[dstIdx + 1] = src[srcIdx + 1];
            }
        }
        return out;
    }
}
