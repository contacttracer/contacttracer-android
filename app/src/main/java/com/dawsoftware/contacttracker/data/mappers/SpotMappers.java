package com.dawsoftware.contacttracker.data.mappers;

import android.net.wifi.ScanResult;

import java.util.ArrayList;
import java.util.List;

import com.dawsoftware.contacttracker.data.db.SpotEntity;
import com.dawsoftware.contacttracker.data.network.models.location.Spot;

public class SpotMappers {
	private SpotMappers() {}
	
	public static SpotEntity spotToDB(final ScanResult spot, long timeSec) {
		if (spot == null || timeSec <= 0) {
			return null;
		}
		
		return new SpotEntity(spot.BSSID, spot.SSID, spot.frequency, spot.level, timeSec);
	}
	
	public static SpotEntity[] spotsToDB(final List<ScanResult> spots, long timeSec) {
		if (spots == null || timeSec <= 0) {
			return null;
		}
		
		SpotEntity[] spotEntities = new SpotEntity[spots.size()];
		
		int iter = 0;
		for (ScanResult spot: spots) {
			spotEntities[iter++] = spotToDB(spot, timeSec);
		}
		
		return spotEntities;
	}
	
	public static Spot spotFromDB(final SpotEntity spot) {
		if (spot == null) {
			return null;
		}
		
		return new Spot(spot.mac, spot.ssid, spot.frequency, spot.level, spot.time);
	}
	
	public static ArrayList<Spot> spotsFromDB(final List<SpotEntity> spotsList) {
		final ArrayList<Spot> result = new ArrayList<>();
		
		if (spotsList == null || spotsList.isEmpty()) {
			return result;
		}
		
		for (int i = 0; i < spotsList.size(); i++) {
			final Spot spot = spotFromDB(spotsList.get(i));
			result.add(spot);
		}
		
		return result;
	}
}
