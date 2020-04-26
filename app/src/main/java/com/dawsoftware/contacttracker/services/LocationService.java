package com.dawsoftware.contacttracker.services;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;

import java.util.Calendar;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.dawsoftware.contacttracker.MainActivity;
import com.dawsoftware.contacttracker.R;
import com.dawsoftware.contacttracker.analytics.Analytics;
import com.dawsoftware.contacttracker.data.Repository;
import com.dawsoftware.contacttracker.data.Repository.SendPushTokenCallback;
import com.dawsoftware.contacttracker.data.Repository.SendSurroundingCallback;
import com.dawsoftware.contacttracker.data.network.models.UserData;
import com.dawsoftware.contacttracker.util.BluetoothUtil;
import com.dawsoftware.contacttracker.util.DrawableUtil;
import com.dawsoftware.contacttracker.util.FirebaseUtil;
import com.dawsoftware.contacttracker.util.FirebaseUtil.GetFirebaseTokenCallback;
import com.dawsoftware.contacttracker.util.LocationUtil;
import com.dawsoftware.contacttracker.util.PermissionUtil;
import com.dawsoftware.contacttracker.util.PreferencesUtil;
import com.dawsoftware.contacttracker.util.StringUtil;

import static com.dawsoftware.contacttracker.data.Repository.BUNDLE_SEND_SUCCESS;


public class LocationService extends Service {
	
	private static final int NOTIFICATION_ID = 123666667;
	private static final int NOTIFICATION_ID_CRITICAL = 123666668;
	
	private static final int TIMER_DEFAULT_TIME_MS = 60 * 1000;
	private static final int COLLECTING_TIME_MS = 15 * 1000;
	
	public static final int MESSAGE_STOP_TRACKING = 321000001;
	public static final int MESSAGE_BLUETOOTH_UNAVAILABLE = 321000002;
	public static final int MESSAGE_BLUETOOTH_AVAILABLE = 321000003;
	
	public final static String STATUS_HEALTHY = "healthy";
	public final static String STATUS_INFECTED = "possible_infected";
	public final static String STATUS_SICK = "infected";
	
	public static final String MAIN_NOTIFICATION_CHANNEL_ID = "COM_DAWSOFTWARE_MAIN_NOTIFICATION_CHANNEL";
	public static final String CRITICAL_NOTIFICATION_CHANNEL_ID = "COM_DAWSOFTWARE_CRITICAL_NOTIFICATION_CHANNEL_ID";
	public static final String SURVEY_NOTIFICATION_CHANNEL_ID = "COM_DAWSOFTWARE_SURVEY_NOTIFICATION_CHANNEL";
	
	public static final String NOTIFICATION_APP_GROUP_ID = "COM_DAWSOFTWARE_NOTIFICATION_APP_GROUP_ID";
	
	public static final String WAKEUP_INTENT = "com.dawsoftware.contacttracer.LocationService.WAKEUP_INTENT";
	public static final String PUSH_INTENT = "com.dawsoftware.contacttracer.LocationService.PUSH_INTENT";
	
	public static final String ACTION_UPDATE_UI = "com.dawsoftware.contacttracer.LocationService.UpdateUI";
	public static final String ACTION_GRANT_PERMISSIONS = "com.dawsoftware.contacttracer.LocationService.GrantPermissions";
	public static final String ACTION_REFRESH_DATA = "com.dawsoftware.contacttracer.LocationService.RefreshData";
	public static final String CROWD_DATA = "com.dawsoftware.contacttracer.LocationService.CrowdData";
	
	public static final String PUSH_MESSAGE_TYPE_BUNDLE_EXTRA = "PUSH_MESSAGE_TYPE_BUNDLE_EXTRA";
	public static final String PUSH_MESSAGE_STATUS_BUNDLE_EXTRA = "PUSH_MESSAGE_STATUS_BUNDLE_EXTRA";
	public static final String PUSH_MESSAGE_24H_COUNT_EXTRA = "PUSH_MESSAGE_24H_COUNT_EXTRA";
	public static final String PUSH_MESSAGE_IS_ACTIVE_EXTRA = "PUSH_MESSAGE_IS_ACTIVE_EXTRA";
	
