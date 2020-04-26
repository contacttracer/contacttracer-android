package com.dawsoftware.contacttracker.data.network.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Intersection {
	
	@SerializedName("user_id")
	public String userId;
	
	public Long time;
	
	public String status;
	
	public Float lat;
	
	@SerializedName("long")
	public Float lng;
	
	public List<IntersectionDetail> detail;
	
	public Intersection(String status, Long time, String userID, Float lat, Float lng) {
		this.status = status;
		this.time = time;
		this.userId = userID;
		this.lat = lat;
		this.lng = lng;
	}
}
