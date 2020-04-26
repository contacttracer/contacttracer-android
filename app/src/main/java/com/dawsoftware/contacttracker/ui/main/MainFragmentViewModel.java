package com.dawsoftware.contacttracker.ui.main;

import android.app.Application;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.dawsoftware.contacttracker.analytics.Analytics;
import com.dawsoftware.contacttracker.data.Repository;
import com.dawsoftware.contacttracker.data.Repository.GetAppLinkCallback;
import com.dawsoftware.contacttracker.data.Repository.GetCountersFromDbCallback;
import com.dawsoftware.contacttracker.data.Repository.GetDataCallback;
import com.dawsoftware.contacttracker.data.Repository.GetIntersectionsFromDbCallback;
import com.dawsoftware.contacttracker.data.Repository.RegisterCallback;
import com.dawsoftware.contacttracker.data.Repository.SendStatusCallback;
import com.dawsoftware.contacttracker.data.network.models.Intersection;
import com.dawsoftware.contacttracker.data.network.models.UserData;
import com.dawsoftware.contacttracker.util.PreferencesUtil;
import com.dawsoftware.contacttracker.util.livedata.SingleLiveEvent;
import com.dawsoftware.contacttracker.util.StringUtil;
import com.dawsoftware.contacttracker.util.executors.MainThreadExecutor;

public class MainFragmentViewModel extends AndroidViewModel {
	
	public final static String STATUS_HEALTHY = "healthy";
	public final static String STATUS_INFECTED = "possible_infected";
	public final static String STATUS_SICK = "infected";
	
	public final static String SYMPTOM_FEVER = "fever";
	public final static String SYMPTOM_COUGH = "cough";
	public final static String SYMPTOM_DISPNEA = "dyspnea";
	
	private final MainThreadExecutor executors;
	private final Repository repository;
	
	private MutableLiveData<String> installationId;
	private MutableLiveData<String> userId;
	private MutableLiveData<UserData> userData;
	
	public MutableLiveData<UserData> previousUserData;
	
	private MutableLiveData<Boolean> isRefreshingUserData;
	
	private SingleLiveEvent<String> networkErrorToastLiveData;
	private SingleLiveEvent<String> surveySuccessFulToastLiveData;
	
	private MutableLiveData<Boolean> isSurveySubmitting;
	
	private MutableLiveData<String> appLinkLiveData;
	
	private MutableLiveData<List<Intersection>> snapshotOfIntersections;
	
	public MutableLiveData<Long> lastIntersectionTimeSec;
	
	private final PreferencesUtil prefs;
	
	private boolean isRegisterInProgress;
	
	public int newIntersections;
	public int totalIntersectionsCount;
	public int intersectionsSinceYesterday;
	public int uniqueIntersections;
	
	public boolean areUpdatesReceived;
	
	public MainFragmentViewModel(Application application) {
		super(application);
		
		executors = new MainThreadExecutor();
		repository = Repository.getInstance(application.getApplicationContext());
		
		prefs = new PreferencesUtil(application);
		
		initLiveData();
		
		initUserIds();
	}
	
	private void initLiveData() {
		installationId = new MutableLiveData<>();
		userId = new MutableLiveData<>();
		
		userData = new MutableLiveData<>();
		previousUserData = new MutableLiveData<>();
		
		isRefreshingUserData = new MutableLiveData<>();
		isRefreshingUserData.setValue(false);
		
		networkErrorToastLiveData = new SingleLiveEvent<>();
		surveySuccessFulToastLiveData = new SingleLiveEvent<>();
		
		isSurveySubmitting = new MutableLiveData<>();
		isSurveySubmitting.setValue(false);
		
		appLinkLiveData = new MutableLiveData<>();
		appLinkLiveData.setValue("");
		
		lastIntersectionTimeSec = new MutableLiveData<>();
		lastIntersectionTimeSec.setValue(0L);
		
		snapshotOfIntersections = new MutableLiveData<>();
	}
	
