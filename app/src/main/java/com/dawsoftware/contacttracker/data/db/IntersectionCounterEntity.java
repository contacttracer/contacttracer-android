package com.dawsoftware.contacttracker.data.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class IntersectionCounterEntity {
	
	@PrimaryKey(autoGenerate = true)
	public int id;
	
	public int totalIntersections;
	public int last24hIntersections;
	public long lastIntersectionTimeS;
	public String status;
	public boolean isActive;
}
