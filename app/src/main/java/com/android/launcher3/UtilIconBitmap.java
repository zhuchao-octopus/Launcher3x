/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.util.DisplayMetrics;
import android.util.Log;

import com.common.utils.MachineConfig;

import java.util.Random;

/**
 * Various utilities shared amongst the Launcher's classes.
 */
final public class UtilIconBitmap {
    @SuppressWarnings("unused")
    private static final String TAG = "Launcher.Utilities";

    private static int sIconWidth = -1;
    private static int sIconHeight = -1;
    private static int sWrappedIconWidth = 80;
    private static int sWrappedIconHeight = 80;
    public static int sIconTextureWidth = -1;
    public static int sIconTextureHeight = -1;

    private static final Paint sBlurPaint = new Paint();
    private static final Paint sGlowColorPressedPaint = new Paint();
    private static final Paint sGlowColorFocusedPaint = new Paint();
    private static final Paint sDisabledPaint = new Paint();
    private static final Rect sOldBounds = new Rect();
    private static final Canvas sCanvas = new Canvas();
    private static int app_bgs[] = {
            R.drawable.app_bg_0, R.drawable.app_bg_1, R.drawable.app_bg_2, R.drawable.app_bg_3
    };

    private static Drawable[] app_bg_drawable = null;

    static {
        sCanvas.setDrawFilter(new PaintFlagsDrawFilter(Paint.DITHER_FLAG, Paint.FILTER_BITMAP_FLAG));
    }

    static int sColors[] = {0xffff0000, 0xff00ff00, 0xff0000ff};
    static int sColorIndex = 0;

    /**
     * Returns a bitmap suitable for the all apps view. Used to convert pre-ICS
     * icon bitmaps that are stored in the database (which were 74x74 pixels at
     * hdpi size) to the proper size (48dp)
     */
    static Bitmap createIconBitmap(Bitmap icon, Context context) {
        int textureWidth = sIconTextureWidth;
        int textureHeight = sIconTextureHeight;
        int sourceWidth = 200;// icon.getWidth();
        int sourceHeight = icon.getHeight();

        if (sourceWidth > textureWidth && sourceHeight > textureHeight) {
            // Icon is bigger than it should be; clip it (solves the GB->ICS
            // migration case)
            return Bitmap.createBitmap(icon, (sourceWidth - textureWidth) / 2, (sourceHeight - textureHeight) / 2, textureWidth, textureHeight);
        } else if (sourceWidth == textureWidth && sourceHeight == textureHeight) {
            // Icon is the right size, no need to change it
            return icon;
        } else {
            // Icon is too small, render to a larger bitmap
            final Resources resources = context.getResources();
            return createIconBitmap(new BitmapDrawable(resources, icon), context);
        }
    }

    public static Bitmap createIconBitmap(Drawable icon, Context context) {
        return createIconBitmap(icon, context, false);
    }

    public static Bitmap convertToBlackWhite(Bitmap bmp) {
        int width = bmp.getWidth(); // 获取位图的宽
        int height = bmp.getHeight(); // 获取位图的高
        int[] pixels = new int[width * height]; // 通过位图的大小创建像素点数组
        bmp.getPixels(pixels, 0, width, 0, 0, width, height);
        int alpha = 0x0 << 24;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {

                if (pixels[width * i + j] != 0) {
                    int grey = pixels[width * i + j];
                    int red = ((grey & 0x00FF0000) >> 16);
                    int green = ((grey & 0x0000FF00) >> 8);
                    int blue = (grey & 0x000000FF);
                    alpha = (grey & 0xff000000);

                    grey = (int) (red * 0.3 + green * 0.59 + blue * 0.11);
                    grey = alpha | (grey << 16) | (grey << 8) | grey;

                    //					 Log.d("aa", i + "???:" + j + ":" + String.format("0x%08x", pixels[width * i + j])
                    //					 + ":" + String.format("0x%08x", grey));

                    pixels[width * i + j] = grey;

                }
            }
        }