	private void initUserIds() {
		if (prefs.getInstallationId().isEmpty()) {
			final String instId = UUID.randomUUID().toString();
			prefs.setInstallationId(instId);
			
			Log.i("wutt", "Generated InstID: " + instId);
		}
		
		installationId.setValue(prefs.getInstallationId());
		
		if (prefs.getUserId().isEmpty()) {
			registerUser();
		} else {
			userId.setValue(prefs.getUserId());
		}
	}
	
	private boolean isRegistered() {
		return !StringUtil.isEmpty(userId.getValue()) && !StringUtil.isEmpty(installationId.getValue());
	}
	
	private void registerUser() {
		registerUser(null);
	}
	
	private void registerUser(@Nullable final Runnable nextOperation) {
		
		if (isRegisterInProgress) {
			return;
		}
		
		final String instId = installationId.getValue();
		if (StringUtil.isEmpty(instId)) {
			return;
		}
		
		final String btMac = prefs.getBtMac();
		
		isRegisterInProgress = true;
		
		repository.register(instId, btMac, new RegisterCallback() {
			@Override
			public void success(final String userId) {
				final MutableLiveData<String> liveData = getUserId();
				if (liveData != null) {
					liveData.postValue(userId);
					Log.i("wutt", "received userId: " + userId);
					
					prefs.setUserId(userId);
					
					executors.execute(() -> {
						isRegisterInProgress = false;
						
						if (nextOperation != null) {
							nextOperation.run();
						}
					});
				}
			}
			
			@Override
			public void fail() {
				Log.i("wutt", "register fail");
				networkErrorToastLiveData.postValue("error");
				isRegisterInProgress = false;
			}
		});
	}
	
	public void sendStatus(final String userSelectedStatus, final ArrayList<String> symptoms, final String oldStatus) {
		
		if (!isRegistered()) {
			registerUser(() -> sendStatus(userSelectedStatus, symptoms, oldStatus));
			return;
		}
		
		isSurveySubmitting.setValue(true);
		
		repository.sendStatus(userSelectedStatus, symptoms, userId.getValue(), installationId.getValue(), new SendStatusCallback() {
			@Override
			public void success() {
				Log.i("wutt", "update status success");
				final MutableLiveData<Boolean> isRefreshingData = getIsRefreshingUserData();
				if (isRefreshingData != null) {
					isRefreshingData.postValue(false);
				}
				
				executors.execute(() -> {
					
					sendStatusChangeToAnalyticsOnStatusSend(oldStatus, userSelectedStatus);
					
					isSurveySubmitting.setValue(false);
					refreshUserData();
					surveySuccessFulToastLiveData.setValue("success");
				});
			}
			
			@Override
			public void fail() {
				Log.i("wutt", "update status fail");
				final MutableLiveData<Boolean> isRefreshingData = getIsRefreshingUserData();
				if (isRefreshingData != null) {
					isRefreshingData.postValue(false);
				}
				
				executors.execute(() -> {
					isSurveySubmitting.setValue(false);
					networkErrorToastLiveData.setValue("error");
				});
			}
		});
	}
	
	private void sendStatusChangeToAnalyticsOnStatusSend(final String oldStatus, final String userSelectedStatus) {
		Analytics.isClientSendingEvent = true;
		Analytics.statusChangeClient(oldStatus, userSelectedStatus);
	}
	
	public void refreshUserData() {
		
		if (!isRegistered()) {
			registerUser(this::refreshUserData);
			return;
		}
		
		UserData currData = getUserData().getValue();
		if (currData == null || currData.infectedTotal == null) {
			repository.getCountersFromDb(new GetCountersFromDbCallback() {
				@Override
				public void success(final Integer total, final Integer last24h, final String status, final Boolean isActive) {
					
					if (total == null || last24h == null || status == null || isActive == null) {
						return;
					}
					
					final UserData newData = new UserData();
					
					newData.infectedTotal = total;
					newData.infected24h = last24h;
					newData.status = status;
					newData.active = isActive;
					
					if (getUserData().getValue() == null || getUserData().getValue().infectedTotal == null) {
						getUserData().postValue(newData);
					}
					
					getDataFromNetwork(newData);
				}
				
				@Override
				public void fail() {
					getDataFromNetwork(null);
				}
			});
		} else {
			getDataFromNetwork(null);
		}
	}
	
