package com.dawsoftware.contacttracker.ui.onboarding;

import android.content.Intent;
import android.os.Bundle;

import com.cuneytayyildiz.onboarder.OnboarderActivity;
import com.cuneytayyildiz.onboarder.OnboarderPage;
import com.cuneytayyildiz.onboarder.utils.OnboarderPageChangeListener;

import java.util.Arrays;
import java.util.List;

import com.dawsoftware.contacttracker.MainActivity;
import com.dawsoftware.contacttracker.R;
import com.dawsoftware.contacttracker.analytics.Analytics;

public class OnboardingActivity extends OnboarderActivity implements OnboarderPageChangeListener {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		List<OnboarderPage> pages = Arrays.asList(
				new OnboarderPage.Builder()
						.titleResourceId(R.string.onboarding_screen_1_title)
						.descriptionResourceId(R.string.onboarding_screen_1_text)
						.imageResourceId(R.drawable.ic_alert)
						.imageSizeDp(200, 200)
						.backgroundColorId(R.color.white)
						.titleColorId(R.color.onboarding_text_color)
						.descriptionColorId(R.color.standard_black)
						.multilineDescriptionCentered(true)
						.textPaddingBottomDp(72)
						.build(),
				
				new OnboarderPage.Builder()
						.titleResourceId(R.string.onboarding_screen_2_title)
						.descriptionResourceId(R.string.onboarding_screen_2_text)
						.imageResourceId(R.drawable.ic_heart)
						.imageSizeDp(200, 200)
						.backgroundColorId(R.color.white)
						.titleColorId(R.color.onboarding_text_color)
						.descriptionColorId(R.color.standard_black)
						.multilineDescriptionCentered(true)
						.textPaddingBottomDp(64)
						.build(),
				
				new OnboarderPage.Builder()
						.titleResourceId(R.string.onboarding_screen_3_title)
						.descriptionResourceId(R.string.onboarding_screen_3_text)
						.imageResourceId(R.drawable.ic_bell)
						.imageSizeDp(200, 200)
						.backgroundColorId(R.color.white)
						.titleColorId(R.color.onboarding_text_color)
						.descriptionColorId(R.color.standard_black)
						.multilineDescriptionCentered(true)
						.textPaddingBottomDp(72)
						.build(),
				
				new OnboarderPage.Builder()
						.titleResourceId(R.string.onboarding_screen_4_title)
						.descriptionResourceId(R.string.onboarding_screen_4_text)
						.imageResourceId(R.drawable.ic_shield)
						.imageSizeDp(200, 200)
						.backgroundColorId(R.color.white)
						.titleColorId(R.color.onboarding_text_color)
						.descriptionColorId(R.color.standard_black)
						.multilineDescriptionCentered(true)
						.textPaddingBottomDp(72)
						.build(),
		
				new OnboarderPage.Builder()
						.titleResourceId(R.string.onboarding_screen_5_title)
						.descriptionResourceId(R.string.onboarding_screen_5_text)
						.imageResourceId(R.drawable.ic_human)
						.imageSizeDp(200, 200)
						.backgroundColorId(R.color.white)
						.titleColorId(R.color.onboarding_text_color)
						.descriptionColorId(R.color.standard_black)
						.multilineDescriptionCentered(true)
						.textPaddingBottomDp(72)
						.build()
		);
		
		setOnboarderPageChangeListener(this);
		initOnboardingPages(pages);
		
		setNextButtonTextColor(R.color.standard_black);
		setFinishButtonTextColor(R.color.standard_black);
		setSkipButtonTextColor(R.color.standard_black);
		
		setActiveIndicatorColor(R.color.standard_black);
		setInactiveIndicatorColor(R.color.onboarding_inactive_indicator_color);
		
		setNextButtonTitle(R.string.onboarding_next_button);
		setSkipButtonTitle(R.string.onboarding_skip_button);
		setFinishButtonTitle(R.string.onboarding_start_button);
		
		Analytics.onboardingOpened(this);
	}
	
	@Override
	public void onFinishButtonPressed() {
		Analytics.onboardingFinished();
		launchMain();
	}
	
	@Override
	public void onPageChanged(final int position) { }
	
	@Override
	protected void onSkipButtonPressed() {
		Analytics.onboardingDismissed();
		launchMain();
	}
	
	@Override
	public void onBackPressed() {
		Analytics.onboardingDismissed();
		launchMain();
		super.onBackPressed();
	}
	
	private void launchMain() {
		final Intent main = new Intent(this, MainActivity.class);
		startActivity(main);
		finish();
	}
}
