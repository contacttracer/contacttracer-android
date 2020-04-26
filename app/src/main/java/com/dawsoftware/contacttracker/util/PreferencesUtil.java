package com.dawsoftware.contacttracker.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Objects;

import androidx.annotation.NonNull;

public class PreferencesUtil {
	private static final String COMMON_PREFERENCES_NAME = "prefs_default";
	
	private static final String COMMON_PREFERENCES_INSTALLATION_ID = "INSTALLATION_ID";
	private static final String COMMON_PREFERENCES_USER_ID = "USER_ID";
	private static final String COMMON_PREFERENCES_FIRST_LAUNCH = "FIRST_LAUNCH";
	private static final String COMMON_PREFERENCES_BT_MAC = "COMMON_PREFERENCES_BT_MAC";
	private static final String COMMON_PREFERENCES_PUSH_TOKEN = "COMMON_PREFERENCES_PUSH_TOKEN";
	private static final String COMMON_PREFERENCES_PUSH_TOKEN_SENT = "COMMON_PREFERENCES_PUSH_TOKEN_SENT";
	
	public static final String APP_DEFAULT_PREFS_MORNING_TIMER = "APP_DEFAULT_PREFS_MORNING_TIMER";
	public static final String APP_DEFAULT_PREFS_EVENING_TIMER = "APP_DEFAULT_PREFS_EVENING_TIMER";
	public static final String APP_DEFAULT_PREFS_MORNING_TIMER_DEF_VAL = "08:00";
	public static final String APP_DEFAULT_PREFS_EVENING_TIMER_DEF_VAL = "20:00";
	
	private Context mContext;
	
	private SharedPreferences commonPreferences;
	private SharedPreferences.Editor commonPreferencesEditor;
	
	public PreferencesUtil(@NonNull final Context applicationContext) {
		mContext = applicationContext;
		
		commonPreferences = mContext.getSharedPreferences(COMMON_PREFERENCES_NAME, Context.MODE_PRIVATE);
		commonPreferencesEditor = commonPreferences.edit();
	}
	
	public static SharedPreferences getAppDefaultPrefs(final Context context) {
		return PreferenceManager.getDefaultSharedPreferences(Objects.requireNonNull(context));
	}
	
	private SharedPreferences.Editor getEditor() {
		return commonPreferencesEditor;
	}

	public String getInstallationId() {
		return commonPreferences.getString(COMMON_PREFERENCES_INSTALLATION_ID, "");
	}
	
	public void setInstallationId(final String uuid) {
		synchronized (PreferencesUtil.class) {
			getEditor().putString(COMMON_PREFERENCES_INSTALLATION_ID, uuid);
			getEditor().commit();
		}
	}
	
	public String getUserId() {
		return commonPreferences.getString(COMMON_PREFERENCES_USER_ID, "");
	}
	
	public void setUserId(final String uuid) {
		synchronized (PreferencesUtil.class) {
			getEditor().putString(COMMON_PREFERENCES_USER_ID, uuid);
			getEditor().commit();
		}
	}
	
	public boolean getIsFirstLaunch() {
		return commonPreferences.getBoolean(COMMON_PREFERENCES_FIRST_LAUNCH, true);
	}
	
	public void setIsFirstLaunch(final boolean isFirstLaunch) {
		synchronized (PreferencesUtil.class) {
			getEditor().putBoolean(COMMON_PREFERENCES_FIRST_LAUNCH, isFirstLaunch);
			getEditor().commit();
		}
	}
	
	public String getBtMac() {
		return commonPreferences.getString(COMMON_PREFERENCES_BT_MAC, null);
	}
	
	public void setBtMac(final String btMac) {
		synchronized (PreferencesUtil.class) {
			getEditor().putString(COMMON_PREFERENCES_BT_MAC, btMac);
			getEditor().commit();
		}
	}
	
	public String getPushToken() {
		return commonPreferences.getString(COMMON_PREFERENCES_PUSH_TOKEN, "");
	}
	
	public void setPushToken(final String token) {
		synchronized (PreferencesUtil.class) {
			getEditor().putString(COMMON_PREFERENCES_PUSH_TOKEN, token);
			getEditor().commit();
		}
	}
	
	public boolean getPushTokenSent() {
		return commonPreferences.getBoolean(COMMON_PREFERENCES_PUSH_TOKEN_SENT, false);
	}
	
	public void setPushTokenSent(final boolean isSent) {
		synchronized (PreferencesUtil.class) {
			getEditor().putBoolean(COMMON_PREFERENCES_PUSH_TOKEN_SENT, isSent);
			getEditor().commit();
		}
	}
}
