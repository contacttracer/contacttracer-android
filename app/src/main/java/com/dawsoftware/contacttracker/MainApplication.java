package com.dawsoftware.contacttracker;

import android.app.Activity;
import android.app.Application;
import android.app.Application.ActivityLifecycleCallbacks;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;

import com.dawsoftware.contacttracker.analytics.Analytics;
import com.dawsoftware.contacttracker.util.PreferencesUtil;
import com.facebook.FacebookSdk;
import com.facebook.LoggingBehavior;

import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class MainApplication extends Application implements ActivityLifecycleCallbacks {
	
	private static final String SECURE_SETTINGS_BLUETOOTH_ADDRESS = "bluetooth_address";
	
	private int activityReferences = 0;
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		Analytics.getInstance(this);
		
		registerActivityLifecycleCallbacks(this);
		
		initBluetooth();
		initFB();
	}
	
	private void initBluetooth() {
		if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) {
			final PreferencesUtil prefs = new PreferencesUtil(this);
			
			String macAddress = Settings.Secure.getString(getContentResolver(), SECURE_SETTINGS_BLUETOOTH_ADDRESS);
			
			if (macAddress == null) {
				Log.i("wutt", "UNABLE TO GET BT MAC");
			}
			
			prefs.setBtMac(macAddress);
		}
	}
	
	private void initFB() {
		FacebookSdk.setAutoLogAppEventsEnabled(true);
		
		if (BuildConfig.DEBUG) {
			FacebookSdk.setIsDebugEnabled(true);
			FacebookSdk.addLoggingBehavior(LoggingBehavior.APP_EVENTS);
		}
	}
	
	/**
	 * Переключение локали в ручном режиме
	 * @param lang код локали
	 */
	public void setLocale(String lang) {
		Locale myLocale;
		if (lang.equals("zh_CN")) {
			myLocale = Locale.SIMPLIFIED_CHINESE;
		} else if (lang.equals("zh_TW")) {
			myLocale = Locale.TRADITIONAL_CHINESE;
		} else {
			myLocale = new Locale(lang);
		}
		
		Locale.setDefault(myLocale);
		Resources res = getResources();
		DisplayMetrics dm = res.getDisplayMetrics();
		Configuration conf = res.getConfiguration();
		conf.locale = myLocale;
		res.updateConfiguration(conf, dm);
	}
	
	@Override
	public void onActivityStarted(@NonNull final Activity activity) {
		if (++activityReferences == 1) {
			Analytics.appForeground();
		}
	}
	
	@Override
	public void onActivityStopped(@NonNull final Activity activity) {
		if (--activityReferences == 0) {
			Analytics.appBackground();
		}
	}
	
	@Override
	public void onActivityCreated(@NonNull final Activity activity, @Nullable final Bundle savedInstanceState) { }
	
	@Override
	public void onActivityResumed(@NonNull final Activity activity) { }
	
	@Override
	public void onActivityPaused(@NonNull final Activity activity) { }
	
	@Override
	public void onActivitySaveInstanceState(@NonNull final Activity activity, @NonNull final Bundle outState) { }
	
	@Override
	public void onActivityDestroyed(@NonNull final Activity activity) { }
}
