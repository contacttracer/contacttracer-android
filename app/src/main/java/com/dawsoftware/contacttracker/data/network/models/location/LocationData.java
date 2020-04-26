package com.dawsoftware.contacttracker.data.network.models.location;

import com.google.gson.annotations.SerializedName;

public class LocationData {
	public String city;
	
	@SerializedName("infected_count")
	public Integer infectedCount;
	
	@SerializedName("dead_count")
	public Integer deadCount;
}
