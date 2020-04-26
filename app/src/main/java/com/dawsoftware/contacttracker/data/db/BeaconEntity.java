package com.dawsoftware.contacttracker.data.db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class BeaconEntity {
	
	public BeaconEntity(String mac, String name, int classOfDevice, int rssi, long time) {
		this.mac = mac;
		this.name = name;
		this.classOfDevice = classOfDevice;
		this.rssi = rssi;
		this.time = time;
	}
	
	@PrimaryKey(autoGenerate = true)
	public int id;
	
	public String mac;
	public String name;
	
	@ColumnInfo(name = "class")
	public int classOfDevice;
	
	public int rssi;
	
	public long time;
}
