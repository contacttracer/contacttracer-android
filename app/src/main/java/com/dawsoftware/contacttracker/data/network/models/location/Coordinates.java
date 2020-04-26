package com.dawsoftware.contacttracker.data.network.models.location;

import android.location.Location;

import com.google.gson.annotations.SerializedName;

import androidx.annotation.NonNull;

public class Coordinates {
	
	public Double lat;
	
	@SerializedName("long")
	public Double lng;
	
	@SerializedName("time")
	public Long timeSec;
	
	public Coordinates(@NonNull final Location location) {
		lat = location.getLatitude();
		lng = location.getLongitude();
		timeSec = location.getTime();
	}
	
	public Coordinates(double lat, double lng, long time) {
		this.lat = lat;
		this.lng = lng;
		this.timeSec = time;
	}
}