	public static final String PUSH_MESSAGE_TYPE_WAKEUP = "wakeup";
	public static final String PUSH_MESSAGE_TYPE_INTERSECTION = "intersection";
	
	public static final String BUNDLE_CROWD_COUNTER = "BUNDLE_CROWD_COUNTER";
	
	public static final String BUNDLE_FROM_NOTIFICATION = "BUNDLE_FROM_NOTIFICATION";
	public static final String BUNDLE_NOTIFICATION_REGULAR = "BUNDLE_NOTIFICATION_REGULAR";
	public static final String BUNDLE_NOTIFICATION_SURVEY = "BUNDLE_NOTIFICATION_SURVEY";
	public static final String BUNDLE_NOTIFICATION_INTERSECTION = "BUNDLE_NOTIFICATION_INTERSECTION";
	public static final String BUNDLE_NOTIFICATION_REMINDER = "BUNDLE_NOTIFICATION_REMINDER";
	
	private final LocationServiceBinder binder = new LocationServiceBinder();
	private LocalBroadcastManager localBroadcastManager;
	
	private HandlerThread thread;
	private Looper serviceLooper;
	public Handler serviceHandler;
	
	private Repository repository;
	
	private PreferencesUtil prefs;
	
	private UserData lastKnownUserData;
	
	private NotificationChannel notificationChannel;
	private NotificationChannel notificationChannelCritical;
	private NotificationManager notificationManager;
	
	private AlarmManager selfWakeupAlarmManager;
	
	private LocationDataCollector locationDataCollector;
	
	private volatile boolean isTracking;
	private volatile boolean isLockedFromUI;
	
	private String intersectionPushTitle;
	private String intersectionPushText;
	
	private String wakeupPushTitle;
	private String wakeupPushText;
	
	private String notActivePushTitle;
	private String notActivePushText;
	
	@Override
	public IBinder onBind(Intent intent) {
		Log.i("wutt", "service binded");
		return binder;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i("wutt", "service command started");
		
		serviceHandler.post(() -> {
			if (intent != null) {
				handleIntent(intent);
			}
		});
		
		return START_STICKY;
	}
	
	@Override
	public void onCreate() {
		
		Log.i("wutt", "service onCreate");
		
		thread = new HandlerThread("LocationServiceThread", Process.THREAD_PRIORITY_DISPLAY);
		thread.start();
		
		serviceLooper = thread.getLooper();
		serviceHandler = new Handler(serviceLooper, new ServiceHandlerCallback());
		
		serviceHandler.post(() -> {
			
			Log.i("wutt", "service handler post");
			
			intersectionPushTitle = getResources().getString(R.string.notification_intersection_title);
			intersectionPushText = getResources().getString(R.string.notification_intersection_text);
			
			wakeupPushTitle = getResources().getString(R.string.notification_start_tracing_title);
			wakeupPushText = getResources().getString(R.string.notification_start_tracing_text);
			
			notActivePushTitle = getResources().getString(R.string.notification_take_survey_title);
			notActivePushText = getResources().getString(R.string.notification_take_survey_text);
			
			prefs = new PreferencesUtil(getApplicationContext());
			
			selfWakeupAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
			
			repository = Repository.getInstance(getApplicationContext());
			
			localBroadcastManager = LocalBroadcastManager.getInstance(this);
			
			if (Build.VERSION.SDK_INT >= VERSION_CODES.O) {
				notificationManager = (NotificationManager) getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
				
				notificationManager.createNotificationChannelGroup(new NotificationChannelGroup(NOTIFICATION_APP_GROUP_ID,
				                                                                                "Dafault group"));
				
				notificationChannel = new NotificationChannel(MAIN_NOTIFICATION_CHANNEL_ID, "Main channel",
				                                              NotificationManager.IMPORTANCE_LOW);
				notificationChannel.setShowBadge(false);
				notificationChannel.setGroup(NOTIFICATION_APP_GROUP_ID);
				
				Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
				
				AudioAttributes audioAttributes = new AudioAttributes.Builder()
						.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
						.setUsage(AudioAttributes.USAGE_NOTIFICATION)
						.build();
				
				notificationChannelCritical = new NotificationChannel(CRITICAL_NOTIFICATION_CHANNEL_ID, "Urgent messages",
				                                                      NotificationManager.IMPORTANCE_HIGH);
				
				notificationChannelCritical.setShowBadge(false);
				notificationChannelCritical.enableLights(true);
				notificationChannelCritical.enableVibration(true);
				notificationChannelCritical.setSound(alarmSound, audioAttributes);
				notificationChannelCritical.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
				notificationChannelCritical.setVibrationPattern(new long[] {0L, 300L, 300L, 300L});
				notificationChannelCritical.setLightColor(Color.GREEN);
				notificationChannelCritical.setGroup(NOTIFICATION_APP_GROUP_ID);
				
				if (notificationManager != null) {
					notificationManager.createNotificationChannel(notificationChannel);
					notificationManager.createNotificationChannel(notificationChannelCritical);
				}
			}
			
			Notification serviceNotification = getDefaultNotification(false);
			startForeground(NOTIFICATION_ID, serviceNotification);
			
			startLocationServices();
		});
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		if (locationDataCollector != null) {
			locationDataCollector.destroyCollector();
		}
		
		if (thread != null) {
			thread.quit();
		}
	}
	
