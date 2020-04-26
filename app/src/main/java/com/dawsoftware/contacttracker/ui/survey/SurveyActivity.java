package com.dawsoftware.contacttracker.ui.survey;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.dawsoftware.contacttracker.analytics.Analytics;
import com.google.android.material.appbar.AppBarLayout;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.dawsoftware.contacttracker.BuildConfig;
import com.dawsoftware.contacttracker.R;
import com.dawsoftware.contacttracker.ui.main.MainFragmentViewModel;
import com.dawsoftware.contacttracker.util.StringUtil;
import com.dawsoftware.contacttracker.util.ViewUtil;

import static com.dawsoftware.contacttracker.ui.main.MainFragmentViewModel.STATUS_HEALTHY;
import static com.dawsoftware.contacttracker.ui.main.MainFragmentViewModel.STATUS_INFECTED;

public class SurveyActivity extends AppCompatActivity {
	
	public static final String STATUS_BUNDLE_EXTRA = "STATUS_BUNDLE_EXTRA";
	public static final String STATE_BUNDLE_EXTRA = "STATE_BUNDLE_EXTRA";
	public static final String TIME_TO_RECOVER_BUNDLE_EXTRA = "TIME_TO_RECOVER_BUNDLE_EXTRA";
	
	public static final String NEW_STATUS_RESULT = "NEW_STATUS_RESULT";
	public static final String SYMPTOMS_RESULT = "SYMPTOMS_RESULT";
	public static final String MUST_SHARE_RESULT = "MUST_SHARE_RESULT";
	
	private RecyclerView recycler;
	private SymptomsAdapter adapter;
	
	private Button sendButton;
	private SendButtonListener sendButtonListener;
	
	private ConstraintLayout surveyCardView;
	private ImageView surveyIcon;
	private TextView surveyTitle;
	private TextView surveyText;
	private TextView surveyExplanationTitle;
	private TextView surveyCheckedView;
	private TextView surveyUncheckedView;
	private ImageView surveyCheckedIcon;
	private ImageView surveyUncheckedIcon;
	
	private TextView infectedWarning;
	private TextView daysLeftCount;
	private TextView daysLeftText;
	
	private AppBarLayout appBar;
	private Toolbar toolbar;
	
	private String mode;
	private boolean isActive;
	private Integer timeToRecover;
	
	@Override
	protected void onCreate(@Nullable final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_survey);
		
		appBar = findViewById(R.id.app_bar_survey);
		toolbar = findViewById(R.id.toolbar_survey);
		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setDisplayShowHomeEnabled(true);
		
		if (BuildConfig.DEBUG) {
			toolbar.inflateMenu(R.menu.debug_menu);
		}
		
		final Intent startIntent = getIntent();
		
		mode = startIntent.getStringExtra(STATUS_BUNDLE_EXTRA);
		isActive = startIntent.getBooleanExtra(STATE_BUNDLE_EXTRA, false);
		timeToRecover = startIntent.getIntExtra(TIME_TO_RECOVER_BUNDLE_EXTRA, -1);
		
