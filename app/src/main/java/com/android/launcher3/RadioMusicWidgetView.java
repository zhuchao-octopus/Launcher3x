package com.android.launcher3;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.common.utils.AppConfig;
import com.common.utils.BroadcastUtil;
import com.common.utils.MachineConfig;
import com.common.utils.MyCmd;
import com.common.utils.ProtocolAk47;
import com.common.utils.SettingProperties;
import com.common.utils.UtilCarKey;
import com.my.radio.MarkFaceView;
import com.zhuchao.android.fbase.MMLog;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

public class RadioMusicWidgetView {
    public static Launcher mContext;
    @SuppressLint("StaticFieldLeak")
    private static RadioMusicWidgetView mThis;

    TextView mClock;
    TextView mDate;
    TextView mDate2;

    TextView mCEDate;
    TextView mRadioBaud;
    TextView mRadioFreq;
    TextView mRadioHz;

    //	View mMusicIcon;
    TextView mMusicName;
    TextView mMusicTime;
    ImageView mMusicArt;

    SeekBar mMusicSeekBar;

    int mMusicCurTime;
    int mMusicTotalTime;
    String mName;
    MarkFaceView mMarkFace;

    private final static String TAG = "RadioMusicWidgetView";
    private boolean mPause = true;

    public static RadioMusicWidgetView getInstance(Launcher ac) {
        if (mThis == null) {
            MMLog.d(TAG, "RadioMusicWidgetView.getInstance:" + ac.findViewById(R.id.entry_time));
            if (ac.findViewById(R.id.entry_time) != null) {
                mThis = new RadioMusicWidgetView();
                mThis.init(ac);
            }
        }
        //mThis.updateRadioInfo();
        return mThis;
    }

    public static void onPause() {
        if (mThis != null) {
            mThis.doPause();
            mThis.mPause = true;
        }
    }

    public static void onResume() {
        if (mThis != null) {
            mThis.mPause = false;
            mThis.doResume();
        }
    }

    public static void onDestroy() {
        if (mThis != null) {
            mThis.unregisterReceiver();
            mThis.mPause = true;
        }
        mThis = null;
    }

    private void doPause() {
        // mSource = MyCmd.SOURCE_NONE;
        mHandler.removeMessages(0);
        // unregisterReceiver();
    }

    private void doResume() {
        // mSource = MyCmd.SOURCE_NONE;
        registerReceiver();
        BroadcastUtil.sendToCarService(mContext, MyCmd.Cmd.QUERY_CURRENT_SOURCE, 0);
        updateTime();
        //updateRadio();
    }

