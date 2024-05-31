package com.android.launcher3;

import java.util.List;
import java.util.Locale;

import com.android.launcher3.ResourceUtil;
import com.android.launcher3.Utilities;
import com.common.util.MachineConfig;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

public class FixScreenShortcut{
	
	public static void init() {		
		//init
		if (MachineConfig.VALUE_SYSTEM_UI28_7451.equals(ResourceUtil.mSystemUI)){
			mFixScreenShortcutData = mFixScreenShortcutData_7451;	//for test	
		}
	}

	public static class FixScreenShortcutData {
		String pkgName;
		String className;
		int screen;
		int screen_x;
		int screen_y;
		
		FixScreenShortcutData(String pkgName, String className, int screen, int screen_x, int screen_y) {
			this.pkgName = pkgName;
			this.className = className;
			this.screen = screen;
			this.screen_x = screen_x;
			this.screen_y = screen_y;
		}
	}
	
	
	public static  FixScreenShortcutData[] mFixScreenShortcutData_TEST = new FixScreenShortcutData[] {
//			new FixScreenShortcutData("com.microntek.worship", "com.microntek.worship.WorshipActivity", "3", "0", "1"),
			new FixScreenShortcutData("com.example.test2", "com.example.test2.MainActivity", 0, 2, 1)};
	
	public static  FixScreenShortcutData[] mFixScreenShortcutData_7451 = new FixScreenShortcutData[] {
	new FixScreenShortcutData("com.suding.speedplay", "com.suding.speedplay.ui.MainActivity", 0, 2, 1)};
	
	public static  FixScreenShortcutData[] mFixScreenShortcutData = null;
	
	public static boolean getFixScreenShortcutData(ComponentName cn,
			ShortcutInfo si) {
		if (mFixScreenShortcutData != null && cn != null) {
			for (FixScreenShortcutData data : mFixScreenShortcutData) {
				if (data.pkgName.equals(cn.getPackageName()) && data.className.equals(cn.getClassName())){
					si.cellX = data.screen_x;
					si.cellY = data.screen_y;
					si.screenId = data.screen;
					return true;
				}
			}
		}
		return false;
	}
	public static CharSequence getAppAlternativeTitle(Context context, ComponentName cn, CharSequence title) {
		if (context == null || cn == null || title == null)
			return title;

		String pkgName = cn.getPackageName();
		String className = cn.getClassName();
		if (pkgName != null && className != null &&
			pkgName.equals("com.car.ui") &&
			className.endsWith(".FrontCameraActivity")) {
			if (com.common.util.MachineConfig.VALUE_SYSTEM_UI28_7451.equals(ResourceUtil.mSystemUI))
				return context.getResources().getString(R.string.f_camera_alternative_7451);
			if (Launcher.mFcameraType == 1){
				return context.getResources().getString(R.string.f_camera_type_1);
			}
			
			
//			else if (Utilities.mSystemUI.equals(com.common.util.MachineConfig.VALUE_SYSTEM_UI22_1050))
//				return context.getResources().getString(R.string.f_camera_alternative_1050);
		} else if (
				"com.google.android.maps.MapsActivity".equals(className)) {
			//for 619 custom
			String locale = Locale.getDefault().getLanguage();
//			Log.d("abcde", "google map locale:"+locale);
			if (locale != null) {
				if (locale.equals("es")) {
					return "Mapas";
				} else if (locale.equals("ru")) {
					return "Карты";
				} 		
				
			}
			
			
			
		} else if (
				"com.estrongs.android.pop.view.FileExplorerActivity".equals(className)) {
			//for 619 custom
			String locale = Locale.getDefault().getLanguage();
//			Log.d("abcde", "google map locale:"+locale);
			if (locale != null) {
				if (locale.equals("es")) {
					return "Explorador de archivos";
				} 		
				
			}
			
			
			
		} else if ("com.eqset".equals(pkgName)) {
//			Log.d("abcde", "eqset map locale:"+Utilities.mIsDSP);
			if (Utilities.mIsDSP){
				return "DSP";
			}
		}  else if (
				"net.easyconn.WelcomeActivity".equals(className)) {
			//for 1680 custom
			String locale = Locale.getDefault().getLanguage();
			if (locale != null) {
				if (locale.equals("tr")) {
					return "Kolay Bağlantı";
				} 		
				
			}
			
		} 
//		Log.d("abcde", "1111111111:"+pkgName);
		return title;
	}
}
