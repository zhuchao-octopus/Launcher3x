package com.my.dispaux;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.android.launcher3.R;
import com.android.launcher3.ResourceUtil;
import com.common.util.AppConfig;
import com.common.util.BroadcastUtil;
import com.common.util.MachineConfig;
import com.common.util.MyCmd;
import com.common.util.ProtocolAk47;
import com.common.util.SystemConfig;
import com.common.util.Util;
import com.my.radio.MarkFaceView;

public class DispAuxActivity extends Activity {
	public static final String TAG = "dispaux";
	public static DispAuxActivity mThis = null;

	// music;
	private View mMusicLayout;
	private TextView mMusicName;
	private TextView mMusicTime;
	private TextView mMusicTime2;
	private ImageView mMusicArt;
	private SeekBar mMusicSeekBar;
	private int mMusicCurTime;
	private int mMusicTotalTime;
	
	//radio
	private TextView mCEDate;
	private TextView mRadioBaud;
	private TextView mRadioFreq;
	private TextView mRadioHz;
	private MarkFaceView mMarkFace;
	
	//gps
	private View mSpeedPoint;
	private TextView mTvSpeed;
	private TextView mTvAtitude;
	private View mBearingPoint;
	private float mSpeed = 0;
	private final static float SPEED_ANGLE_0 = 250.5f;// 0 start //913
	private final static float SPEED_ANGLE_0_KLD007 = 220.0f;// 0 start //kld007
	private final static float SPEED_MID = 120.0f;// 0 start
	private final static float SPEED_MAX = 240.0f;// 0 start	

	
	private static int mSource = MyCmd.SOURCE_NONE;
	private int mMusicPlaying = 0;
	
	private String mStrMusicName = "";
	private String mID3Name = "";
	private String mID3Artist = "";
	private String mID3Album = "";
	
	private Bitmap mArtWork;
	