        Bitmap newBmp = Bitmap.createBitmap(pixels, width, height, Config.ARGB_8888);
        return newBmp;
    }

    public static Bitmap convertToNoneAndWhite(Bitmap bmp) {
        int width = bmp.getWidth(); // 获取位图的宽
        int height = bmp.getHeight(); // 获取位图的高
        int[] pixels = new int[width * height]; // 通过位图的大小创建像素点数组
        bmp.getPixels(pixels, 0, width, 0, 0, width, height);
        int alpha = 0x0 << 24;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {

                if (pixels[width * i + j] != 0) {
                    int grey = pixels[width * i + j];
                    int red = ((grey & 0x00FF0000) >> 16);
                    int green = ((grey & 0x0000FF00) >> 8);
                    int blue = (grey & 0x000000FF);
                    alpha = (grey & 0xff000000);

                    grey = (int) (red * 0.3 + green * 0.59 + blue * 0.11);
                    grey = (grey << 16) | (grey << 8) | grey;

                    if (grey >= 0x999999) {
                        grey = alpha | 0xffffff;
                        //						alpha = 0xff;
                    } else {
                        grey = alpha | grey;
                    }

                    //					 Log.d("aa", i + "???:" + j + ":" + String.format("0x%08x", pixels[width * i + j])
                    //					 + ":" + String.format("0x%08x", grey));

                    pixels[width * i + j] = grey;

                }
            }
        }

        Bitmap newBmp = Bitmap.createBitmap(pixels, width, height, Config.ARGB_8888);
        return newBmp;
    }

    /**
     * Returns a bitmap suitable for the all apps view.
     */
    public static Bitmap createIconBitmap(Drawable icon, Context context, boolean needBg) {
        // needBg = false;
        synchronized (sCanvas) { // we share the statics :-(
            if (sIconWidth == -1) {
                initStatics(context);
            }

            int width = sIconWidth;
            int height = sIconHeight;
            //			Log.d(TAG, "createIconBitmap sIconWidth [" + width + "," + height + "] " + needBg);
            if (icon instanceof PaintDrawable) {
                PaintDrawable painter = (PaintDrawable) icon;
                painter.setIntrinsicWidth(width);
                painter.setIntrinsicHeight(height);
            } else if (icon instanceof BitmapDrawable) {
                // Ensure the bitmap has a density.
                BitmapDrawable bitmapDrawable = (BitmapDrawable) icon;
                Bitmap bitmap = bitmapDrawable.getBitmap();

                if (Launcher.mIconType == 1 && needBg) {
                    bitmap = convertToBlackWhite(bitmap);
                    icon = new BitmapDrawable(bitmap);
                }
                if (bitmap.getDensity() == Bitmap.DENSITY_NONE) {
                    bitmapDrawable.setTargetDensity(context.getResources().getDisplayMetrics());
                }
            }
            int sourceWidth = icon.getIntrinsicWidth();
            int sourceHeight = icon.getIntrinsicHeight();
            //			Log.d(TAG, "createIconBitmap sourceWidth [" + sourceWidth + "," + sourceWidth + "]");
            //			Log.d("aad", "1111:"+sourceWidth+":"+sourceHeight);
            if (sourceWidth > 0 && sourceHeight > 0) {
                // There are intrinsic sizes.
                if (needBg) {
                    width = sWrappedIconWidth;
                    height = sWrappedIconHeight;
                    //					Log.d(TAG, "createIconBitmap sWrappedIconWidth [" + sourceWidth + "," + sourceWidth + "]");
                    if (width != sourceWidth && height != sourceHeight) {
                        // It's too big, scale it down.
                        final float ratio = (float) sourceWidth / sourceHeight;
                        if (sourceWidth > sourceHeight) {
                            height = (int) (width / ratio);
                        } else if (sourceHeight > sourceWidth) {
                            width = (int) (height * ratio);
                        }
                    }
                    //					Log.d("aad", "1111222:"+width+":"+height);
                } else {

                    if (width < sourceWidth || height < sourceHeight) {
                        // It's too big, scale it down.
                        final float ratio = (float) sourceWidth / sourceHeight;
                        if (sourceWidth > sourceHeight) {
                            height = (int) (width / ratio);
                        } else if (sourceHeight > sourceWidth) {
                            width = (int) (height * ratio);
                        }
                    } else if (sourceWidth < width && sourceHeight < height) {
                        // Don't scale up the icon
                        //allen . scale up for 1280x720
                        //						width = sourceWidth;
                        //						height = sourceHeight;
                    }
                }
            }
            //			Log.d(TAG, "createIconBitmap width [" + width + "," + width + "]");

            // no intrinsic size --> use default size
            int textureWidth = sIconTextureWidth;
            int textureHeight = sIconTextureHeight;
            //			Log.d(TAG, "createIconBitmap sIconTextureWidth [" + textureWidth + "," + textureHeight + "]");
            if (MachineConfig.VALUE_SYSTEM_UI43_3300.equals(ResourceUtil.mSystemUI)) {
                //from values dimens app_icon_bg_width and app_icon_bg_height. ww
                textureWidth = ResourceUtil.mGeneralAppIconBgWidth;
                textureHeight = ResourceUtil.mGeneralAppIconBgHeight;
            }
            Bitmap bitmap = Bitmap.createBitmap(textureWidth, textureHeight, Bitmap.Config.ARGB_8888);
            final Canvas canvas = sCanvas;
            canvas.setBitmap(bitmap);
            if (needBg) {
                Drawable d;
                int i = doGetMagicNumber(icon) % app_bgs.length;
                if (app_bg_drawable != null && app_bg_drawable.length >= app_bgs.length) {
                    d = app_bg_drawable[i];
                } else {
                    d = context.getResources().getDrawable(app_bgs[i]);
                }

                sOldBounds.set(d.getBounds());
                d.setBounds(0, 0, textureWidth, textureHeight);
                d.draw(canvas);
                d.setBounds(sOldBounds);


            }

            int left = (textureWidth - width) / 2;
            int top = (textureHeight - height) / 2;
            if (MachineConfig.VALUE_SYSTEM_UI43_3300.equals(ResourceUtil.mSystemUI)) {
                //from values dimens app_icon_offset_left and app_icon_offset_top. ww
                left = ResourceUtil.mGeneralAppIconOffsetLeft;
                top = ResourceUtil.mGeneralAppIconOffsetTop;
            }
            //			Log.d(TAG, "createIconBitmap left [" + left + "," + left + "]");
            @SuppressWarnings("all")
            // suppress dead code warning
            final boolean debug = false;
            if (debug) {
                // draw a big box for the icon for debugging
                canvas.drawColor(sColors[sColorIndex]);
                if (++sColorIndex >= sColors.length) sColorIndex = 0;
                Paint debugPaint = new Paint();
                debugPaint.setColor(0xffcccc00);
                canvas.drawRect(left, top, left + width, top + height, debugPaint);
            }

            sOldBounds.set(icon.getBounds());
            icon.setBounds(left, top, left + width, top + height);
            icon.draw(canvas);
            icon.setBounds(sOldBounds);
            canvas.setBitmap(null);


            return bitmap;
        }
    }

    public static Bitmap createIconBitmap(Drawable icon, Context context, int bg_id) {
        // needBg = false;
        synchronized (sCanvas) { // we share the statics :-(
            if (sIconWidth == -1) {
                initStatics(context);
            }

            int width = sIconWidth;
            int height = sIconHeight;

            if (icon instanceof PaintDrawable) {
                PaintDrawable painter = (PaintDrawable) icon;
                painter.setIntrinsicWidth(width);
                painter.setIntrinsicHeight(height);
            } else if (icon instanceof BitmapDrawable) {
                // Ensure the bitmap has a density.
                BitmapDrawable bitmapDrawable = (BitmapDrawable) icon;
                Bitmap bitmap = bitmapDrawable.getBitmap();
                if (bitmap.getDensity() == Bitmap.DENSITY_NONE) {
                    bitmapDrawable.setTargetDensity(context.getResources().getDisplayMetrics());
                }
            }
            int sourceWidth = icon.getIntrinsicWidth();
            int sourceHeight = icon.getIntrinsicHeight();
            if (sourceWidth > 0 && sourceHeight > 0) {
                // There are intrinsic sizes.

                width = sWrappedIconWidth;
                height = sWrappedIconHeight;

                if (width != sourceWidth && height != sourceHeight) {
                    // It's too big, scale it down.
                    final float ratio = (float) sourceWidth / sourceHeight;
                    if (sourceWidth > sourceHeight) {
                        height = (int) (width / ratio);
                    } else if (sourceHeight > sourceWidth) {
                        width = (int) (height * ratio);
                    }
                }

            }

            // no intrinsic size --> use default size
            int textureWidth = sIconTextureWidth;
            int textureHeight = sIconTextureHeight;

            Bitmap bitmap = Bitmap.createBitmap(textureWidth, textureHeight, Bitmap.Config.ARGB_8888);
            final Canvas canvas = sCanvas;
            canvas.setBitmap(bitmap);
            if (bg_id != 0) {
                int i = doGetMagicNumber(icon) % app_bgs.length;

                Drawable d = context.getResources().getDrawable(bg_id);
                sOldBounds.set(d.getBounds());
                d.setBounds(0, 0, textureWidth, textureHeight);
                d.draw(canvas);
                d.setBounds(sOldBounds);
            }

            final int left = (textureWidth - width) / 2;
            final int top = (textureHeight - height) / 2;

            @SuppressWarnings("all")
            // suppress dead code warning
            final boolean debug = false;
            if (debug) {
                // draw a big box for the icon for debugging
                canvas.drawColor(sColors[sColorIndex]);
                if (++sColorIndex >= sColors.length) sColorIndex = 0;
                Paint debugPaint = new Paint();
                debugPaint.setColor(0xffcccc00);
                canvas.drawRect(left, top, left + width, top + height, debugPaint);
            }

            sOldBounds.set(icon.getBounds());
            icon.setBounds(left, top, left + width, top + height);
            icon.draw(canvas);
            icon.setBounds(sOldBounds);
            canvas.setBitmap(null);

            // bitmap = convertToBlackWhite(bitmap);

            return bitmap;
        }
    }

    public static Bitmap createIconBitmap(Context context, Drawable icon) {
        synchronized (sCanvas) {
            Bitmap bitmap = Bitmap.createBitmap(ResourceUtil.mGeneralAppIconBgWidth, ResourceUtil.mGeneralAppIconBgHeight, Bitmap.Config.ARGB_8888);
            final Canvas canvas = sCanvas;
            canvas.setBitmap(bitmap);
            sOldBounds.set(icon.getBounds());
            icon.setBounds(0, 0, ResourceUtil.mGeneralAppIconBgWidth, ResourceUtil.mGeneralAppIconBgHeight);
            icon.draw(canvas);
            icon.setBounds(sOldBounds);

            return bitmap;
        }
    }

    private static int getColorIndex(int color) {
        int r = (color & 0xff0000) >> 16;
        int g = (color & 0xff00) >> 8;
        int b = (color & 0xff) >> 0;

        int index = 0;

        // if (r == g && g == b) {
        // index = 3;
        // } else {
        index = Math.min(r, g);
        index = Math.min(b, index);

        // Log.d("allen", Integer.toHexString(color) + ":" + r + ":" + g + ":"
        // + b + ":" + index);

        if (index == r) {
            index = 0;
        } else if (index == g) {
            index = 1;
        } else if (index == b) {
            index = 2;
        }
        // }

        // Log.d("allen", Integer.toHexString(color) + ":" + index);

        return (0x1 << index);
    }

    private static int mFakeBackgroundIndex = 0;

    public static int getFakeRandomBackgroundId() {
        mFakeBackgroundIndex = (mFakeBackgroundIndex + 1) % app_bgs.length;
        return app_bgs[mFakeBackgroundIndex];
    }

    private static final int app_bgs_color[] = {
            0x5b36b7, 0x00c654, 0x178fff, 0xff017e, 0xff8a00, 0x00aba2, 0x45c95b, 0x900c0c

    };

    public static int getFakeRandomBackgroundColor() {
        mFakeBackgroundIndex = (mFakeBackgroundIndex + 1) % app_bgs.length;
        return app_bgs[mFakeBackgroundIndex];
    }

    public static int getmagicBackgroundId(Bitmap icon) {
        int i = doGetMagicNumber(icon) % app_bgs_color.length;
        //		Log.d("ab", "getmagicBackgroundId:" + i);
        return app_bgs_color[i] | 0xee000000;
    }

    public static int getmagicBackgroundId(Drawable icon) {
        int i = doGetMagicNumber(icon) % app_bgs.length;
        //		Log.d("ab ", "::" + i);
        return app_bgs[i];
    }

    private static int doGetMagicNumber(Bitmap bm) {
        int x = bm.getWidth() / 2;
        int y = bm.getHeight() / 2;

        int color1 = bm.getPixel(x, y);
        int color2 = bm.getPixel(x, y - (sWrappedIconWidth / 4));
        int color3 = bm.getPixel(x + (sWrappedIconWidth / 4), y + (sWrappedIconWidth / 4));
        int color4 = bm.getPixel(x - (sWrappedIconWidth / 4), y + (sWrappedIconWidth / 4));

        //		String ss = String.format("0x%x,0x%x,0x%x,0x%x,", color1, color2,
        //				color3, color4);
        // Log.d("ab", "doGetMagicNumber:"+ ss);

        int index = getColorIndex(color1) | getColorIndex(color4) | getColorIndex(color2) | getColorIndex(color3);

        //		Log.d("allen", "1:" + index);

        // if (index == 3) {
        // index = 0;
        // } else if (index == 5) {
        // index = 2;
        // } else if (index == 6) {
        // index = 1;
        // } else if (index == 7) {
        // index = 3;
        // }

        // Log.d("allen", "3:" + index);
        return index;
    }

    private static int doGetMagicNumber(Drawable drawable) {
        Bitmap bm = drawableToBitmap(drawable);

        int x = drawable.getIntrinsicWidth() / 4;
        int y = drawable.getIntrinsicHeight() / 4;

        int color1 = bm.getPixel(x, y);
        int color2 = bm.getPixel(x * 3, y);
        int color3 = bm.getPixel(x, y * 3);
        int color4 = bm.getPixel(x * 3, y * 3);

        int index = getColorIndex(color1) | getColorIndex(color4) | getColorIndex(color2) | getColorIndex(color3);

        // Log.d("allen", "1:" + index);

        if (index == 3) {
            index = 0;
        } else if (index == 5) {
            index = 2;
        } else if (index == 6) {
            index = 1;
        } else if (index == 7) {
            index = 3;
        }

        // Log.d("allen", "3:" + index);
        return index;

        // int color5 = bm.getPixel(x * 2, y * 2);

        // int color = color1 | color2 | color3 | color4;

        // Log.d("allen",
        // Integer.toHexString(r) + ":" + Integer.toHexString(g)
        // + ":" + Integer.toHexString(b) + ":"
        // + Integer.toHexString(color4) + ":"
        // + Integer.toHexString(color) + ":");

        // return (drawableToBitmap(drawable).getPixel(x, y) & 0x00FFFFFF >> 3);
    }

    private static Bitmap drawableToBitmap(Drawable drawable) {
        int w = drawable.getIntrinsicWidth();
        int h = drawable.getIntrinsicHeight();

        Bitmap.Config config = drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
        Bitmap bitmap = Bitmap.createBitmap(w, h, config);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, w, h);
        drawable.draw(canvas);
        return bitmap;
    }

    static void drawSelectedAllAppsBitmap(Canvas dest, int destWidth, int destHeight, boolean pressed, Bitmap src) {
        synchronized (sCanvas) { // we share the statics :-(
            if (sIconWidth == -1) {
                // We can't have gotten to here without src being initialized,
                // which
                // comes from this file already. So just assert.
                // initStatics(context);
                throw new RuntimeException("Assertion failed: Utilities not initialized");
            }

            dest.drawColor(0, PorterDuff.Mode.CLEAR);

            int[] xy = new int[2];
            Bitmap mask = src.extractAlpha(sBlurPaint, xy);

            float px = (destWidth - src.getWidth()) / 2;
            float py = (destHeight - src.getHeight()) / 2;
            dest.drawBitmap(mask, px + xy[0], py + xy[1], pressed ? sGlowColorPressedPaint : sGlowColorFocusedPaint);

            mask.recycle();
        }
    }

    /**
     * Returns a Bitmap representing the thumbnail of the specified Bitmap. The
     * size of the thumbnail is defined by the dimension
     * android.R.dimen.launcher_application_icon_size.
     *
     * @param bitmap  The bitmap to get a thumbnail of.
     * @param context The application's context.
     * @return A thumbnail for the specified bitmap or the bitmap itself if the
     * thumbnail could not be created.
     */
    static Bitmap resampleIconBitmap(Bitmap bitmap, Context context) {
        synchronized (sCanvas) { // we share the statics :-(
            if (sIconWidth == -1) {
                initStatics(context);
            }

            if (bitmap.getWidth() == sIconWidth && bitmap.getHeight() == sIconHeight) {
                return bitmap;
            } else {
                final Resources resources = context.getResources();
                return createIconBitmap(new BitmapDrawable(resources, bitmap), context);
            }
        }
    }

    static Bitmap drawDisabledBitmap(Bitmap bitmap, Context context) {
        synchronized (sCanvas) { // we share the statics :-(
            if (sIconWidth == -1) {
                initStatics(context);
            }
            final Bitmap disabled = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
            final Canvas canvas = sCanvas;
            canvas.setBitmap(disabled);

            canvas.drawBitmap(bitmap, 0.0f, 0.0f, sDisabledPaint);

            canvas.setBitmap(null);

            return disabled;
        }
    }

    private static void initStatics(Context context) {
        final Resources resources = context.getResources();
        final DisplayMetrics metrics = resources.getDisplayMetrics();
        final float density = metrics.density;

        sIconWidth = sIconHeight = (int) resources.getDimension(R.dimen.app_icon_size);

        sWrappedIconWidth = sWrappedIconHeight = (int) resources.getDimension(R.dimen.app_wrap_icon_size);

        sIconTextureWidth = sIconTextureHeight = sIconWidth;

        sBlurPaint.setMaskFilter(new BlurMaskFilter(5 * density, BlurMaskFilter.Blur.NORMAL));
        sGlowColorPressedPaint.setColor(0xffffc300);
        sGlowColorFocusedPaint.setColor(0xffff8e00);

        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0.2f);
        sDisabledPaint.setColorFilter(new ColorMatrixColorFilter(cm));
        sDisabledPaint.setAlpha(0x88);

