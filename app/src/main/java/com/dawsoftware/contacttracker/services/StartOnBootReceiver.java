package com.dawsoftware.contacttracker.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import java.util.Calendar;
import java.util.Date;

import com.dawsoftware.contacttracker.util.PreferencesUtil;
import com.dawsoftware.contacttracker.util.TimerUtil;

public class StartOnBootReceiver extends BroadcastReceiver {
	
	@Override
	public void onReceive(final Context context, final Intent intent) {
		startService(context);
		launchNotificationTimer(context);
	}
	
	private void startService(final Context context) {
		final Intent startIntent = new Intent(context, LocationService.class);
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			context.startForegroundService(startIntent);
		} else {
			context.startService(startIntent);
		}
	}
	
	private void launchNotificationTimer(final Context context) {
		Intent alarmIntent = new Intent(context, AlarmReceiver.class);
		PendingIntent morningIntent = PendingIntent.getBroadcast(context, TimerUtil.MORNING_TIMER_ID, alarmIntent, 0);
		PendingIntent eveningIntent = PendingIntent.getBroadcast(context, TimerUtil.EVENING_TIMER_ID, alarmIntent, 0);
		
		final AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		final SharedPreferences sharedPref = PreferencesUtil.getAppDefaultPrefs(context);
		
		String morningDateStr = sharedPref.getString(PreferencesUtil.APP_DEFAULT_PREFS_MORNING_TIMER,
		                                             PreferencesUtil.APP_DEFAULT_PREFS_MORNING_TIMER_DEF_VAL);
		
		Date morningDate = TimerUtil.timeConverter(morningDateStr);
		
		Calendar morningHelpCalendar = Calendar.getInstance();
		morningHelpCalendar.setTime(morningDate);
		
		int morningHours = morningHelpCalendar.get(Calendar.HOUR_OF_DAY);
		int morningMinutes = morningHelpCalendar.get(Calendar.MINUTE);
		
		String eveningDateStr = sharedPref.getString(PreferencesUtil.APP_DEFAULT_PREFS_EVENING_TIMER,
		                                             PreferencesUtil.APP_DEFAULT_PREFS_EVENING_TIMER_DEF_VAL);
		
		Date eveningDate = TimerUtil.timeConverter(eveningDateStr);
		
		Calendar eveningHelpCalendar = Calendar.getInstance();
		eveningHelpCalendar.setTime(eveningDate);
		
		int eveningHours = eveningHelpCalendar.get(Calendar.HOUR_OF_DAY);
		int eveningMinutes = eveningHelpCalendar.get(Calendar.MINUTE);
		
		Calendar morningCal = Calendar.getInstance();
		morningCal.setTimeInMillis(System.currentTimeMillis());
		morningCal.set(Calendar.HOUR_OF_DAY, morningHours);
		morningCal.set(Calendar.MINUTE, morningMinutes);
		morningCal.set(Calendar.SECOND, 1);
		
		if (morningCal.before(Calendar.getInstance()) || morningCal.equals(Calendar.getInstance())) {
			morningCal.add(Calendar.DATE, 1);
		}
		
		Calendar eveningCal = Calendar.getInstance();
		eveningCal.setTimeInMillis(System.currentTimeMillis());
		eveningCal.set(Calendar.HOUR_OF_DAY, eveningHours);
		eveningCal.set(Calendar.MINUTE, eveningMinutes);
		eveningCal.set(Calendar.SECOND, 1);
		
		if (eveningCal.before(Calendar.getInstance()) || eveningCal.equals(Calendar.getInstance())) {
			eveningCal.add(Calendar.DATE, 1);
		}
		
		if (manager != null) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, morningCal.getTimeInMillis(), morningIntent);
				Log.i("wutt", "set time boot: " + morningCal.getTime());
				manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, eveningCal.getTimeInMillis(), eveningIntent);
				Log.i("wutt", "set time boot: " + eveningCal.getTime());
			} else {
				manager.setRepeating(AlarmManager.RTC_WAKEUP, morningCal.getTimeInMillis(), AlarmManager.INTERVAL_DAY, morningIntent);
				Log.i("wutt", "set time boot: " + morningCal.getTime());
				manager.setRepeating(AlarmManager.RTC_WAKEUP, eveningCal.getTimeInMillis(), AlarmManager.INTERVAL_DAY, eveningIntent);
				Log.i("wutt", "set time boot: " + eveningCal.getTime());
			}
		}
	}
}
