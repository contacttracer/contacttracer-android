package com.dawsoftware.contacttracker.util;

import android.content.Context;
import android.location.LocationManager;

import com.dawsoftware.contacttracker.BuildConfig;

public class LocationUtil {
	private LocationUtil() { }
	
	public static boolean areGeoServicesAvailable(final Context context) {
		
		if (context == null) {
			return false;
		}
		
		boolean gpsEnabled = false;
		boolean networkEnabled = false;
		
		final LocationManager locationManager =
				(LocationManager) context.getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
		
		if (locationManager == null) {
			return false;
		}
		
		try {
			gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
		} catch (Exception ex) {}
		
		try {
			networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
		} catch (Exception e) {}
		
		if (BuildConfig.DEBUG) {
			return gpsEnabled;
		}
		
		return gpsEnabled || networkEnabled;
	}
}
