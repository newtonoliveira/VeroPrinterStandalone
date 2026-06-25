package com.veroprinterstandalone;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.RemoteException;
import android.util.Log;

import java.util.Locale;

import br.com.execucao.posmp_api.printer.Printer;
import br.com.execucao.posmp_api.printer.PrinterListener;

public final class VeroPrinterRouter {

    private static final String TAG = "VeroStandalone";
    private static final String[] SUPPORTED_MODELS = new String[]{
            "GPOS700", "P2-B", "L3", "L300NEW", "N950", "N950K", "N950S", "X990 PRO"
    };

    private VeroPrinterRouter() {
    }

    public static void printText(final Activity activity, final String text, final boolean feedAuto) {
        printTextAsBitmap(activity, text, feedAuto);
    }

    public static void printProtocol(final Activity activity, final String protocolJson, final boolean feedAuto) {
        if (activity == null) {
            Log.d(TAG, "[VERO-PRINT] router.printProtocol: activity=null");
            return;
        }

        final String normalizedModel = normalizeModel(Build.MODEL);
        Log.d(TAG, "[VERO-PRINT] router.printProtocol: model=" + normalizedModel + " len=" + (protocolJson == null ? 0 : protocolJson.length()));

        if (!isSupportedModel(normalizedModel)) {
            Log.d(TAG, "[VERO-PRINT] router.printProtocol: modelo nao suportado");
            return;
        }

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Printer printer = null;
                try {
                    Bitmap bitmap = PrintBitmapRenderer.buildProtocolBitmap(protocolJson);
                    Log.d(TAG, "[VERO-PRINT] router.printProtocol: bitmap width=" + bitmap.getWidth() + " height=" + bitmap.getHeight());

                    printer = Printer.getInstance(activity);
                    if (printer == null) {
                        Log.d(TAG, "[VERO-PRINT] router.printProtocol: Printer.getInstance retornou null");
                        return;
                    }

                    printer.open();
                    printer.setFeedLineAuto(feedAuto);
                    final Printer finalPrinter = printer;

                    printer.print(bitmap, new PrinterListener.Stub() {
                        @Override
                        public void onFinish() throws RemoteException {
                            Log.d(TAG, "[VERO-PRINT] router.printProtocol: onFinish");
                            closePrinterQuietly(finalPrinter, "router.protocol.onFinish");
                        }

                        @Override
                        public void onError(int errorCode) throws RemoteException {
                            Log.d(TAG, "[VERO-PRINT] router.printProtocol: onError code=" + errorCode);
                            closePrinterQuietly(finalPrinter, "router.protocol.onError");
                        }
                    });

                    printer = null;
                    Log.d(TAG, "[VERO-PRINT] router.printProtocol: dispatch ok");
                } catch (Throwable throwable) {
                    Log.d(TAG, "[VERO-PRINT] router.printProtocol-exception: " + Log.getStackTraceString(throwable));
                    closePrinterQuietly(printer, "router.protocol.catch");
                }
            }
        });
    }

    public static void printTextAsBitmap(final Activity activity, final String text, final boolean feedAuto) {
        if (activity == null) {
            Log.d(TAG, "[VERO-PRINT] router.printTextAsBitmap: activity=null");
            return;
        }

        final String normalizedModel = normalizeModel(Build.MODEL);
        final String normalizedText = normalizeLineBreaks(text).trim();
        Log.d(TAG, "[VERO-PRINT] router.printTextAsBitmap: model=" + normalizedModel + " len=" + normalizedText.length());

        if (!isSupportedModel(normalizedModel)) {
            Log.d(TAG, "[VERO-PRINT] router.printTextAsBitmap: modelo nao suportado");
            return;
        }

        if (normalizedText.isEmpty()) {
            Log.d(TAG, "[VERO-PRINT] router.printTextAsBitmap: texto vazio");
            return;
        }

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Printer printer = null;
                try {
                    Bitmap bitmap = PrintBitmapRenderer.buildTextBitmap(normalizedText);
                    Log.d(TAG, "[VERO-PRINT] router.printTextAsBitmap: bitmap width=" + bitmap.getWidth() + " height=" + bitmap.getHeight());

                    printer = Printer.getInstance(activity);
                    if (printer == null) {
                        Log.d(TAG, "[VERO-PRINT] router.printTextAsBitmap: Printer.getInstance retornou null");
                        return;
                    }

                    printer.open();
                    printer.setFeedLineAuto(feedAuto);
                    final Printer finalPrinter = printer;

                    printer.print(bitmap, new PrinterListener.Stub() {
                        @Override
                        public void onFinish() throws RemoteException {
                            Log.d(TAG, "[VERO-PRINT] router.printTextAsBitmap: onFinish");
                            closePrinterQuietly(finalPrinter, "router.onFinish");
                        }

                        @Override
                        public void onError(int errorCode) throws RemoteException {
                            Log.d(TAG, "[VERO-PRINT] router.printTextAsBitmap: onError code=" + errorCode);
                            closePrinterQuietly(finalPrinter, "router.onError");
                        }
                    });

                    printer = null;
                    Log.d(TAG, "[VERO-PRINT] router.printTextAsBitmap: dispatch ok");
                } catch (Throwable throwable) {
                    Log.d(TAG, "[VERO-PRINT] router.printTextAsBitmap-exception: " + Log.getStackTraceString(throwable));
                    closePrinterQuietly(printer, "router.catch");
                }
            }
        });
    }

    private static boolean isSupportedModel(String model) {
        if (model == null) {
            return false;
        }

        for (String item : SUPPORTED_MODELS) {
            if (model.equals(item)) {
                return true;
            }
        }
        return false;
    }

    private static void closePrinterQuietly(Printer printer, String origin) {
        if (printer == null) {
            return;
        }

        try {
            printer.close();
            Log.d(TAG, "[VERO-PRINT] " + origin + ": printerClose=ok");
        } catch (Throwable ignored) {
        }
    }

    private static String normalizeModel(String model) {
        if (model == null) {
            return "";
        }
        return model.trim().toUpperCase(Locale.US);
    }

    private static String normalizeLineBreaks(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\r\n", "\n").replace('\r', '\n');
    }
}
