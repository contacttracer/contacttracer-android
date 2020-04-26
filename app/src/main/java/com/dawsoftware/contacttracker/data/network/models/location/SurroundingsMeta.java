package com.dawsoftware.contacttracker.data.network.models.location;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class SurroundingsMeta {

	@SerializedName("current_time")
	public Long currentTimeSec;
	
	@SerializedName("int_request")
	public Boolean mustCalculateIntersections;
	
	public ArrayList<Beacon> beacons;
	
	public ArrayList<Spot> spots;
	
	public ArrayList<Coordinates> locations;
	
	public SurroundingsMeta(Long currentTimeSec, Boolean mustCalculateIntersections, ArrayList<Beacon> beacons,
	                        ArrayList<Spot> spots, ArrayList<Coordinates> locations) {
		this.currentTimeSec = currentTimeSec;
		this.mustCalculateIntersections = mustCalculateIntersections;
		this.beacons = beacons;
		this.spots = spots;
		this.locations = locations;
	}
}
