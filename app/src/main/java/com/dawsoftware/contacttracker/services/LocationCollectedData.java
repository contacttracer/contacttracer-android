package com.dawsoftware.contacttracker.services;

import android.location.Location;
import android.net.wifi.ScanResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.dawsoftware.contacttracker.services.BluetoothHelper.BluetoothDeviceHolder;

public class LocationCollectedData {
	
	private Set<BluetoothDeviceHolder> bluetoothDevices;
	private List<ScanResult> wifiSpots;
	private Location location;
	
	public LocationCollectedData() {
		bluetoothDevices = new HashSet<>();
		wifiSpots = new ArrayList<>();
		location = null;
	}
	
	public void setBluetoothDevices(final Set<BluetoothDeviceHolder> bluetoothDevices) {
		this.bluetoothDevices = bluetoothDevices;
	}
	
	public Set<BluetoothDeviceHolder> getBluetoothDevices() {
		return bluetoothDevices;
	}
	
	public void setWifiSpots(final List<ScanResult> wifiSpots) {
		this.wifiSpots = wifiSpots;
	}
	
	public List<ScanResult> getWifiSpots() {
		return wifiSpots;
	}
	
	public void setLocation(final Location location) {
		this.location = location;
	}
	
	public Location getLocation() {
		return location;
	}
}