/*		mSystemUI = MachineConfig
				.getPropertyReadOnly(MachineConfig.KEY_SYSTEM_UI);*/

        Drawable d1, d2, d3, d4;
        d1 = Drawable.createFromPath("/mnt/paramter/icon/app_bg_0.png");
        //Log.d("cc", "initStatics:"+d1);
        if (d1 != null) {
            d2 = Drawable.createFromPath("/mnt/paramter/icon/app_bg_1.png");
            d3 = Drawable.createFromPath("/mnt/paramter/icon/app_bg_2.png");
            d4 = Drawable.createFromPath("/mnt/paramter/icon/app_bg_3.png");
            if (d2 == null) {
                d2 = d1;
            }
            if (d3 == null) {
                d3 = d1;
            }
            if (d4 == null) {
                d4 = d1;
            }
            //			if(d2!=null&&d3!=null&d4!=null){

            //Log.d("cc", "initStatics: ok !!!!!!!!!");
            app_bg_drawable = new Drawable[4];
            app_bg_drawable[0] = d1;
            app_bg_drawable[1] = d2;
            app_bg_drawable[2] = d3;
            app_bg_drawable[3] = d4;
            //			}
        }


    }

    /**
     * Only works for positive numbers.
     */
    static int roundToPow2(int n) {
        int orig = n;
        n >>= 1;
        int mask = 0x8000000;
        while (mask != 0 && (n & mask) == 0) {
            mask >>= 1;
        }
        while (mask != 0) {
            n |= mask;
            mask >>= 1;
        }
        n += 1;
        if (n != orig) {
            n <<= 1;
        }
        return n;
    }

    static int generateRandomId() {
        return new Random(System.currentTimeMillis()).nextInt(1 << 24);
    }

    // for wince style icon
    private static void initStaticsWince(Context context) {
        final Resources resources = context.getResources();
        final DisplayMetrics metrics = resources.getDisplayMetrics();
        final float density = metrics.density;

        sIconWidth = sIconHeight = (int) resources.getDimension(R.dimen.app_icon_size);

        sWrappedIconWidth = sWrappedIconHeight = (int) resources.getDimension(R.dimen.app_wrap_icon_size);

        sIconTextureWidth = sIconTextureHeight = sIconWidth;

        sBlurPaint.setMaskFilter(new BlurMaskFilter(5 * density, BlurMaskFilter.Blur.NORMAL));
        sGlowColorPressedPaint.setColor(0xffffc300);
        sGlowColorFocusedPaint.setColor(0xffff8e00);

        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0.2f);
        sDisabledPaint.setColorFilter(new ColorMatrixColorFilter(cm));
        sDisabledPaint.setAlpha(0x88);

    }

    public static Bitmap createIconBitmapWinCEUI(Drawable icon, Context context, int bg_id) {
        // needBg = false;
        synchronized (sCanvas) { // we share the statics :-(
            if (sIconWidth == -1) {
                initStatics(context);
            }
            int sIconWidth = 238;
            int sIconHeight = 222;

            int sWrappedIconWidth = 106;
            int sWrappedIconHeight = 106;

            int sIconTextureWidth = sIconWidth;
            int sIconTextureHeight = sIconHeight;

            int width = sIconWidth;
            int height = sIconHeight;

            if (icon instanceof PaintDrawable) {
                // Log.d("cc", "PaintDrawable");
                PaintDrawable painter = (PaintDrawable) icon;
                painter.setIntrinsicWidth(width);
                painter.setIntrinsicHeight(height);
            } else if (icon instanceof BitmapDrawable) {
                // Log.d("cc", "BitmapDrawable");
                // Ensure the bitmap has a density.
                BitmapDrawable bitmapDrawable = (BitmapDrawable) icon;
                Bitmap bitmap = bitmapDrawable.getBitmap();
                if (bitmap.getDensity() == Bitmap.DENSITY_NONE) {
                    bitmapDrawable.setTargetDensity(context.getResources().getDisplayMetrics());
                }

                // bitmap = convertToBlackWhite(bitmap);
                // icon =new BitmapDrawable(bitmap);
            }
            int sourceWidth = icon.getIntrinsicWidth();
            int sourceHeight = icon.getIntrinsicHeight();
            if (sourceWidth > 0 && sourceHeight > 0) {
                // There are intrinsic sizes.

                width = sWrappedIconWidth;
                height = sWrappedIconHeight;

                if (width != sourceWidth && height != sourceHeight) {
                    // It's too big, scale it down.
                    final float ratio = (float) sourceWidth / sourceHeight;
                    if (sourceWidth > sourceHeight) {
                        height = (int) (width / ratio);
                    } else if (sourceHeight > sourceWidth) {
                        width = (int) (height * ratio);
                    }
                }

            }

            // no intrinsic size --> use default size
            int textureWidth = sIconTextureWidth;
            int textureHeight = sIconTextureHeight;

            Bitmap bitmap = Bitmap.createBitmap(textureWidth, textureHeight, Bitmap.Config.ARGB_8888);
            final Canvas canvas = sCanvas;
            canvas.setBitmap(bitmap);
            if (bg_id != 0) {
                // int i = doGetMagicNumber(icon) % app_bgs.length;

                Drawable d = context.getResources().getDrawable(bg_id);
                sOldBounds.set(d.getBounds());
                d.setBounds(0, 0, textureWidth, textureHeight);
                d.draw(canvas);
                d.setBounds(sOldBounds);

                // bitmap = convertToBlackWhite(bitmap);
            }

            final int left = (textureWidth - width) / 2;
            final int top = (textureHeight - height) / 2;

            @SuppressWarnings("all")
            // suppress dead code warning
            final boolean debug = false;
            if (debug) {
                // draw a big box for the icon for debugging
                canvas.drawColor(sColors[sColorIndex]);
                if (++sColorIndex >= sColors.length) sColorIndex = 0;
                Paint debugPaint = new Paint();
                debugPaint.setColor(0xffcccc00);
                canvas.drawRect(left, top, left + width, top + height, debugPaint);
            }

            sOldBounds.set(icon.getBounds());
            icon.setBounds(left, top, left + width, top + height);
            icon.draw(canvas);
            icon.setBounds(sOldBounds);
            canvas.setBitmap(null);

            return bitmap;
        }
    }

    //	public static String mSystemUI;

    public static Drawable getIconBackground(String packageName, String className, Context context) {
        String s = null;
        Drawable d = null;
        if ("com.car.ui".equals(packageName)) {
            if ("com.my.navi.NaviEmptyActivity".equals(className)) {
                s = "navi";
            } else if ("com.my.audio.MusicActivity".equals(className)) {
                s = "music";
            } else if ("com.my.dvd.DVDPlayer".equals(className)) {
                s = "dvd";
            } else if ("com.my.radio.RadioActivity".equals(className)) {
                s = "radio";
            } else if ("com.my.video.VideoActivity".equals(className)) {
                s = "video";
            } else if ("com.my.auxplayer.AUXPlayer".equals(className)) {
                s = "auxin";
            } else if ("com.my.btmusic.BTMusicActivity".equals(className)) {
                s = "bt_music";
            } else if ("com.my.frontcamera.FrontCameraActivity".equals(className) || "com.my.frontcamera.FrontCameraActivity4".equals(className)) {
                s = "f_camera";
            } else if ("com.my.tv.TVActivity".equals(className)) {
                s = "tv";
            }

        } else if ("com.my.bt".equals(packageName)) {
            if ("com.my.bt.ATBluetoothActivity".equals(className)) {
                s = "bt";
            } else if ("com.my.bt.VoiceControlActivity".equals(className)) {
                s = "bt_voice";
            }
        } else if ("com.android.gallery3d".equals(packageName)) {
            s = "gallery";
        } else if ("com.eqset".equals(packageName)) {
            if (Utilities.mIsDSP) {
                d = context.getDrawable(R.drawable.dsp);
                return d;
            } else {
                s = "eq";
            }
        } else if ("com.android.browser".equals(packageName)) {
            s = "browse";
        } else if ("com.my.dvr".equals(packageName)) {
            if ("com.my.dvr.MainActivity".equals(className)) {
                s = "dvr";
            }
        } else if ("com.SwcApplication".equals(packageName)) {
            if ("com.SwcApplication.SwcActivity".equals(className)) {
                s = "keystudy";
            }
        } else if ("com.android.settings".equals(packageName)) {
            //			if ("com.android.settings.SwcActivity".equals(className)) {
            s = "set";
            //			}
        } else if ("com.google.android.gm.ConversationListActivityGmail".equals(className)) {
            s = "gmail";
        } else if ("com.google.android.gms.app.settings.GoogleSettingsActivity".equals(className)) {
            s = "gsettings";
        } else if ("com.android.vending.AssetBrowserActivity".equals(className)) {
            s = "gvending";
        } else if ("com.google.android.googlequicksearchbox.VoiceSearchActivity".equals(className)) {
            s = "gvoice";
        } else if ("com.google.android.googlequicksearchbox.SearchActivity".equals(className)) {
            s = "gsearch";
        } else if ("com.my.instructions.InstructionsActivity".equals(className)) {
            s = "instructions";
        } else if ("com.estrongs.android.pop.view.FileExplorerActivity".equals(className)) {
            s = "file_explor";
        } else if ("com.android.calendar.AllInOneActivity".equals(className)) {
            s = "calendar";
        } else if ("com.android.calculator2.Calculator".equals(className)) {
            s = "calc";
        } else if ("com.google.android.maps.MapsActivity".equals(className)) {
            s = "gmap";
        } else if ("com.car.dvdplayer".equals(packageName)) {
            s = "dvd";
        }

        if (s != null) {

            Log.d("ccd1", s + ":" + ResourceUtil.mSystemUI);

            if (MachineConfig.VALUE_SYSTEM_UI43_3300.equals(ResourceUtil.mSystemUI)) {

                if (Utilities.mDark) {
                    s += "_d";
                }
                int j = context.getResources().getIdentifier(s, "drawable", context.getPackageName());
                Log.d("ccd", s + ":" + j);
                if (j != 0) {
                    d = context.getDrawable(j);
                }

            }

            if (d == null) {

                s = Utilities.mIconPath + s + ".png";

                d = Drawable.createFromPath(s);
            }

            if (d instanceof BitmapDrawable) {
                // Ensure the bitmap has a density.
                BitmapDrawable bitmapDrawable = (BitmapDrawable) d;
                bitmapDrawable.setTargetDensity(context.getResources().getDisplayMetrics());
            }
        }

        //		Log.d("allen", className + ":" + s + ":" + d);
        return d;

    }

    @SuppressLint("DiscouragedApi")
    public static void loadGernalAppBGs(Context context) {
        int count = 0;
        int id;
        for (int i = 0; i < 100; i++) {
            id = context.getResources().getIdentifier("app_bg_" + i, "drawable", context.getPackageName());
            if (id > 0) count++;
            else break;
        }
        if (count > 0) {
            app_bgs = new int[count];
            for (int i = 0; i < count; i++) {
                app_bgs[i] = context.getResources().getIdentifier("app_bg_" + i, "drawable", context.getPackageName());
            }
        }

        Log.d(TAG, "load " + count + " icon_bgs");
    }
}
