package com.dawsoftware.contacttracker;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.dawsoftware.contacttracker.analytics.Analytics;
import com.dawsoftware.contacttracker.ui.DebugActivity;
import com.dawsoftware.contacttracker.ui.views.CustomViewPager;
import com.dawsoftware.contacttracker.util.BluetoothUtil;
import com.dawsoftware.contacttracker.util.DrawableUtil;
import com.dawsoftware.contacttracker.util.IntentUtil;
import com.dawsoftware.contacttracker.util.LocationUtil;
import com.dawsoftware.contacttracker.util.PermissionUtil;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.Lifecycle.State;
import androidx.lifecycle.ViewModelProviders;
import androidx.viewpager.widget.ViewPager.OnPageChangeListener;

import static com.dawsoftware.contacttracker.CustomFragmentPagerAdapter.HEALTH_PAGE_INDEX;
import static com.dawsoftware.contacttracker.CustomFragmentPagerAdapter.INTERSECTIONS_PAGE_INDEX;
import static com.dawsoftware.contacttracker.CustomFragmentPagerAdapter.STATISTICS_PAGE_INDEX;
import static com.dawsoftware.contacttracker.services.LocationService.BUNDLE_FROM_NOTIFICATION;

public class MainActivity extends AppCompatActivity {
	
	private ArrayList<String> permissionsToRequest = new ArrayList<>();
	private ArrayList<String> permissionsRejected = new ArrayList<>();
	
	public final static int LOCATION_PERMISSIONS_RESULT = 101;
	
	public final static int GEO_SERVICES_PERMISSIONS_RESULT = 1001;
	
	private boolean isAskingForLocationPermission;
	
	private MainActivityViewModel viewModel;
	
	private AlertDialog geoServicesDialog;
	
	private TextView healthButton;
	private TextView intersectionsButton;
	private TextView statisticsButton;
	
	private ImageView intersectionsButtonBadge;
	
	private Drawable healthButtonDrawable;
	private Drawable statisticsButtonDrawable;
	private Drawable casesButtonDrawable;
	
	private Toolbar toolbar;
	
