package com.my.dispaux;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.hardware.display.DisplayManager;
import android.util.Log;
import android.view.Display;

import com.zhuchao.android.fbase.TAppUtils;

import java.util.ArrayList;

public class Utils {

    /*private static ArrayList<ActivityManager.RunningTaskInfo> getRunningTaskList(int maxNum) {
        ArrayList<ActivityManager.RunningTaskInfo> taskList = null;
        try {
            taskList = (ArrayList) ActivityManager.getService().getFilteredTasks(maxNum, ACTIVITY_TYPE_RECENTS, WINDOWING_MODE_PINNED);
			///TAppUtils.getRunningProcess()
        } catch (Exception e) {
            Log.e(DispAuxActivity.TAG, "getRunningTaskList error: ", e);
        }
        return taskList;
    }*/

    public static boolean appIsRunning(Context context,String pkgName, String className) {
		return TAppUtils.isProcessRunning(context,pkgName);
        /*try {
            ArrayList<ActivityManager.RunningTaskInfo> listRunning = getRunningTaskList(100);
            // dumpTaskInfo("closeApp", listRunning);
            int taskId = -1;
            for (int i = 0; listRunning != null && i < listRunning.size(); i++) {
                ActivityManager.RunningTaskInfo rti = listRunning.get(i);
                if (rti != null) {
                    if (rti.baseActivity != null) {
                        int id = -1;
                        if (pkgName != null && className != null) {
                            if (pkgName.equals(rti.baseActivity.getPackageName()) && className.equals(rti.baseActivity.getClassName())) {
                                id = rti.id;
                            }
                        } else if (pkgName != null) {
                            if (pkgName.equals(rti.baseActivity.getPackageName())) {
                                id = rti.id;
                            }
                        }

                        if (id != -1) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(DispAuxActivity.TAG, "appIsRunning error: ", e);
        }
        return false;*/
    }

    public static Bitmap createReflectedImage(Bitmap paramBitmap, int paramInt) {
        Bitmap localBitmap2 = null;
        try {
            int i = paramBitmap.getWidth();
            int j = paramBitmap.getHeight();
            Matrix localMatrix = new Matrix();
            localMatrix.preScale(1.0F, -1.0F);
            Bitmap localBitmap1 = Bitmap.createBitmap(paramBitmap, 0, Math.max((j - paramInt), 0), i, paramInt, localMatrix, false);
            localBitmap2 = Bitmap.createBitmap(i, j + paramInt, Bitmap.Config.ARGB_8888);
            Canvas localCanvas = new Canvas(localBitmap2);
            Paint localPaint1 = new Paint();
            localCanvas.drawBitmap(paramBitmap, 0.0F, 0.0F, localPaint1);
            localCanvas.drawBitmap(localBitmap1, 0.0F, j, localPaint1);
            Paint localPaint2 = new Paint();
            localPaint2.setShader(new LinearGradient(0.0F, paramBitmap.getHeight(), 0.0F, localBitmap2.getHeight(), 1895825407, 16777215, Shader.TileMode.MIRROR));
            localPaint2.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
            localCanvas.drawRect(0.0F, j, i, localBitmap2.getHeight(), localPaint2);
            localBitmap1.recycle();
        } catch (Exception e) {

        }
        if (localBitmap2 == null) {
            return paramBitmap;
        }
        return localBitmap2;
    }

    public static Display getDispAuxInfo(Context context) {
        DisplayManager displayManager = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
        Display[] display = displayManager.getDisplays();

        if (display.length >= 2) {
            return display[1];
        }
        return null;
    }
}
