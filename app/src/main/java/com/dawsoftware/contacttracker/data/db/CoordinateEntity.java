package com.dawsoftware.contacttracker.data.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class CoordinateEntity {
	
	public CoordinateEntity(double lat, double lng, long time) {
		this.lat = lat;
		this.lng = lng;
		this.time = time;
	}
	
	@PrimaryKey(autoGenerate = true)
	public int id;
	
	public double lat;
	public double lng;
	public long time;
}
