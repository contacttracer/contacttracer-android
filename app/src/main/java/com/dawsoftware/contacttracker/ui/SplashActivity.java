package com.dawsoftware.contacttracker.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.dawsoftware.contacttracker.MainActivity;
import com.dawsoftware.contacttracker.ui.onboarding.OnboardingActivity;
import com.dawsoftware.contacttracker.util.PreferencesUtil;

public class SplashActivity extends AppCompatActivity {
	
	private PreferencesUtil prefs;
	
	@Override
	protected void onCreate(@Nullable final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		prefs = new PreferencesUtil(getApplicationContext());
		
		if (prefs.getIsFirstLaunch()) {
			Intent onboarding = new Intent(this, OnboardingActivity.class);
			startActivity(onboarding);
		} else {
			Intent main = new Intent(this, MainActivity.class);
			startActivity(main);
		}
		
		finish();
	}
}