	@Override
	public boolean onUnbind(final Intent intent) {
		return super.onUnbind(intent);
	}
	
	private void handleIntent(final Intent intent) {
		if (intent != null) {
			
			final String action = intent.getAction();
			if (StringUtil.isEmpty(action)) {
				return;
			}
			
			switch (action) {
				case WAKEUP_INTENT: {
					boolean command = intent.getBooleanExtra(WAKEUP_INTENT, false);
					
					if (command) {
						Log.i("wutt", "intent start");
						startLocationServices();
					}
					
					break;
				}
				
				case PUSH_INTENT: {
					Log.i("wutt", "intent push");
					final Bundle bundle = intent.getExtras();
					if (bundle != null && !bundle.isEmpty()) {
						handlePushIntent(bundle);
					}
					
					break;
				}
			}
			
		}
	}
	
	private void handlePushIntent(final Bundle bundle) {
		Log.i("wutt", "incoming bundle: " + bundle);
		
		final String type = bundle.getString(PUSH_MESSAGE_TYPE_BUNDLE_EXTRA);
		
		if (type == null) {
			return;
		}
		
		reanimateServiceDataIfNeeded();
		
		switch (type) {
			case PUSH_MESSAGE_TYPE_WAKEUP: {
				
				final String newStatus = bundle.getString(PUSH_MESSAGE_STATUS_BUNDLE_EXTRA);
				final String new24hCounter = bundle.getString(PUSH_MESSAGE_24H_COUNT_EXTRA);
				final Boolean isActiveNow = Boolean.valueOf(bundle.getString(PUSH_MESSAGE_IS_ACTIVE_EXTRA));
				
				if (!StringUtil.isEmpty(newStatus) && !StringUtil.isEmpty(new24hCounter) && isActiveNow != null) {
					
					try {
						lastKnownUserData.infected24h = Integer.valueOf(new24hCounter);
					} catch (NumberFormatException e) {
						e.printStackTrace();
					}
					
					if (lastKnownUserData.infected24h == null) {
						lastKnownUserData.infected24h = 0;
					}
					
					lastKnownUserData.active = isActiveNow;
					lastKnownUserData.status = newStatus;
					
					updateNotification(getDefaultNotification(false));  // false - чтобы не звенеть 2 раза
					
					if (isAcceptableToRing()) {
						appendNotification(getWakeupNotification());
					}
				}
					
				if (locationDataCollector == null) {
					startLocationServices();
				} else {
					startTracking(false);
				}
				
				break;
			}
			
			case PUSH_MESSAGE_TYPE_INTERSECTION: {
				
				final String newStatus = bundle.getString(PUSH_MESSAGE_STATUS_BUNDLE_EXTRA);
				final Boolean isActiveNow = Boolean.valueOf(bundle.getString(PUSH_MESSAGE_IS_ACTIVE_EXTRA));
				final String new24hCounter = bundle.getString(PUSH_MESSAGE_24H_COUNT_EXTRA);
				
				if (!StringUtil.isEmpty(newStatus) && !StringUtil.isEmpty(new24hCounter) && isActiveNow != null) {
					
					try {
						lastKnownUserData.infected24h = Integer.valueOf(new24hCounter);
					} catch (NumberFormatException e) {
						e.printStackTrace();
					}
					
					if (lastKnownUserData.infected24h == null) {
						lastKnownUserData.infected24h = 0;
					}
					
					lastKnownUserData.status = newStatus;
					lastKnownUserData.active = isActiveNow;
					
					updateNotification(getDefaultNotification(false)); // false - чтобы не звенеть 2 раза
					
					if (isAcceptableToRing()) {
						appendNotification(getIntersectionFoundNotification());
					}
				}
				
				if (locationDataCollector == null) {
					startLocationServices();
				}
				
				serviceRequestsRefreshData();
				
				break;
			}
		}
	}
	
