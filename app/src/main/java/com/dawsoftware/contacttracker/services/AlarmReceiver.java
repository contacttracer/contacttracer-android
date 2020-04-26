package com.dawsoftware.contacttracker.services;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.util.Log;

import com.dawsoftware.contacttracker.MainActivity;
import com.dawsoftware.contacttracker.R;
import com.dawsoftware.contacttracker.util.PreferencesUtil;
import com.dawsoftware.contacttracker.util.TimerUtil;

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import androidx.core.app.NotificationCompat;

import static com.dawsoftware.contacttracker.services.LocationService.BUNDLE_FROM_NOTIFICATION;
import static com.dawsoftware.contacttracker.services.LocationService.BUNDLE_NOTIFICATION_SURVEY;
import static com.dawsoftware.contacttracker.services.LocationService.SURVEY_NOTIFICATION_CHANNEL_ID;
import static com.dawsoftware.contacttracker.util.TimerUtil.EVENING_TIMER_ID;
import static com.dawsoftware.contacttracker.util.TimerUtil.MORNING_TIMER_ID;

public class AlarmReceiver extends BroadcastReceiver {
	
	public AlarmReceiver() { }
	
	@Override
	public void onReceive(final Context context, final Intent intent) {
		
		final SharedPreferences sharedPref = PreferencesUtil.getAppDefaultPrefs(context);
		
		NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		
		Intent notificationIntent = new Intent(context, MainActivity.class);
		notificationIntent.putExtra(BUNDLE_FROM_NOTIFICATION, BUNDLE_NOTIFICATION_SURVEY);
		
		PendingIntent pendingI = PendingIntent.getActivity(context, UUID.randomUUID().hashCode(), notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
			NotificationChannel channel = new NotificationChannel(SURVEY_NOTIFICATION_CHANNEL_ID,
			                                                      context.getResources().getString(R.string.survey_button),
			                                                      NotificationManager.IMPORTANCE_HIGH);
			channel.setDescription(context.getResources().getString(R.string.survey_button));
			channel.setShowBadge(true);
			channel.setImportance(NotificationManager.IMPORTANCE_HIGH);
			
			if (nm != null) {
				nm.createNotificationChannel(channel);
			}
		}
		
		NotificationCompat.Builder b = new NotificationCompat.Builder(context, SURVEY_NOTIFICATION_CHANNEL_ID);
		
		b.setAutoCancel(true)
		 .setDefaults(NotificationCompat.DEFAULT_ALL)
		 .setWhen(System.currentTimeMillis())
		 .setSmallIcon(R.drawable.ic_launcher_foreground)
		 .setContentTitle(context.getResources().getString(R.string.notification_take_survey_title))
		 .setContentText(context.getResources().getString(R.string.notification_take_survey_text))
		 .setContentIntent(pendingI)
		 .setPriority(Notification.PRIORITY_HIGH)
		 .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
		
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
			b.setChannelId(SURVEY_NOTIFICATION_CHANNEL_ID);
		}
		
		if (nm != null) {
			nm.notify(1, b.build());
			
			Calendar nextNotifyTime;
			
			int id = intent.getIntExtra(TimerUtil.TIMER_TYPE_INTENT_EXTRA, -1);
			
			switch (id) {
				case MORNING_TIMER_ID: {
					
					String morningDateStr = sharedPref.getString(PreferencesUtil.APP_DEFAULT_PREFS_MORNING_TIMER,
					                                             PreferencesUtil.APP_DEFAULT_PREFS_MORNING_TIMER_DEF_VAL);
					
					Date morningDate = TimerUtil.timeConverter(morningDateStr);
					
					Calendar morningHelpCalendar = Calendar.getInstance();
					morningHelpCalendar.setTime(morningDate);
					
					int morningHours = morningHelpCalendar.get(Calendar.HOUR_OF_DAY);
					int morningMinutes = morningHelpCalendar.get(Calendar.MINUTE);
					
					Calendar morningCal = Calendar.getInstance();
					morningCal.setTimeInMillis(System.currentTimeMillis());
					morningCal.set(Calendar.HOUR_OF_DAY, morningHours);
					morningCal.set(Calendar.MINUTE, morningMinutes);
					morningCal.set(Calendar.SECOND, 1);
					
					if (morningCal.before(Calendar.getInstance()) || morningCal.equals(Calendar.getInstance())) {
						morningCal.add(Calendar.DATE, 1);
					}
					
					nextNotifyTime = morningCal;
					
					break;
				}
				default:
				case EVENING_TIMER_ID: {
					
					String eveningDateStr = sharedPref.getString(PreferencesUtil.APP_DEFAULT_PREFS_EVENING_TIMER,
					                                             PreferencesUtil.APP_DEFAULT_PREFS_EVENING_TIMER_DEF_VAL);
					
					Date eveningDate = TimerUtil.timeConverter(eveningDateStr);
					
					Calendar eveningHelpCalendar = Calendar.getInstance();
					eveningHelpCalendar.setTime(eveningDate);
					
					int eveningHours = eveningHelpCalendar.get(Calendar.HOUR_OF_DAY);
					int eveningMinutes = eveningHelpCalendar.get(Calendar.MINUTE);
					
					Calendar eveningCal = Calendar.getInstance();
					eveningCal.setTimeInMillis(System.currentTimeMillis());
					eveningCal.set(Calendar.HOUR_OF_DAY, eveningHours);
					eveningCal.set(Calendar.MINUTE, eveningMinutes);
					eveningCal.set(Calendar.SECOND, 1);
					
					if (eveningCal.before(Calendar.getInstance()) || eveningCal.equals(Calendar.getInstance())) {
						eveningCal.add(Calendar.DATE, 1);
					}
					
					nextNotifyTime = eveningCal;
					
					break;
				}
			}
			
			final AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
			
			if (am != null) {
				Intent alarmIntent = new Intent(context, AlarmReceiver.class);
				
				PendingIntent pendingIntent = PendingIntent.getBroadcast(context, id, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);

				Log.i("wutt", "set time receiver: " + nextNotifyTime.getTime());
				
				if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
					am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextNotifyTime.getTimeInMillis(), pendingIntent);
				} else {
					am.setExact(AlarmManager.RTC_WAKEUP, nextNotifyTime.getTimeInMillis(), pendingIntent);
				}
			}
		}
	}
}
