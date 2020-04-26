package com.dawsoftware.contacttracker.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import com.dawsoftware.contacttracker.data.network.models.UserData;
import com.dawsoftware.contacttracker.services.AlarmReceiver;

import static com.dawsoftware.contacttracker.services.LocationService.BUNDLE_FROM_NOTIFICATION;
import static com.dawsoftware.contacttracker.services.LocationService.BUNDLE_NOTIFICATION_SURVEY;

public class TimerUtil {
	
	public static final String TIMER_TYPE_INTENT_EXTRA = "TIMER_TYPE_INTENT_EXTRA";
	public static final int MORNING_TIMER_ID = 666660;
	public static final int EVENING_TIMER_ID = 666661;
	
	private TimerUtil() { }
	
	public static void refreshSurveyNotificationTimers(final UserData data, final Context context) {
		if (data == null) {
			return;
		}

		final String[] timers = data.checkupTime;
		if (timers == null || timers.length < 2) {
			setDefaultNotificationTime(context);
			return;
		}

		final SharedPreferences sharedPref = PreferencesUtil.getAppDefaultPrefs(context);
		final SharedPreferences.Editor editor = sharedPref.edit();

		String morningDateStr = sharedPref.getString(PreferencesUtil.APP_DEFAULT_PREFS_MORNING_TIMER, "");
		String eveningDateStr = sharedPref.getString(PreferencesUtil.APP_DEFAULT_PREFS_EVENING_TIMER, "");

		if (StringUtil.isEmpty(morningDateStr) || StringUtil.isEmpty(eveningDateStr)) {
			editor.putString(PreferencesUtil.APP_DEFAULT_PREFS_MORNING_TIMER, PreferencesUtil.APP_DEFAULT_PREFS_MORNING_TIMER_DEF_VAL);
			editor.putString(PreferencesUtil.APP_DEFAULT_PREFS_EVENING_TIMER, PreferencesUtil.APP_DEFAULT_PREFS_EVENING_TIMER_DEF_VAL);
			editor.commit();

			morningDateStr = PreferencesUtil.APP_DEFAULT_PREFS_MORNING_TIMER_DEF_VAL;
			eveningDateStr = PreferencesUtil.APP_DEFAULT_PREFS_EVENING_TIMER_DEF_VAL;

			setDefaultNotificationTime(context);
		}

		if (morningDateStr.equals(timers[0]) && eveningDateStr.equals(timers[1])) {
			return;
		}

		editor.putString(PreferencesUtil.APP_DEFAULT_PREFS_MORNING_TIMER, timers[0]);
		editor.putString(PreferencesUtil.APP_DEFAULT_PREFS_EVENING_TIMER, timers[1]);
		editor.commit();

		ArrayList<Date> timersMs = new ArrayList<>(timers.length);

		for (int i = 0; i < timers.length; i ++) {
			if (!StringUtil.isEmpty(timers[i])) {
				final Date convertedTime = timeConverter(timers[i]);
				if (convertedTime != null) {
					timersMs.add(convertedTime);
				}
			}
		}

		setNotificationTimers(timersMs, context);
		
//		setDefaultNotificationTime(context);
	}
	
	public static Date timeConverter(final String date) {
		final SimpleDateFormat f = new SimpleDateFormat("hh:mm");
		Date result = null;
		
		try {
			result = f.parse(date);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	public static void setDefaultNotificationTime(final Context context) {
		final ArrayList<Date> defaultTimers = new ArrayList<>(2);
		defaultTimers.add(timeConverter(PreferencesUtil.APP_DEFAULT_PREFS_MORNING_TIMER_DEF_VAL));
		defaultTimers.add(timeConverter(PreferencesUtil.APP_DEFAULT_PREFS_EVENING_TIMER_DEF_VAL));
		
		setNotificationTimers(defaultTimers, context);
	}
	
	public static void setNotificationTimers(final ArrayList<Date> timers, final Context context) {
		if (context == null) {
			return;
		}
		
		for (int i = 0; i < 2; i++) {
			
			final Calendar timerCalendar = Calendar.getInstance();
			timerCalendar.setTime(timers.get(i));
			
			int hours = timerCalendar.get(Calendar.HOUR_OF_DAY);
			int minutes = timerCalendar.get(Calendar.MINUTE);
			
			final Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(System.currentTimeMillis());
			calendar.set(Calendar.HOUR_OF_DAY, hours);
			calendar.set(Calendar.MINUTE, minutes);
			calendar.set(Calendar.SECOND, 1);
			
			if (calendar.before(Calendar.getInstance())) {
				calendar.add(Calendar.DATE, 1);
			}
			
			int id = i == 0 ? MORNING_TIMER_ID : EVENING_TIMER_ID;
			
			Intent alarmIntent = new Intent(context, AlarmReceiver.class);
			alarmIntent.putExtra(TIMER_TYPE_INTENT_EXTRA, id);
			alarmIntent.putExtra(BUNDLE_FROM_NOTIFICATION, BUNDLE_NOTIFICATION_SURVEY);
			
			PendingIntent pendingIntent = PendingIntent.getBroadcast(context, id, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
			
			AlarmManager am = (AlarmManager) context.getApplicationContext().getSystemService(Context.ALARM_SERVICE);
			if (am != null && pendingIntent != null) {
				
				am.cancel(pendingIntent);
				
				Log.i("wutt", "set time: " + calendar.getTime());
				
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
					am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
				} else {
					am.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);
				}
			}
		}
	}
}