	private boolean isAcceptableToRing() {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(System.currentTimeMillis());
		
		return cal.get(Calendar.HOUR_OF_DAY) < 21 && cal.get(Calendar.HOUR_OF_DAY) > 7;
	}
	
	private void reanimateServiceDataIfNeeded() {
		if (lastKnownUserData == null) {
			lastKnownUserData = new UserData();
			lastKnownUserData.active = true;
		}
	}
	
	/**
	 * Запуск основного цикла сбора координат после получения разрешения отпользователя
	 *
	 * Запускать на потоке сервиса
	 */
	private void startLocationServices() {
		
		Log.i("wutt", "start location service");
		
		initLocationServices();
		startTracking(false);
		startCollectingAlarm();
	}
	
	private void initLocationServices() {
		if (locationDataCollector == null) {
			Log.i("wutt", "init collector");
			locationDataCollector = new LocationDataCollectorImpl(this);
		}
	}
	
	private void startTracking(boolean isRequestedForUI) {
		
		Log.i("wutt", "start tracking");
		
		if (isTracking) {
			Log.i("wutt", "already tracking. abort.");
			return;
		}
		
		if (!checkPermissions()) {
			Log.i("wutt", "start tracking failed: no permissions");
			serviceRequestsPermissions();
			return;
		}
		
		isTracking = true;
		
		locationDataCollector.startCollectData(isRequestedForUI);
		
		serviceHandler.sendEmptyMessageDelayed(MESSAGE_STOP_TRACKING, COLLECTING_TIME_MS);
		
		Log.i("wutt", "start tracking: success");
		
		checkPushTokenSent();
	}
	
	private void stopTracking(boolean isAborted) {
		
		if (!isTracking) {
			return;
		}
		
		isTracking = false;
		
		locationDataCollector.stopCollectData();
		
		if (isAborted) {
			serviceHandler.removeMessages(MESSAGE_STOP_TRACKING);
			stopCollectingAlarm();
			Log.i("wutt", "stop tracking: abort");
			return;
		}
		
		final LocationCollectedData collectedData = locationDataCollector.getCollectedData();
		Log.i("wutt", "data collected");
		
		serviceProvidesCrowdCounter(collectedData.getBluetoothDevices().size());
		
		final boolean mustCalculateIntersections = locationDataCollector.isRequestedForUI();
		
		Analytics.scanningCountTotal();
		
		repository.saveSurroundingData(collectedData, mustCalculateIntersections, new SendSurroundingCallback() {
			@Override
			public void success(final Bundle dataBundle) {
				
				if (isLockedFromUI && !mustCalculateIntersections) {
					return;
				}
				
				serviceHandler.post(() -> {
					if (mustCalculateIntersections) {
						dataBundle.putBoolean(BUNDLE_SEND_SUCCESS, true);
						serviceRequestsUpdateUI(dataBundle);
						releaseUILock();
					}
					
					repository.checkAndRunJanitor();
				});
			}
			
			@Override
			public void fail() {
				if (isLockedFromUI) {
					final Bundle failBundle = new Bundle();
					failBundle.putBoolean(BUNDLE_SEND_SUCCESS, false);
					serviceRequestsUpdateUI(failBundle);
					
					return;
				}
			}
			
			@Override
			public void nothingToSend() {
				if (isLockedFromUI) {
					final Bundle failBundle = new Bundle();
					failBundle.putBoolean(BUNDLE_SEND_SUCCESS, false);
					serviceRequestsUpdateUI(failBundle);
					
					return;
				}
			}
		});
	}
	
