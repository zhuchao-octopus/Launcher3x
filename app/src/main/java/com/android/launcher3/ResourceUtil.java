package com.android.launcher3;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.util.DisplayMetrics;
import android.util.Log;

import com.common.utils.MachineConfig;
import com.common.utils.SettingProperties;
import com.zhuchao.android.fbase.MMLog;

public class ResourceUtil {
    private final static String TAG = "ResourceUtil";
    private final static String LAUNCHER_UI = "launcher_ui";
    public final static int RESOLUTION_800X480 = 0;
    public final static int RESOLUTION_1024X600 = 1;
    public final static int RESOLUTION_1280X480 = 2;
    public final static int RESOLUTION_1280X720 = 3;
    public final static int RESOLUTION_1920X1080 = 4;

    public static String mSystemUI;
    public static int mScreenWidth = 0;

    private static int smallestScreenWidthDp = 324;


    public static String updateUi(Context context) { // only launcher use now
        String value = MachineConfig.getPropertyReadOnly(LAUNCHER_UI);
        if (value == null) {
            value = MachineConfig.getPropertyReadOnly(MachineConfig.KEY_SYSTEM_UI);
        }

        int dsp = SettingProperties.getIntProperty(context, SettingProperties.KEY_DSP);
        Utilities.mIsDSP = (dsp == 1);
        mSystemUI = value;


        Configuration configuration = context.getResources().getConfiguration();
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        mScreenWidth = dm.widthPixels;
        if (dm.widthPixels == 800 && dm.heightPixels == 480) {
            //type = RESOLUTION_800X480;
            smallestScreenWidthDp = 324;
        } else if (dm.widthPixels == 1024 && dm.heightPixels == 600) {
            //type = RESOLUTION_1024X600;
            smallestScreenWidthDp = 329;
        } else if (dm.widthPixels == 1280 && dm.heightPixels == 480) {
            //type = RESOLUTION_1280X480;
            smallestScreenWidthDp = 321;
        } else if (dm.widthPixels == 1280 && dm.heightPixels == 720) {
            //type = RESOLUTION_1280X720;
            smallestScreenWidthDp = 323;
        } else if (dm.widthPixels == 1920 && dm.heightPixels == 1080) {
            //type = RESOLUTION_1920X1080;
            smallestScreenWidthDp = 352;
        } else {
            smallestScreenWidthDp = 329;
            //type = RESOLUTION_1024X600;
        }
        ///sw = 324;

        MMLog.d(TAG, "configuration:" + configuration);
        configuration.smallestScreenWidthDp = smallestScreenWidthDp;
        context.getResources().updateConfiguration(configuration, null);
        if (MachineConfig.VALUE_SYSTEM_UI43_3300.equals(value)) {
            try {
                loadGeneralAppRes(context);
            } catch (Exception e) {
                ///e.printStackTrace();
                MMLog.e(TAG, String.valueOf(e));
            }

            try {
                UtilIconBitmap.loadGernalAppBGs(context);
            } catch (Exception e) {
                ///e.printStackTrace();
                MMLog.e(TAG, String.valueOf(e));
            }
        }

        MMLog.d(TAG, "configuration:" + configuration);
        return value;
    }

    public static int mGeneralAppIconBgWidth = 0;        //bg width in BubbleTextView
    public static int mGeneralAppIconBgHeight = 0;        //bg height in BubbleTextView
    public static int mGeneralAppIconOffsetLeft = 0;    //icon left offset in bg
    public static int mGeneralAppIconOffsetTop = 0;        //icon top offset in bg
    public static int mGeneralAppTextPositionX = 0;        //text draw x position
    public static int mGeneralAppTextPositionY = 0;        //text draw y position
    public static int mGeneralAppTextSize = 0;            //text size in BubbleTextView

    @SuppressLint("DiscouragedApi")
    public static void loadGeneralAppRes(Context context) {
        int id = context.getResources().getIdentifier("app_icon_bg_width", "dimen", context.getPackageName());
        if (id > 0) mGeneralAppIconBgWidth = (int) context.getResources().getDimension(id);

        id = context.getResources().getIdentifier("app_icon_bg_height", "dimen", context.getPackageName());
        if (id > 0) mGeneralAppIconBgHeight = (int) context.getResources().getDimension(id);

        id = context.getResources().getIdentifier("app_icon_offset_left", "dimen", context.getPackageName());
        if (id > 0) mGeneralAppIconOffsetLeft = (int) context.getResources().getDimension(id);

        id = context.getResources().getIdentifier("app_icon_offset_top", "dimen", context.getPackageName());
        if (id > 0) mGeneralAppIconOffsetTop = (int) context.getResources().getDimension(id);

        id = context.getResources().getIdentifier("app_iocn_text_position_x", "dimen", context.getPackageName());
        if (id > 0) mGeneralAppTextPositionX = (int) context.getResources().getDimension(id);

        id = context.getResources().getIdentifier("app_iocn_text_position_y", "dimen", context.getPackageName());
        if (id > 0) mGeneralAppTextPositionY = (int) context.getResources().getDimension(id);

        id = context.getResources().getIdentifier("app_icon_text_size", "dimen", context.getPackageName());
        if (id > 0) mGeneralAppTextSize = (int) context.getResources().getDimension(id);

        MMLog.d("ResourceUtil", "[" + mGeneralAppIconBgWidth + "," + mGeneralAppIconBgHeight + "][" + mGeneralAppIconOffsetLeft + "," + mGeneralAppIconOffsetTop + "][" + mGeneralAppTextPositionX + "," + mGeneralAppTextPositionY + "," + mGeneralAppTextSize + "]");
    }
}
