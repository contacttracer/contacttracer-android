package com.dawsoftware.contacttracker.data.network.models.location;

import com.google.gson.annotations.SerializedName;

public class Spot {

	@SerializedName("spot_id")
	public String mac;
	
	public String ssid;
	
	public Integer frequency;
	
	public Integer level;
	
	@SerializedName("time")
	public Long timeSec;
	
	public Spot(String mac, String ssid, Integer frequency, Integer level, Long receivingTime) {
		this.mac = mac;
		this.ssid = ssid;
		this.frequency = frequency;
		this.level = level;
		this.timeSec = receivingTime;
	}

}
