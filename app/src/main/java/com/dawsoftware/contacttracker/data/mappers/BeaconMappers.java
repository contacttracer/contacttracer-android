package com.dawsoftware.contacttracker.data.mappers;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.dawsoftware.contacttracker.data.db.BeaconEntity;
import com.dawsoftware.contacttracker.data.network.models.location.Beacon;
import com.dawsoftware.contacttracker.services.BluetoothHelper.BluetoothDeviceHolder;

public class BeaconMappers {
	private BeaconMappers() {}
	
	public static BeaconEntity beaconToDB(final BluetoothDeviceHolder beacon, long timeSec) {
		if (beacon == null || timeSec <= 0 || beacon.device == null) {
			return null;
		}
		
		int deviceClass = 0;
		
		if (beacon.device.getBluetoothClass() != null) {
			deviceClass = beacon.device.getBluetoothClass().getDeviceClass();
		}
		
		return new BeaconEntity(beacon.device.getAddress(), beacon.device.getName(),
		                        deviceClass, beacon.rssi, timeSec);
	}
	
	public static BeaconEntity[] beaconsToDB(final Set<BluetoothDeviceHolder> beacons, long timeSec) {
		if (beacons == null || timeSec <= 0) {
			return null;
		}
		
		BeaconEntity[] beaconEntities = new BeaconEntity[beacons.size()];
		
		int iter = 0;
		for (final BluetoothDeviceHolder beacon: beacons) {
			beaconEntities[iter++] = beaconToDB(beacon, timeSec);
		}
		
		return beaconEntities;
	}
	
	public static Beacon beaconFromDB(final BeaconEntity beacon) {
		if (beacon == null) {
			return null;
		}
		
		return new Beacon(beacon.mac, beacon.name, beacon.classOfDevice, beacon.rssi, beacon.time);
	}
	
	public static ArrayList<Beacon> beaconsFromDB(final List<BeaconEntity> beaconsList) {
		final ArrayList<Beacon> result = new ArrayList<>();
		
		if (beaconsList == null || beaconsList.isEmpty()) {
			return result;
		}
		
		for (int i = 0; i < beaconsList.size(); i++) {
			final Beacon beacon = beaconFromDB(beaconsList.get(i));
			
			if (beacon != null) {
				result.add(beacon);
			}
		}
		
		return result;
	}
}
