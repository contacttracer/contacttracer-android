package com.dawsoftware.contacttracker.analytics;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.dawsoftware.contacttracker.ui.main.MainFragmentViewModel;
import com.dawsoftware.contacttracker.util.StringUtil;
import com.google.firebase.analytics.FirebaseAnalytics;

import androidx.annotation.NonNull;

import static com.dawsoftware.contacttracker.analytics.Contract.APP_START_INTERSECTION_NOTIF;
import static com.dawsoftware.contacttracker.analytics.Contract.APP_START_MANUAL;
import static com.dawsoftware.contacttracker.analytics.Contract.APP_START_REMINDER_NOTIF;
import static com.dawsoftware.contacttracker.analytics.Contract.APP_START_SERVICE;
import static com.dawsoftware.contacttracker.analytics.Contract.APP_START_SURVEY_NOTIF;
import static com.dawsoftware.contacttracker.analytics.Contract.INTERSECTIONS_SCREEN_OPENED;
import static com.dawsoftware.contacttracker.analytics.Contract.MAIN_SCREEN_OPENED;
import static com.dawsoftware.contacttracker.analytics.Contract.ONBOARDING_DISMISSED;
import static com.dawsoftware.contacttracker.analytics.Contract.ONBOARDING_FINISHED;
import static com.dawsoftware.contacttracker.analytics.Contract.ONBOARDING_OPENED;
import static com.dawsoftware.contacttracker.analytics.Contract.START_SCANNING_TOTAL;
import static com.dawsoftware.contacttracker.analytics.Contract.SEND_SURVEY_BUTTON;
import static com.dawsoftware.contacttracker.analytics.Contract.SHARED_FROM_SURVEY;
import static com.dawsoftware.contacttracker.analytics.Contract.START_SCANNING_BUTTON;
import static com.dawsoftware.contacttracker.analytics.Contract.STATE_ACTIVE_TO_INACTIVE;
import static com.dawsoftware.contacttracker.analytics.Contract.STATE_INACTIVE_TO_ACTIVE;
import static com.dawsoftware.contacttracker.analytics.Contract.STATISTICS_SCREEN_OPENED;
import static com.dawsoftware.contacttracker.analytics.Contract.STATUS_HEALTHY_TO_INFECTED_CLIENT;
import static com.dawsoftware.contacttracker.analytics.Contract.STATUS_HEALTHY_TO_INFECTED_SERVER;
import static com.dawsoftware.contacttracker.analytics.Contract.STATUS_INFECTED_TO_HEALTHY_CLIENT;
import static com.dawsoftware.contacttracker.analytics.Contract.STATUS_INFECTED_TO_HEALTHY_SERVER;
import static com.dawsoftware.contacttracker.analytics.Contract.STATUS_INFECTED_TO_SICK_CLIENT;
import static com.dawsoftware.contacttracker.analytics.Contract.STATUS_INFECTED_TO_SICK_SERVER;
import static com.dawsoftware.contacttracker.analytics.Contract.STATUS_SICK_TO_HEALTHY_CLIENT;
import static com.dawsoftware.contacttracker.analytics.Contract.STATUS_SICK_TO_HEALTHY_SERVER;
import static com.dawsoftware.contacttracker.analytics.Contract.SURVEY_SCREEN_OPENED;
import static com.dawsoftware.contacttracker.services.LocationService.BUNDLE_FROM_NOTIFICATION;
import static com.dawsoftware.contacttracker.services.LocationService.BUNDLE_NOTIFICATION_INTERSECTION;
import static com.dawsoftware.contacttracker.services.LocationService.BUNDLE_NOTIFICATION_REGULAR;
import static com.dawsoftware.contacttracker.services.LocationService.BUNDLE_NOTIFICATION_REMINDER;
import static com.dawsoftware.contacttracker.services.LocationService.BUNDLE_NOTIFICATION_SURVEY;
import static com.dawsoftware.contacttracker.services.LocationService.STATUS_HEALTHY;
import static com.dawsoftware.contacttracker.services.LocationService.STATUS_INFECTED;
import static com.dawsoftware.contacttracker.services.LocationService.STATUS_SICK;

public class Analytics {
	
	private static Analytics instance = null;
	
	public static Analytics getInstance(@NonNull final Context applicationContext) {
		
		if (instance == null) {
			synchronized (Analytics.class) {
				if (instance == null) {
					instance = new Analytics(applicationContext);
				}
			}
		}
		
		return instance;
	}
	
	private enum STATUS_CHANGE_SOURCE {CLIENT, SERVER}
	
	private FirebaseAnalytics firebase;
	
	private static boolean isSharingSent = false;
	
	private static boolean canDetectLaunch = false;
	
	private static String startNotificationType = "";
	
	public static boolean isClientSendingEvent = false;
	
	private Analytics(@NonNull final Context applicationContext) {
		initFirebase(applicationContext);
	}
	
