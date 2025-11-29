package com.example.calculatrice.qr;

import android.graphics.Bitmap;
import android.graphics.Color;

import java.nio.charset.StandardCharsets;

/**
 * Tiny QR encoder that supports byte mode, version 1 (21x21), error level L.
 * Trade-off: payload must be <= 19 bytes.
 */
public final class SimpleQrGenerator {

    private static final int QR_SIZE = 21;
    private static final int DATA_CODEWORDS = 19;
    private static final int[] ECC_POLY = {87, 229, 146, 149, 238, 102, 21};
    private static final int MASK = 0; // pattern 0: (row + col) % 2 == 0

    private SimpleQrGenerator() {
    }

    public static Bitmap generate(String data, int sizePx) {
        byte[] payload = data.getBytes(StandardCharsets.ISO_8859_1);
        if (payload.length > DATA_CODEWORDS) {
            throw new IllegalArgumentException("QR payload too long for simple generator");
        }
        byte[] dataCodewords = buildDataCodewords(payload);
        byte[] ecc = computeEcc(dataCodewords);
        boolean[][] matrix = buildMatrix(dataCodewords, ecc);
        return toBitmap(matrix, sizePx);
    }

    private static byte[] buildDataCodewords(byte[] payload) {
        BitBuffer buffer = new BitBuffer();
        buffer.append(0b0100, 4); // byte mode
        buffer.append(payload.length, 8);
        for (byte b : payload) {
            buffer.append(b & 0xFF, 8);
        }
        int capacityBits = DATA_CODEWORDS * 8;
        int remaining = capacityBits - buffer.length();
        int terminator = Math.min(4, remaining);
        buffer.append(0, terminator);
        while (buffer.length() % 8 != 0) {
            buffer.appendBit(false);
        }
        int padByte = 0xEC;
        int padAlt = 0x11;
        while (buffer.length() < capacityBits) {
            buffer.append(padByte, 8);
            int tmp = padByte;
            padByte = padAlt;
            padAlt = tmp;
        }
        return buffer.toByteArray();
    }

    private static byte[] computeEcc(byte[] data) {
        byte[] ecc = new byte[ECC_POLY.length];
        for (byte datum : data) {
            int factor = (datum ^ ecc[0]) & 0xFF;
            System.arraycopy(ecc, 1, ecc, 0, ecc.length - 1);
            ecc[ecc.length - 1] = 0;
            if (factor != 0) {
                for (int i = 0; i < ECC_POLY.length; i++) {
                    ecc[i] ^= (byte) gfMultiply(ECC_POLY[i], factor);
                }
            }
        }
        return ecc;
    }

    private static boolean[][] buildMatrix(byte[] data, byte[] ecc) {
        boolean[][] modules = new boolean[QR_SIZE][QR_SIZE];
        boolean[][] isFunction = new boolean[QR_SIZE][QR_SIZE];

        for (int y = 0; y < QR_SIZE; y++) {
            for (int x = 0; x < QR_SIZE; x++) {
                modules[y][x] = false;
                isFunction[y][x] = false;
            }
        }

        drawFinderPattern(modules, isFunction, 0, 0);
        drawFinderPattern(modules, isFunction, QR_SIZE - 7, 0);
        drawFinderPattern(modules, isFunction, 0, QR_SIZE - 7);
        drawSeparators(isFunction);
        drawTimingPatterns(modules, isFunction);
        drawDarkModule(modules, isFunction);

        byte[] allCodewords = new byte[data.length + ecc.length];
        System.arraycopy(data, 0, allCodewords, 0, data.length);
        System.arraycopy(ecc, 0, allCodewords, data.length, ecc.length);
        boolean[] bits = toBits(allCodewords);
        placeDataBits(modules, isFunction, bits);
        applyMask(modules, isFunction);
        drawFormatInformation(modules, isFunction);

        return modules;
    }

    private static void drawFinderPattern(boolean[][] modules, boolean[][] isFunction, int startX, int startY) {
        for (int y = 0; y < 7; y++) {
            for (int x = 0; x < 7; x++) {
                int xx = startX + x;
                int yy = startY + y;
                boolean val = (x == 0 || x == 6 || y == 0 || y == 6) || (x >= 2 && x <= 4 && y >= 2 && y <= 4);
                modules[yy][xx] = val;
                isFunction[yy][xx] = true;
            }
        }
    }

    private static void drawSeparators(boolean[][] isFunction) {
        for (int i = 0; i < 8; i++) {
            markFunction(isFunction, i, 7);
            markFunction(isFunction, 7, i);
            markFunction(isFunction, QR_SIZE - 8 + i, 7);
            markFunction(isFunction, QR_SIZE - 8, i);
            markFunction(isFunction, i, QR_SIZE - 8);
            markFunction(isFunction, 7, QR_SIZE - 8 + i);
        }
    }

    private static void drawTimingPatterns(boolean[][] modules, boolean[][] isFunction) {
        for (int i = 0; i < QR_SIZE; i++) {
            boolean val = i % 2 == 0;
            modules[6][i] = val;
            modules[i][6] = val;
            isFunction[6][i] = true;
            isFunction[i][6] = true;
        }
    }

    private static void drawDarkModule(boolean[][] modules, boolean[][] isFunction) {
        modules[QR_SIZE - 8][8] = true;
        isFunction[QR_SIZE - 8][8] = true;
    }

    private static boolean[] toBits(byte[] codewords) {
        boolean[] bits = new boolean[codewords.length * 8];
        for (int i = 0; i < codewords.length; i++) {
            for (int j = 0; j < 8; j++) {
                bits[i * 8 + (7 - j)] = ((codewords[i] >>> j) & 1) == 1;
            }
        }
        return bits;
    }