	private void getDataFromNetwork(final UserData dbUserData) {
		repository.getData(userId.getValue(), installationId.getValue(), new GetDataCallback() {
			@Override
			public void success(final UserData userData, long lastIntersectionTimeS) {
				final MutableLiveData<UserData> data = getUserData();
				
				if (data != null) {
					final UserData prevUserData = data.getValue();
					if (prevUserData != null) {
						if (prevUserData.intersections != null) {
							prevUserData.intersections.clear();
						}
						
						sendStatusChangeToAnalyticsOnDataRefresh(prevUserData, userData);
					} else {
						sendStatusChangeToAnalyticsOnDataRefresh(dbUserData, userData);
					}
					previousUserData.postValue(data.getValue());
				}
				
				// for screenshoting

//				UserData ud = data.getValue();
//
//				if (ud == null) {
//					ud = new UserData();
//				}
//
//				ud.active = true;
//				ud.status = "possible_infected";
//				ud.infected24h = 2;
//				ud.infectedTotal = 3;
				
				//userData
				data.postValue(userData);
				
				if (lastIntersectionTimeSec != null) {
					lastIntersectionTimeSec.postValue(lastIntersectionTimeS);
				}
				
				final MutableLiveData<Boolean> isRefreshingData = getIsRefreshingUserData();
				if (isRefreshingData != null) {
					isRefreshingData.postValue(false);
				}
			}
			
			@Override
			public void fail() {
				final MutableLiveData<Boolean> isRefreshingData = getIsRefreshingUserData();
				if (isRefreshingData != null) {
					isRefreshingData.postValue(false);
				}
				
				networkErrorToastLiveData.postValue("error");
			}
		});
	}
	
	private void sendStatusChangeToAnalyticsOnDataRefresh(final UserData oldData, final UserData newData) {
		
		if (oldData == null || newData == null) {
			return;
		}
		
		if (!oldData.active && newData.active) {
			Analytics.stateInactiveToActive();
			return;
		}
		
		if (oldData.active && !newData.active) {
			Analytics.stateActiveToInactive();
			return;
		}
		
		String oldStatus = oldData.status;
		String newStatus = newData.status;
		
//		Log.i("wwww", oldStatus + " - " + newStatus);
		
		if (Analytics.isClientSendingEvent) {
			Analytics.isClientSendingEvent = false;
			return;
		}
		
		Analytics.statusChangeServer(oldStatus, newStatus);
	}
	
	public void getAppLink() {
		repository.getAppLink(new GetAppLinkCallback() {
			@Override
			public void success(final String appLink) {
				final MutableLiveData<String> data = getAppLinkLiveData();
				if (data != null) {
					data.postValue(appLink);
				}
			}
			
			@Override
			public void fail() {
				networkErrorToastLiveData.postValue("error");
			}
		});
	}
	
	public void getAllIntersectionsFromDb() {
		repository.getAllIntersectionsFromDbByDESC(new GetIntersectionsFromDbCallback() {
			@Override
			public void success(final List<Intersection> intersections) {
				if (snapshotOfIntersections != null) {
					snapshotOfIntersections.postValue(intersections);
				}
			}
			
			@Override
			public void fail() {
				if (snapshotOfIntersections != null) {
					snapshotOfIntersections.postValue(null);
				}
			}
		});
	}
	
	public MutableLiveData<String> getUserId() {
		return userId;
	}
	
	public MutableLiveData<UserData> getUserData() {
		return userData;
	}
	
	public MutableLiveData<Boolean> getIsRefreshingUserData() {
		return isRefreshingUserData;
	}
	
	public SingleLiveEvent<String> getNetworkErrorToastLiveData() {
		return networkErrorToastLiveData;
	}
	
	public SingleLiveEvent<String> getSurveySuccessFulToastLiveData() {
		return surveySuccessFulToastLiveData;
	}
	
	public MutableLiveData<Boolean> getIsSurveySubmitting() {
		return isSurveySubmitting;
	}
	
	public MutableLiveData<String> getAppLinkLiveData() {
		return appLinkLiveData;
	}
	
	public MutableLiveData<List<Intersection>> getSnapshotOfIntersections() {
		return snapshotOfIntersections;
	}
}