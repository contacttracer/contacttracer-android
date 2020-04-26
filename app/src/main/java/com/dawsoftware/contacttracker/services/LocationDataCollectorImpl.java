package com.dawsoftware.contacttracker.services;

import android.content.Context;

public class LocationDataCollectorImpl implements LocationDataCollector {
	
	private final BluetoothHelper btHelper;
	private final WiFiHelper wifiHelper;
	private final LocationHelper locationHelper;
	
	private LocationCollectedData lastLocationData;
	
	private boolean isRequestedForUI;
	
	public LocationDataCollectorImpl(final Context parentService) {
		this.btHelper = new BluetoothHelper(parentService);
		this.wifiHelper = new WiFiHelper(parentService);
		this.locationHelper = new LocationHelper(parentService);
		
		btHelper.initSource();
		wifiHelper.initSource();
		locationHelper.initSource();
		
		lastLocationData = new LocationCollectedData();
	}
	
	@Override
	public void startCollectData(boolean isRequestedForUI) {
		this.isRequestedForUI = isRequestedForUI;
		
		wifiHelper.startCollectData();
		locationHelper.startCollectData();
		btHelper.startCollectData();
	}
	
	@Override
	public void stopCollectData() {
		lastLocationData = new LocationCollectedData();
		
		lastLocationData.setBluetoothDevices(btHelper.stopAndGetCollectedData());
		lastLocationData.setLocation(locationHelper.stopAndGetCollectedData());
		lastLocationData.setWifiSpots(wifiHelper.stopAndGetCollectedData());
	}
	
	@Override
	public LocationCollectedData getCollectedData() {
		return lastLocationData;
	}
	
	@Override
	public void destroyCollector() {
		btHelper.destroySource();
		wifiHelper.destroySource();
		locationHelper.destroySource();
	}
	
	@Override
	public boolean isRequestedForUI() {
		return isRequestedForUI;
	}
}