    private static void placeDataBits(boolean[][] modules, boolean[][] isFunction, boolean[] bits) {
        int bitIndex = 0;
        int direction = -1;
        for (int col = QR_SIZE - 1; col > 0; col -= 2) {
            if (col == 6) col--;
            for (int rowIter = 0; rowIter < QR_SIZE; rowIter++) {
                int row = direction == -1 ? QR_SIZE - 1 - rowIter : rowIter;
                for (int offset = 0; offset < 2; offset++) {
                    int c = col - offset;
                    if (isFunction[row][c]) {
                        continue;
                    }
                    boolean bit = bitIndex < bits.length && bits[bitIndex];
                    modules[row][c] = bit;
                    bitIndex++;
                }
            }
            direction = -direction;
        }
    }

    private static void applyMask(boolean[][] modules, boolean[][] isFunction) {
        for (int y = 0; y < QR_SIZE; y++) {
            for (int x = 0; x < QR_SIZE; x++) {
                if (isFunction[y][x]) continue;
                boolean invert = ((x + y) % 2 == 0); // mask 0
                if (invert) {
                    modules[y][x] = !modules[y][x];
                }
            }
        }
    }

    private static void drawFormatInformation(boolean[][] modules, boolean[][] isFunction) {
        int eclBits = 0b01; // Level L
        int formatValue = (eclBits << 3) | MASK;
        int formatBits = computeBchCode(formatValue) ^ 0b101010000010010;
        int[][] positionsA = {
                {8, 0}, {8, 1}, {8, 2}, {8, 3}, {8, 4}, {8, 5}, {8, 7}, {8, 8},
                {7, 8}, {5, 8}, {4, 8}, {3, 8}, {2, 8}, {1, 8}, {0, 8}
        };
        int[][] positionsB = {
                {QR_SIZE - 1, 8}, {QR_SIZE - 2, 8}, {QR_SIZE - 3, 8}, {QR_SIZE - 4, 8},
                {QR_SIZE - 5, 8}, {QR_SIZE - 6, 8}, {QR_SIZE - 7, 8}, {QR_SIZE - 8, 8},
                {8, QR_SIZE - 7}, {8, QR_SIZE - 6}, {8, QR_SIZE - 5},
                {8, QR_SIZE - 4}, {8, QR_SIZE - 3}, {8, QR_SIZE - 2}, {8, QR_SIZE - 1}
        };
        for (int i = 0; i < 15; i++) {
            boolean bit = ((formatBits >> i) & 1) == 1;
            setFormatModule(modules, isFunction, positionsA[i][0], positionsA[i][1], bit);
            setFormatModule(modules, isFunction, positionsB[i][0], positionsB[i][1], bit);
        }
    }

    private static void setFormatModule(boolean[][] modules, boolean[][] isFunction, int x, int y, boolean value) {
        modules[y][x] = value;
        isFunction[y][x] = true;
    }

    private static int computeBchCode(int value) {
        int poly = 0x537;
        int result = value << 10;
        for (int i = 14; i >= 10; i--) {
            if (((result >> i) & 1) == 1) {
                result ^= poly << (i - 10);
            }
        }
        return (value << 10) | (result & 0x3FF);
    }

    private static void markFunction(boolean[][] isFunction, int x, int y) {
        if (within(x, y)) {
            isFunction[y][x] = true;
        }
    }

    private static boolean within(int x, int y) {
        return x >= 0 && y >= 0 && x < QR_SIZE && y < QR_SIZE;
    }

    private static int gfMultiply(int a, int b) {
        int result = 0;
        while (b != 0) {
            if ((b & 1) != 0) {
                result ^= a;
            }
            int hi = a & 0x80;
            a <<= 1;
            if (hi != 0) {
                a ^= 0x11D;
            }
            a &= 0xFF;
            b >>= 1;
        }
        return result;
    }

    private static Bitmap toBitmap(boolean[][] matrix, int sizePx) {
        int moduleSize = sizePx / QR_SIZE;
        int dim = moduleSize * QR_SIZE;
        if (moduleSize <= 0) {
            moduleSize = 1;
            dim = QR_SIZE;
        }
        Bitmap bitmap = Bitmap.createBitmap(dim, dim, Bitmap.Config.ARGB_8888);
        for (int y = 0; y < QR_SIZE; y++) {
            for (int x = 0; x < QR_SIZE; x++) {
                int color = matrix[y][x] ? Color.BLACK : Color.WHITE;
                for (int dy = 0; dy < moduleSize; dy++) {
                    for (int dx = 0; dx < moduleSize; dx++) {
                        bitmap.setPixel(x * moduleSize + dx, y * moduleSize + dy, color);
                    }
                }
            }
        }
        return bitmap;
    }

    private static final class BitBuffer {
        private final StringBuilder bits = new StringBuilder();

        void append(int value, int length) {
            for (int i = length - 1; i >= 0; i--) {
                bits.append(((value >> i) & 1) == 1 ? '1' : '0');
            }
        }

        void appendBit(boolean bit) {
            bits.append(bit ? '1' : '0');
        }

        int length() {
            return bits.length();
        }

        byte[] toByteArray() {
            byte[] result = new byte[bits.length() / 8];
            for (int i = 0; i < result.length; i++) {
                int val = 0;
                for (int j = 0; j < 8; j++) {
                    val = (val << 1) | (bits.charAt(i * 8 + j) == '1' ? 1 : 0);
                }
                result[i] = (byte) val;
            }
            return result;
        }
    }
}

