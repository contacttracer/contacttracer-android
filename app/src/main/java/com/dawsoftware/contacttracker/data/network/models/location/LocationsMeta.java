package com.dawsoftware.contacttracker.data.network.models.location;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class LocationsMeta {
	
	@SerializedName("current_time")
	public Long currentTime;
	public ArrayList<Coordinates> locations;
	
	public LocationsMeta(final Long currentSendingTime, final ArrayList<Coordinates> locationsList) {
		currentTime = currentSendingTime;
		locations = locationsList;
	}
}