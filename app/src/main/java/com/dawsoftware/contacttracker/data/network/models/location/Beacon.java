package com.dawsoftware.contacttracker.data.network.models.location;

import com.google.gson.annotations.SerializedName;

public class Beacon {
	
	@SerializedName("beacon_id")
	public String mac;
	
	public String name;
	
	@SerializedName("class")
	public Integer classOfDevice;
	
	public Integer rssi;
	
	@SerializedName("time")
	public Long timeSec;
	
	public Beacon(String mac, String name, Integer deviceClass, Integer rssi, Long timeOfReceiving) {
		this.mac = mac;
		this.name = name;
		this.classOfDevice = deviceClass;
		this.rssi = rssi;
		this.timeSec = timeOfReceiving;
	}
}
