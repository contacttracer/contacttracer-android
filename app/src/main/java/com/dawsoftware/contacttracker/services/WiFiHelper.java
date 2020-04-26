package com.dawsoftware.contacttracker.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class WiFiHelper implements LocationDataSource<List<ScanResult>> {
	
	private Context parentService;
	
	private WifiManager wifiManager;
	
	private List<ScanResult> scanResults;
	
	public WiFiHelper(final Context parentService) {
		this.parentService = parentService;
		
		wifiManager = (WifiManager) parentService.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
		
		scanResults = new ArrayList<>();
	}
	
	private void scanWiFi() {
		if (wifiManager != null) {
			Log.i("wutt", "scan started");
			wifiManager.startScan();
		}
	}
	
	private void registerWiFi() {
		
		if (wifiManager == null) {
			Log.i("wutt", "wifi error");
			return;
		}
		
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
		parentService.registerReceiver(wifiScanReceiver, intentFilter);
	}
	
	private void unregisterWiFi() {
		parentService.unregisterReceiver(wifiScanReceiver);
	}
	
	private BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context c, Intent intent) {
			Log.i("wutt", "scan finished");
			scanFinished();
		}
	};
	
	private void scanFinished() {
		if (wifiManager != null) {
			scanResults.clear();
			scanResults = wifiManager.getScanResults();
		}
	}
	
	@Override
	public void initSource() {
		registerWiFi();
	}
	
	@Override
	public void destroySource() {
		unregisterWiFi();
	}
	
	@Override
	public void startCollectData() {
		scanResults.clear();
		scanWiFi();
	}
	
	@Override
	public List<ScanResult> stopAndGetCollectedData() {
		return scanResults;
	}
}