	private void initFirebase(@NonNull final Context applicationContext) {
		firebase = FirebaseAnalytics.getInstance(applicationContext);
	}
	
	private static void sendEvent(@NonNull String eventName) {
		instance.firebase.logEvent(eventName, null);
	}
	
	public static void onboardingOpened(@NonNull final Activity activity) {
		
		if (!isInitialized()) {
			return;
		}
		
		sendEvent(ONBOARDING_OPENED);
		instance.firebase.setCurrentScreen(activity, "onboarding_screen", null);
	}
	
	public static void onboardingDismissed() {
		if (!isInitialized()) {
			return;
		}
		
		sendEvent(ONBOARDING_DISMISSED);
	}
	
	public static void onboardingFinished() {
		if (!isInitialized()) {
			return;
		}
		
		sendEvent(ONBOARDING_FINISHED);
	}
	
	public static void mainScreenOpened(@NonNull final Activity activity) {
		
		if (!isInitialized()) {
			return;
		}
		
		if (isSharingSent) {
			isSharingSent = false;
			return;
		}
		
		sendEvent(MAIN_SCREEN_OPENED);
		instance.firebase.setCurrentScreen(activity, "main_screen", null);
	}
	
	public static void intersectionsScreenOpened(@NonNull final Activity activity) {
		if (!isInitialized()) {
			return;
		}
		
		sendEvent(INTERSECTIONS_SCREEN_OPENED);
		instance.firebase.setCurrentScreen(activity, "intersections_screen", null);
	}
	
	public static void surveyScreenOpened(@NonNull final Activity activity) {
		
		if (!isInitialized()) {
			return;
		}
		
		sendEvent(SURVEY_SCREEN_OPENED);
		instance.firebase.setCurrentScreen(activity, "survey_screen", null);
	}
	
	public static void statisticsScreenOpened(@NonNull final Activity activity) {
		
		if (!isInitialized()) {
			return;
		}
		
		sendEvent(STATISTICS_SCREEN_OPENED);
		instance.firebase.setCurrentScreen(activity, "statistics_screen", null);
	}
	
	private static void appStartManual() {
		if (!isInitialized()) {
			return;
		}
		
		sendEvent(APP_START_MANUAL);
	}
	
	private static void appStartService() {
		if (!isInitialized()) {
			return;
		}
		
		sendEvent(APP_START_SERVICE);
	}
	
	private static void appStartSurveyNotif() {
		
		if (!isInitialized()) {
			return;
		}
		
		sendEvent(APP_START_SURVEY_NOTIF);
	}
	
	private static void appStartIntersectionNotif() {
		if (!isInitialized()) {
			return;
		}
		
		sendEvent(APP_START_INTERSECTION_NOTIF);
	}
	
	private static void appStartReminderNotif() {
		
		if (!isInitialized()) {
			return;
		}
		
		sendEvent(APP_START_REMINDER_NOTIF);
	}
	
	public static void startScanningButton() {
		
		if (!isInitialized()) {
			return;
		}
		
		sendEvent(START_SCANNING_BUTTON);
	}
	
	public static void sendSurveyButton() {
		
		if (!isInitialized()) {
			return;
		}
		
		sendEvent(SEND_SURVEY_BUTTON);
	}
	
	public static void sharedFromSurvey() {
		
		if (!isInitialized()) {
			return;
		}
		
		isSharingSent = true;
		sendEvent(SHARED_FROM_SURVEY);
	}
	
	public static void scanningCountTotal() {
		sendEvent(START_SCANNING_TOTAL);
	}
	
	public static void stateActiveToInactive() {
		sendEvent(STATE_ACTIVE_TO_INACTIVE);
	}
	
	public static void stateInactiveToActive() {
		sendEvent(STATE_INACTIVE_TO_ACTIVE);
	}
	
	private static void statusHealthyToInfectedClient() {
//		Log.i("wwww", "h_i_c");
		sendEvent(STATUS_HEALTHY_TO_INFECTED_CLIENT);
	}
	
	private static void statusHealthyToInfectedServer() {
//		Log.i("wwww", "h_i_s");
		sendEvent(STATUS_HEALTHY_TO_INFECTED_SERVER);
	}
	
	private static void statusInfectedToHealthyClient() {
//		Log.i("wwww", "i_h_c");
		sendEvent(STATUS_INFECTED_TO_HEALTHY_CLIENT);
	}
	
	private static void statusInfectedToHealthyServer() {
//		Log.i("wwww", "i_h_s");
		sendEvent(STATUS_INFECTED_TO_HEALTHY_SERVER);
	}
	
	private static void statusInfectedToSickClient() {
//		Log.i("wwww", "i_s_c");
		sendEvent(STATUS_INFECTED_TO_SICK_CLIENT);
	}
	
	private static void statusInfectedToSickServer() {
//		Log.i("wwww", "i_s_s");
		sendEvent(STATUS_INFECTED_TO_SICK_SERVER);
	}
	
