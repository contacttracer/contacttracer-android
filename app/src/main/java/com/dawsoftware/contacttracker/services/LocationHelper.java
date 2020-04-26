package com.dawsoftware.contacttracker.services;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import com.dawsoftware.contacttracker.BuildConfig;

public class LocationHelper implements LocationDataSource<Location> {
	
	private enum LISTENER_TYPE {GPS, NETWORK}
	
	private static final String TAG = "LocationHelper";
	
	private GpsLocationListener gpsListener;
	private NetworkLocationListener networkListener;
	
	private LocationManager mLocationManager;
	
	private Location newGPSLocation;
	private Location newNetworkLocation;
	
	private Location lastKnownLocation;
	
	final Context parentService;
	
	public LocationHelper(final Context parentService) {
		this.parentService = parentService;
		
		mLocationManager = (LocationManager) parentService.getSystemService(Context.LOCATION_SERVICE);
	}
	
	private void stopTracking() {
		if (mLocationManager != null) {
			try {
				mLocationManager.removeUpdates(gpsListener);
			} catch (Exception ex) {
				Log.i(TAG, "gps listener is already null", ex);
			}
			
			try {
				mLocationManager.removeUpdates(networkListener);
			} catch (Exception ex) {
				Log.i(TAG, "net listener is already null", ex);
			}
		}
		
		gpsListener = null;
		networkListener = null;
	}
	
	private void startTracking() {
		requestGPS();
		requestNetwork();
	}
	
	private void requestGPS() {
		
		if (gpsListener == null) {
			gpsListener = new GpsLocationListener();
		}
		
		try {
			Log.i("wutt", "gps enabled");
			mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, gpsListener);
		} catch (java.lang.SecurityException ex) {
			gpsListener = null;
			Log.i(TAG, "fail to request lastGPSLocation update, ignore", ex);
		} catch (IllegalArgumentException ex) {
			gpsListener = null;
			Log.i(TAG, "gps provider does not exist " + ex.getMessage());
		}
	}
	
	private void requestNetwork() {
		
		if (networkListener == null) {
			networkListener = new NetworkLocationListener();
		}
		
		try {
			Log.i("wutt", "network enabled");
			mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, networkListener);
		} catch (java.lang.SecurityException ex) {
			networkListener = null;
			Log.i(TAG, "fail to request lastGPSLocation update on network, ignore", ex);
		} catch (IllegalArgumentException ex) {
			networkListener = null;
			Log.i(TAG, "network provider does not exist " + ex.getMessage());
		}
	}
	
	private void chooseLastKnownLocation() {
		lastKnownLocation = getMoreActual(newNetworkLocation, newGPSLocation);
	}
	
	private Location getMoreActual(final Location first, final Location second) {
		if (first != null && second != null) {
			return first.getTime() > second.getTime() ? first : second;
		}
		
		return first == null ? second : first;
	}
	
	private void updateLastLocation(LISTENER_TYPE type, Location location) {
		
		switch (type) {
			case GPS: {
				if (BuildConfig.DEBUG) {
					String newLocation = location == null ? "none" : location.toString();
					Log.i(TAG, "new GPS Coordinates: " + newLocation);
				}
				
				newGPSLocation = location;
				
				break;
			}
			case NETWORK: {
				if (BuildConfig.DEBUG) {
					String newLocation = location == null ? "none" : location.toString();
					Log.i(TAG, "new network location: " + newLocation);
				}
				
				newNetworkLocation = location;
				
				break;
			}
		}
	}
	
	@Override
	public void initSource() {}
	
	@Override
	public void destroySource() {
		stopTracking();
	}
	
	@Override
	public void startCollectData() {
		lastKnownLocation = null;
		
		startTracking();
	}
	
	@Override
	public Location stopAndGetCollectedData() {
		stopTracking();
		chooseLastKnownLocation();
		
		return lastKnownLocation;
	}
	
	
	private class GpsLocationListener implements android.location.LocationListener {
		private final String TAG = "GpsLocationListener";
		
		public GpsLocationListener() {
			Log.i(TAG, "listener created");
		}
		
		@Override
		public void onLocationChanged(Location location) {
			updateLastLocation(LISTENER_TYPE.GPS, location);
			Log.i(TAG, "LocationChanged: " + location);
		}
		
		@Override
		public void onProviderDisabled(String provider) {
			Log.i(TAG, "onProviderDisabled: " + provider);
		}
		
		@Override
		public void onProviderEnabled(String provider) {
			Log.i(TAG, "onProviderEnabled: " + provider);
		}
		
		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			Log.i(TAG, "onStatusChanged: " + status);
		}
	}
	
	private class NetworkLocationListener implements android.location.LocationListener {
		private final String TAG = "NetworkLocationListener";
		
		public NetworkLocationListener() {
			Log.i(TAG, "listener created");
		}
		
		@Override
		public void onLocationChanged(Location location) {
			updateLastLocation(LISTENER_TYPE.NETWORK, location);
			Log.i(TAG, "LocationChanged: " + location);
		}
		
		@Override
		public void onProviderDisabled(String provider) {
			Log.i(TAG, "onProviderDisabled: " + provider);
		}
		
		@Override
		public void onProviderEnabled(String provider) {
			Log.i(TAG, "onProviderEnabled: " + provider);
		}
		
		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			Log.i(TAG, "onStatusChanged: " + status);
		}
	}
}
