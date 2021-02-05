package com.example.thermalprinter;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Picture;
import android.graphics.Rect;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory.Options;
import android.net.Uri;
import android.util.Log;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public class BitmapUtils {
    private static final String TAG = "BitmapUtils";
    private static final boolean D = true;
    private static int[][] Floyd16x16 = new int[][]{{0, 128, 32, 160, 8, 136, 40, 168, 2, 130, 34, 162, 10, 138, 42, 170}, {192, 64, 224, 96, 200, 72, 232, 104, 194, 66, 226, 98, 202, 74, 234, 106}, {48, 176, 16, 144, 56, 184, 24, 152, 50, 178, 18, 146, 58, 186, 26, 154}, {240, 112, 208, 80, 248, 120, 216, 88, 242, 114, 210, 82, 250, 122, 218, 90}, {12, 140, 44, 172, 4, 132, 36, 164, 14, 142, 46, 174, 6, 134, 38, 166}, {204, 76, 236, 108, 196, 68, 228, 100, 206, 78, 238, 110, 198, 70, 230, 102}, {60, 188, 28, 156, 52, 180, 20, 148, 62, 190, 30, 158, 54, 182, 22, 150}, {252, 124, 220, 92, 244, 116, 212, 84, 254, 126, 222, 94, 246, 118, 214, 86}, {3, 131, 35, 163, 11, 139, 43, 171, 1, 129, 33, 161, 9, 137, 41, 169}, {195, 67, 227, 99, 203, 75, 235, 107, 193, 65, 225, 97, 201, 73, 233, 105}, {51, 179, 19, 147, 59, 187, 27, 155, 49, 177, 17, 145, 57, 185, 25, 153}, {243, 115, 211, 83, 251, 123, 219, 91, 241, 113, 209, 81, 249, 121, 217, 89}, {15, 143, 47, 175, 7, 135, 39, 167, 13, 141, 45, 173, 5, 133, 37, 165}, {207, 79, 239, 111, 199, 71, 231, 103, 205, 77, 237, 109, 197, 69, 229, 101}, {63, 191, 31, 159, 55, 183, 23, 151, 61, 189, 29, 157, 53, 181, 21, 149}, {254, 127, 223, 95, 247, 119, 215, 87, 253, 125, 221, 93, 245, 117, 213, 85}};

    private BitmapUtils() {
        throw new UnsupportedOperationException("cannot be instantiated");
    }

    public static Bitmap convertToBlackWhite(Bitmap bmp) {
        int width = bmp.getWidth();
        int height = bmp.getHeight();
        int[] pixels = new int[width * height];
        bmp.getPixels(pixels, 0, width, 0, 0, width, height);
        int alpha = -16777216;

        for (int i = 0; i < height; ++i) {
            for (int j = 0; j < width; ++j) {
                int grey = pixels[width * i + j];
                int red = (grey & 16711680) >> 16;
                int green = (grey & '\uff00') >> 8;
                int blue = grey & 255;
                grey = (int) ((double) red * 0.3D + (double) green * 0.59D + (double) blue * 0.11D);
                grey |= alpha | grey << 16 | grey << 8;
                pixels[width * i + j] = grey;
            }
        }

        Bitmap newBmp = Bitmap.createBitmap(width, height, Config.RGB_565);
        newBmp.setPixels(pixels, 0, width, 0, 0, width, height);
        return newBmp;
    }

    public static Bitmap convert2GreyImg(Bitmap img) {
        int width = img.getWidth();
        int height = img.getHeight();
        int[] pixels = new int[width * height];
        img.getPixels(pixels, 0, width, 0, 0, width, height);
        byte[] bytePixels = new byte[width * height];

        int alpha;
        for (alpha = 0; alpha < pixels.length; ++alpha) {
            bytePixels[alpha] = (byte) pixels[alpha];
        }

        alpha = -16777216;

        int i;
        for (i = 0; i < height; ++i) {
            for (int j = 0; j < width; ++j) {
                int grey = pixels[width * i + j];
                int red = (grey & 16711680) >> 16;
                int green = (grey & '\uff00') >> 8;
                int blue = grey & 255;
                grey = (int) ((double) ((float) red) * 0.3D + (double) ((float) green) * 0.59D + (double) ((float) blue) * 0.11D);
                grey |= alpha | grey << 16 | grey << 8;
                pixels[width * i + j] = ~grey;
            }
        }

        for (i = 0; i < pixels.length; ++i) {
            bytePixels[i] = (byte) pixels[i];
        }

        Bitmap result = Bitmap.createBitmap(width, height, Config.RGB_565);
        result.setPixels(pixels, 0, width, 0, 0, width, height);
        return result;
    }

    public static Bitmap bitmap2Gray(Bitmap bmSrc) {
        int height = bmSrc.getHeight();
        int width = bmSrc.getWidth();
        Bitmap bmpGray = null;
        bmpGray = Bitmap.createBitmap(width, height, Config.RGB_565);
        Canvas c = new Canvas(bmpGray);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0.0F);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmSrc, 0.0F, 0.0F, paint);
        return bmpGray;
    }

    public static byte[] convert(Bitmap bm) {
        int oldWidth = bm.getWidth();
        int height = bm.getHeight();
        int[] intPixels = new int[oldWidth * height];
        bm.getPixels(intPixels, 0, oldWidth, 0, 0, oldWidth, height);
        int newWidth = (oldWidth - 1) / 8 + 1;
        byte[] bytePixels = new byte[newWidth * height];

        for (int i = 0; i < height; ++i) {
            for (int j = 0; j < oldWidth; ++j) {
                int x = oldWidth * i + j;
                int y = newWidth * i + j / 8;
                int z = 7 - j % 8;
                if ((intPixels[x] & 255) < Floyd16x16[i & 15][j & 15]) {
                    bytePixels[y] = (byte) (bytePixels[y] | 1 << z);
                }
            }
        }

        return bytePixels;
    }

    public static byte[] convert2(Bitmap bm) {
        int oldWidth = bm.getWidth();
        int height = bm.getHeight();
        int[] intPixels = new int[oldWidth * height];
        bm.getPixels(intPixels, 0, oldWidth, 0, 0, oldWidth, height);
        int newWidth = (oldWidth - 1) / 8 + 1;
        byte[] bytePixels = new byte[newWidth * height];

        for (int i = 0; i < height; ++i) {
            for (int j = 0; j < oldWidth; ++j) {
                int x = oldWidth * i + j;
                int y = newWidth * i + j / 8;
                int z = 7 - j % 8;
                if ((intPixels[x] & 255) > Floyd16x16[i & 15][j & 15]) {
                    bytePixels[y] = (byte) (bytePixels[y] | 1 << z);
                }
            }
        }

        return bytePixels;
    }

    public static int calculateOutsideInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;

            for (int halfWidth = width / 2; halfHeight / inSampleSize > reqHeight && halfWidth / inSampleSize > reqWidth; inSampleSize *= 2) {
            }
        }

        return inSampleSize;
    }

    public static int calculateOutsideInSampleSize(Bitmap bm, int reqWidth, int reqHeight) {
        int height = bm.getHeight();
        int width = bm.getWidth();
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;

            for (int halfWidth = width / 2; halfHeight / inSampleSize > reqHeight && halfWidth / inSampleSize > reqWidth; inSampleSize *= 2) {
            }
        }

        return inSampleSize;
    }

    public static int calculateInsideInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        Log.d("BitmapUtil", "reqWidth = " + reqWidth);
        Log.d("BitmapUtil", "reqHeight = " + reqHeight);
        int height = options.outHeight;
        int width = options.outWidth;
        Log.d("BitmapUtil", "height = " + height);
        Log.d("BitmapUtil", "width = " + width);

        int inSampleSize;
        for (inSampleSize = 1; height / inSampleSize > reqHeight || width / inSampleSize > reqWidth; inSampleSize *= 2) {
        }

        return inSampleSize;
    }

    public static int calculateInsideInSampleSize(Bitmap bm, int reqWidth, int reqHeight) {
        int height = bm.getHeight();
        int width = bm.getWidth();

        int inSampleSize;
        for (inSampleSize = 1; height / inSampleSize > reqHeight || width / inSampleSize > reqWidth; inSampleSize *= 2) {
        }

        return inSampleSize;
    }

    public static Bitmap decodeSampledBitmapFromStream(InputStream is, int reqWidth, int reqHeight) {
        BufferedInputStream bis = new BufferedInputStream(is);
        bis.mark(0);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(bis, (Rect) null, options);
        options.inSampleSize = calculateInsideInSampleSize(options, reqWidth, reqHeight);
        Log.d("BitmapUtil", "options inSampleSize = " + options.inSampleSize);
        options.inJustDecodeBounds = false;

        try {
            bis.reset();
        } catch (IOException var6) {
            var6.printStackTrace();
        }

        return BitmapFactory.decodeStream(bis, (Rect) null, options);
    }

    public static Bitmap decodeSampledBitmapFromUri(Context context, Uri uri, int reqWidth, int reqHeight) {
        String filePath = getFilePath(context, uri);
        if (filePath == null) {
            return null;
        } else {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(filePath, options);
            options.inSampleSize = calculateInsideInSampleSize(options, reqWidth, reqHeight);
            Log.d("BitmapUtil", "options inSampleSize = " + options.inSampleSize);
            options.inJustDecodeBounds = false;
            return BitmapFactory.decodeFile(filePath, options);
        }
    }

    public static Bitmap decodeSampledBitmapFromBitmap(Bitmap bm, int reqWidth) {
        float scaleSize;
        if (bm.getWidth() > reqWidth) {
            scaleSize = (float) bm.getWidth() / (float) reqWidth;
        } else {
            scaleSize = 1.0F;
        }

        Matrix matrix = new Matrix();
        matrix.postScale(1.0F / scaleSize, 1.0F / scaleSize);
        return Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);
    }

    public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId, int reqWidth, int reqHeight) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);
        options.inSampleSize = calculateInsideInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }

    public static byte[] encodeBitmapToPixelsByteArray(Bitmap bm) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        int[] intPixels = new int[width * height];
        bm.getPixels(intPixels, 0, width, 0, 0, width, height);
        byte[] bytePixels = new byte[width * height];

        for (int i = 0; i < bytePixels.length; ++i) {
            bytePixels[i] = (byte) intPixels[i];
        }

        return bytePixels;
    }

    public static Bitmap big(Bitmap bitmap) {
        Matrix matrix = new Matrix();
        matrix.postScale(1.0F, 8.0F);
        Bitmap resizeBmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        return resizeBmp;
    }

    public static Bitmap small(Bitmap bitmap) {
        Matrix matrix = new Matrix();
        matrix.postScale(0.125F, 1.0F);
        Bitmap resizeBmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        return resizeBmp;
    }

    public static Bitmap scaleToRequiredWidth(Bitmap bitmap, int reqWidth) {
        Matrix matrix = new Matrix();
        matrix.postScale(1.0F * (float) reqWidth / (float) bitmap.getWidth(), 1.0F * (float) reqWidth / (float) bitmap.getWidth());
        Bitmap resizeBmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        return resizeBmp;
    }

    public static Bitmap createBitmapFromPicture(Picture picture) {
        int width = picture.getWidth();
        int height = picture.getHeight();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        picture.draw(canvas);
        return bitmap;
    }

    private static String getFilePath(Context context, Uri uri) {
        String filePath = null;
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            Cursor cursor = context.getContentResolver().query(uri, new String[]{"_data"}, (String) null, (String[]) null, (String) null);
            if (cursor == null) {
                return null;
            }

            try {
                if (cursor.moveToNext()) {
                    filePath = cursor.getString(cursor.getColumnIndex("_data"));
                }
            } finally {
                cursor.close();
            }
        }

        if ("file".equalsIgnoreCase(uri.getScheme())) {
            filePath = uri.getPath();
        }

        return filePath;
    }
}
