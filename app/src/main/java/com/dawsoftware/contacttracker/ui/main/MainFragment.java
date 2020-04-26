package com.dawsoftware.contacttracker.ui.main;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.dawsoftware.contacttracker.BuildConfig;
import com.dawsoftware.contacttracker.MainActivity;
import com.dawsoftware.contacttracker.MainActivityViewModel;
import com.dawsoftware.contacttracker.R;
import com.dawsoftware.contacttracker.analytics.Analytics;
import com.dawsoftware.contacttracker.data.network.models.UserData;
import com.dawsoftware.contacttracker.services.LocationService;
import com.dawsoftware.contacttracker.ui.survey.SurveyActivity;
import com.dawsoftware.contacttracker.ui.views.CustomScaleView;
import com.dawsoftware.contacttracker.util.AnimatorUtil.CustomAnimatorListener;
import com.dawsoftware.contacttracker.util.DisplayUtil;
import com.dawsoftware.contacttracker.util.IntentUtil;
import com.dawsoftware.contacttracker.util.PreferencesUtil;
import com.dawsoftware.contacttracker.util.StringUtil;
import com.dawsoftware.contacttracker.util.TimerUtil;
import com.dawsoftware.contacttracker.util.ViewUtil;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.core.widget.TextViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle.State;
import androidx.lifecycle.ViewModelProviders;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener;

import static com.dawsoftware.contacttracker.data.Repository.BUNDLE_INFECTED_24H;
import static com.dawsoftware.contacttracker.data.Repository.BUNDLE_INFECTED_NEAR;
import static com.dawsoftware.contacttracker.data.Repository.BUNDLE_INFECTED_TOTAL;
import static com.dawsoftware.contacttracker.data.Repository.BUNDLE_LAST_INTERSECTION_TIME_S;
import static com.dawsoftware.contacttracker.data.Repository.BUNDLE_SEND_SUCCESS;
import static com.dawsoftware.contacttracker.data.Repository.BUNDLE_STATUS;
import static com.dawsoftware.contacttracker.data.Repository.BUNDLE_UNIQUE_INTERSECTIONS;
import static com.dawsoftware.contacttracker.services.LocationService.ACTION_REFRESH_DATA;
import static com.dawsoftware.contacttracker.services.LocationService.BUNDLE_CROWD_COUNTER;
import static com.dawsoftware.contacttracker.services.LocationService.CROWD_DATA;
import static com.dawsoftware.contacttracker.ui.survey.SurveyActivity.MUST_SHARE_RESULT;
import static com.dawsoftware.contacttracker.ui.survey.SurveyActivity.NEW_STATUS_RESULT;
import static com.dawsoftware.contacttracker.ui.survey.SurveyActivity.STATE_BUNDLE_EXTRA;
import static com.dawsoftware.contacttracker.ui.survey.SurveyActivity.STATUS_BUNDLE_EXTRA;
import static com.dawsoftware.contacttracker.ui.survey.SurveyActivity.SYMPTOMS_RESULT;
import static com.dawsoftware.contacttracker.ui.survey.SurveyActivity.TIME_TO_RECOVER_BUNDLE_EXTRA;

public class MainFragment extends Fragment {
	
	private static final int TEN_SECONDS_MS = 1000 * 10;
	
	private static final int SURVEY_FRAGMENT_ACTION = 601;

	private MainFragmentViewModel mMainFragmentViewModel;
	private MainActivityViewModel activityViewModel;
	
	private NestedScrollView scrollView;
	
	private SwipeRefreshLayout swipeLayout;
	private SwipeLayoutListener swipeListener;
	
	private Button surveyButton;
	private SurveyButtonListener surveyButtonListener;
	
	private Button scanButton;
	private ScanButtonListener scanButtonListener;
	
	private FrameLayout statusLayout;
	private ViewGroup statusWithData;
	private ViewGroup statusWithoutData;
	
	private TextView infectedIntersectionsCount;
	private TextView infectedIntersectionsCountAnim;
	
	private TextView infectedIntersectionLastTime;
	
	private ImageView infectedNearIcon;
	
	private TextView sinceYesterdayCounterView;
	private TextView sinceYesterdayTextView;
	private TextView sinceYesterdayCounterAnim;
	
	private TextView infectedPeopleNearCount;
	private TextView infectedPeopleNearCountAnim;
	private ImageView scanningIndicatorView;
	private TextView scanningViewAnim;
	private TextView scanningNoSymptomsView;
	private TextView chosenAnimatingTextView;
	
	private ImageView statusEmotion;
	private TextView statusTitle;
	private TextView statusText;
	
	private View topDivider;
	
	private CustomScaleView riskView;
	private TextView riskTextView;
	private CustomScaleView crowdView;
	private TextView crowdTextView;
	
	private String unknownRisk;
	private String lowRisk;
	private String mediumRisk;
	private String highRisk;
	
	private String unknownCrowd;
	private String lowCrowd;
	private String mediumCrowd;
	private String highCrowd;
	
	private Toast toast;
	
	private LocationService gpsService;
	private Intent intent;
	
	private PreferencesUtil prefs;
	
	private TextView debugView;
	
	private AnimatorSet scanningIndicatorAnimator;
	private AnimatorSet crowdIndicatorAnimator;
	
	private Handler handler;
	private boolean isShowingResultOfScan;
	
	private AlertDialog internetConnectionDialog;
	
	private String nearCounterDefaultText;
	
	@Override
	public void onCreate(@Nullable final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		initResources();
		
		intent = new Intent(getActivity(), LocationService.class);
		
		final Context applicationContext = getActivity().getApplicationContext();
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			ContextCompat.startForegroundService(applicationContext, intent);
		} else {
			applicationContext.startService(intent);
		}
		
		prefs = new PreferencesUtil(applicationContext);
		
		initServiceConnection();
		
