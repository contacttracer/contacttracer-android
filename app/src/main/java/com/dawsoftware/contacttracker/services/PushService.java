package com.dawsoftware.contacttracker.services;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.jetbrains.annotations.NotNull;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import com.dawsoftware.contacttracker.BuildConfig;
import com.dawsoftware.contacttracker.util.LogUtil;
import com.dawsoftware.contacttracker.util.PreferencesUtil;

import static com.dawsoftware.contacttracker.services.LocationService.PUSH_INTENT;
import static com.dawsoftware.contacttracker.services.LocationService.PUSH_MESSAGE_24H_COUNT_EXTRA;
import static com.dawsoftware.contacttracker.services.LocationService.PUSH_MESSAGE_IS_ACTIVE_EXTRA;
import static com.dawsoftware.contacttracker.services.LocationService.PUSH_MESSAGE_STATUS_BUNDLE_EXTRA;
import static com.dawsoftware.contacttracker.services.LocationService.PUSH_MESSAGE_TYPE_BUNDLE_EXTRA;

public class PushService extends FirebaseMessagingService {
	
	private static final String TAG = "FCM_PUSH_SERVICE";
	
	private static final String TYPE_KEY = "type";
	private static final String STATUS_KEY = "status";
	private static final String COUNTER_24H_KEY = "infected_24h";
	private static final String IS_ACTIVE_KEY = "active";
	
	@Override
	public void onNewToken(@NotNull String token) {
		Log.i(TAG, "Refreshed token: " + token);
		
		sendRegistrationToServer(token);
		
		final String pushReceivedLog = getCurrentTimeString() + "Token received: \n" + token;
		
		LogUtil.writeToFile(pushReceivedLog, this.getApplicationContext(), LogUtil.PUSH);
	}
	
	private void sendRegistrationToServer(String token) {
		
		final PreferencesUtil prefs = new PreferencesUtil(getApplicationContext());

		prefs.setPushToken(token);
		prefs.setPushTokenSent(false);
	}
	
	@Override
	public void onMessageReceived(@NonNull final RemoteMessage remoteMessage) {
		super.onMessageReceived(remoteMessage);
		
		if (remoteMessage.getData().size() > 0 && BuildConfig.DEBUG) {
			Log.i(TAG, "Message data payload: " + remoteMessage.getData());
		}
		
		final String pushReceivedLog = "\n" + getCurrentTimeString() + "Push received: ";
		
		LogUtil.writeToFile(pushReceivedLog, this.getApplicationContext(), LogUtil.PUSH);
		
		final Map<String, String> incomingData = remoteMessage.getData();
		
		if (incomingData != null && incomingData.size() > 0) {
			final Bundle bundle = new Bundle();
			bundle.putString(PUSH_MESSAGE_TYPE_BUNDLE_EXTRA, incomingData.get(TYPE_KEY));
			bundle.putString(PUSH_MESSAGE_STATUS_BUNDLE_EXTRA, incomingData.get(STATUS_KEY));
			bundle.putString(PUSH_MESSAGE_24H_COUNT_EXTRA, incomingData.get(COUNTER_24H_KEY));
			bundle.putString(PUSH_MESSAGE_IS_ACTIVE_EXTRA, incomingData.get(IS_ACTIVE_KEY));
			
			final String pushLog = "Type: " + incomingData.get(TYPE_KEY) + ", status: " +  incomingData.get(STATUS_KEY) + ", " +
					"24h count: " + incomingData.get(COUNTER_24H_KEY) + ", is active: " + incomingData.get(IS_ACTIVE_KEY);
			
			LogUtil.writeToFile(pushLog, this.getApplicationContext(), LogUtil.PUSH);
			
			sendDataToLocationService(bundle);
		}
	}
	
	private void sendDataToLocationService(final Bundle bundle) {
		final Context applicationContext = getApplicationContext();
		
		final Intent intent = new Intent(applicationContext, LocationService.class);
		intent.putExtras(bundle);
		intent.setAction(PUSH_INTENT);
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			ContextCompat.startForegroundService(applicationContext, intent);
		} else {
			applicationContext.startService(intent);
		}
	}
	
	private String getCurrentTimeString() {
		final Date date = new Date(System.currentTimeMillis());
		final Calendar cal = Calendar.getInstance();
		
		cal.setTime(date);
		
		return "[" + cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE) + ":" + cal.get(Calendar.SECOND) +
				" GMT+3]: ";
	}
}