	private final int A2DP_INFO_INITIAL = 0;
	private final int A2DP_INFO_READY = 1;
	private final int A2DP_INFO_CONNECTING = 2;
	private final int A2DP_INFO_CONNECTED = 3;
	private final int A2DP_INFO_PLAY = 4;
	private final int A2DP_INFO_PAUSED = 5;
	private int mPlayStatus = A2DP_INFO_INITIAL;

	
	private final static int MSG_UPDATE_MUSIC = 1;
	private final static int MSG_UPDATE_TIMECLOCK = 4;
	private final static int MSG_UPDATE_TIMEFLASH = 5;
	Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 0:
				break;
			case MSG_UPDATE_MUSIC:
				mMusicCurTime += 1000;
				updateMusicTime();
				startUpdateMusicTime();
				break;
			case MSG_UPDATE_TIMECLOCK:
				break;
			case MSG_UPDATE_TIMEFLASH:
				break;
			}
			super.handleMessage(msg);
		}
	};
	
	private void initMusicView() {
		mMusicLayout = findViewById(R.id.entry_music);
		mMusicName = (TextView) findViewById(R.id.music_name);
		mMusicTime = (TextView) findViewById(R.id.music_time);
		mMusicTime2 = (TextView) findViewById(R.id.music_time2);
		View v = findViewById(R.id.music_progress);
		if (v != null) {
			mMusicSeekBar = (SeekBar) v;
		}
		mMusicArt = (ImageView) findViewById(R.id.album_art);
	}
	
	private void initRadioView() {
		mRadioBaud = (TextView) findViewById(R.id.radio_music_baud);
		mRadioFreq = (TextView) findViewById(R.id.radio_music_freq);
		mRadioHz = (TextView) findViewById(R.id.radio_music_hz);
		mMarkFace = (MarkFaceView)findViewById(R.id.radio_mark_face_view);
	}
	
	private void initGpsView(){
		mSpeedPoint = findViewById(R.id.speed_point);
		mBearingPoint = findViewById(R.id.bearing_point);
		mTvSpeed = (TextView)findViewById(R.id.speed_text);
		mTvAtitude = (TextView)findViewById(R.id.atitude_text);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Display display = Utils.getDispAuxInfo(this);
		if (display == null) {
			finish();
			return;
		}
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		super.onCreate(savedInstanceState);
		
		if (display.getMode().getPhysicalWidth() == 1280
				&& display.getMode().getPhysicalHeight() == 480) {
			setContentView(R.layout.display_aux_1280x480);
		} else {
			finish();
			return;
		}

		mThis = this;

		initGpsView();
		initMusicView();
		initRadioView();

		setViewVisible(R.id.entry_radio, View.VISIBLE);
		setViewVisible(R.id.entry_music, View.GONE);

		startReceiver();

		updateRadio();
		BroadcastUtil.sendToCarService(this, MyCmd.Cmd.QUERY_CURRENT_SOURCE, 0);
		BroadcastUtil.sendToCarServiceMcuRadio(this, ProtocolAk47.SEND_RADIO_SUB_QUERY_RADIO_INFO, 0);
	}

	@Override
	protected void onDestroy() {
		mThis = null;
		super.onDestroy();
		stopReceiver();

		startDisplayAux(getApplicationContext());
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK
				|| keyCode == KeyEvent.KEYCODE_HOME) {
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	
	

	private void setViewVisible(int id, int visibility) {
		View v = findViewById(id);
		if (v != null) {
			v.setVisibility(visibility);
		}
	}
	
	private void setNumImage(int id, int num) {
		try {
			View v = findViewById(id);
			if (v == null) {
				return;
			}
			((ImageView) v).getDrawable().setLevel(num);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void updateRadioInfo() {
//		 Log.d(TAG, "updateRadioInfo:"+mSource);
		if (mSource == MyCmd.SOURCE_RADIO) {
			findViewById(R.id.radio_info_layout).setVisibility(View.VISIBLE);
			mMarkFace.setEnable(true);
			setViewVisible(R.id.entry_radio, View.VISIBLE);
			setViewVisible(R.id.entry_music, View.GONE);
		} else if(mSource == MyCmd.SOURCE_MUSIC||mSource == MyCmd.SOURCE_BT_MUSIC){
			setViewVisible(R.id.entry_radio, View.GONE);
			setViewVisible(R.id.entry_music, View.VISIBLE);
			mMarkFace.setEnable(false);
		}
		
		if (mSource == MyCmd.SOURCE_MUSIC) {
			setViewVisible(R.id.music_icon, View.GONE);
			mMusicName.setVisibility(View.VISIBLE);
			mMusicTime.setVisibility(View.VISIBLE);
			mMusicArt.setImageDrawable(getResources().getDrawable(R.drawable.music_pic));
			mMusicName.setText(mStrMusicName);
			if(mMusicPlaying != 1){
				mMusicTime.setText("");
				if (mMusicSeekBar != null) {				
					mMusicSeekBar.setProgress(0);
				}
			} else {
				setPlayButtonStatus(true);
				startUpdateMusicTime();
			}
		} else if (mSource == MyCmd.SOURCE_BT_MUSIC) {
			mMusicArt.setImageDrawable(getResources().getDrawable(R.drawable.bt_music_pic));
			if (mPlayStatus < 3) {
				mMusicName.setText("");
				mMusicTime.setText("");
				setViewVisible(R.id.music_icon, View.VISIBLE);
			}else{
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
			mMusicArt.setImageDrawable(getResources().getDrawable(R.drawable.music_pic));
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
			((ImageView) findViewById(R.id.music_button_play))
					.getDrawable().setLevel(1);
		} else {
			((ImageView) findViewById(R.id.music_button_play))
					.getDrawable().setLevel(0);
		}
	}
	
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
	}
	
	//////////////////////////////Music//////////////////////////////
	private final static int MAX_MUSICTIME = 36000000;
	private void updateMusicTime() {
		String time = "";
		if (mMusicCurTime >= 0 && mMusicCurTime < MAX_MUSICTIME
				&& mMusicTotalTime >= 0 && mMusicTotalTime < MAX_MUSICTIME) {

			if (mMusicTime2 != null) {
				time = stringForTime(mMusicTotalTime);
				mMusicTime2.setText(time);
				time = stringForTime(mMusicCurTime);
			} else {
				if (mMusicTotalTime > 0) {
					time = stringForTime(mMusicCurTime) + "/"
							+ stringForTime(mMusicTotalTime);
				} else {
					time = stringForTime(mMusicCurTime);
				}
			}
		}
		mMusicTime.setText(time);
//		Log.d(TAG, "updateMusicTime:" + mMusicSeekBar);
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
		mHandler.sendEmptyMessageDelayed(MSG_UPDATE_MUSIC, 1000);
	}

	private void stopUpdateMusicTime() {
		mHandler.removeMessages(MSG_UPDATE_MUSIC);
	}

	//////////////////////////////A2DP//////////////////////////////////
	private void doBTCmd(Intent intent) {
		int cmd = intent.getIntExtra(MyCmd.EXTRA_COMMON_CMD, 0);
		switch (cmd) {
			case MyCmd.Cmd.BT_SEND_A2DP_STATUS: {
				int play = intent.getIntExtra(MyCmd.EXTRA_COMMON_DATA, 0);
				if (play != mPlayStatus) {
					mPlayStatus = play;
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
			}
			break;
		case MyCmd.Cmd.BT_SEND_TIME_STATUS: {
				mMusicCurTime = intent.getIntExtra(MyCmd.EXTRA_COMMON_DATA, 0) * 1000;
				mMusicTotalTime = intent.getIntExtra(MyCmd.EXTRA_COMMON_DATA2, 0) * 1000;
				if (mSource == MyCmd.SOURCE_BT_MUSIC) {
					updateMusicTime();
				}
			}
		default:
			return;
		}
	}
	
	//////////////////////////////////////GPS////////////////////////////////////////
	private byte mMRDBand;
	private byte mMRDNumber = 0;
	private short mMRDFreqency = 8750;


	private short mFreqMax;
	private short mFreqMin;
	
	private int mCurPlayIndex = 0;

	private final int MRD_FM1 = 0;
	private final int MRD_FM2 = 1;
	private final int MRD_FM3 = 2;
	private final int MRD_AM = 3;
	private final int MRD_AM2 = 3;

	private int mFMNum = 1;

	private String getCurBaundText() {
		String s;
		int index = 1;

		/*if (MachineConfig.VALUE_SYSTEM_UI22_1050.equals(ResourceUtil.mSystemUI)) {
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
		} else */{
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
		} catch (Exception e) {
		}
	}
	
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

	private void updateRadio() {

		if (mMRDBand <= 2) {

			// mRadioFreq.setText(String.format("%d.%d", mMRDFreqency / 100,
			// mMRDFreqency % 100));
			String s = (mMRDFreqency / 100) + "." + ((mMRDFreqency % 100) / 10)
					+ (mMRDFreqency % 10);
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
			MarkFaceView.mFrequencyNum = mMRDFreqency / 100;
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
	

	//////////////////////////////GPS//////////////////////////////
	private LocationManager mLocationManager;
	private void updateSpeed() {
//		Log.d(TAG2, "updateSpeed:" + mSpeed);
		/*if (MachineConfig.VALUE_SYSTEM_UI42_913.equals(Utilities.mSystemUI)) {
			if (mLeftId != R.id.widget_speed) {
				return;
			}
		}*/
		if (mSpeedPoint != null) {
			float rotate = SPEED_ANGLE_0;
			/*if (MachineConfig.VALUE_SYSTEM_UI44_KLD007.equals(Utilities.mSystemUI)) {
				rotate = SPEED_ANGLE_0_KLD007;
			}*/
			float step;
			float angle;
			if (mSpeed < SPEED_MID) {
				angle = 360.0f - SPEED_ANGLE_0;
				step = mSpeed / SPEED_MID;
				rotate += (angle * step);
			} else {
				angle = 360.0f - SPEED_ANGLE_0;
				step = (mSpeed - SPEED_MID) / (SPEED_MAX - SPEED_MID);
				rotate = 360.0f + (angle * step);
			}
			mSpeedPoint.setRotation(rotate);
		}
		
		if (mTvSpeed!=null){
			mTvSpeed.setText(((int)mSpeed)+"");
		}
	}
	
	private LocationListener mLocationListener = new LocationListener() {
		public void onLocationChanged(Location location) {
//			Log.d(TAG, "onLocationChanged:" + location.toString());
			mSpeed = (location.getSpeed() * 3.6f);
			updateSpeed();
			if (mTvAtitude != null) {
				int mAltitude = (int) (location.getAltitude() * 100);
				String s = mAltitude / 100 + "." + mAltitude % 100 + "m";
				mTvAtitude.setText(s);
			}
			if (mBearingPoint != null) {
				mBearingPoint.setRotation(location.getBearing());
			}
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
		}

		public void onProviderEnabled(String provider) {
		}

		public void onProviderDisabled(String provider) {
		}
	};
	
	private void initGPSInfo(){
		if (mLocationManager == null) {
			mLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
			Log.d(TAG, "init gps:" + mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER));
			if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
				if (mLocationListener != null) {
					mLocationManager.removeUpdates((LocationListener) mLocationListener);
				}
				mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0,	mLocationListener);
				Log.d(TAG, "initGPSInfo");
			}
		}
	}

	///////////////////////////////////display aux///////////////////////////////////
	private String prevTopActivity;
	private void onTopActivityChanged(String s){
		if (MachineConfig.getPropertyIntReadOnly(MachineConfig.KEY_DISPAUX_ENABLE) == 1 &&
				s != null && !s.isEmpty()) {
			String pkgName = SystemConfig.getProperty(DispAuxActivity.this, MachineConfig.KEY_GPS_PACKAGE);
			String className = SystemConfig.getProperty(DispAuxActivity.this, MachineConfig.KEY_GPS_CLASS);

			if (prevTopActivity != null && pkgName != null && prevTopActivity.startsWith(pkgName + "/")
					&& s.startsWith("com.android.launcher")) {
				String value = SystemConfig.getProperty(DispAuxActivity.this, MachineConfig.KEY_DISPAUX_APPAUTO_DISABLE);
				final boolean autoDispMapToAux = (value != null && value.equals("1") ? false : true);
				if (autoDispMapToAux && Utils.appIsRunning(this.getApplicationContext(),pkgName, className)) {
					Log.d(TAG, pkgName + "/" + className + " is running, display to aux");
					try {
						ActivityOptions options = ActivityOptions.makeBasic();
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
							options.setLaunchDisplayId(1);
						} Intent intent = new Intent();
						intent.setClassName(pkgName, className);
//						intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						startActivity(intent, options.toBundle());
					} catch (Exception e) {
						Log.e(TAG, "onTopActivityChanged err" + e);
					}

				} else {
					Log.d(TAG, pkgName + "/" + className + " is closed");
				}
			}
		}
		prevTopActivity = s;
	}
	private Bitmap Bytes2Bimap(byte[] b) {
		if (b.length != 0) {
			return BitmapFactory.decodeByteArray(b, 0, b.length);
		} else {
			return null;
		}
	}
	private BroadcastReceiver mReceiver = null;
	private void startReceiver() {
		if (mReceiver == null) {
			mReceiver = new BroadcastReceiver() {
				@SuppressLint("UseCompatLoadingForDrawables")
				@Override
				public void onReceive(Context context, Intent intent) {
					String action = intent.getAction();
//					Log.d(TAG, "onReceive " + action);
					if (action.equals(MyCmd.BROADCAST_ACTIVITY_STATUS)) {
						onTopActivityChanged(intent.getStringExtra(MyCmd.EXTRA_COMMON_CMD));
					} else if (action.equals(MyCmd.BROADCAST_CAR_SERVICE_SEND)) {
						int cmd = intent.getIntExtra(MyCmd.EXTRA_COMMON_CMD, 0);
						if (cmd == MyCmd.Cmd.SOURCE_CHANGE) {
							mSource = intent.getIntExtra(MyCmd.EXTRA_COMMON_DATA, MyCmd.SOURCE_NONE);
							if (mSource != MyCmd.SOURCE_MUSIC) {
								stopUpdateMusicTime();
							}
							updateRadioInfo();
						} else if (cmd == MyCmd.Cmd.MCU_RADIO_RECEIVE_DATA) {
							byte[] buf = intent
									.getByteArrayExtra(MyCmd.EXTRA_COMMON_DATA);
							doMcuData(buf);
						}
					} else if (MyCmd.BROADCAST_CMD_FROM_MUSIC.equals(action)) {
//						Log.d(TAG, "BROADCAST_CMD_FROM_MUSIC:" + mSource);
						if (mSource == MyCmd.SOURCE_MUSIC) {
							int cmd = intent.getIntExtra(MyCmd.EXTRA_COMMON_CMD, 0);
							if (cmd == MyCmd.Cmd.MUSIC_SEND_PLAY_STATUS) {
								int data = intent.getIntExtra(MyCmd.EXTRA_COMMON_DATA, 0);
								mMusicCurTime = intent.getIntExtra(MyCmd.EXTRA_COMMON_DATA2, -1);
								mMusicTotalTime = intent.getIntExtra(MyCmd.EXTRA_COMMON_DATA3, -1);
								///Object obj = intent.getExtra(MyCmd.EXTRA_COMMON_OBJECT);
								byte[] buff = intent.getByteArrayExtra(MyCmd.EXTRA_COMMON_OBJECT);
								if (mSource != MyCmd.SOURCE_BT_MUSIC) {
									if (buff != null) {
										///mArtWork = (Bitmap) obj;
										mArtWork = Bytes2Bimap(buff);//(Bitmap) obj;
										mArtWork = Utils.createReflectedImage(mArtWork, 200);
										mMusicArt.setImageBitmap(mArtWork);
										mMusicArt.getDrawable().setDither(true);
										mMusicArt.setScaleType(ImageView.ScaleType.FIT_CENTER);
									} else {
										mArtWork = null;
										mMusicArt.setImageDrawable(DispAuxActivity.this.getResources().getDrawable(R.drawable.music_pic));
										mMusicArt.setScaleType(ImageView.ScaleType.FIT_CENTER);
									}
								}
								String name = intent.getStringExtra(MyCmd.EXTRA_COMMON_DATA4);
								mStrMusicName = name;
								mMusicPlaying = data;
								if (mSource == MyCmd.SOURCE_MUSIC) {
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
						}
					} else if (action.equals(MyCmd.BROADCAST_CMD_FROM_BT)) {
						doBTCmd(intent);
					}
				}
			};

			IntentFilter iFilter = new IntentFilter();
			iFilter.addAction(MyCmd.BROADCAST_ACTIVITY_STATUS);

			iFilter.addAction(MyCmd.BROADCAST_CAR_SERVICE_SEND);
			iFilter.addAction(MyCmd.BROADCAST_CMD_FROM_MUSIC);
			iFilter.addAction(MyCmd.BROADCAST_CMD_FROM_BT);

			registerReceiver(mReceiver, iFilter);
			Log.d(TAG, "start startReceiver");
		}

		getContentResolver().registerContentObserver(
						Settings.Global.getUriFor(MachineConfig.KEY_DISPAUX_APPAUTO_DISABLE),
						true, mDispAuxAppAutoObserver);
		
		initGPSInfo();
	}

	private void stopReceiver() {
		if (mReceiver != null) {
			unregisterReceiver(mReceiver);
		}
		getContentResolver().unregisterContentObserver(mDispAuxAppAutoObserver);
		
		if (mLocationManager != null && mLocationListener != null) {
			mLocationManager.removeUpdates((LocationListener) mLocationListener);
		}
		Log.d(TAG, "stopReceiver");
	}

	final private ContentObserver mDispAuxAppAutoObserver = new ContentObserver(null) {
		@Override
		public void onChange(boolean selfChange) {
			String value = SystemConfig.getProperty(DispAuxActivity.this, MachineConfig.KEY_DISPAUX_APPAUTO_DISABLE);
			final boolean autoDispMapToAux = (value == null || !value.equals("1"));
			Log.d(TAG, "DispAuxObserver onChange: " + autoDispMapToAux);
			if (!autoDispMapToAux) {
				String pkgName = SystemConfig.getProperty(DispAuxActivity.this, MachineConfig.KEY_GPS_PACKAGE);
				String className = SystemConfig.getProperty(DispAuxActivity.this, MachineConfig.KEY_GPS_CLASS);
				String auxTop = AppConfig.getTopActivity(1);
				if (auxTop != null && auxTop.startsWith(pkgName + "/")) {
					try {
						ActivityOptions options = ActivityOptions.makeBasic();
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
							options.setLaunchDisplayId(0);
						} Intent intent = new Intent();
						intent.setClassName(pkgName, className);
						startActivity(intent, options.toBundle());
//						Log.d("allen", "map to DEFAULT_DISPLAY ok");
					} catch (Exception e) {
						Log.e(TAG, "map to DEFAULT_DISPLAY failed: " + e);
					}
				}
			}
		}
	};

	public static void startDisplayAux(Context context) {
		if (Util.isAndroidQ() && (Util.isPX30() || Util.isPX6()) &&
			MachineConfig.getPropertyIntReadOnly(MachineConfig.KEY_DISPAUX_ENABLE) == 1 &&
			Utils.getDispAuxInfo(context) != null) {
			try {
				ActivityOptions options = ActivityOptions.makeBasic();
				options.setLaunchDisplayId(1);
				Intent intent = new Intent("com.car.dispaux.DispAuxActivity");
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				context.startActivity(intent, options.toBundle());
			} catch (Exception e) {
				Log.e(TAG, "l3 startDisplayAux err: " + e);
			}
		}
	}
}
