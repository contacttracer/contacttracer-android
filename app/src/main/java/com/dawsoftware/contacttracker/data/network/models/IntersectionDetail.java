package com.dawsoftware.contacttracker.data.network.models;

import com.google.gson.annotations.SerializedName;

public class IntersectionDetail {

	public String type;
	
	
	// BT data
	public String subtype;
	
	@SerializedName("beacon_id")
	public String beaconId;
	
	public String name;
	
	public Integer rssi;
	
	
	// WiFi data
	@SerializedName("spot_id")
	public String spotId;
	
	public String ssid;
	
	public Integer level;
}