	private static void statusSickToHealthyClient() {
//		Log.i("wwww", "s_h_c");
		sendEvent(STATUS_SICK_TO_HEALTHY_CLIENT);
	}
	
	private static void statusSickToHealthyServer() {
//		Log.i("wwww", "s_h_s");
		sendEvent(STATUS_SICK_TO_HEALTHY_SERVER);
	}
	
	public static void statusChangeClient(@NonNull final String oldStatus, @NonNull final String newStatus) {
		STATUS_CHANGE_SOURCE src = STATUS_CHANGE_SOURCE.CLIENT;
		statusChangeEvent(src, oldStatus, newStatus);
	}
	
	public static void statusChangeServer(@NonNull final String oldStatus, @NonNull final String newStatus) {
		STATUS_CHANGE_SOURCE src = STATUS_CHANGE_SOURCE.SERVER;
		statusChangeEvent(src, oldStatus, newStatus);
	}
	
	private static void statusChangeEvent(final STATUS_CHANGE_SOURCE src, @NonNull final String oldStatus, @NonNull final String newStatus) {
		
		if (StringUtil.isEmpty(oldStatus) || StringUtil.isEmpty(newStatus) || oldStatus.equals(newStatus)) {
			isClientSendingEvent = false;
			return;
		}
		
		if (MainFragmentViewModel.STATUS_HEALTHY.equals(oldStatus)) {
			switch (newStatus) {
				case STATUS_INFECTED: {
					if (src == STATUS_CHANGE_SOURCE.CLIENT) {
						statusHealthyToInfectedClient();
					} else {
						statusHealthyToInfectedServer();
					}
					break;
				}
			}
			return;
		}
		
		if (MainFragmentViewModel.STATUS_INFECTED.equals(oldStatus)) {
			switch (newStatus) {
				case STATUS_HEALTHY: {
					if (src == STATUS_CHANGE_SOURCE.CLIENT) {
						statusInfectedToHealthyClient();
					} else {
						statusInfectedToHealthyServer();
					}
					break;
				}
				
				case STATUS_SICK: {
					if (src == STATUS_CHANGE_SOURCE.CLIENT) {
						statusInfectedToSickClient();
					} else {
						statusInfectedToSickServer();
					}
					break;
				}
			}
			return;
		}
		
		if (MainFragmentViewModel.STATUS_SICK.equals(oldStatus)) {
			switch (newStatus) {
				case STATUS_HEALTHY: {
					if (src == STATUS_CHANGE_SOURCE.CLIENT) {
						statusSickToHealthyClient();
					} else {
						statusSickToHealthyServer();
					}
					break;
				}
			}
			return;
		}
		
		isClientSendingEvent = false;
	}
	
	public static void appForeground() {
		canDetectLaunch = true;
	}
	
	public static void appBackground() {
		canDetectLaunch = false;
	}
	
	public static void detectLaunchType() {
		
		if (!canDetectLaunch) {
			return;
		}
		
		if (StringUtil.isEmpty(startNotificationType)) {
			appStartManual();
		} else {
			switch (startNotificationType) {
				case BUNDLE_NOTIFICATION_SURVEY: {
					appStartSurveyNotif();
					break;
				}
				
				case BUNDLE_NOTIFICATION_REGULAR: {
					appStartService();
					break;
				}
				
				case BUNDLE_NOTIFICATION_REMINDER: {
					appStartReminderNotif();
					break;
				}
				
				case BUNDLE_NOTIFICATION_INTERSECTION: {
					appStartIntersectionNotif();
					break;
				}
			}
		}
		
		canDetectLaunch = false;
		startNotificationType = "";
	}
	
	public static void detectNotificationType(final Intent intent) {
		// Запуск из нотификашки
		if (intent != null && intent.getExtras() != null && intent.getExtras().containsKey(BUNDLE_FROM_NOTIFICATION)) {
			
			final String notificationType = intent.getStringExtra(BUNDLE_FROM_NOTIFICATION);
			
			if (StringUtil.isEmpty(notificationType)) {
				startNotificationType = "";
				return;
			}
			
			switch (notificationType) {
				case BUNDLE_NOTIFICATION_SURVEY: {
					startNotificationType = BUNDLE_NOTIFICATION_SURVEY;
					return;
				}
				
				case BUNDLE_NOTIFICATION_REGULAR: {
					startNotificationType = BUNDLE_NOTIFICATION_REGULAR;
					return;
				}
				
				case BUNDLE_NOTIFICATION_REMINDER: {
					startNotificationType = BUNDLE_NOTIFICATION_REMINDER;
					return;
				}
				
				case BUNDLE_NOTIFICATION_INTERSECTION: {
					startNotificationType = BUNDLE_NOTIFICATION_INTERSECTION;
					return;
				}
			}
		}
		
		// Обычный запуск
		startNotificationType = "";
	}
	
	private static boolean isInitialized() {
		return instance != null && instance.firebase != null;
	}
}
