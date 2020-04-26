package com.dawsoftware.contacttracker.data.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class SpotEntity {
	
	public SpotEntity(String mac, String ssid, int frequency, int level, long time) {
		this.mac = mac;
		this.ssid = ssid;
		this.frequency = frequency;
		this.level = level;
		this.time = time;
	}
	
	@PrimaryKey(autoGenerate = true)
	public int id;
	
	public String mac;
	public String ssid;
	public int frequency;
	public int level;
	public long time;
}