		internetConnectionDialog = getInternetConnectionDialog();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		destroyServiceConnection();
	}
	
	public View onCreateView(@NonNull LayoutInflater inflater,
	                         ViewGroup container, Bundle savedInstanceState) {
		
		final View root = inflater.inflate(R.layout.fragment_main, container, false);
		
		handler = new Handler(Looper.getMainLooper());
		
		initViews(root);
		
		return root;
	}
	
	@Override
	public void onPause() {
		super.onPause();
		
		DisplayUtil.setScreenAlwaysOn(false, getActivity());
		
		if (scanningIndicatorAnimator != null) {
			if (scanningIndicatorAnimator.isRunning()) {
				scanningIndicatorAnimator.cancel();
			}
		}
		
		if (crowdIndicatorAnimator != null) {
			if (crowdIndicatorAnimator.isRunning()) {
				crowdIndicatorAnimator.cancel();
			}
		}
		
		if (internetConnectionDialog != null && internetConnectionDialog.isShowing()) {
			internetConnectionDialog.cancel();
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		Analytics.mainScreenOpened(getActivity());
		
		DisplayUtil.setScreenAlwaysOn(true, getActivity());
		
		if (StringUtil.isEmpty(mMainFragmentViewModel.getAppLinkLiveData().getValue())) {
			mMainFragmentViewModel.getAppLink();
		}
		
		if (activityViewModel.isScanningInProgress) {
			if (mMainFragmentViewModel.areUpdatesReceived) {
				setScanUnlock();
				setVisualStatus(mMainFragmentViewModel.getUserData().getValue());
				setDataStatus(mMainFragmentViewModel.getUserData().getValue());
				crowdTextView.setVisibility(View.VISIBLE);
				crowdTextView.setAlpha(1f);
			} else {
				startScanUI();
			}
		} else {
			if (!mMainFragmentViewModel.getIsSurveySubmitting().getValue()) {
				if (activityViewModel.getAllPermissionsGranted().getValue()) {
					checkInternetConnection();
					return;
				}
				mMainFragmentViewModel.refreshUserData();
			}
		}
		
		if (prefs.getIsFirstLaunch()) {
			prefs.setIsFirstLaunch(false);
		}
		
		if (gpsService != null) {
			gpsService.removeCriticalNotifications();
		}
	}
	
	private void initResources() {
		
		final Resources res = getContext().getResources();
		
		unknownRisk = res.getString(R.string.card_risk_unknown);
		lowRisk = res.getString(R.string.card_risk_low);
		mediumRisk = res.getString(R.string.card_risk_medium);
		highRisk = res.getString(R.string.card_risk_high);
		
		unknownCrowd = res.getString(R.string.card_crowd_unknown);
		lowCrowd = res.getString(R.string.card_crowd_low);
		mediumCrowd = res.getString(R.string.card_crowd_medium);
		highCrowd = res.getString(R.string.card_crowd_high);
		
		nearCounterDefaultText = res.getString(R.string.card_no_infected_people_around);
	}
	
	private void initServiceConnection() {
		getActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
		
		IntentFilter filterUpdateUI = new IntentFilter(LocationService.ACTION_UPDATE_UI);
		IntentFilter filterGrantPermissions = new IntentFilter(LocationService.ACTION_GRANT_PERMISSIONS);
		IntentFilter filterRefreshData = new IntentFilter(ACTION_REFRESH_DATA);
		IntentFilter filterCrowdData = new IntentFilter(CROWD_DATA);
		
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(locationServiceMessagesReceiver, filterUpdateUI);
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(locationServiceMessagesReceiver, filterGrantPermissions);
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(locationServiceMessagesReceiver, filterRefreshData);
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(locationServiceMessagesReceiver, filterCrowdData);
	}
	
	private void destroyServiceConnection() {
		getActivity().unbindService(serviceConnection);
		
		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(locationServiceMessagesReceiver);
	}
	
	private void initViews(final View root) {
		scrollView = root.findViewById(R.id.scroll_view);
		
		swipeListener = new SwipeLayoutListener();
		swipeLayout = root.findViewById(R.id.swipe_view);
		swipeLayout.setOnRefreshListener(swipeListener);
		swipeLayout.setEnabled(BuildConfig.DEBUG);
		
		statusLayout = root.findViewById(R.id.status_card_inclusion);
		statusWithData = root.findViewById(R.id.included_status);
		statusWithoutData = root.findViewById(R.id.included_status_no_data);
		
		updateStatusCardBindings(root);
		
		surveyButton = root.findViewById(R.id.survey_button);
		surveyButtonListener = new SurveyButtonListener();
		surveyButton.setOnClickListener(surveyButtonListener);
		surveyButton.setEnabled(false);
		
		scanButton = root.findViewById(R.id.scan_button);
		scanButtonListener = new ScanButtonListener();
		scanButton.setOnClickListener(scanButtonListener);
		scanButton.setEnabled(false);
		
		statusEmotion = root.findViewById(R.id.status_emotion);
		statusTitle = root.findViewById(R.id.status_title_text_view);
		statusText = root.findViewById(R.id.status_text_view);
		
		initStatus(null);

		if (BuildConfig.DEBUG) {
			debugView = root.findViewById(R.id.status_debug_view);

			debugView.setOnClickListener(v -> {
				final ClipboardManager clipboard = (ClipboardManager)getActivity().getSystemService(Context.CLIPBOARD_SERVICE);

				if (clipboard != null) {
					final ClipData clip = android.content.ClipData.newPlainText("Copied Text", debugView.getText());
					clipboard.setPrimaryClip(clip);
					showToast("copied");
				}
			});
		}
	}
	
	private void updateStatusCardBindings(final View root) {
		infectedIntersectionsCount = root.findViewById(R.id.direct_contacts_counter_view);
		infectedIntersectionsCountAnim = root.findViewById(R.id.direct_contacts_counter_anim_view);
		infectedIntersectionLastTime = root.findViewById(R.id.last_time_view_value);
		infectedPeopleNearCount = root.findViewById(R.id.now_near_counter_view);
		infectedPeopleNearCountAnim = root.findViewById(R.id.now_near_counter_view_anim);
		infectedNearIcon = root.findViewById(R.id.card_people_1);
		sinceYesterdayCounterView = root.findViewById(R.id.since_yesterday_counter);
		sinceYesterdayCounterAnim = root.findViewById(R.id.since_yesterday_counter_anim);
		sinceYesterdayTextView = root.findViewById(R.id.since_yesterday_text);
		scanningIndicatorView = root.findViewById(R.id.scanning_indicator);
		scanningViewAnim = root.findViewById(R.id.scanning_view_anim);
		scanningNoSymptomsView = root.findViewById(R.id.scanning_no_symptoms);
		riskView = root.findViewById(R.id.risk_meter_view);
		riskTextView = root.findViewById(R.id.risk_text);
		crowdView = root.findViewById(R.id.crowd_meter_view);
		crowdTextView = root.findViewById(R.id.crowd_text);
	}
	
	private void initStatus(final UserData data) {
		if (statusLayout == null) {
			return;
		}
		
		setVisualStatus(data);
		setDataStatus(data);
	}
	
	private void setVisualStatus(final UserData data) {
		
		if (data == null) {
			setDefaultCard();
			return;
		}
		
		if (data.active) {
			switch (data.status) {
				case MainFragmentViewModel.STATUS_HEALTHY: {
					setHealthyCard();
					break;
				}
				
				case MainFragmentViewModel.STATUS_INFECTED: {
					setInfectedCard();
					break;
				}
				
				case MainFragmentViewModel.STATUS_SICK: {
					setSickCard();
					break;
				}
			}
		} else {
			// status inactive
			setDefaultCard();
		}
	}
	
	private void setDefaultCard() {
		
		statusWithData.setVisibility(View.VISIBLE);
		statusWithoutData.setVisibility(View.GONE);
		
		updateStatusCardBindings(statusWithData);
		
		changeStatusCard(R.drawable.background_unknown,
		                 R.color.sick_red);
		
		changeViews(R.drawable.ic_error_outline_black_24dp,
		            R.color.colorPrimaryDark,
		            R.string.status_unknown_title,
		            R.string.status_unknown_text,
		            R.drawable.button_default_selector);
		
		surveyButton.setVisibility(View.VISIBLE);
		surveyButton.setEnabled(false);
		
		scanButton.setVisibility(View.VISIBLE);
		scanButton.setEnabled(false);
		
		riskView.setFilledElementsCount(0);
		riskTextView.setText(unknownRisk);
	}
	
	private void setHealthyCard() {
		
		statusWithData.setVisibility(View.VISIBLE);
		statusWithoutData.setVisibility(View.GONE);
		
		updateStatusCardBindings(statusWithData);
		
		changeStatusCard(R.drawable.background_healthy,
		                 R.color.sick_red);
		
		changeViews(R.drawable.ic_sentiment_satisfied_black_24dp,
		            R.color.healthy_green,
		            R.string.status_healthy_title,
		            R.string.status_healthy_text,
		            R.drawable.button_default_selector);
		
		surveyButton.setVisibility(View.VISIBLE);
		surveyButton.setEnabled(true);
		
		scanButton.setVisibility(View.VISIBLE);
		scanButton.setEnabled(true);
		
		riskView.setFilledElementsCount(1);
		riskTextView.setText(lowRisk);
	}
	
	private void setInfectedCard() {
		
		statusWithData.setVisibility(View.VISIBLE);
		statusWithoutData.setVisibility(View.GONE);
		
		updateStatusCardBindings(statusWithData);
		
		changeStatusCard(R.drawable.background_potentially_sick,
		                 R.color.sick_red);
		
		changeViews(R.drawable.ic_sentiment_neutral_black_24dp,
		            R.color.infected_yellow,
		            R.string.status_potentially_sick_title,
		            R.string.status_potentially_sick_text,
		            R.drawable.button_infected_selector);
		
		surveyButton.setVisibility(View.VISIBLE);
		surveyButton.setEnabled(true);
		
		scanButton.setVisibility(View.VISIBLE);
		scanButton.setEnabled(true);
		
		riskView.setFilledElementsCount(2);
		riskTextView.setText(mediumRisk);
		
		// for screenshoting
//		handleCrowdCounter(null);
	}
	
	private void setSickCard() {
		
		statusWithData.setVisibility(View.GONE);
		statusWithoutData.setVisibility(View.VISIBLE);
		
		statusWithoutData.setBackground(getResources().getDrawable(R.drawable.background_sick));
		
		updateStatusCardBindings(statusWithoutData);
		
		final TextView text = statusWithoutData.findViewById(R.id.status_no_data_text);
		text.setText(getResources().getString(R.string.status_card_sick_title));
		
		changeViews(R.drawable.ic_sentiment_dissatisfied_black_24dp,
		            R.color.sick_red,
		            R.string.status_sick_title,
		            R.string.status_sick_text,
		            R.drawable.button_sick_selector);
		
		surveyButton.setVisibility(View.VISIBLE);
		surveyButton.setEnabled(true);
		
		scanButton.setVisibility(View.VISIBLE);
		scanButton.setEnabled(false);
	}
	
	private void changeStatusCard(int statusCardBackground, int statusCardInfectedNearColor) {
		
		if (statusWithData != null && statusWithData.getVisibility() == View.VISIBLE) {
			statusWithData.setBackground(getResources().getDrawable(statusCardBackground));
		}
		
		if (statusWithoutData != null && statusWithoutData.getVisibility() == View.VISIBLE) {
			statusWithoutData.setBackground(getResources().getDrawable(statusCardBackground));
		}
		
		ViewUtil.setTint(getContext(), infectedNearIcon, statusCardInfectedNearColor);
		
		infectedPeopleNearCount.setTextColor(getResources().getColor(statusCardInfectedNearColor));
		infectedPeopleNearCountAnim.setTextColor(getResources().getColor(statusCardInfectedNearColor));
		scanningNoSymptomsView.setTextColor(getResources().getColor(statusCardInfectedNearColor));
		
		if (chosenAnimatingTextView == null) {
			chosenAnimatingTextView = infectedPeopleNearCount;
		}
		
		if (isShowingResultOfScan) {
			chosenAnimatingTextView.setVisibility(View.VISIBLE);
			chosenAnimatingTextView.setAlpha(1f);
			scanningIndicatorView.setVisibility(View.INVISIBLE);
			scanningIndicatorView.setAlpha(0f);
		} else {
			chosenAnimatingTextView.setVisibility(View.INVISIBLE);
			chosenAnimatingTextView.setAlpha(0f);
			scanningIndicatorView.setVisibility(View.VISIBLE);
			scanningIndicatorView.setAlpha(1f);
		}
		
		crowdTextView.setVisibility(View.VISIBLE);
		crowdTextView.setAlpha(1f);
	}
	
	private void changeViews(int emotionIcon, int emotionIconTint, int titleString, int textString, int sendButtonBackground) {
		statusEmotion.setImageResource(emotionIcon);
		ViewUtil.setTint(getContext(), statusEmotion, emotionIconTint);
		
		statusTitle.setText(getResources().getText(titleString));
		statusText.setText(getResources().getText(textString));
		
		final Drawable surveyButtonDrawable = getResources().getDrawable(sendButtonBackground);
		surveyButton.setBackground(surveyButtonDrawable);
		
		final Drawable scanButtonDrawable = getResources().getDrawable(sendButtonBackground);
		scanButton.setBackground(scanButtonDrawable);
	}
	
	private void setDataStatus(final UserData data) {
		if (data == null || !data.active) {
			
			final String none = getResources().getString(R.string.status_unknown_counters);
			
			sinceYesterdayTextView.setVisibility(View.GONE);
			sinceYesterdayCounterView.setVisibility(View.GONE);
			
			infectedIntersectionsCount.setText(none);
			infectedIntersectionsCountAnim.setText(none);
			
			infectedIntersectionLastTime.setText(none);
			
			return;
		}
		
		switch (data.status) {
			case MainFragmentViewModel.STATUS_SICK: {
				break;
			}
			
			default: {
				
				//TODO: раскидать по разным методам
				
				if (mMainFragmentViewModel.totalIntersectionsCount > 0) {
					data.infectedTotal = mMainFragmentViewModel.totalIntersectionsCount;
				}
				
				final String intersectionsText = String.valueOf(data.infectedTotal);
				infectedIntersectionsCount.setText(intersectionsText);
				infectedIntersectionsCountAnim.setText(intersectionsText);
				
				final String intersectionTimeAgoH = getLastIntersectionTimeAgoH();
				infectedIntersectionLastTime.setText(intersectionTimeAgoH);
				
				
				if (mMainFragmentViewModel.intersectionsSinceYesterday > 0) {
					data.infected24h = mMainFragmentViewModel.intersectionsSinceYesterday;
				}
				
				if (data.infected24h > 0) {
					sinceYesterdayCounterView.setText("+" + data.infected24h);
					
					sinceYesterdayCounterView.setVisibility(View.VISIBLE);
					sinceYesterdayTextView.setVisibility(View.VISIBLE);
				} else {
					sinceYesterdayTextView.setVisibility(View.GONE);
					sinceYesterdayCounterView.setVisibility(View.GONE);
				}
				
				if (isShowingResultOfScan) {
					// разделение логики для виджета с числом опасных контактов рядом и виджета "нет опасных контактов рядом"
					if (mMainFragmentViewModel.newIntersections > 0) {
						infectedPeopleNearCount.setText(String.valueOf(mMainFragmentViewModel.newIntersections));
					}
				} else {
					mMainFragmentViewModel.newIntersections = 0;
				}
				
				mMainFragmentViewModel.intersectionsSinceYesterday = 0;
				mMainFragmentViewModel.totalIntersectionsCount = 0;
			}
			
			scrollView.smoothScrollTo(0,0);
		}
	}
	
	private String getLastIntersectionTimeAgoH() {
		
		String hourString = getResources().getString(R.string.hours_short);
		String minuteString = getResources().getString(R.string.minutes_short);
		
		Long time = mMainFragmentViewModel.lastIntersectionTimeSec.getValue();
		
		if (time == null) {
			return getResources().getString(R.string.status_unknown_counters);
		}
		
		long diff = time == 0 ? 0 : TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) - time;
		
		long hours = TimeUnit.SECONDS.toHours(diff);
		long minutes = TimeUnit.SECONDS.toMinutes(diff);
		
		
		return minutes > 59 ? hours + " " + hourString : minutes + " " + minuteString;
	}
	
	@Override
	public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		
		topDivider = view.findViewById(R.id.card_inner_divider_top);
		
		initViewModel();
	}
	
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		
		handler.removeCallbacksAndMessages(null);
		
		surveyButton.setOnClickListener(null);
		surveyButtonListener = null;
		
		swipeLayout.setOnRefreshListener(null);
		swipeListener = null;
	}
	
	private boolean isTotalAnimNeeded(final UserData userData, final UserData oldData) {
		
		if (userData == null || oldData == null) {
			return false;
		}
		
		if (!userData.active) {
			return false;
		}
		
		if (MainFragmentViewModel.STATUS_SICK.equals(userData.status)) {
			return false;
		}
		
		if (userData.infectedTotal == null || oldData.infectedTotal == null) {
			return false;
		}
		
		if (userData.infected24h == null || oldData.infected24h == null) {
			return false;
		}
		
		return userData.infectedTotal > oldData.infectedTotal && userData.infected24h > oldData.infected24h;
	}
	
	private void initViewModel() {
		
		final MainActivity activity = (MainActivity) getActivity();
		if (activity == null) {
			return;
		}
		
		mMainFragmentViewModel = ViewModelProviders.of(activity).get(MainFragmentViewModel.class);
		
		mMainFragmentViewModel.getUserId().observe(this, s -> {
			surveyButton.setEnabled(!StringUtil.isEmpty(s));
			if (debugView != null) {
				debugView.setText(s);
				debugView.setVisibility(View.VISIBLE);
			}
		});
		
		mMainFragmentViewModel.getUserData().observe(this, userData -> {
			if (!isShowingResultOfScan) {
				mMainFragmentViewModel.newIntersections = 0;
			}
			
			mMainFragmentViewModel.totalIntersectionsCount = 0;
			mMainFragmentViewModel.intersectionsSinceYesterday = 0;
			
			UserData oldData = null;
			
			if (mMainFragmentViewModel.previousUserData != null && mMainFragmentViewModel.previousUserData.getValue() != null) {
				oldData = mMainFragmentViewModel.previousUserData.getValue();
			}
			
			if (isTotalAnimNeeded(userData, oldData)) {
				runTotalToTodayAnimation(userData, oldData);
			} else {
				layoutInitiation(userData);
			}
		});
		
		mMainFragmentViewModel.getIsRefreshingUserData().observe(this, isRefreshing -> {
			if (isRefreshing != swipeLayout.isRefreshing()) {
				swipeLayout.setRefreshing(isRefreshing);
				surveyButton.setEnabled(!isRefreshing);
			}
		});
		
		mMainFragmentViewModel.getNetworkErrorToastLiveData().observe(this, s -> {
			swipeLayout.setRefreshing(false);
			showToast(getResources().getString(R.string.network_error));
			
			if (activityViewModel.isScanningInProgress) {
				if (scanningIndicatorAnimator != null) {
					if (scanningIndicatorAnimator.isRunning()) {
						scanningIndicatorAnimator.cancel();
					}
					
					scanningIndicatorAnimator = null;
				}
				
				if (crowdIndicatorAnimator != null) {
					if (crowdIndicatorAnimator.isRunning()) {
						crowdIndicatorAnimator.cancel();
					}
					
					crowdIndicatorAnimator = null;
				}
				
				setScanUnlock();
			}
		});
		
		mMainFragmentViewModel.getSurveySuccessFulToastLiveData().observe(this, s -> {
			showToast(getResources().getString(R.string.send_status_successfull));
		});
		
		mMainFragmentViewModel.lastIntersectionTimeSec.observe(this, aLong -> {
			final UserData userData = mMainFragmentViewModel.getUserData().getValue();
			if (userData != null && !MainFragmentViewModel.STATUS_SICK.equals(userData.status)) {
				if (userData.active) {
					infectedIntersectionLastTime.setText(getLastIntersectionTimeAgoH());
				}
			}
       });
		
		activityViewModel = ViewModelProviders.of(getActivity()).get(MainActivityViewModel.class);

		activityViewModel.getAllPermissionsGranted().observe(this, isGranted -> {
			if (isGranted) {
				
				Log.i("wutt", "permissions granted");
				
				if (gpsService != null && !activityViewModel.isScanningInProgress) {
					gpsService.restartTracking();
				}
			}
		});
	}
	
	private void layoutInitiation(final UserData userData) {
		initStatus(userData);
		
		if (userData != null) {
			surveyButton.setEnabled(true);
		}
		
		TimerUtil.refreshSurveyNotificationTimers(userData, getActivity().getApplicationContext());
		
		if (gpsService != null && userData != null) {
			gpsService.tellNewStatusToService(userData);
		}
	}
	
	// Преобразует карточку в изображение и шарит
	private void shareCard() {
		final View view = statusLayout;
		if (view != null) {
			final Bitmap bitmap = ViewUtil.createBitmapFromView(view);
			IntentUtil.shareBitmap(getContext(), bitmap);
		}
	}
	
	private void showToast(final String text) {
		if (toast != null && toast.getView().getWindowVisibility() == View.VISIBLE) {
			return;
		}
		
		toast = Toast.makeText(getContext(), text, Toast.LENGTH_LONG);
		toast.show();
	}
	
	class SurveyButtonListener implements OnClickListener {
		
		@Override
		public void onClick(final View v) {
			
			final UserData data = mMainFragmentViewModel.getUserData().getValue();

			if (data != null) {
				Intent intent = new Intent(getActivity(), SurveyActivity.class);
				intent.putExtra(STATUS_BUNDLE_EXTRA, data.status);
				intent.putExtra(STATE_BUNDLE_EXTRA, data.active);
				intent.putExtra(TIME_TO_RECOVER_BUNDLE_EXTRA, data.timeToRecover);

				startActivityForResult(intent, SURVEY_FRAGMENT_ACTION);
			}
		}
	}
	
	@Override
	public void onActivityResult(final int requestCode, final int resultCode, @Nullable final Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		switch (requestCode) {
			case SURVEY_FRAGMENT_ACTION: {
				
				if (data == null || mMainFragmentViewModel == null) {
					return;
				}
				
				String oldStatus = "";
				
				if (mMainFragmentViewModel.getUserData() != null) {
					final UserData userData = mMainFragmentViewModel.getUserData().getValue();
					if (userData != null) {
						oldStatus = userData.status;
					}
				}
				
				final String newStatus = data.getStringExtra(NEW_STATUS_RESULT);
				final ArrayList<String> symptoms = data.getStringArrayListExtra(SYMPTOMS_RESULT);
				final boolean mustShare = data.getBooleanExtra(MUST_SHARE_RESULT, false);
				
				if (!StringUtil.isEmpty(newStatus)) {
					mMainFragmentViewModel.sendStatus(newStatus, symptoms, oldStatus);
				}
				
				if (mustShare) {
					if (getActivity() != null) {
						Analytics.sharedFromSurvey();
						
						final String url = mMainFragmentViewModel.getAppLinkLiveData().getValue();
						getActivity().startActivity(IntentUtil.getShareIntent(url));
					}
				}
				
				break;
			}
		}
	}
	
	class ScanButtonListener implements OnClickListener {
		
		@Override
		public void onClick(final View v) {
			Analytics.startScanningButton();
			
			if (gpsService != null) {
				startScan();
			}
		}
	}
	
	class SwipeLayoutListener implements OnRefreshListener {
		
		@Override
		public void onRefresh() {
			if (mMainFragmentViewModel != null) {
				if (!activityViewModel.isScanningInProgress) {
					mMainFragmentViewModel.refreshUserData();
				}
			}
		}
	}
	
	private ServiceConnection serviceConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			String name = className.getClassName();
			if (name.endsWith("LocationService")) {
				gpsService = ((LocationService.LocationServiceBinder) service).getService();
				
				gpsService.removeCriticalNotifications();
				
				if (mMainFragmentViewModel.getUserData().getValue() != null) {
					gpsService.tellNewStatusToService(mMainFragmentViewModel.getUserData().getValue());
				}
				
				if (activityViewModel.getAllPermissionsGranted().getValue() && !gpsService.getIsTracking()) {
					Log.i("wutt", "restart service after connect");
					gpsService.restartTracking();
				}
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			if (className.getClassName().equals("LocationService")) {
				gpsService = null;
			}
		}
	};
	
	private void startScan() {
		if (activityViewModel.getAllPermissionsGranted().getValue()) {
			Log.i("wutt", "start scan UI");
			
			if (activityViewModel.isScanningInProgress) {
				Log.i("wutt", "stop scan: already running");
				return;
			}
			
			if (BuildConfig.DEBUG) {
				showToast("Start scanning");
			}
			
			if (mMainFragmentViewModel.getUserData().getValue() == null) {
				Log.i("wutt", "stop scan: user data NULL");
				return;
			}
			
			if (!mMainFragmentViewModel.getUserData().getValue().active) {
				Log.i("wutt", "stop scan: user inactive");
				return;
			}
			
			gpsService.collectDataImmediately();
			
			startScanUI();
		}
	}
	
	private void startScanUI() {
		setScanLock();
		
		if (scanningIndicatorAnimator != null) {
			if (scanningIndicatorAnimator.isRunning()) {
				scanningIndicatorAnimator.cancel();
			}
			
			scanningIndicatorAnimator = null;
		}
		
		scanningIndicatorAnimator = scanningIndicatorAnimation();
		
		if (crowdIndicatorAnimator != null) {
			if (crowdIndicatorAnimator.isRunning()) {
				crowdIndicatorAnimator.cancel();
			}
			
			crowdIndicatorAnimator = null;
		}
		
		crowdIndicatorAnimator = crowdAnimationSet();
		
		chosenAnimatingTextView.setVisibility(View.INVISIBLE);
		
		// for screenshoting
//		crowdTextView.setText(getContext().getResources().getString(R.string.tracing_crowd_anim));
		
		scanningIndicatorAnimator.start();
		crowdIndicatorAnimator.start();
	}
	
	private void setScanLock() {
		activityViewModel.isScanningInProgress = true;
		handler.removeCallbacks(hidingResultMessage);
		
		surveyButton.setEnabled(false);
		scanButton.setEnabled(false);
		
		if (BuildConfig.DEBUG) {
			swipeLayout.setEnabled(false);
		}
	}
	
	private void setScanUnlock() {
		activityViewModel.isScanningInProgress = false;
		mMainFragmentViewModel.areUpdatesReceived = false;
		
		surveyButton.setEnabled(true);
		scanButton.setEnabled(true);
		
		if (BuildConfig.DEBUG) {
			swipeLayout.setEnabled(true);
		}
		
		if (!activityViewModel.isScanningInProgress) {
			runHidingTimer();
		}
	}
	
	private void runHidingTimer() {
		isShowingResultOfScan = true;
		
		handler.removeCallbacks(hidingResultMessage);
		handler.postDelayed(hidingResultMessage, TEN_SECONDS_MS);
	}
	
	private Runnable hidingResultMessage = new Runnable() {
		@Override
		public void run() {
			isShowingResultOfScan = false;
			hideNearContactsAnimation();
		}
	};
	
	private BroadcastReceiver locationServiceMessagesReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			
			if (intent == null) {
				return;
			}
			
			final String action = intent.getAction();
			if (action == null) {
				return;
			}
			
			switch (action) {
				case LocationService.ACTION_UPDATE_UI: {
					
					Log.i("wutt", "stop scan UI");
					
					final Bundle bundle = intent.getExtras();
					if (bundle != null) {
						handleNewIntersections(bundle);
					}
					
					break;
				}
				
				case LocationService.ACTION_REFRESH_DATA: {
					if (!activityViewModel.isScanningInProgress) {
						mMainFragmentViewModel.refreshUserData();
					}
					
					break;
				}
				
				case LocationService.ACTION_GRANT_PERMISSIONS: {
					MainActivity activity = (MainActivity)getActivity();
					if (activity != null) {
						activity.serviceRequestsPermissions();
					}
					
					break;
				}
				
				case LocationService.CROWD_DATA: {
					final Bundle bundle = intent.getExtras();
					if (bundle != null) {
						handleCrowdCounter(bundle);
					}
					
					break;
				}
			}
			
		}
	};
	
	private void handleCrowdCounter(final Bundle data) {
		
		final UserData userData;

		if (mMainFragmentViewModel == null || mMainFragmentViewModel.getUserData() == null) {
			return;
		}

		userData = mMainFragmentViewModel.getUserData().getValue();

		if (userData == null || !userData.active) {
			return;
		}
		
		// for screenshoting
//		 final int counter = 1;
		final int counter = data.getInt(BUNDLE_CROWD_COUNTER);
		
		if (crowdView == null || crowdTextView == null) {
			return;
		}
		
		if (counter <= 0) {
			crowdView.setFilledElementsCount(0);
			crowdTextView.setText(unknownCrowd);
			return;
		}
		
		if (counter < 5) {
			crowdView.setFilledElementsCount(1);
			crowdTextView.setText(lowCrowd);
			return;
		}
		
		if (counter < 15) {
			crowdView.setFilledElementsCount(2);
			crowdTextView.setText(mediumCrowd);
			return;
		}
		
		crowdView.setFilledElementsCount(3);
		crowdTextView.setText(highCrowd);
	}
	
	private void handleNewIntersections(final Bundle data) {
		
		final boolean isSuccess = data.getBoolean(BUNDLE_SEND_SUCCESS);
		
		if (!isSuccess) {
			mMainFragmentViewModel.getNetworkErrorToastLiveData().setValue("");
			return;
		}
		
		String newStatus = data.getString(BUNDLE_STATUS);
		Integer newTotalCount = data.getInt(BUNDLE_INFECTED_TOTAL);
		Integer newNearCount = data.getInt(BUNDLE_INFECTED_NEAR);
		Integer new24hCount = data.getInt(BUNDLE_INFECTED_24H);
		Long lastIntersectionTimeS = data.getLong(BUNDLE_LAST_INTERSECTION_TIME_S);
		Integer uniqueIntersectionsCount = data.getInt(BUNDLE_UNIQUE_INTERSECTIONS);
		
		mMainFragmentViewModel.areUpdatesReceived = true;
		
		final UserData currentUserData = mMainFragmentViewModel.getUserData().getValue();
		if (currentUserData != null) {
			if (!currentUserData.status.equals(newStatus) || !currentUserData.infected24h.equals(new24hCount)) {
				if (gpsService != null) {
					UserData newUserData = new UserData();
					newUserData.status = newStatus;
					newUserData.infected24h = new24hCount;
					newUserData.active = currentUserData.active;
					newUserData.infectedTotal = newTotalCount;
					newUserData.timeToRecover = currentUserData.timeToRecover;
					
					gpsService.tellNewStatusToService(newUserData);
				}
			}
		}
		
		if (lastIntersectionTimeS > 0) {
			mMainFragmentViewModel.lastIntersectionTimeSec.setValue(lastIntersectionTimeS);
		}
		
		if (mMainFragmentViewModel.getUserData().getValue() != null && newStatus != null) {
			mMainFragmentViewModel.getUserData().getValue().status = newStatus;
		}
		
		// Animation test
//		newNearCount = 5;
//		uniqueIntersectionsCount = 5;
//		new24hCount = 5;
//		newTotalCount = 5;
		
		mMainFragmentViewModel.newIntersections = newNearCount;
		mMainFragmentViewModel.intersectionsSinceYesterday = new24hCount;
		mMainFragmentViewModel.totalIntersectionsCount = newTotalCount;
		
		mMainFragmentViewModel.uniqueIntersections = uniqueIntersectionsCount;
		
		if (getLifecycle().getCurrentState().ordinal() <= State.STARTED.ordinal()) {
			return;
		}
		
		if (scanningIndicatorAnimator != null) {
			if (scanningIndicatorAnimator.isRunning()) {
				scanningIndicatorAnimator.cancel();
			}
			
			scanningIndicatorAnimator = null;
		}
		
		if (crowdIndicatorAnimator != null) {
			if (crowdIndicatorAnimator.isRunning()) {
				crowdIndicatorAnimator.cancel();
			}
			
			crowdIndicatorAnimator = null;
		}
		
		if (BuildConfig.DEBUG) {
			showToast("Found: " + newNearCount);
		}
		
		boolean isThereAreUniqueIntersections = uniqueIntersectionsCount > 0 && mMainFragmentViewModel.newIntersections > 0;
		final String uniqueText = isThereAreUniqueIntersections ? String.valueOf(uniqueIntersectionsCount) : "0";
		
		if (mMainFragmentViewModel.newIntersections > 0) {
			infectedPeopleNearCount.setText(String.valueOf(mMainFragmentViewModel.newIntersections));
			infectedPeopleNearCountAnim.setText(uniqueText);
			chosenAnimatingTextView = infectedPeopleNearCount;
		} else {
			chosenAnimatingTextView = scanningNoSymptomsView;
		}
		
		Animation appearAnimation = resultAppearAnimation(isThereAreUniqueIntersections);
		
		chosenAnimatingTextView.startAnimation(appearAnimation);
	}
	
	private Animation resultAppearAnimation(boolean isThereAreUniqueIntersections) {
		
		Animation aniFade = AnimationUtils.loadAnimation(getContext().getApplicationContext(), R.anim.fade_in);
		aniFade.setAnimationListener(new AnimationListener() {
			@Override
			public void onAnimationStart(final Animation animation) {
				chosenAnimatingTextView.setAlpha(0f);
				chosenAnimatingTextView.setVisibility(View.INVISIBLE);
			}
			
			@Override
			public void onAnimationEnd(final Animation animation) {
				chosenAnimatingTextView.setAlpha(1f);
				chosenAnimatingTextView.setVisibility(View.VISIBLE);
				
				if (isThereAreUniqueIntersections) {
					runTranslateAnimation();
				} else {
					setScanUnlock();
					setVisualStatus(mMainFragmentViewModel.getUserData().getValue());
					setDataStatus(mMainFragmentViewModel.getUserData().getValue());
				}
				
			}
			
			@Override
			public void onAnimationRepeat(final Animation animation) {}
		});
		
		return aniFade;
	}
	
	private void runTranslateAnimation() {
		
		if (mMainFragmentViewModel.newIntersections <= 0) {
			return;
		}
		
		AnimatorSet nearCountAnims = nearCountAnimation();
		AnimatorSet sinceYesterdayScaleSet = sinceYesterdayAnimation();
		
		AnimatorSet rightPart = new AnimatorSet();
		rightPart.playSequentially(nearCountAnims, sinceYesterdayScaleSet);
		
		sinceYesterdayScaleSet.addListener(new AnimatorListener() {
			@Override
			public void onAnimationStart(final Animator animation) {
				sinceYesterdayCounterView.setVisibility(View.VISIBLE);
				sinceYesterdayTextView.setVisibility(View.VISIBLE);
				
				updateSinceYesterdayViewValue();
			}
			
			@Override
			public void onAnimationEnd(final Animator animation) { }
			
			@Override
			public void onAnimationCancel(final Animator animation) { }
			
			@Override
			public void onAnimationRepeat(final Animator animation) { }
		});
		
		rightPart.addListener(new AnimatorListener() {
			@Override
			public void onAnimationStart(final Animator animation) { }
			
			@Override
			public void onAnimationEnd(final Animator animation) {
				runLeftPartAnimations();
			}
			
			@Override
			public void onAnimationCancel(final Animator animation) {
				setVisualStatus(mMainFragmentViewModel.getUserData().getValue());
				setDataStatus(mMainFragmentViewModel.getUserData().getValue());
				setScanUnlock();
			}
			
			@Override
			public void onAnimationRepeat(final Animator animation) { }
		});
		
		rightPart.start();
	}
	
	private void runLeftPartAnimations() {
		AnimatorSet sinceYesterdayMoveAnims = sinceYesterdayToIntersectionsAnimation();
		sinceYesterdayMoveAnims.setStartDelay(200);
		
		sinceYesterdayMoveAnims.addListener(new AnimatorListener() {
			@Override
			public void onAnimationStart(final Animator animation) {
				sinceYesterdayCounterAnim.setVisibility(View.VISIBLE);
			}
			
			@Override
			public void onAnimationEnd(final Animator animation) {
				sinceYesterdayCounterAnim.setVisibility(View.GONE);
				sinceYesterdayCounterAnim.setTranslationY(0);
				sinceYesterdayCounterAnim.setTranslationX(0);
				sinceYesterdayCounterAnim.setScaleY(1f);
				sinceYesterdayCounterAnim.setScaleX(1f);
			}
			
			@Override
			public void onAnimationCancel(final Animator animation) { }
			
			@Override
			public void onAnimationRepeat(final Animator animation) { }
		});
		
		AnimatorSet totalIntersectionsAnimation = totalIntersectionsAnimation();
		totalIntersectionsAnimation.addListener(new AnimatorListener() {
			@Override
			public void onAnimationStart(final Animator animation) {
				infectedIntersectionsCount.setText(String.valueOf(mMainFragmentViewModel.totalIntersectionsCount));
			}
			
			@Override
			public void onAnimationEnd(final Animator animation) { }
			
			@Override
			public void onAnimationCancel(final Animator animation) { }
			
			@Override
			public void onAnimationRepeat(final Animator animation) { }
		});
		
		AnimatorSet leftPart = new AnimatorSet();
		leftPart.playSequentially(sinceYesterdayMoveAnims, totalIntersectionsAnimation);
		
		leftPart.addListener(new AnimatorListener() {
			@Override
			public void onAnimationStart(final Animator animation) { }
			
			@Override
			public void onAnimationEnd(final Animator animation) {
				setScanUnlock();
				setVisualStatus(mMainFragmentViewModel.getUserData().getValue());
				setDataStatus(mMainFragmentViewModel.getUserData().getValue());
				
				activityViewModel.getMustShowBottomBadge().setValue(true);
			}
			
			@Override
			public void onAnimationCancel(final Animator animation) {
				setScanUnlock();
				setVisualStatus(mMainFragmentViewModel.getUserData().getValue());
				setDataStatus(mMainFragmentViewModel.getUserData().getValue());
				
				activityViewModel.getMustShowBottomBadge().setValue(true);
			}
			
			@Override
			public void onAnimationRepeat(final Animator animation) { }
		});
		
		leftPart.start();
	}
	
	private AnimatorSet nearCountAnimation() {
		Rect endRect = new Rect();
		topDivider.getGlobalVisibleRect(endRect);
		
		Rect startRect = new Rect();
		scanningIndicatorView.getGlobalVisibleRect(startRect);
		
		ObjectAnimator translateX = new ObjectAnimator();
		translateX.setPropertyName("translationX");
		translateX.setFloatValues(0, endRect.right - startRect.left + 20);
		translateX.setTarget(infectedPeopleNearCountAnim);
		
		ObjectAnimator translateY = new ObjectAnimator();
		translateY.setPropertyName("translationY");
		translateY.setFloatValues(0, -35);
		translateY.setTarget(infectedPeopleNearCountAnim);
		
		AnimatorSet translateSet = new AnimatorSet();
		translateSet.playTogether(translateX, translateY);
		
		ObjectAnimator scaleX = new ObjectAnimator();
		scaleX.setPropertyName("scaleX");
		scaleX.setFloatValues(1f, 0.95f);
		scaleX.setTarget(infectedPeopleNearCountAnim);
		
		ObjectAnimator scaleY = new ObjectAnimator();
		scaleY.setPropertyName("scaleY");
		scaleY.setFloatValues(1f, 0.95f);
		scaleY.setTarget(infectedPeopleNearCountAnim);
		
		AnimatorSet scaleSet = new AnimatorSet();
		scaleSet.playTogether(scaleX, scaleY);
		
		AnimatorSet uniteSet = new AnimatorSet();
		uniteSet.setDuration(600);
		
		uniteSet.addListener(new AnimatorListener() {
			@Override
			public void onAnimationStart(final Animator animation) {
				infectedPeopleNearCountAnim.setVisibility(View.VISIBLE);
			}
			
			@Override
			public void onAnimationEnd(final Animator animation) {
				infectedPeopleNearCountAnim.setVisibility(View.GONE);
			}
			
			@Override
			public void onAnimationCancel(final Animator animation) { }
			
			@Override
			public void onAnimationRepeat(final Animator animation) { }
		});
		
		uniteSet.playTogether(translateSet, scaleSet);
		
		return uniteSet;
	}
	
	/**
	 * Вызывается при старте анимации И ДО обнуления новых значений при вызове {@link MainFragment#setDataStatus(UserData)}
	 */
	private void updateSinceYesterdayViewValue() {
		
		if (mMainFragmentViewModel.intersectionsSinceYesterday <= 0) {
			return;
		}
		
		if (sinceYesterdayCounterView.getVisibility() != View.VISIBLE) {
			sinceYesterdayCounterView.setVisibility(View.VISIBLE);
			sinceYesterdayTextView.setVisibility(View.VISIBLE);
		}
		
		sinceYesterdayCounterView.setText("+"+mMainFragmentViewModel.intersectionsSinceYesterday);
		sinceYesterdayCounterAnim.setText("+"+mMainFragmentViewModel.uniqueIntersections);
	}
	
	private void updateSinceYesterdayByNew(int newValue) {
		
		if (sinceYesterdayCounterView.getVisibility() != View.VISIBLE) {
			sinceYesterdayCounterView.setVisibility(View.VISIBLE);
			sinceYesterdayTextView.setVisibility(View.VISIBLE);
		}
		
		sinceYesterdayCounterView.setText("+" + String.valueOf(newValue));
		sinceYesterdayCounterAnim.setText("+" + String.valueOf(newValue));
	}
	
	private AnimatorSet sinceYesterdayAnimation() {
		ObjectAnimator scaleX = new ObjectAnimator();
		scaleX.setTarget(sinceYesterdayCounterView);
		scaleX.setPropertyName("scaleX");
		scaleX.setFloatValues(sinceYesterdayCounterView.getScaleX(), sinceYesterdayCounterView.getScaleX()*1.5f);
		
		ObjectAnimator scaleY = new ObjectAnimator();
		scaleY.setTarget(sinceYesterdayCounterView);
		scaleY.setPropertyName("scaleY");
		scaleY.setFloatValues(sinceYesterdayCounterView.getScaleY(), sinceYesterdayCounterView.getScaleY()*1.5f);
		
		AnimatorSet scaleSet = new AnimatorSet();
		scaleSet.playTogether(scaleX, scaleY);
		
		ObjectAnimator scaleXreverse = new ObjectAnimator();
		scaleXreverse.setTarget(sinceYesterdayCounterView);
		scaleXreverse.setPropertyName("scaleX");
		scaleXreverse.setFloatValues(sinceYesterdayCounterView.getScaleX()*1.5f, sinceYesterdayCounterView.getScaleX());
		
		ObjectAnimator scaleYreverse = new ObjectAnimator();
		scaleYreverse.setTarget(sinceYesterdayCounterView);
		scaleYreverse.setPropertyName("scaleY");
		scaleYreverse.setFloatValues(sinceYesterdayCounterView.getScaleY()*1.5f, sinceYesterdayCounterView.getScaleY());
		
		AnimatorSet scaleSetReverse = new AnimatorSet();
		scaleSetReverse.playTogether(scaleXreverse, scaleYreverse);
		
		AnimatorSet uniteSet = new AnimatorSet();
		uniteSet.setDuration(200);
		uniteSet.playSequentially(scaleSet, scaleSetReverse);
		
		return uniteSet;
	}
	
	private AnimatorSet sinceYesterdayToIntersectionsAnimation() {
		ObjectAnimator scaleX = new ObjectAnimator();
		scaleX.setTarget(sinceYesterdayCounterAnim);
		scaleX.setPropertyName("scaleX");
		scaleX.setFloatValues(sinceYesterdayCounterView.getScaleX(), sinceYesterdayCounterView.getScaleX()*1.5f);
		
		ObjectAnimator scaleY = new ObjectAnimator();
		scaleY.setTarget(sinceYesterdayCounterAnim);
		scaleY.setPropertyName("scaleY");
		scaleY.setFloatValues(sinceYesterdayCounterView.getScaleY(), sinceYesterdayCounterView.getScaleY()*1.5f);
		
		AnimatorSet scaleSet = new AnimatorSet();
		scaleSet.playTogether(scaleX, scaleY);
		
		Rect endRect = new Rect();
		infectedIntersectionsCount.getGlobalVisibleRect(endRect);
		
		Rect startRect = new Rect();
		sinceYesterdayCounterView.getGlobalVisibleRect(startRect);
		
		ObjectAnimator translateX = new ObjectAnimator();
		translateX.setPropertyName("translationX");
		translateX.setFloatValues(0, endRect.right - startRect.left - 20);
		translateX.setTarget(sinceYesterdayCounterAnim);
		
		ObjectAnimator translateY = new ObjectAnimator();
		translateY.setPropertyName("translationY");
		translateY.setFloatValues(0, 35);
		translateY.setTarget(sinceYesterdayCounterAnim);
		
		AnimatorSet translateSet = new AnimatorSet();
		translateSet.playTogether(translateX, translateY);
		
		AnimatorSet scaleSetReverse = new AnimatorSet();
		scaleSetReverse.playTogether(scaleSet, translateSet);
		
		AnimatorSet uniteSet = new AnimatorSet();
		uniteSet.setDuration(600);
		uniteSet.playTogether(scaleSet, scaleSetReverse);
		
		return uniteSet;
	}
	
	private AnimatorSet totalIntersectionsAnimation() {
		ObjectAnimator scaleX = new ObjectAnimator();
		scaleX.setTarget(infectedIntersectionsCount);
		scaleX.setPropertyName("scaleX");
		scaleX.setFloatValues(infectedIntersectionsCount.getScaleX(), infectedIntersectionsCount.getScaleX()*1.2f);
		
		ObjectAnimator scaleY = new ObjectAnimator();
		scaleY.setTarget(infectedIntersectionsCount);
		scaleY.setPropertyName("scaleY");
		scaleY.setFloatValues(infectedIntersectionsCount.getScaleY(), infectedIntersectionsCount.getScaleY()*1.2f);
		
		AnimatorSet scaleSet = new AnimatorSet();
		scaleSet.playTogether(scaleX, scaleY);
		
		ObjectAnimator scaleXreverse = new ObjectAnimator();
		scaleXreverse.setTarget(infectedIntersectionsCount);
		scaleXreverse.setPropertyName("scaleX");
		scaleXreverse.setFloatValues(infectedIntersectionsCount.getScaleX()*1.2f, infectedIntersectionsCount.getScaleX());
		
		ObjectAnimator scaleYreverse = new ObjectAnimator();
		scaleYreverse.setTarget(infectedIntersectionsCount);
		scaleYreverse.setPropertyName("scaleY");
		scaleYreverse.setFloatValues(infectedIntersectionsCount.getScaleY()*1.2f, infectedIntersectionsCount.getScaleY());
		
		AnimatorSet scaleSetReverse = new AnimatorSet();
		scaleSetReverse.playTogether(scaleXreverse, scaleYreverse);
		
		AnimatorSet uniteSet = new AnimatorSet();
		uniteSet.setDuration(200);
		uniteSet.playSequentially(scaleSet, scaleSetReverse);
		
		return uniteSet;
	}
	
	private AnimatorSet scanningIndicatorAnimation() {
		AnimatorSet set = new AnimatorSet();
		
		ObjectAnimator imageIn = (ObjectAnimator)AnimatorInflater.loadAnimator(getContext(), R.animator.fade_in);
		ObjectAnimator imageOut = (ObjectAnimator)AnimatorInflater.loadAnimator(getContext(), R.animator.fade_out);
		
		AnimatorSet imageSet = new AnimatorSet();
		imageSet.playSequentially(imageIn, imageOut);
		imageSet.setTarget(scanningIndicatorView);
		
		imageSet.addListener(new AnimatorListener() {
			@Override
			public void onAnimationStart(final Animator animation) {
				scanningIndicatorView.setAlpha(0f);
				scanningIndicatorView.setVisibility(View.VISIBLE);
			}
			
			@Override
			public void onAnimationEnd(final Animator animation) {
				scanningIndicatorView.setAlpha(0f);
				scanningIndicatorView.setVisibility(View.INVISIBLE);
			}
			
			@Override
			public void onAnimationCancel(final Animator animation) {
				scanningIndicatorView.setAlpha(0f);
				scanningIndicatorView.setVisibility(View.INVISIBLE);
			}
			
			@Override
			public void onAnimationRepeat(final Animator animation) { }
		});
		
		ObjectAnimator textIn = (ObjectAnimator)AnimatorInflater.loadAnimator(getContext(), R.animator.fade_in);
		ObjectAnimator textOut = (ObjectAnimator)AnimatorInflater.loadAnimator(getContext(), R.animator.fade_out);
		
		AnimatorSet textSet = new AnimatorSet();
		textSet.playSequentially(textIn, textOut);
		textSet.setTarget(scanningViewAnim);
		
		textSet.addListener(new AnimatorListener() {
			@Override
			public void onAnimationStart(final Animator animation) {
				scanningViewAnim.setAlpha(0f);
				scanningViewAnim.setVisibility(View.VISIBLE);
			}
			
			@Override
			public void onAnimationEnd(final Animator animation) {
				scanningViewAnim.setAlpha(0f);
				scanningViewAnim.setVisibility(View.INVISIBLE);
			}
			
			@Override
			public void onAnimationCancel(final Animator animation) {
				scanningViewAnim.setAlpha(0f);
				scanningViewAnim.setVisibility(View.INVISIBLE);
			}
			
			@Override
			public void onAnimationRepeat(final Animator animation) { }
		});
		
		set.playSequentially(imageSet, textSet);
		set.addListener(new CustomAnimatorListener() {
			@Override
			public void onAnimationEnd(final Animator animation) {
				if (!mustStop) {
					set.start();
				} else {
					scanningViewAnim.setAlpha(0f);
					scanningViewAnim.setVisibility(View.INVISIBLE);
					
					scanningIndicatorView.setAlpha(0f);
					scanningIndicatorView.setVisibility(View.INVISIBLE);
				}
			}
			
			@Override
			public void onAnimationCancel(final Animator animation) {
				super.onAnimationCancel(animation);
				
				scanningViewAnim.setAlpha(0f);
				scanningViewAnim.setVisibility(View.INVISIBLE);
				
				scanningIndicatorView.setAlpha(0f);
				scanningIndicatorView.setVisibility(View.INVISIBLE);
			}
		});
		
		return set;
	}
	
	private AnimatorSet crowdAnimationSet() {
		final AnimatorSet crowdTextAnimator = new AnimatorSet();
		ObjectAnimator crowdIn = (ObjectAnimator)AnimatorInflater.loadAnimator(getContext(), R.animator.fade_in);
		ObjectAnimator crowdOut = (ObjectAnimator)AnimatorInflater.loadAnimator(getContext(), R.animator.fade_out);
		crowdTextAnimator.playSequentially(crowdIn, crowdOut);
		crowdTextAnimator.setTarget(crowdTextView);
		
		crowdTextAnimator.addListener(new CustomAnimatorListener() {
			
			@Override
			public void onAnimationStart(final Animator animation) {
				super.onAnimationStart(animation);
				crowdTextView.setText(getContext().getResources().getString(R.string.tracing_crowd_anim));
			}
			
			@Override
			public void onAnimationEnd(final Animator animation) {
				if (!mustStop) {
					crowdTextAnimator.start();
				} else {
					crowdTextView.setAlpha(1f);
					crowdTextView.setVisibility(View.VISIBLE);
				}
			}
			
			@Override
			public void onAnimationCancel(final Animator animation) {
				crowdTextView.setAlpha(1f);
				crowdTextView.setVisibility(View.VISIBLE);
				super.onAnimationCancel(animation);
			}
		});
		
		return crowdTextAnimator;
	}
	
	private void hideNearContactsAnimation() {
		
		if (scanningIndicatorView == null || chosenAnimatingTextView == null) {
			return;
		}
		
		ObjectAnimator out = (ObjectAnimator)AnimatorInflater.loadAnimator(getContext(), R.animator.fade_out);
		out.setTarget(chosenAnimatingTextView);
		
		out.addListener(new AnimatorListener() {
			@Override
			public void onAnimationStart(final Animator animation) {
				chosenAnimatingTextView.setAlpha(1f);
				chosenAnimatingTextView.setVisibility(View.VISIBLE);
			}
			
			@Override
			public void onAnimationEnd(final Animator animation) { }
			
			@Override
			public void onAnimationCancel(final Animator animation) { }
			
			@Override
			public void onAnimationRepeat(final Animator animation) { }
		});
		
		ObjectAnimator in = (ObjectAnimator)AnimatorInflater.loadAnimator(getContext(), R.animator.fade_in);
		in.setTarget(scanningIndicatorView);
		
		in.addListener(new AnimatorListener() {
			@Override
			public void onAnimationStart(final Animator animation) {
				scanningIndicatorView.setAlpha(1f);
				scanningIndicatorView.setVisibility(View.VISIBLE);
			}
			
			@Override
			public void onAnimationEnd(final Animator animation) { }
			
			@Override
			public void onAnimationCancel(final Animator animation) { }
			
			@Override
			public void onAnimationRepeat(final Animator animation) { }
		});
		
		AnimatorSet animSet = new AnimatorSet();
		animSet.playSequentially(out, in);
		animSet.start();
	}
	
	private void runTotalToTodayAnimation(final UserData userData, final UserData oldData) {
		
		int diff = userData.infectedTotal - oldData.infectedTotal;
		
		if (diff > 0) {
			mMainFragmentViewModel.uniqueIntersections = diff;
		}
		
		AnimatorSet unite = new AnimatorSet();
		
		AnimatorSet totalToToday = totalCountToTodayAnimation();
		totalToToday.addListener(new AnimatorListener() {
			@Override
			public void onAnimationStart(final Animator animation) {
				String text = String.valueOf(diff);
				infectedIntersectionsCountAnim.setText(text);
				
				infectedIntersectionsCount.setText(String.valueOf(userData.infectedTotal));
			}
			
			@Override
			public void onAnimationEnd(final Animator animation) { }
			
			@Override
			public void onAnimationCancel(final Animator animation) { }
			
			@Override
			public void onAnimationRepeat(final Animator animation) { }
		});
		
		
		AnimatorSet yesterdayAnimation = sinceYesterdayAnimation();
		yesterdayAnimation.addListener(new AnimatorListener() {
			@Override
			public void onAnimationStart(final Animator animation) {
				sinceYesterdayCounterView.setVisibility(View.VISIBLE);
				sinceYesterdayTextView.setVisibility(View.VISIBLE);
				
				updateSinceYesterdayByNew(diff);
			}
			
			@Override
			public void onAnimationEnd(final Animator animation) { }
			
			@Override
			public void onAnimationCancel(final Animator animation) { }
			
			@Override
			public void onAnimationRepeat(final Animator animation) { }
		});
		
		unite.addListener(new AnimatorListener() {
			@Override
			public void onAnimationStart(final Animator animation) { }
			
			@Override
			public void onAnimationEnd(final Animator animation) {
				layoutInitiation(userData);
				activityViewModel.getMustShowBottomBadge().setValue(true);
			}
			
			@Override
			public void onAnimationCancel(final Animator animation) {
				layoutInitiation(userData);
				activityViewModel.getMustShowBottomBadge().setValue(true);
			}
			
			@Override
			public void onAnimationRepeat(final Animator animation) { }
		});
		
		unite.playSequentially(totalToToday, yesterdayAnimation);
		unite.start();
	}
	
	private AnimatorSet totalCountToTodayAnimation() {
		Rect endRect = new Rect();
		topDivider.getGlobalVisibleRect(endRect);
		
		Rect startRect = new Rect();
		infectedIntersectionsCount.getGlobalVisibleRect(startRect);
		
		ObjectAnimator translateX = new ObjectAnimator();
		translateX.setPropertyName("translationX");
		translateX.setFloatValues(0, endRect.left - startRect.left - 20);
		translateX.setTarget(infectedIntersectionsCountAnim);
		
		ObjectAnimator translateY = new ObjectAnimator();
		translateY.setPropertyName("translationY");
		translateY.setFloatValues(0, -35);
		translateY.setTarget(infectedIntersectionsCountAnim);
		
		AnimatorSet translateSet = new AnimatorSet();
		translateSet.playTogether(translateX, translateY);
		
		ObjectAnimator scaleX = new ObjectAnimator();
		scaleX.setPropertyName("scaleX");
		scaleX.setFloatValues(1f, 0.95f);
		scaleX.setTarget(infectedIntersectionsCountAnim);
		
		ObjectAnimator scaleY = new ObjectAnimator();
		scaleY.setPropertyName("scaleY");
		scaleY.setFloatValues(1f, 0.95f);
		scaleY.setTarget(infectedIntersectionsCountAnim);
		
		AnimatorSet scaleSet = new AnimatorSet();
		scaleSet.playTogether(scaleX, scaleY);
		
		AnimatorSet uniteSet = new AnimatorSet();
		uniteSet.setDuration(600);
		
		uniteSet.addListener(new AnimatorListener() {
			@Override
			public void onAnimationStart(final Animator animation) {
				infectedIntersectionsCountAnim.setVisibility(View.VISIBLE);
			}
			
			@Override
			public void onAnimationEnd(final Animator animation) {
				infectedIntersectionsCountAnim.setVisibility(View.GONE);
			}
			
			@Override
			public void onAnimationCancel(final Animator animation) { }
			
			@Override
			public void onAnimationRepeat(final Animator animation) { }
		});
		
		uniteSet.playTogether(translateSet, scaleSet);
		
		return uniteSet;
	}
	
	
	private AlertDialog getInternetConnectionDialog() {
		String text = getResources().getString(R.string.need_permissions);
		
		return new AlertDialog.Builder(getActivity())
				.setMessage(text)
				.setPositiveButton("OK", (dialog, which) -> {})
				.setOnDismissListener(dialog -> checkInternetConnection())
				.create();
	}
	
	private void checkInternetConnection() {
		final ConnectivityManager cm = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
		
		if (cm == null) {
			return;
		}
		
		final NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
		
		if (activeNetwork == null || !activeNetwork.isConnectedOrConnecting()) {
			if (internetConnectionDialog.isShowing()) {
				return;
			}
			
			internetConnectionDialog.show();
		} else {
			mMainFragmentViewModel.refreshUserData();
		}
	}
}