		initViews();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		Analytics.detectLaunchType();
		Analytics.surveyScreenOpened(this);
	}
	
	@Override
	public boolean onSupportNavigateUp() {
		onBackPressed();
		return true;
	}
	
	private void initViews() {
		
		appBar.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
			@Override
			public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
				ViewCompat.setElevation(appBarLayout, 60);
			}
		});
		
		if (StringUtil.isEmpty(mode)) {
			mode = STATUS_HEALTHY;
			isActive = false;
		}
		
		adapter = new SymptomsAdapter(mode, this, isActive);
		
		recycler = findViewById(R.id.symptoms_recycler_view);
		recycler.setAdapter(adapter);
		recycler.setLayoutManager(new LinearLayoutManager(this));
		recycler.setNestedScrollingEnabled(false);
		recycler.setHasFixedSize(true);
		
		sendButton = findViewById(R.id.send_button);
		sendButtonListener = new SendButtonListener();
		sendButton.setOnClickListener(sendButtonListener);
		
		surveyCardView = findViewById(R.id.survey_card);
		surveyIcon = findViewById(R.id.survey_card_icon);
		surveyTitle = findViewById(R.id.survey_card_title);
		surveyText = findViewById(R.id.survey_card_text);
		surveyExplanationTitle = findViewById(R.id.survey_card_explanation_title);
		surveyCheckedView = findViewById(R.id.survey_card_explanation_checked);
		surveyUncheckedView = findViewById(R.id.survey_card_explanation_unchecked);
		
		infectedWarning = findViewById(R.id.survey_card_infected_warning);
		if (infectedWarning != null) {
			infectedWarning.setVisibility(View.GONE);
		}
		
		if (isActive) {
			switch (mode) {
				case STATUS_HEALTHY: {
					surveyCardView.setBackground(getResources().getDrawable(R.drawable.background_healthy));
					sendButton.setBackground(getResources().getDrawable(R.drawable.button_default_state));
					
					surveyIcon.setImageResource(R.drawable.ic_sentiment_satisfied_black_24dp);
					ViewUtil.setTint(this, surveyIcon, R.color.white);
					
					surveyTitle.setText(getResources().getString(R.string.status_healthy_survey_title));
					surveyText.setText(getResources().getString(R.string.status_healthy_survey_text));
					
					surveyExplanationTitle.setText(getResources().getString(R.string.status_healthy_survey_explanation_title));
					surveyCheckedView.setText(getResources().getString(R.string.status_healthy_survey_explanation_checked));
					surveyUncheckedView.setText(getResources().getString(R.string.status_healthy_survey_explanation_unchecked));
					break;
				}
				
				case STATUS_INFECTED: {
					surveyCardView.setBackground(getResources().getDrawable(R.drawable.background_potentially_sick));
					sendButton.setBackground(getResources().getDrawable(R.drawable.button_infected_state));
					
					surveyIcon.setImageResource(R.drawable.ic_sentiment_neutral_black_24dp);
					ViewUtil.setTint(this, surveyIcon, R.color.white);
					
					surveyTitle.setText(getResources().getString(R.string.status_potentially_sick_survey_title));
					surveyText.setText(getResources().getString(R.string.status_potentially_sick_survey_text));
					
					surveyExplanationTitle.setText(getResources().getString(R.string.status_potentially_sick_survey_explanation_title));
					surveyCheckedView.setText(getResources().getString(R.string.status_potentially_sick_survey_explanation_checked));
					surveyUncheckedView.setText(getResources().getString(R.string.status_potentially_sick_survey_explanation_unchecked));
					
					if (infectedWarning != null) {
						infectedWarning.setVisibility(View.VISIBLE);
					}
					
					break;
				}
				
				case MainFragmentViewModel.STATUS_SICK: {
					surveyCardView.setBackground(getResources().getDrawable(R.drawable.background_sick));
					sendButton.setBackground(getResources().getDrawable(R.drawable.button_sick_state));
					
					surveyIcon.setImageResource(R.drawable.ic_sentiment_dissatisfied_black_24dp);
					ViewUtil.setTint(this, surveyIcon, R.color.white);
					
					surveyTitle.setText(getResources().getString(R.string.status_sick_survey_title));
					surveyText.setText(getResources().getString(R.string.status_sick_survey_text));
					
					daysLeftCount = findViewById(R.id.survey_card_days_number);
					daysLeftText = findViewById(R.id.survey_card_days_left);
					
					surveyCheckedIcon = findViewById(R.id.survey_card_checked_icon);
					surveyUncheckedIcon = findViewById(R.id.survey_card_unchecked_icon);
					
					if (timeToRecover != null && timeToRecover > 0) {
						surveyCheckedView.setVisibility(View.GONE);
						surveyUncheckedView.setVisibility(View.GONE);
						surveyCheckedIcon.setVisibility(View.GONE);
						surveyUncheckedIcon.setVisibility(View.GONE);
						recycler.setVisibility(View.INVISIBLE);
						sendButton.setVisibility(View.INVISIBLE);
						
						String timeToRecoverString = String.valueOf(TimeUnit.SECONDS.toDays(timeToRecover) + 1);
						String daysString = getResources().getQuantityString(R.plurals.days, timeToRecover);
						
						daysLeftCount.setVisibility(View.VISIBLE);
						daysLeftCount.setText(timeToRecoverString);
						
						daysLeftText.setVisibility(View.VISIBLE);
						daysLeftText.setText(daysString);
						
						surveyExplanationTitle.setText(getResources().getString(R.string.status_sick_survey_explanation_timer_title));
					} else {
						surveyExplanationTitle.setText(getResources().getString(R.string.status_sick_survey_explanation_title));
						surveyCheckedView.setText(getResources().getString(R.string.status_sick_survey_explanation_checked));
						surveyUncheckedView.setText(getResources().getString(R.string.status_sick_survey_explanation_unchecked));
					}
					
					break;
				}
			}
		} else {
			surveyCardView.setBackground(getResources().getDrawable(R.drawable.background_unknown));
			
			sendButton.setBackground(getResources().getDrawable(R.drawable.button_default_state));
			
			surveyIcon.setImageResource(R.drawable.ic_error_outline_black_24dp);
			ViewUtil.setTint(this, surveyIcon, R.color.white);
			
			surveyTitle.setText(getResources().getString(R.string.status_unknown_survey_title));
			surveyText.setText(getResources().getString(R.string.status_unknown_survey_text));
			
			surveyExplanationTitle.setText(getResources().getString(R.string.status_unknown_survey_explanation_title));
			surveyCheckedView.setText(getResources().getString(R.string.status_unknown_survey_explanation_checked));
			surveyUncheckedView.setText(getResources().getString(R.string.status_unknown_survey_explanation_unchecked));
		}
	}
	
	private String defineUserStatus(final String userSelectedStatus) {
		String newStatus;
		
		if (userSelectedStatus.equals(SymptomsAdapter.NOTHING_SELECTED)) {
			switch (mode) {
				case STATUS_INFECTED:
				case STATUS_HEALTHY: {
					newStatus = STATUS_HEALTHY;
					break;
				}
				default: {
					newStatus = mode;
				}
			}
		} else {
			newStatus = userSelectedStatus;
		}
		
		return newStatus;
	}
	
	class SendButtonListener implements OnClickListener {
		@Override
		public void onClick(final View v) {
			if (adapter != null) {
				
				Analytics.sendSurveyButton();
				
				final String userSelectedStatus = adapter.getUserSelectedStatus();
				final String newStatus = defineUserStatus(userSelectedStatus);
				
				if (StringUtil.isEmpty(newStatus)) {
					Log.i("wutt", "Send status: user data is null");
					return;
				}
				
				final ArrayList<String> symptoms = adapter.getSelectedSymptoms();
				
				Intent intent = new Intent();
				intent.putExtra(NEW_STATUS_RESULT, newStatus);
				intent.putStringArrayListExtra(SYMPTOMS_RESULT, symptoms);
				intent.putExtra(MUST_SHARE_RESULT, adapter.getIsSharedChecked());
				
				setResult(RESULT_OK, intent);
				finish();
			}
		}
	}
}