	public void restartTracking() {
		serviceHandler.post(() -> {
			stopTracking(true);
			startLocationServices();
		});
	}
	
	public boolean getIsTracking() {
		return isTracking;
	}
	
	private void checkPushTokenSent() {
		if (!prefs.getPushTokenSent()) {
			Log.i("wutt", "push token sending from location service");
			FirebaseUtil.getFirebaseToken(new GetFirebaseTokenCallback() {
				@Override
				public void onSuccess(final String token) {
					repository.sendPushToken(token, new SendPushTokenCallback() {
						@Override
						public void success() {
							prefs.setPushTokenSent(true);
						}
						
						@Override
						public void fail() {
							prefs.setPushTokenSent(false);
						}
					});
				}
				
				@Override
				public void onFail() {}
			});
		}
	}
	
	private boolean checkPermissions() {

		if (!LocationUtil.areGeoServicesAvailable(getApplicationContext())) {
			updateNotification(getTurnOnLocationNotification());
			return false;
		}
		
		if (PermissionUtil.unaskedLocationPermissions(getApplicationContext()).size() > 0) {
			updateNotification(getNoPermissionsNotification());
			return false;
		}
		
		if (!BluetoothUtil.checkBluetoothEnabled(getApplicationContext())) {
			updateNotification(getNoBluetoothNotification());
			return false;
		}
		
		return true;
	}
	
	private String getStatus() {
		if (lastKnownUserData != null) {
			if (lastKnownUserData.active != null) {
				if (!lastKnownUserData.active) {
					return getResources().getString(R.string.notification_take_survey_title);
				}
			}
			
			if (lastKnownUserData.status != null) {
				switch (lastKnownUserData.status) {
					case STATUS_HEALTHY: {
						return getResources().getString(R.string.status_healthy_title);
					}
					case STATUS_INFECTED: {
						return getResources().getString(R.string.status_potentially_sick_title);
					}
					case STATUS_SICK: {
						return getResources().getString(R.string.status_sick_title);
					}
					default: {
						return getResources().getString(R.string.app_name);
					}
				}
			}
		}
		
		return getResources().getString(R.string.notification_take_survey_title);
	}
	
	private String getIntersectionsCount() {
		if (lastKnownUserData != null) {
			if (lastKnownUserData.active) {
				if (lastKnownUserData.infected24h != null) {
					return String.valueOf(lastKnownUserData.infected24h);
				}
			} else {
				return getResources().getString(R.string.status_unknown_counters);
			}
		}
		
		return String.valueOf(0);
	}
	
	private int getDrawableInt() {
		if (lastKnownUserData != null) {
			if (lastKnownUserData.active != null) {
				if (!lastKnownUserData.active) {
					return R.drawable.ic_error_outline_black_24dp;
				}
			}
			
			if (lastKnownUserData.status != null) {
				switch (lastKnownUserData.status) {
					case STATUS_HEALTHY: {
						return R.drawable.ic_sentiment_satisfied_black_24dp;
					}
					case STATUS_INFECTED: {
						return R.drawable.ic_sentiment_neutral_black_24dp;
					}
					case STATUS_SICK: {
						return R.drawable.ic_sentiment_dissatisfied_black_24dp;
					}
					default: {
						return R.drawable.ic_error_outline_black_24dp;
					}
				}
			}
		}
		
		return R.drawable.ic_error_outline_black_24dp;
	}
	
	private Integer getDrawableTintColor() {
		if (lastKnownUserData != null) {
			if (lastKnownUserData.active != null) {
				if (!lastKnownUserData.active) {
					return R.color.status_unknown_background;
				}
			}
			
			if (lastKnownUserData.status != null) {
				switch (lastKnownUserData.status) {
					case STATUS_HEALTHY: {
						return R.color.healthy_green;
					}
					case STATUS_INFECTED: {
						return R.color.infected_yellow;
					}
					case STATUS_SICK: {
						return R.color.sick_red;
					}
					default: {
						return R.color.status_unknown_background;
					}
				}
			}
		}
		
		return R.color.status_unknown_background;
	}
	
