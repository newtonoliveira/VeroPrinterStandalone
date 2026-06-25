package com.veroprinterstandalone;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class PrintBitmapRenderer {

    private static final String TAG = "VeroStandalone";
    private static final int DEFAULT_COLUMNS = 32;
    private static final int RECEIPT_WIDTH = 384;
    private static final int HORIZONTAL_PADDING = 8;
    private static final int TOP_PADDING = 16;
    private static final int FOOTER_SPACE = 48;
    private static final float DEFAULT_TEXT_SIZE = 18f;
    private static final int DOTS_PER_MM = 8;

    private static final class RenderOptions {
        int columns;
        int contentWidth;
        float baseTextSize;
        int baseLineHeight;
        int feedUnit;
    }

    private static final class PrintBlock {
        private static final int TYPE_TEXT = 1;
        private static final int TYPE_LINE = 2;
        private static final int TYPE_FEED = 3;
        private static final int TYPE_IMAGE = 4;

        int type;
        String text;
        String align;
        boolean bold;
        int fontSize;
        int lines;
        String lineChar;
        Bitmap bitmap;
        int imageWidth;
        int imageHeight;
        boolean qrCode;
    }

    private PrintBitmapRenderer() {
    }

    static Bitmap buildProtocolBitmap(String protocolJson) throws Exception {
        Log.d(TAG, "[PRINT-BITMAP] buildProtocolBitmap: jsonLen=" + (protocolJson == null ? 0 : protocolJson.length()));
        JSONObject root = new JSONObject(protocolJson);
        JSONObject document = root.optJSONObject("document");
        if (document == null) {
            document = root;
        }

        JSONArray items = document.optJSONArray("items");
        if (items == null || items.length() == 0) {
            throw new IllegalArgumentException("items ausente no protocolo");
        }

        RenderOptions options = createRenderOptions(document.optInt("width", DEFAULT_COLUMNS));
        List<PrintBlock> blocks = parseProtocolItems(items);
        if (blocks.isEmpty()) {
            throw new IllegalArgumentException("nenhum item valido no protocolo");
        }

        Log.d(TAG, "[PRINT-BITMAP] buildProtocolBitmap: items=" + items.length() + " blocks=" + blocks.size()
                + " columns=" + options.columns + " textSize=" + options.baseTextSize);
        return renderBlocksToBitmap(blocks, options);
    }

    static Bitmap buildTextBitmap(String text) {
        RenderOptions options = createRenderOptions(DEFAULT_COLUMNS);
        Paint paint = createReceiptPaint(options);
        int maxTextWidth = options.contentWidth;
        List<String> lines = wrapReceiptLines(text, paint, maxTextWidth);
        int lineHeight = options.baseLineHeight;
        int baseline = TOP_PADDING + Math.round(-paint.ascent());
        int imageHeight = Math.max(
                baseline + FOOTER_SPACE,
                TOP_PADDING + (lines.size() * lineHeight) + FOOTER_SPACE
        );

        Bitmap bitmap = Bitmap.createBitmap(RECEIPT_WIDTH, imageHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);

        int currentY = baseline;
        for (String line : lines) {
            canvas.drawText(line, HORIZONTAL_PADDING, currentY, paint);
            currentY += lineHeight;
        }

        return bitmap;
    }

    private static Bitmap renderBlocksToBitmap(List<PrintBlock> blocks, RenderOptions options) {
        int imageHeight = measureBlocksHeight(blocks, options);
        Log.d(TAG, "[PRINT-BITMAP] renderBlocksToBitmap: blocks=" + blocks.size() + " imageHeight=" + imageHeight
                + " columns=" + options.columns);
        Bitmap bitmap = Bitmap.createBitmap(RECEIPT_WIDTH, imageHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);

        int currentY = TOP_PADDING;
        for (PrintBlock block : blocks) {
            currentY = drawBlock(canvas, block, currentY, options);
        }

        return bitmap;
    }

    private static Paint createReceiptPaint(RenderOptions options) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.BLACK);
        paint.setTextSize(options.baseTextSize);
        paint.setTypeface(Typeface.MONOSPACE);
        return paint;
    }

    private static Paint createProtocolPaint(PrintBlock block, RenderOptions options) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.BLACK);
        paint.setTypeface(block.bold ? Typeface.create(Typeface.MONOSPACE, Typeface.BOLD) : Typeface.MONOSPACE);
        paint.setTextSize(resolveFontSize(block.fontSize, options.baseTextSize));
        return paint;
    }

    private static float resolveFontSize(int fontSize, float baseTextSize) {
        if (fontSize <= 1) {
            return baseTextSize;
        }
        if (fontSize == 2) {
            return baseTextSize * 1.18f;
        }
        if (fontSize == 3) {
            return baseTextSize * 1.35f;
        }
        return baseTextSize * 1.55f;
    }

    private static List<PrintBlock> parseProtocolItems(JSONArray items) {
        List<PrintBlock> blocks = new ArrayList<>();
        for (int iIndex = 0; iIndex < items.length(); iIndex++) {
            JSONObject item = items.optJSONObject(iIndex);
            if (item == null) {
                continue;
            }

            String type = normalizeItemType(item.optString("type", ""));
            if ("text".equals(type)) {
                PrintBlock block = new PrintBlock();
                block.type = PrintBlock.TYPE_TEXT;
                block.text = normalizeLineBreaks(resolveItemText(item));
                block.align = normalizeAlign(item.optString("align", "left"));
                block.bold = item.optBoolean("bold", false);
                block.fontSize = Math.max(1, item.optInt("font_size", 1));
                blocks.add(block);
            } else if ("line".equals(type)) {
                PrintBlock block = new PrintBlock();
                block.type = PrintBlock.TYPE_LINE;
                block.lineChar = resolveLineChar(item.optString("char", "-"));
                blocks.add(block);
            } else if ("feed".equals(type)) {
                PrintBlock block = new PrintBlock();
                block.type = PrintBlock.TYPE_FEED;
                block.lines = Math.max(1, item.optInt("lines", 1));
                blocks.add(block);
            } else if ("image".equals(type) || "qrcode".equals(type)) {
                Bitmap bitmap = decodeBase64Bitmap(item.optString("base64", ""));
                if (bitmap == null) {
                    Log.d(TAG, "[PRINT-BITMAP] parseProtocolItems: bitmap base64 invalido para type=" + type);
                    continue;
                }

                PrintBlock block = new PrintBlock();
                block.type = PrintBlock.TYPE_IMAGE;
                block.align = normalizeAlign(item.optString("align", "center"));
                block.qrCode = "qrcode".equals(type);
                block.bitmap = block.qrCode ? toMonochromeBitmap(bitmap) : bitmap;
                block.imageWidth = resolveImageWidth(item, bitmap);
                block.imageHeight = resolveImageHeight(item, bitmap, block.imageWidth);
                blocks.add(block);
            }
        }
        return blocks;
    }

    private static int measureBlocksHeight(List<PrintBlock> blocks, RenderOptions options) {
        int currentY = TOP_PADDING;
        for (PrintBlock block : blocks) {
            currentY = advanceBlockHeight(block, currentY, options);
        }
        return currentY + FOOTER_SPACE;
    }

    private static int drawBlock(Canvas canvas, PrintBlock block, int currentY, RenderOptions options) {
        if (block.type == PrintBlock.TYPE_FEED) {
            return currentY + (block.lines * options.feedUnit);
        }

        if (block.type == PrintBlock.TYPE_LINE) {
            Paint paint = createProtocolPaint(block, options);
            int lineY = currentY + 12;
            canvas.drawLine(HORIZONTAL_PADDING, lineY, RECEIPT_WIDTH - HORIZONTAL_PADDING, lineY, paint);
            return currentY + 24;
        }

        if (block.type == PrintBlock.TYPE_IMAGE) {
            float x = HORIZONTAL_PADDING;
            if ("center".equals(block.align)) {
                x = (RECEIPT_WIDTH - block.imageWidth) / 2f;
            } else if ("right".equals(block.align)) {
                x = RECEIPT_WIDTH - HORIZONTAL_PADDING - block.imageWidth;
            }

            Bitmap scaledBitmap = Bitmap.createScaledBitmap(block.bitmap, block.imageWidth, block.imageHeight, !block.qrCode);
            canvas.drawBitmap(scaledBitmap, x, currentY, null);
            if (scaledBitmap != block.bitmap) {
                scaledBitmap.recycle();
            }
            return currentY + block.imageHeight + 12;
        }

        Paint paint = createProtocolPaint(block, options);
        List<String> lines = wrapReceiptLines(block.text, paint, options.contentWidth);
        int lineHeight = Math.max(options.baseLineHeight, Math.round(paint.getFontSpacing()));
        int baselineOffset = Math.round(-paint.ascent());
        int nextY = currentY;

        for (String line : lines) {
            float textWidth = paint.measureText(line);
            float x = HORIZONTAL_PADDING;
            if ("center".equals(block.align)) {
                x = (RECEIPT_WIDTH - textWidth) / 2f;
            } else if ("right".equals(block.align)) {
                x = RECEIPT_WIDTH - HORIZONTAL_PADDING - textWidth;
            }

            canvas.drawText(line, x, nextY + baselineOffset, paint);
            nextY += lineHeight;
        }

        return nextY;
    }

    private static int advanceBlockHeight(PrintBlock block, int currentY, RenderOptions options) {
        if (block.type == PrintBlock.TYPE_FEED) {
            return currentY + (block.lines * options.feedUnit);
        }

        if (block.type == PrintBlock.TYPE_LINE) {
            return currentY + 24;
        }

        if (block.type == PrintBlock.TYPE_IMAGE) {
            return currentY + block.imageHeight + 12;
        }

        Paint paint = createProtocolPaint(block, options);
        List<String> lines = wrapReceiptLines(block.text, paint, options.contentWidth);
        int lineHeight = Math.max(options.baseLineHeight, Math.round(paint.getFontSpacing()));
        return currentY + (lines.size() * lineHeight);
    }

    private static RenderOptions createRenderOptions(int columns) {
        RenderOptions options = new RenderOptions();
        options.columns = columns <= 0 ? DEFAULT_COLUMNS : columns;
        options.contentWidth = RECEIPT_WIDTH - (HORIZONTAL_PADDING * 2);
        options.baseTextSize = resolveBaseTextSize(options.columns, options.contentWidth);
        options.baseLineHeight = Math.max(18, Math.round(options.baseTextSize * 1.35f));
        options.feedUnit = Math.max(16, options.baseLineHeight);
        return options;
    }

    private static float resolveBaseTextSize(int columns, int contentWidth) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTypeface(Typeface.MONOSPACE);
        StringBuilder probeBuilder = new StringBuilder();
        for (int iIndex = 0; iIndex < columns; iIndex++) {
            probeBuilder.append('0');
        }
        String probe = probeBuilder.toString();

        for (float size = DEFAULT_TEXT_SIZE; size >= 8f; size -= 0.5f) {
            paint.setTextSize(size);
            if (paint.measureText(probe) <= contentWidth) {
                return size;
            }
        }
        return 8f;
    }

    private static List<String> wrapReceiptLines(String text, Paint paint, int maxWidth) {
        List<String> result = new ArrayList<>();
        if (text == null) {
            result.add("");
            return result;
        }

        String[] rawLines = text.split("\n", -1);
        for (String rawLine : rawLines) {
            if (rawLine.isEmpty()) {
                result.add("");
                continue;
            }

            String remaining = rawLine;
            while (!remaining.isEmpty()) {
                String fittedLine = fitLine(remaining, paint, maxWidth);
                result.add(fittedLine);
                remaining = remaining.substring(fittedLine.length()).trim();
            }
        }

        if (result.isEmpty()) {
            result.add("");
        }

        return result;
    }

    private static String fitLine(String text, Paint paint, int maxWidth) {
        if (paint.measureText(text) <= maxWidth) {
            return text;
        }

        int breakIndex = text.length();
        while (breakIndex > 1 && paint.measureText(text, 0, breakIndex) > maxWidth) {
            breakIndex--;
        }

        int preferredBreak = text.lastIndexOf(' ', breakIndex - 1);
        if (preferredBreak > 0) {
            return text.substring(0, preferredBreak).trim();
        }

        return text.substring(0, Math.max(1, breakIndex));
    }

    private static String normalizeItemType(String itemType) {
        String normalized = itemType == null ? "" : itemType.trim().toLowerCase(Locale.US);
        if ("texto".equals(normalized)) {
            return "text";
        }
        if ("linha".equals(normalized)) {
            return "line";
        }
        if ("salto".equals(normalized)) {
            return "feed";
        }
        if ("imagem".equals(normalized)) {
            return "image";
        }
        if ("qr".equals(normalized)) {
            return "qrcode";
        }
        return normalized;
    }

    private static String normalizeAlign(String align) {
        String normalized = align == null ? "" : align.trim().toLowerCase(Locale.US);
        if ("centro".equals(normalized)) {
            return "center";
        }
        if ("direita".equals(normalized)) {
            return "right";
        }
        if ("esquerda".equals(normalized)) {
            return "left";
        }
        if (!"center".equals(normalized) && !"right".equals(normalized)) {
            return "left";
        }
        return normalized;
    }

    private static String resolveItemText(JSONObject item) {
        String text = item.optString("text", "");
        if (text.isEmpty()) {
            text = item.optString("value", "");
        }
        return text;
    }

    private static String resolveLineChar(String lineChar) {
        if (lineChar == null || lineChar.trim().isEmpty()) {
            return "-";
        }
        return String.valueOf(lineChar.charAt(0));
    }

    private static Bitmap decodeBase64Bitmap(String base64Value) {
        if (base64Value == null || base64Value.trim().isEmpty()) {
            return null;
        }

        try {
            String normalizedBase64 = base64Value.trim();
            int base64Index = normalizedBase64.indexOf("base64,");
            if (base64Index >= 0) {
                normalizedBase64 = normalizedBase64.substring(base64Index + 7);
            }
            byte[] bytes = Base64.decode(normalizedBase64, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Throwable throwable) {
            Log.d(TAG, "[PRINT-BITMAP] decodeBase64Bitmap-exception: " + Log.getStackTraceString(throwable));
            return null;
        }
    }

    private static Bitmap toMonochromeBitmap(Bitmap sourceBitmap) {
        if (sourceBitmap == null) {
            return null;
        }

        Bitmap bitmap = sourceBitmap.copy(Bitmap.Config.ARGB_8888, false);
        if (bitmap == null) {
            return sourceBitmap;
        }

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int iIndex = 0; iIndex < pixels.length; iIndex++) {
            int pixel = pixels[iIndex];
            int alpha = Color.alpha(pixel);
            if (alpha < 32) {
                pixels[iIndex] = Color.WHITE;
                continue;
            }

            int gray = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3;
            pixels[iIndex] = gray < 180 ? Color.BLACK : Color.WHITE;
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }

    private static int resolveImageWidth(JSONObject item, Bitmap bitmap) {
        int sizeMm = Math.max(0, item.optInt("size_mm", 0));
        if (sizeMm > 0) {
            return Math.min(RECEIPT_WIDTH - (HORIZONTAL_PADDING * 2), sizeMm * DOTS_PER_MM);
        }

        int width = Math.max(0, item.optInt("width", 0));
        if (width > 0) {
            return Math.min(RECEIPT_WIDTH - (HORIZONTAL_PADDING * 2), width);
        }

        return Math.min(RECEIPT_WIDTH - (HORIZONTAL_PADDING * 2), bitmap.getWidth());
    }

    private static int resolveImageHeight(JSONObject item, Bitmap bitmap, int imageWidth) {
        int sizeMm = Math.max(0, item.optInt("size_mm", 0));
        if (sizeMm > 0) {
            return sizeMm * DOTS_PER_MM;
        }

        int height = Math.max(0, item.optInt("height", 0));
        if (height > 0) {
            return height;
        }

        if (bitmap.getWidth() <= 0) {
            return bitmap.getHeight();
        }

        return Math.max(1, Math.round((bitmap.getHeight() * imageWidth) / (float) bitmap.getWidth()));
    }

    private static String normalizeLineBreaks(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\r\n", "\n").replace('\r', '\n');
    }
}