	private CustomViewPager viewPager;
	private CustomFragmentPagerAdapter adapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_main);
		
		toolbar = findViewById(R.id.toolbar);
		
		if (BuildConfig.DEBUG) {
			toolbar.inflateMenu(R.menu.debug_menu);
		}
		
		setSupportActionBar(toolbar);
		
		geoServicesDialog = getGeoServicesDialog();
		
		initViewModel();
		initViews();
	}
	
	@Override
	public void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		geoServicesDialog.dismiss();
		geoServicesDialog = null;
	}
	
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		if (BuildConfig.DEBUG) {
			final MenuInflater inflater = getMenuInflater();
			inflater.inflate(R.menu.debug_menu, menu);
			return true;
		}
		
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
		if (BuildConfig.DEBUG) {
			switch (item.getItemId()) {
				case R.id.debug_menu_item: {
					startActivity(new Intent(getApplicationContext(), DebugActivity.class));
					return true;
				}
			}
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	private void initViewModel() {
		viewModel = ViewModelProviders.of(this).get(MainActivityViewModel.class);
		
		viewModel.getMustShowBottomBadge().observe(this, mustShowBadge -> {
			if (intersectionsButtonBadge != null) {
				intersectionsButtonBadge.setVisibility(mustShowBadge ? View.VISIBLE : View.INVISIBLE);
			}
			
			// for screenshoting
			// intersectionsButtonBadge.setVisibility(View.VISIBLE);
		});
	}
	
	private void initViews() {
		adapter = new CustomFragmentPagerAdapter(getSupportFragmentManager());
		
		viewPager = findViewById(R.id.view_pager);
		viewPager.setAdapter(adapter);
		viewPager.setPagingEnabled(false);
		viewPager.setOffscreenPageLimit(5);
		
		viewPager.addOnPageChangeListener(new OnPageChangeListener() {
			@Override
			public void onPageScrolled(final int position, final float positionOffset, final int positionOffsetPixels) { }
			
			@Override
			public void onPageSelected(final int position) {
				setPagerTitle(position);
				setBottomBarState(position);
			}
			
			@Override
			public void onPageScrollStateChanged(final int state) {}
		});
		
		healthButtonDrawable = getResources().getDrawable(R.drawable.ic_heart_outline);
		statisticsButtonDrawable = getResources().getDrawable(R.drawable.ic_statistics);
		casesButtonDrawable = getResources().getDrawable(R.drawable.ic_people_outline_black_24dp);
		
		healthButton = findViewById(R.id.health_button);
		intersectionsButton = findViewById(R.id.cases_button);
		statisticsButton = findViewById(R.id.statistics_button);
		
		intersectionsButtonBadge = findViewById(R.id.infected_contacts_badge);
		
		intersectionsButton.setOnClickListener(v -> {
			if (!viewModel.isScanningInProgress) {
				viewPager.setCurrentItem(INTERSECTIONS_PAGE_INDEX);
			}
		});
		
		statisticsButton.setOnClickListener(v -> {
			if (!viewModel.isScanningInProgress) {
				viewPager.setCurrentItem(STATISTICS_PAGE_INDEX);
			}
		});

		healthButton.setOnClickListener(v -> {
			if (!viewModel.isScanningInProgress) {
				viewPager.setCurrentItem(HEALTH_PAGE_INDEX);
			}
		});
		
		viewPager.setCurrentItem(HEALTH_PAGE_INDEX);
	}
	
	private void setPagerTitle(int page) {
		ActionBar ab = getSupportActionBar();
		
		if (ab == null) {
			return;
		}
		
		switch (page) {
			case INTERSECTIONS_PAGE_INDEX: {
				ab.setTitle(R.string.button_infected_contacts);
				return;
			}
			
			case STATISTICS_PAGE_INDEX: {
				ab.setTitle(R.string.button_statistics);
				return;
			}
			
			case HEALTH_PAGE_INDEX: {
				ab.setTitle(R.string.app_name);
				break;
			}
		}
	}
	
	private void setBottomBarState(int position) {

		
		switch (position) {
			case INTERSECTIONS_PAGE_INDEX: {
				healthButtonDrawable = DrawableUtil.getTintedDrawable(this, healthButtonDrawable,
				                                                      getResources().getColor(R.color.unavailable_gray));
				casesButtonDrawable = DrawableUtil.getTintedDrawable(this, casesButtonDrawable,
				                                                     getResources().getColor(R.color.standard_black));
				
				statisticsButtonDrawable = DrawableUtil.getTintedDrawable(this, statisticsButtonDrawable,
				                                                          getResources().getColor(R.color.unavailable_gray));
				
				healthButton.setTextColor(getResources().getColor(R.color.unavailable_gray));
				healthButton.setCompoundDrawablesWithIntrinsicBounds(null, healthButtonDrawable, null, null);
				
				statisticsButton.setTextColor(getResources().getColor(R.color.unavailable_gray));
				statisticsButton.setCompoundDrawablesWithIntrinsicBounds(null, statisticsButtonDrawable, null, null);
				
				intersectionsButton.setTextColor(getResources().getColor(R.color.standard_black));
				intersectionsButton.setCompoundDrawablesWithIntrinsicBounds(null, casesButtonDrawable, null, null);
				
				return;
			}
			
			case STATISTICS_PAGE_INDEX: {
				
				healthButtonDrawable = DrawableUtil.getTintedDrawable(this, healthButtonDrawable,
				                                                      getResources().getColor(R.color.unavailable_gray));
				casesButtonDrawable = DrawableUtil.getTintedDrawable(this, casesButtonDrawable,
				                                                     getResources().getColor(R.color.unavailable_gray));
				
				statisticsButtonDrawable = DrawableUtil.getTintedDrawable(this, statisticsButtonDrawable,
				                                                          getResources().getColor(R.color.standard_black));
				
				healthButton.setTextColor(getResources().getColor(R.color.unavailable_gray));
				healthButton.setCompoundDrawablesWithIntrinsicBounds(null, healthButtonDrawable, null, null);
				
				statisticsButton.setTextColor(getResources().getColor(R.color.standard_black));
				statisticsButton.setCompoundDrawablesWithIntrinsicBounds(null, statisticsButtonDrawable, null, null);
				
				intersectionsButton.setTextColor(getResources().getColor(R.color.unavailable_gray));
				intersectionsButton.setCompoundDrawablesWithIntrinsicBounds(null, casesButtonDrawable, null, null);
				
				return;
			}
			
			case HEALTH_PAGE_INDEX: {
				healthButtonDrawable = DrawableUtil.getTintedDrawable(this, healthButtonDrawable,
				                                                      getResources().getColor(R.color.standard_black));
				casesButtonDrawable = DrawableUtil.getTintedDrawable(this, casesButtonDrawable,
				                                                     getResources().getColor(R.color.unavailable_gray));
				
				statisticsButtonDrawable = DrawableUtil.getTintedDrawable(this, statisticsButtonDrawable,
				                                                          getResources().getColor(R.color.unavailable_gray));
				
				healthButton.setTextColor(getResources().getColor(R.color.standard_black));
				healthButton.setCompoundDrawablesWithIntrinsicBounds(null, healthButtonDrawable, null, null);
				
				statisticsButton.setTextColor(getResources().getColor(R.color.unavailable_gray));
				statisticsButton.setCompoundDrawablesWithIntrinsicBounds(null, statisticsButtonDrawable, null, null);
				
				intersectionsButton.setTextColor(getResources().getColor(R.color.unavailable_gray));
				intersectionsButton.setCompoundDrawablesWithIntrinsicBounds(null, casesButtonDrawable, null, null);
				
				break;
			}
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		final Intent intent = getIntent();
		
		if (intent.getExtras() != null && intent.getExtras().containsKey(BUNDLE_FROM_NOTIFICATION)) {
			Analytics.detectNotificationType(intent);
			intent.removeExtra(BUNDLE_FROM_NOTIFICATION);
		}
		
		Analytics.detectLaunchType();
		
		checkAllPermissions();
	}
	
	@Override
	protected void onNewIntent(final Intent intent) {
		super.onNewIntent(intent);
		Analytics.detectNotificationType(intent);
	}
	
	private void checkAllPermissions() {
		if (isAskingForLocationPermission) {
			return;
		}
		
		boolean isLocationPermissionGranted = checkLocationPermissions();
		if (!isLocationPermissionGranted) {
			askLocationPermissions();
			return;
		}
		
		boolean isGeoServicesEnabled = LocationUtil.areGeoServicesAvailable(this);
		if (!isGeoServicesEnabled) {
			checkGeoServicesState();
			return;
		}
		
		boolean checkBluetoothSupported = BluetoothUtil.checkBluetoothSupported(this);
		
		if (checkBluetoothSupported) {
			
			boolean isBluetoothTurnedOn = BluetoothUtil.checkBluetoothEnabled(this);
			if (!isBluetoothTurnedOn) {
				askForBluetoothTurningOn();
				return;
			}
		}
		
		viewModel.getAllPermissionsGranted().setValue(true);
	}
	
	private void checkGeoServicesState() {
		
		LocationRequest locationRequest = LocationRequest.create();
		locationRequest.setInterval(5000);
		locationRequest.setFastestInterval(5000);
		locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
		
		LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
		
		SettingsClient client = LocationServices.getSettingsClient(this);
		Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());
		
		task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
			@Override
			public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
				Log.i("wutt", "location enabled");
				checkAllPermissions();
			}
		});
		
		task.addOnFailureListener(this, new OnFailureListener() {
			@Override
			public void onFailure(@NonNull Exception e) {
				Log.i("wutt", "location disabled");
				
				if (e instanceof ResolvableApiException) {
					// Location settings are not satisfied, but this can be fixed
					// by showing the user a dialog.
					try {
						// Show the dialog by calling startResolutionForResult(),
						// and check the result in onActivityResult().
						ResolvableApiException resolvable = (ResolvableApiException) e;
						resolvable.startResolutionForResult(MainActivity.this, GEO_SERVICES_PERMISSIONS_RESULT);
					} catch (IntentSender.SendIntentException sendEx) {
						// Ignore the error.
					}
				}
			}
		});
	}
	
	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, @Nullable final Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		switch (requestCode) {
			case GEO_SERVICES_PERMISSIONS_RESULT:
				switch (resultCode) {
					case Activity.RESULT_OK:
						Log.i("wutt", "location OK");
						break;
					case Activity.RESULT_CANCELED:
						Log.i("wutt", "location NOT OK");
						break;
					default:
						break;
				}
				break;
		}
	}
	
	private void askForGeoServices() {
		if (!geoServicesDialog.isShowing()) {
			geoServicesDialog.show();
		}
	}
	
	private boolean checkLocationPermissions() {
		boolean result = false;
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			
			permissionsToRequest = PermissionUtil.unaskedLocationPermissions(this);
			
			result = permissionsToRequest.size() <= 0;
		}
		
		return result;
	}
	
	private void askLocationPermissions() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[permissionsToRequest.size()]),
			                                  LOCATION_PERMISSIONS_RESULT);
			isAskingForLocationPermission = true;
		}
	}
	
	private void askForBluetoothTurningOn() {
		Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		enableBtIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		startActivity(enableBtIntent);
	}
	
	@TargetApi(Build.VERSION_CODES.M)
	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		
		switch (requestCode) {
			
			case LOCATION_PERMISSIONS_RESULT:
				
				if (permissionsToRequest != null) {
					for (String perms : permissionsToRequest) {
						if (!PermissionUtil.hasPermission(perms, this)) {
							permissionsRejected.add(perms);
						}
					}
				}
				
				if (permissionsRejected.size() > 0) {
					
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
						if (shouldShowRequestPermissionRationale(permissionsRejected.get(0))) {
							showMessageOKCancel((DialogInterface dialog, int which) -> {
								
								String[] permissionsToAsk = permissionsRejected.toArray(new String[permissionsRejected.size()]);
								if (permissionsToAsk == null || permissionsToAsk.length == 0) {
									return;
								}
								
								requestPermissions(permissionsToAsk, LOCATION_PERMISSIONS_RESULT);
								
								permissionsRejected.clear();
							}, getResources().getString(R.string.need_permissions));
							return;
						} else {
							showMessageOKCancel((DialogInterface dialog, int which) -> {
								
								callForAppSettings();
								
								permissionsRejected.clear();
							}, getResources().getString(R.string.need_permissions));
						}
					}
				} else {
					isAskingForLocationPermission = false;
				}
				
				break;
		}
		
	}
	
	public void serviceRequestsPermissions() {
		if (getLifecycle().getCurrentState().isAtLeast(State.RESUMED)) {
			isAskingForLocationPermission = false;
			checkAllPermissions();
		}
	}
	
	private void callForAppSettings() {
		startActivity(IntentUtil.getAppSettingsIntent());
	}
	
	private void callLocationServices() {
		startActivity(IntentUtil.getLocationServicesIntent());
	}
	
	private void showMessageOKCancel(final DialogInterface.OnClickListener okListener, final String text) {
		new AlertDialog.Builder(MainActivity.this)
				.setMessage(text)
				.setPositiveButton("OK", okListener)
				.create()
				.show();
	}
	
	private AlertDialog getGeoServicesDialog() {
		String text = getResources().getString(R.string.need_permissions);
		
		return new AlertDialog.Builder(MainActivity.this)
				.setMessage(text)
				.setPositiveButton("OK", (dialog, which) -> callLocationServices())
				.create();
	}
}