    public void init(Launcher ac) {
        Log.d("aa", "init:" + ac);
        mContext = ac;
        mPause = false;
        mClock = (TextView) mContext.findViewById(R.id.radio_music_time);

        // mClock.setTypeface(fromAsset);

        mDate = (TextView) mContext.findViewById(R.id.radio_music_date);
        mCEDate = (TextView) mContext.findViewById(R.id.ce_date);

        mDate2 = (TextView) mContext.findViewById(R.id.radio_music_date2);
        mRadioBaud = (TextView) mContext.findViewById(R.id.radio_music_baud);
        mRadioFreq = (TextView) mContext.findViewById(R.id.radio_music_freq);

        mViewHour = mContext.findViewById(R.id.clock_hour);
        mViewMinutus = mContext.findViewById(R.id.clock_minute);

        mViewSecond = mContext.findViewById(R.id.clock_second);
        mRadioHz = (TextView) mContext.findViewById(R.id.radio_music_hz);

        //mMusicIcon = mContext.findViewById(R.id.music_icon);
        mMusicName = (TextView) mContext.findViewById(R.id.music_name);
        mMusicTime = (TextView) mContext.findViewById(R.id.music_time);
        View v = mContext.findViewById(R.id.music_progress);
        if (v != null) {
            mMusicSeekBar = (SeekBar) v;
        }
        mMusicArt = (ImageView) mContext.findViewById(R.id.album_art);
        mMarkFace = (MarkFaceView) mContext.findViewById(R.id.radio_mark_face_view);

        // TextView mRadioBaud;
        // TextView mRadioFreq;
        // TextView mRadioHz;

        //		String value = MachineConfig
        //				.getPropertyReadOnly(MachineConfig.KEY_SYSTEM_UI);
        //		if (MachineConfig.VALUE_SYSTEM_UI_KLD3_8702.equals(value)) {
        //			Typeface fromAsset = Typeface
        //					.createFromFile("/system/fonts/AndroidClock_Highlight.ttf");
        //			fromAsset = Typeface.create(fromAsset, Typeface.BOLD);
        //			mClock.setTypeface(fromAsset);
        //			mRadioFreq.setTypeface(fromAsset);
        //		}
        //		if (MachineConfig.VALUE_SYSTEM_UI_KLD3.equals(value)||
        //				MachineConfig.VALUE_SYSTEM_UI_KLD10_887.equals(value)) {
        //
        //		} else {
        //			AssetManager assets = ac.getAssets();
        //
        //			Typeface fromAsset = Typeface.createFromAsset(assets,
        //					"fonts/DS-DIGIT.TTF");
        //			fromAsset = Typeface.create(fromAsset, Typeface.ITALIC);
        //			mRadioFreq.setTypeface(fromAsset);
        //		}
        setViewVisible(R.id.entry_radio, View.VISIBLE);
        setViewVisible(R.id.entry_music, View.GONE);

        registerReceiver();
        updateRadio();
        BroadcastUtil.sendToCarService(mContext, MyCmd.Cmd.QUERY_CURRENT_SOURCE, 0);
        BroadcastUtil.sendToCarServiceMcuRadio(mContext, ProtocolAk47.SEND_RADIO_SUB_QUERY_RADIO_INFO, 0);
        updateTime();
    }

    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.radio_button_play) {
            if (mSource != MyCmd.SOURCE_RADIO) {
                BroadcastUtil.sendToCarServiceMcuRadio(mContext, ProtocolAk47.SEND_RADIO_SUB_QUERY_RADIO_INFO, 0);
            }
            BroadcastUtil.sendKey(mContext, AppConfig.getCarAppPackageName(mContext), MyCmd.Keycode.RADIO_POWER);
        } else if (id == R.id.radio_button_prev) {
            if (mSource == MyCmd.SOURCE_RADIO) {
                BroadcastUtil.sendKey(mContext, AppConfig.getCarAppPackageName(mContext), MyCmd.Keycode.PREVIOUS);
                // BroadcastUtil.sendKey(mContext, AppConfig.PACKAGE_CAR_UI, MyCmd.Keycode.PREVIOUS);
            }

            // BroadcastUtil.sendToCarServiceMcuRadio(mContext,
            // ProtocolAk47.SEND_RADIO_SUB_RADIO_OPERATION, 1, 3);
        } else if (id == R.id.radio_button_next) {
            if (mSource == MyCmd.SOURCE_RADIO) {
                BroadcastUtil.sendKey(mContext, AppConfig.getCarAppPackageName(mContext), MyCmd.Keycode.NEXT);
            }
            // BroadcastUtil.sendToCarServiceMcuRadio(mContext,
            // ProtocolAk47.SEND_RADIO_SUB_RADIO_OPERATION, 1, 4);
        } else if (id == R.id.music_button_prev) {// setSource(MyCmd.SOURCE_MUSIC);

            if (mSource == MyCmd.SOURCE_MUSIC || mSource == MyCmd.SOURCE_BT_MUSIC) {
                BroadcastUtil.sendKey(mContext, AppConfig.getCarAppPackageName(mContext), MyCmd.Keycode.PREVIOUS);
            }
        } else if (id == R.id.music_button_play) {
            if (mSource == MyCmd.SOURCE_MUSIC || mSource == MyCmd.SOURCE_BT_MUSIC) {
                BroadcastUtil.sendKey(mContext, AppConfig.getCarAppPackageName(mContext), MyCmd.Keycode.PLAY_PAUSE);
            }
        } else if (id == R.id.music_button_next) {
            if (mSource == MyCmd.SOURCE_MUSIC || mSource == MyCmd.SOURCE_BT_MUSIC) {

                BroadcastUtil.sendKey(mContext, AppConfig.getCarAppPackageName(mContext), MyCmd.Keycode.NEXT);
            }
        } else if (id == R.id.album_art || id == R.id.entry_music || id == R.id.entry_music2) {
            if (mSource == MyCmd.SOURCE_BT_MUSIC /*&& (mPlayStatus >= 3)*/) {
                UtilCarKey.doKeyBTMusic(mContext);
            } else {
                UtilCarKey.doKeyAudio(mContext);
            }
        }
    }

    public static void onRadioMusicClick(View v) {
        if (mThis != null) {
            mThis.onClick(v);
        }
    }

    Handler mHandler = new Handler(Objects.requireNonNull(Looper.myLooper())) {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    updateTime();
                    break;
                case 1:
                    mMusicCurTime += 1000;
                    updateMusicTime();
                    startUpdateMusicTime();
                    break;
            }
            super.handleMessage(msg);
        }
    };

    private void updateTime() {
        if (!mPause) {
            setTime();
        }
    }

    private final static int MAX_MUSICTIME = 36000000;

    private void updateMusicTime() {
        String time = "";
        if (mMusicCurTime >= 0 && mMusicCurTime < MAX_MUSICTIME && mMusicTotalTime >= 0 && mMusicTotalTime < MAX_MUSICTIME) {
            if (mMusicTotalTime > 0) {
                time = stringForTime(mMusicCurTime) + "/" + stringForTime(mMusicTotalTime);
            } else {
                time = stringForTime(mMusicCurTime);
            }
        }
        mMusicTime.setText(time);
        if (mMusicSeekBar != null) {
            int process = 0;
            if (mMusicTotalTime > 0) {
                process = mMusicCurTime * 100 / mMusicTotalTime;
            }
            if (process < 0) {
                process = 0;
            } else if (process > 100) {
                process = 100;
            }
            mMusicSeekBar.setProgress(process);
        }
    }

    private void startUpdateMusicTime() {
        stopUpdateMusicTime();
        mHandler.sendEmptyMessageDelayed(1, 1000);
    }

    private void stopUpdateMusicTime() {
        mHandler.removeMessages(1);
    }

    public byte mMRDBand;
    public byte mMRDNumber = 0;
    public short mMRDFreqency = 8750;


    public short mFreqMax;
    public short mFreqMin;

    private void doMcuData(byte[] buf) {
        int ret = 0;
        if (buf != null && buf.length > 2) {
            switch (buf[1]) {
                case 0x2:
                    if (buf.length > 11) {

                        mCurPlayIndex = (int) (buf[3] & 0xff);
                        mMRDBand = buf[2];
                        mMRDFreqency = (short) ((((int) buf[4] & 0xff) << 8) | ((int) buf[5]) & 0xff);


                        mFreqMin = (short) ((((int) buf[8] & 0xff) << 8) | ((int) buf[9]) & 0xff);
                        mFreqMax = (short) ((((int) buf[10] & 0xff) << 8) | ((int) buf[11]) & 0xff);
                        updateRadio();
                    }
                    break;
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private void updateRadio() {
        if (mMRDBand <= 2) {
            // mRadioFreq.setText(String.format("%d.%d", mMRDFreqency / 100,
            // mMRDFreqency % 100));
            String s = (mMRDFreqency / 100) + "." + ((mMRDFreqency % 100) / 10) + (mMRDFreqency % 10);
            // if ((mMRDFreqency % 100) == 0 && (mMRDFreqency / 100) != 0) {
            // s += 0;
            // }
            mRadioFreq.setText(s);
            // int index = getCurBaundText();
            mRadioBaud.setText(getCurBaundText());
            mRadioHz.setText("MHz");
            setImageNumRadio(mMRDFreqency, true);
            mMarkFace.setmIsAM(false);

            mMarkFace.setFmFrequencyRange(mFreqMin, mFreqMax);
            MarkFaceView.mFrequencyNum = (float) mMRDFreqency / 100;
        } else {

            mRadioFreq.setText("" + mMRDFreqency);
            setImageNumRadio(mMRDFreqency, false);
            // int index = getCurBaundText();
            mRadioBaud.setText(getCurBaundText());
            mRadioHz.setText("KHz");
            mMarkFace.setmIsAM(true);
            MarkFaceView.mFrequencyNum = (float) ((float) mMRDFreqency);
        }
        mMarkFace.postInvalidate();
    }

    private View mViewHour;
    private View mViewMinutus;
    private View mViewSecond;

    /*
     * private void setTime() { // 24
     *
     * Date curDate = new Date(System.currentTimeMillis()); int h =
     * curDate.getHours();
     *
     * String strTimeFormat = Settings.System.getString(
     * mContext.getContentResolver(),
     * android.provider.Settings.System.TIME_12_24);
     *
     * if ("12".equals(strTimeFormat)) { if (h > 12) { h -= 12; } }
     *
     * mClock.setText(String.format("%02d:%02d", h, curDate.getMinutes())); }
     */

    @SuppressLint("SimpleDateFormat")
    private void setTime() { // 24

        //		Calendar c = Calendar.getInstance();
        //		String s;
        //
        //		int h = Calendar.HOUR_OF_DAY;
        //
        //		String strTimeFormat = Settings.System.getString(
        //				mContext.getContentResolver(),
        //				android.provider.Settings.System.TIME_12_24);
        //
        //		if ("12".equals(strTimeFormat)) {
        //			h = c.get(Calendar.HOUR);
        //			if (h == 0) {
        //				h = 12;
        //			}
        //		} else {
        //
        //			h = c.get(Calendar.HOUR_OF_DAY);
        //		}
        //
        //		s = String.format("%02d:%02d", h, c.get(Calendar.MINUTE));

        String s = SettingProperties.getProperty(mContext, SettingProperties.KEY_DATE_FORMAT);
        if (s == null) {
            s = "yyyy/MM/dd";
        }
        long time = System.currentTimeMillis();
        Date d1 = new Date(time);
        SimpleDateFormat format = new SimpleDateFormat(s);
        String t1 = format.format(d1);

        int h = d1.getHours();

        int m = d1.getMinutes();
        int sec = d1.getSeconds();

        boolean is24 = DateFormat.is24HourFormat(mContext);
        if (is24) {
            format = new SimpleDateFormat("HH:mm");
        } else {
            format = new SimpleDateFormat("hh:mm");
        }
        String t2 = format.format(d1);

        mClock.setText(t2);

        setImageNumTime(t2);
        Calendar c = Calendar.getInstance();

        if (MachineConfig.VALUE_SYSTEM_UI19_KLD1.equals(ResourceUtil.mSystemUI)) {
            //	t1 = getMonth(c);
            mDate.setText(getMonth(c));
        } else {
            mDate.setText(t1);
        }
        if (mCEDate != null) {
            mCEDate.setText(t1);
        }

        mDate2.setText(getWeek(c));
        int second = c.get(Calendar.SECOND);
        mHandler.removeMessages(0);
        second = ((60 - second) % 60);
        if (second == 0) {
            second = 60;
        }
        //	Log.d(TAG, t2+"mClock:"+second+":"+mContext);
        mHandler.sendEmptyMessageDelayed(0, 1000);

        if (mViewHour != null) {
            float rotate;
            rotate = ((h * 60 + m) / 720.0f) * 360.0f;// (h * 360.0f) / 12.0f;
            mViewHour.setRotation(rotate);
            rotate = (m * 360.0f) / 60.0f;
            mViewMinutus.setRotation(rotate);
            if (mViewSecond != null) {
                rotate = (sec * 360.0f) / 60.0f;
                mViewSecond.setRotation(rotate);
            }
        }

        View d3;
        d3 = mContext.findViewById(R.id.radio_music_date3);
        if (d3 != null) {
            s = "dd";
            format = new SimpleDateFormat(s);
            t1 = format.format(d1);

            ((TextView) d3).setText(t1);
        }
    }

    private void setImageNumRadio(int freq, boolean fm) {
        try {
            if (fm) {
                setViewVisible(R.id.num_dot, View.VISIBLE);
                if (freq >= 10000) {
                    setViewVisible(R.id.radionum1, View.VISIBLE);
                    setNumImage(R.id.radionum1, 1);
                } else {
                    setViewVisible(R.id.radionum1, View.GONE);
                }
                setViewVisible(R.id.radionum2, View.VISIBLE);
                setNumImage(R.id.radionum2, (freq / 1000) % 10);
                setNumImage(R.id.radionum3, (freq / 100) % 10);
                setNumImage(R.id.radionum4, (freq / 10) % 10);
                setNumImage(R.id.radionum5, freq % 10);
            } else {
                setViewVisible(R.id.radionum1, View.GONE);
                setViewVisible(R.id.num_dot, View.GONE);
                if (freq >= 1000) {
                    setViewVisible(R.id.radionum2, View.VISIBLE);
                    setNumImage(R.id.radionum2, (freq / 1000) % 10);
                } else {
                    setViewVisible(R.id.radionum2, View.GONE);
                }

                setNumImage(R.id.radionum3, (freq / 100) % 10);
                setNumImage(R.id.radionum4, (freq / 10) % 10);
                setNumImage(R.id.radionum5, freq % 10);
            }
        } catch (Exception ignored) {
        }
    }

    private void setImageNumTime(String time) {
        try {
            String[] ss = time.split(":");
            setNumImage(R.id.timehour_h, Integer.parseInt(ss[0].substring(0, 1)));
            setNumImage(R.id.timehour_l, Integer.parseInt(ss[0].substring(1, 2)));
            setNumImage(R.id.timeminute_h, Integer.parseInt(ss[1].substring(0, 1)));
            setNumImage(R.id.timeminute_l, Integer.parseInt(ss[1].substring(1, 2)));
        } catch (Exception ignored) {
        }
    }

    private void setNumImage(int id, int num) {
        try {
            View v = mContext.findViewById(id);
            if (v == null) {
                return;
            }
            ((ImageView) v).getDrawable().setLevel(num);
        } catch (Exception ignored) {
        }
    }

    public static String getMonth(Calendar cal) {
        int i = cal.get(Calendar.MONTH);
        int id = R.string.month1;
        switch (i) {
            case 0:
                id = R.string.month1;
                break;
            case 1:
                id = R.string.month2;
                break;
            case 2:
                id = R.string.month3;
                break;
            case 3:
                id = R.string.month4;
                break;
            case 4:
                id = R.string.month5;
                break;
            case 5:
                id = R.string.month6;
                break;
            case 6:
                id = R.string.month7;
                break;
            case 7:
                id = R.string.month8;
                break;
            case 8:
                id = R.string.month9;
                break;
            case 9:
                id = R.string.montha;
                break;
            case 10:
                id = R.string.monthb;
                break;
            case 11:
                id = R.string.monthc;
                break;
        }

        //String s = mContext.getString(R.string.month1+(i-1));
        String s = mContext.getString(id);
        //Log.d("abcd", i+"::::"+s);
        return s;
    }

    public static String getWeek(Calendar cal) {

        int i = cal.get(Calendar.DAY_OF_WEEK);
        return mContext.getString(R.string.week1 + (i - 1));
	        /*
	        switch (i) {
	        case 1:
	            return "Sun";
	        case 2:
	            return "Mon";
	        case 3:
	            return "Tues";
	        case 4:
	            return "Wed";
	        case 5:
	            return "Thur";
	        case 6:
	            return "Fri";
	        case 7:
	            return "Sat";
	        default:
	            return ""+i;
	        }*/
	        
	        /*
	      　　星期一： Mon.=Monday
	    		　　星期二： Tues.=Tuesday
	    		　　星期三：Wed.=Wednesday
	    		　　星期四： Thur.=Thurday 
	    		　　星期五： Fri.=Friday
	    		　　星期六： Sat.=Saturday 
	    		　　星期天： Sun.=Sunday
	    */
    }

    private static BroadcastReceiver mBroadcastReceiver = null;

    private void unregisterReceiver() {
        if (mBroadcastReceiver != null) {
            mContext.unregisterReceiver(mBroadcastReceiver);
            mBroadcastReceiver = null;
        }
    }

    private void setViewVisible(int id, int visibility) {
        View v = mContext.findViewById(id);
        if (v != null) {
            v.setVisibility(visibility);
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void updateRadioInfo() {
        //	Log.d(TAG, "updateRadioInfo:"+mSource);
        if (mSource == MyCmd.SOURCE_RADIO) {
            mContext.findViewById(R.id.radio_info_layout).setVisibility(View.VISIBLE);
            mContext.findViewById(R.id.radio_button_prev).setVisibility(View.VISIBLE);
            mContext.findViewById(R.id.radio_button_next).setVisibility(View.VISIBLE);
            mMarkFace.setEnable(true);
            setViewVisible(R.id.entry_radio, View.VISIBLE);
            setViewVisible(R.id.entry_music, View.GONE);

        } else if (mSource == MyCmd.SOURCE_MUSIC || mSource == MyCmd.SOURCE_BT_MUSIC) {
            setViewVisible(R.id.entry_radio, View.GONE);
            setViewVisible(R.id.entry_music, View.VISIBLE);
            //	setViewVisible(R.id.radio_info_layout, View.GONE);
            mMarkFace.setEnable(false);
        } else {
            //				mContext.findViewById(R.id.radio_info_layout).setVisibility(
            //					View.GONE);
            //			mContext.findViewById(R.id.radio_button_prev).setVisibility(
            //					View.INVISIBLE);

            //			mContext.findViewById(R.id.radio_button_next).setVisibility(
            //					View.INVISIBLE);

            //			mMarkFace.setEnable(false);

        }

        if (mSource == MyCmd.SOURCE_MUSIC) {
            setViewVisible(R.id.music_icon, View.GONE);
            mMusicName.setVisibility(View.VISIBLE);
            mMusicTime.setVisibility(View.VISIBLE);
            mMusicArt.setImageDrawable(mContext.getResources().getDrawable(R.drawable.music_pic));
            mMusicName.setText(mStrMusicName);
            if (mMusicPlaying != 1) {
                mMusicTime.setText("");
                if (mMusicSeekBar != null) {
                    mMusicSeekBar.setProgress(0);
                }
            } else {
                setPlayButtonStatus(true);
                startUpdateMusicTime();
            }
        } else if (mSource == MyCmd.SOURCE_BT_MUSIC) {
            mMusicArt.setImageDrawable(mContext.getResources().getDrawable(R.drawable.bt_music_pic));
            if (mPlayStatus < 3) {
                mMusicName.setText("");
                mMusicTime.setText("");

                setViewVisible(R.id.music_icon, View.VISIBLE);
            } else {
                setViewVisible(R.id.music_icon, View.GONE);
            }

            mMusicName.setVisibility(View.VISIBLE);
            mMusicTime.setVisibility(View.VISIBLE);
            mMusicName.setText(mID3Name);
            mMusicTime.setText("");
            if (mMusicSeekBar != null) {
                mMusicSeekBar.setProgress(0);
            }
        } else {
            setViewVisible(R.id.music_icon, View.VISIBLE);
            mMusicName.setVisibility(View.GONE);
            mMusicTime.setVisibility(View.GONE);
            stopUpdateMusicTime();
            mMusicArt.setImageDrawable(mContext.getResources().getDrawable(R.drawable.music_pic));
            mMusicName.setText("");
            mMusicTime.setText("");
            if (mMusicSeekBar != null) {
                mMusicSeekBar.setProgress(0);
            }
            setPlayButtonStatus(false);
        }
    }

    private void setPlayButtonStatus(boolean playing) {
        if (playing) {
            ((ImageView) mContext.findViewById(R.id.music_button_play)).getDrawable().setLevel(1);
        } else {
            ((ImageView) mContext.findViewById(R.id.music_button_play)).getDrawable().setLevel(0);
        }
    }

    public static int mSource = MyCmd.SOURCE_NONE;

    public static void setSource(int source) {
        if (mSource != source) {
            BroadcastUtil.sendToCarServiceSetSource(mContext, source);
        }
    }

    @SuppressLint("DefaultLocale")
    private String stringForTime(int timeMs) {
        int totalSeconds = timeMs / 1000;

        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return String.format("%02d:%02d", minutes, seconds).toString();
        }
        // return str;
    }


    public String mStrMusicName = "";

    public int mPlayStatus = A2DP_INFO_INITIAL;
    public String mID3Name = "";
    public String mID3Artist = "";
    public String mID3Album = "";

    public static final int A2DP_INFO_INITIAL = 0;
    public static final int A2DP_INFO_READY = 1;
    public static final int A2DP_INFO_CONNECTING = 2;
    public static final int A2DP_INFO_CONNECTED = 3;
    public static final int A2DP_INFO_PLAY = 4;
    public static final int A2DP_INFO_PAUSED = 5;

    private void doBTCmd(Intent intent) {
        int cmd = intent.getIntExtra(MyCmd.EXTRA_COMMON_CMD, 0);
        switch (cmd) {
            case MyCmd.Cmd.BT_SEND_A2DP_STATUS: {
                int play = intent.getIntExtra(MyCmd.EXTRA_COMMON_DATA, 0);

                //			String name = intent.getStringExtra(MyCmd.EXTRA_COMMON_DATA2);
                //			String pin = intent.getStringExtra(MyCmd.EXTRA_COMMON_DATA3);

                //			if (name != null) {
                //				mBtName = name;
                //			}
                //			if (pin != null) {
                //				mBtPin = pin;
                //			}

                if (play != mPlayStatus) {
                    mPlayStatus = play;
                    // updateConnectView();
                }
                if (mSource == MyCmd.SOURCE_BT_MUSIC) {
                    setPlayButtonStatus(mPlayStatus == A2DP_INFO_PLAY);
                    if (mPlayStatus < 3) {
                        mMusicName.setText("");
                        mMusicTime.setText("");
                        if (mMusicSeekBar != null) {
                            mMusicSeekBar.setProgress(0);
                        }
                        setViewVisible(R.id.music_icon, View.VISIBLE);
                    } else {

                        setViewVisible(R.id.music_icon, View.GONE);
                    }
                }
            }
            break;
            case MyCmd.Cmd.BT_SEND_ID3_INFO: {
                mID3Name = intent.getStringExtra(MyCmd.EXTRA_COMMON_DATA);
                if (mSource == MyCmd.SOURCE_BT_MUSIC) {
                    mMusicName.setText(mID3Name);
                }
                //			Log.d("allen", mID3Name);
                //			mID3Artist = intent.getStringExtra(MyCmd.EXTRA_COMMON_DATA2);
                //			mID3Album = intent.getStringExtra(MyCmd.EXTRA_COMMON_DATA3);
                // updateView();
            }
            break;
            case MyCmd.Cmd.BT_SEND_TIME_STATUS: {
                mMusicCurTime = intent.getIntExtra(MyCmd.EXTRA_COMMON_DATA, 0) * 1000;
                mMusicTotalTime = intent.getIntExtra(MyCmd.EXTRA_COMMON_DATA2, 0) * 1000;
                if (mSource == MyCmd.SOURCE_BT_MUSIC) {
                    updateMusicTime();
                }
                // updateView();
            }
            default:
                return;
        }
    }


    private Bitmap mArtWork;

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void registerReceiver() {
        if (mBroadcastReceiver != null) {
            try {
                mThis.unregisterReceiver();
            } catch (Exception e) {
                if (e != null) Log.e(TAG, "onRegisterReceiver: " + e.toString());
            }
            mBroadcastReceiver = null;
        }

        mBroadcastReceiver = new BroadcastReceiver() {
            @SuppressLint("UseCompatLoadingForDrawables")
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                //Log.d(TAG, "onReceive:"+action);
                if (Intent.ACTION_TIME_CHANGED.equals(action)) {
                    updateTime();
                } else if (MyCmd.BROADCAST_CMD_FROM_MUSIC.equals(action)) {
                    int cmd = intent.getIntExtra(MyCmd.EXTRA_COMMON_CMD, 0);
                    if (cmd == MyCmd.Cmd.MUSIC_SEND_PLAY_STATUS) {
                        int data = intent.getIntExtra(MyCmd.EXTRA_COMMON_DATA, 0);
                        mMusicCurTime = intent.getIntExtra(MyCmd.EXTRA_COMMON_DATA2, -1);
                        mMusicTotalTime = intent.getIntExtra(MyCmd.EXTRA_COMMON_DATA3, -1);
                        Object obj = null;//intent.getExtra (MyCmd.EXTRA_COMMON_OBJECT);
                        Bundle b = intent.getExtras();
                        if (b != null) {
                            obj = b.get(MyCmd.EXTRA_COMMON_OBJECT);
                        }

                        if (mSource != MyCmd.SOURCE_BT_MUSIC) {
                            if (obj != null) {
                                mArtWork = (Bitmap) obj;
                                mArtWork = createReflectedImage(mArtWork, 80);
                                mMusicArt.setImageBitmap(mArtWork);
                                mMusicArt.getDrawable().setDither(true);
                                mMusicArt.setScaleType(ImageView.ScaleType.FIT_CENTER);
                                //mMusicArt.setImageBitmap(mArtWork);
                            } else {
                                mArtWork = null;
                                mMusicArt.setImageDrawable(mContext.getResources().getDrawable(R.drawable.music_pic));
                                mMusicArt.setScaleType(ImageView.ScaleType.FIT_CENTER);
                            }
                        }
                        String name = intent.getStringExtra(MyCmd.EXTRA_COMMON_DATA4);

                        mStrMusicName = name;
                        mMusicPlaying = data;
                        if (mSource == MyCmd.SOURCE_MUSIC) {
                            //Log.d("allen", "music name:"+name);
                            mMusicName.setText(name);
                            setPlayButtonStatus(data == 1);
                            updateMusicTime();
                        }

                        if (data == 1) {
                            if (mSource == MyCmd.SOURCE_MUSIC) {
                                startUpdateMusicTime();
                            }
                        } else {
                            stopUpdateMusicTime();
                        }
                    }

                } else if (action.equals(MyCmd.BROADCAST_CAR_SERVICE_SEND)) {
                    int cmd = intent.getIntExtra(MyCmd.EXTRA_COMMON_CMD, 0);
                    //	Log.d("tt", "cmd:" + cmd);
                    switch (cmd) {
                        case MyCmd.Cmd.MCU_RADIO_RECEIVE_DATA:
                            byte[] buf = intent.getByteArrayExtra(MyCmd.EXTRA_COMMON_DATA);
                            doMcuData(buf);
                            break;

                        case MyCmd.Cmd.SOURCE_CHANGE:
                        case MyCmd.Cmd.RETURN_CURRENT_SOURCE:

                            mSource = intent.getIntExtra(MyCmd.EXTRA_COMMON_DATA, MyCmd.SOURCE_NONE);
                            if (mSource != MyCmd.SOURCE_MUSIC) {
                                stopUpdateMusicTime();
                            }
                            if (cmd == MyCmd.Cmd.RETURN_CURRENT_SOURCE) {
                                if (mSource == MyCmd.SOURCE_RADIO) {
                                    BroadcastUtil.sendToCarServiceMcuRadio(mContext, ProtocolAk47.SEND_RADIO_SUB_QUERY_RADIO_INFO, 0);
                                } else if (mSource == MyCmd.SOURCE_MUSIC) {

                                    Intent i = new Intent(MyCmd.BROADCAST_CMD_TO_MUSIC);
                                    i.putExtra(MyCmd.EXTRA_COMMON_CMD, MyCmd.Cmd.MUSIC_REQUEST_PLAY_STATUS);

                                    mContext.sendBroadcast(i);

                                } else if (mSource == MyCmd.SOURCE_BT_MUSIC) {
                                    Intent it = new Intent(MyCmd.BROADCAST_CMD_LAUNCHER_TO_BT);
                                    it.putExtra(MyCmd.EXTRA_COMMON_CMD, MyCmd.Cmd.BT_REQUEST_A2DP_INFO);

                                    mContext.sendBroadcast(it);
                                }
                            }

                            updateRadioInfo();
                            break;
                        case MyCmd.Cmd.LAUNCHER_SHOW_ALL_APP:
                            mContext.onClickAllAppsButton(null);
                            break;
                    }

                } else if (action.equals(MyCmd.BROADCAST_CMD_FROM_BT)) {
                    doBTCmd(intent);
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_TIME_CHANGED);
        intentFilter.addAction(MyCmd.BROADCAST_CAR_SERVICE_SEND);
        intentFilter.addAction(MyCmd.BROADCAST_CMD_FROM_MUSIC);
        intentFilter.addAction(MyCmd.BROADCAST_CMD_FROM_BT);
        mContext.registerReceiver(mBroadcastReceiver, intentFilter);
    }

    private int mMusicPlaying = 0;

    private Bitmap createReflectedImage(Bitmap paramBitmap, int paramInt) {
        Bitmap localBitmap2 = null;
        try {
            int i = paramBitmap.getWidth();
            int j = paramBitmap.getHeight();
            Matrix localMatrix = new Matrix();
            localMatrix.preScale(1.0F, -1.0F);
            Bitmap localBitmap1 = Bitmap.createBitmap(paramBitmap, 0, (j - paramInt) > 0 ? (j - paramInt) : 0, i, paramInt, localMatrix, false);
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
        } catch (Exception ignored) {
        }
        if (localBitmap2 == null) {
            return paramBitmap;
        }
        return localBitmap2;
    }

    public int mCurPlayIndex = 0;
    public static final int MRD_FM1 = 0;
    public static final int MRD_FM2 = 1;
    public static final int MRD_FM3 = 2;
    public static final int MRD_AM = 3;
    public static final int MRD_AM2 = 3;

    int mFMNum = 1;

    public String getCurBaundText() {
        String s;
        int index = 1;

        if (MachineConfig.VALUE_SYSTEM_UI22_1050.equals(ResourceUtil.mSystemUI)) {
            index = mCurPlayIndex / 6;
            if (mMRDBand >= MRD_AM) {
                s = "AM";

                if (index < 0 || index > 2) {
                    index = 0;
                }
            } else {
                s = "FM";
                if (index < 0 || index > 5) {
                    index = 0;
                }
            }

            s += (index + 1);
        } else {
            if (mMRDBand >= MRD_AM) {
                s = "AM";
                index = mMRDBand - MRD_AM;
            } else {
                s = "FM";
                index = mMRDBand - MRD_FM1;
            }

            if (mFMNum == 3) {
                s += (index + 1);
            }
        }
        return s;
    }
}