	private Notification getDefaultNotification(boolean isUrgent) {
		Intent intent = new Intent(this.getApplicationContext(), MainActivity.class);
		intent.putExtra(BUNDLE_FROM_NOTIFICATION, BUNDLE_NOTIFICATION_REGULAR);
		intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		
		PendingIntent pendingIntent = PendingIntent.getActivity(this.getApplicationContext(), UUID.randomUUID().hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
		
		String intersectionsCountString = getIntersectionsCount();
		
		String statusString = getStatus();
		String intersectionsString = getResources().getString(R.string.notification_infected_contacts_today) + " " + intersectionsCountString;
		
		return getNotification(statusString,
		                       intersectionsString,
		                       pendingIntent,
		                       isUrgent);
	}
	
	private Notification getCustomNotification(String title, String text, boolean isUrgent) {
		Intent intent = new Intent(this.getApplicationContext(), MainActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(this.getApplicationContext(), UUID.randomUUID().hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
		
		return getNotification(title,
		                       text,
		                       pendingIntent,
		                       isUrgent);
	}
	
	private Notification getTurnOnLocationNotification() {
		Intent intent = new Intent(this.getApplicationContext(), MainActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(this.getApplicationContext(), UUID.randomUUID().hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
		
		boolean mustMakeSound = lastKnownUserData != null;
		
		return getNotification(getResources().getString(R.string.attention_string),
		                       getResources().getString(R.string.need_permissions),
		                       pendingIntent,
		                       mustMakeSound);
	}
	
	private Notification getNoPermissionsNotification() {
		Intent intent = new Intent(this.getApplicationContext(), MainActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(this.getApplicationContext(), UUID.randomUUID().hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
		
		boolean mustMakeSound = lastKnownUserData != null;
		
		return getNotification(getResources().getString(R.string.attention_string),
		                       getResources().getString(R.string.need_permissions),
		                       pendingIntent,
		                       mustMakeSound);
	}
	
	private Notification getNoBluetoothNotification() {
		Intent intent = new Intent(this.getApplicationContext(), MainActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(this.getApplicationContext(), UUID.randomUUID().hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
		
		boolean mustMakeSound = lastKnownUserData != null;
		
		return getNotification(getResources().getString(R.string.attention_string),
		                       getResources().getString(R.string.need_permissions),
		                       pendingIntent,
		                       mustMakeSound);
	}
	
	private Notification getIntersectionFoundNotification() {
		Intent intent = new Intent(this.getApplicationContext(), MainActivity.class);
		intent.putExtra(BUNDLE_FROM_NOTIFICATION, BUNDLE_NOTIFICATION_INTERSECTION);
		
		PendingIntent pendingIntent = PendingIntent.getActivity(this.getApplicationContext(), UUID.randomUUID().hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
		
		String title = null;
		String text = null;
		
		if (lastKnownUserData != null && lastKnownUserData.active) {
			title = intersectionPushTitle;
			text = intersectionPushText;
		} else {
			title = notActivePushTitle;
			text = notActivePushText;
		}
		
		return getCriticalNotification(title, text, pendingIntent);
	}
	
	private Notification getWakeupNotification() {
		Intent intent = new Intent(this.getApplicationContext(), MainActivity.class);
		intent.putExtra(BUNDLE_FROM_NOTIFICATION, BUNDLE_NOTIFICATION_REMINDER);
		
		PendingIntent pendingIntent = PendingIntent.getActivity(this.getApplicationContext(), UUID.randomUUID().hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
		
		String title = null;
		String text = null;
		
		if (lastKnownUserData != null && lastKnownUserData.active) {
			title = wakeupPushTitle;
			text = wakeupPushText;
		} else {
			title = notActivePushTitle;
			text = notActivePushText;
		}
		
		return getCriticalNotification(title, text, pendingIntent);
	}
	
	private Notification getNotification(String title, String text, PendingIntent pendingIntent, boolean isVeryImportant) {
		
		NotificationCompat.Builder builder;
		
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
			
			int notificationImportance = isVeryImportant ? NotificationManager.IMPORTANCE_HIGH :
			                             NotificationManager.IMPORTANCE_LOW;
			
			if (notificationChannel != null) {
				notificationChannel.setImportance(notificationImportance);
			}
		}
		
		int priority = isVeryImportant ? Notification.PRIORITY_HIGH : Notification.PRIORITY_DEFAULT;
		
		builder = new NotificationCompat.Builder(getApplicationContext(), MAIN_NOTIFICATION_CHANNEL_ID);
		builder.setPriority(priority);
		builder.setCategory(Notification.CATEGORY_SERVICE);
		
		builder.setContentTitle(title);
		builder.setStyle(new NotificationCompat.BigTextStyle().bigText(text));
		builder.setContentText(text);
		builder.setSmallIcon(R.drawable.ic_launcher_foreground);
		
		
		final Bitmap largeIconBitmap = DrawableUtil.getLargeIconForNotification(getDrawableInt(), getApplicationContext(),
		                                                         getDrawableTintColor());
		builder.setLargeIcon(largeIconBitmap);
		
		builder.setContentIntent(pendingIntent);
		builder.setGroup(NOTIFICATION_APP_GROUP_ID);
		builder.setGroupSummary(true);
		
		Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		
		if (isVeryImportant) {
			builder.setSound(alarmSound);
		}
		
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
			builder.setChannelId(MAIN_NOTIFICATION_CHANNEL_ID);
		}
		
		return builder.build();
	}
	
	private Notification getCriticalNotification(String title, String text, PendingIntent pendingIntent) {
		
		NotificationCompat.Builder builder;
		
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
			if (notificationChannelCritical != null) {
				notificationChannelCritical.setImportance(NotificationManager.IMPORTANCE_HIGH);
			}
		}
		
		int priority = Notification.PRIORITY_HIGH;
		
		builder = new NotificationCompat.Builder(getApplicationContext(), CRITICAL_NOTIFICATION_CHANNEL_ID);
		builder.setPriority(priority);
		builder.setCategory(Notification.CATEGORY_MESSAGE);
		
		builder.setContentTitle(title);
		builder.setStyle(new NotificationCompat.BigTextStyle().bigText(text));
		builder.setContentText(text);
		builder.setSmallIcon(R.drawable.ic_launcher_foreground);
		
		final Bitmap largeIconBitmap = DrawableUtil.getLargeIconForNotification(getDrawableInt(), getApplicationContext(),
		                                                                        getDrawableTintColor());
		builder.setLargeIcon(largeIconBitmap);
		
		builder.setVibrate(new long[] {0,0,300,300});
		
		builder.setContentIntent(pendingIntent);
		builder.setGroup(NOTIFICATION_APP_GROUP_ID);
		builder.setGroupSummary(true);
		builder.setAutoCancel(true);
		
		Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		
		builder.setSound(alarmSound);
		
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
			builder.setChannelId(CRITICAL_NOTIFICATION_CHANNEL_ID);
		}
		
		return builder.build();
	}
	
	private void updateNotification(Notification newNotification) {
		
		if (Build.VERSION.SDK_INT >= VERSION_CODES.O) {
			if (this.notificationManager != null) {
				notificationManager.cancel(NOTIFICATION_ID);
				notificationManager.notify(NOTIFICATION_ID, newNotification);
			}
		} else {
			NotificationManager notificationManager =
					(NotificationManager) getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
			
			if (notificationManager != null) {
				notificationManager.cancel(NOTIFICATION_ID);
				notificationManager.notify(NOTIFICATION_ID, newNotification);
			}
		}
	}
	
	private void appendNotification(Notification newNotification) {
		
		if (Build.VERSION.SDK_INT >= VERSION_CODES.O) {
			if (this.notificationManager != null) {
				notificationManager.cancel(NOTIFICATION_ID_CRITICAL);
				notificationManager.notify(NOTIFICATION_ID_CRITICAL, newNotification);
			}
		} else {
			NotificationManager notificationManager =
					(NotificationManager) getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
			
			if (notificationManager != null) {
				notificationManager.cancel(NOTIFICATION_ID_CRITICAL);
				notificationManager.notify(NOTIFICATION_ID_CRITICAL, newNotification);
			}
		}
	}
	
	public void removeCriticalNotifications() {
		serviceHandler.post(() -> {
			if (notificationManager != null) {
				notificationManager.cancelAll();
			}
		});
	}
	
	private void updateUserData(final UserData userData) {
		if (userData != null) {
			if (lastKnownUserData == null) {
				lastKnownUserData = new UserData();
			}
			
			lastKnownUserData.infected24h = userData.infected24h;
			lastKnownUserData.active = userData.active;
			lastKnownUserData.infectedTotal = userData.infectedTotal;
			lastKnownUserData.status = userData.status;
			lastKnownUserData.timeToRecover = userData.timeToRecover;
			
			updateNotification(getDefaultNotification(false));
		}
	}
	
	public void tellNewStatusToService(final UserData data) {
		serviceHandler.post(() -> {
			updateUserData(data);
		});
	}
	
	/**
	 * Немедленный запуск сканирования с получением данных о пересечениях
	 */
	public void collectDataImmediately() {
		serviceHandler.post(() -> {
			setUILock();
			startTracking(true);
		});
	}
	
	private void setUILock() {
		isLockedFromUI = true;
		stopTracking(true);
	}
	
	private void releaseUILock() {
		isLockedFromUI = false;
		startCollectingAlarm();
	}
	
	private void serviceRequestsUpdateUI(final Bundle dataBundle) {
		final Intent intent = new Intent(ACTION_UPDATE_UI);
		
		if (dataBundle != null) {
			intent.putExtras(dataBundle);
		}
		
		localBroadcastManager.sendBroadcast(intent);
	}
	
	private void serviceRequestsRefreshData() {
		final Intent intent = new Intent(ACTION_REFRESH_DATA);
		localBroadcastManager.sendBroadcast(intent);
	}
	
	private void serviceRequestsPermissions() {
		final Intent intent = new Intent(ACTION_GRANT_PERMISSIONS);
		localBroadcastManager.sendBroadcast(intent);
	}
	
	private void serviceProvidesCrowdCounter(int btDevicesAround) {
		final Intent intent = new Intent(CROWD_DATA);
		intent.putExtra(BUNDLE_CROWD_COUNTER, btDevicesAround);
		
		localBroadcastManager.sendBroadcast(intent);
	}
	
	private void startCollectingAlarm() {
		Log.i("wutt", "start alarm");
		
		final Intent i = new Intent(LocationService.this, LocationService.class);
		i.setAction(WAKEUP_INTENT);
		i.putExtra(WAKEUP_INTENT, true);
		
		final PendingIntent pi = PendingIntent.getService(LocationService.this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
		
		selfWakeupAlarmManager.cancel(pi);
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			selfWakeupAlarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,
			                                                 SystemClock.elapsedRealtime() + TIMER_DEFAULT_TIME_MS + COLLECTING_TIME_MS,
			                                                 pi);
		} else {
			selfWakeupAlarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                                         SystemClock.elapsedRealtime() + TIMER_DEFAULT_TIME_MS + COLLECTING_TIME_MS,
                                            pi);
		}
	}
	
	private void stopCollectingAlarm() {
		Log.i("wutt", "stop alarm");
		
		final Intent i = new Intent(this, LocationService.class);
		i.setAction(WAKEUP_INTENT);
		i.putExtra(WAKEUP_INTENT, false);
		
		final PendingIntent pi = PendingIntent.getService(LocationService.this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
		
		if (selfWakeupAlarmManager != null) {
			selfWakeupAlarmManager.cancel(pi);
		}
	}
	
	
	public class LocationServiceBinder extends Binder {
		public LocationService getService() {
			return LocationService.this;
		}
	}
	
	private class ServiceHandlerCallback implements Callback {
		
		@Override
		public boolean handleMessage(@NonNull final Message msg) {
			
			switch (msg.what) {
				case MESSAGE_BLUETOOTH_UNAVAILABLE: {
					stopTracking(true);
					updateNotification(getNoBluetoothNotification());
					serviceRequestsPermissions();
					break;
				}
				case MESSAGE_BLUETOOTH_AVAILABLE: {
					startLocationServices();
					updateNotification(getDefaultNotification(false));
					break;
				}
				case MESSAGE_STOP_TRACKING: {
					stopTracking(false);
					break;
				}
			}
			
			return true;
		}
	}
}
