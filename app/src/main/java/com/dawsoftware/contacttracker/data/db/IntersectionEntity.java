package com.dawsoftware.contacttracker.data.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class IntersectionEntity {
	
	public IntersectionEntity(String status, long time, String userId, Float lat, Float lng) {
		this.status = status;
		this.time = time;
		this.userId = userId;
		this.lat = lat;
		this.lng = lng;
	}
	
	@PrimaryKey(autoGenerate = true)
	public int id;
	
	public String status;
	public Long time;
	public String userId;
	public Float lat;
	public Float lng;
}
