package com.dawsoftware.contacttracker.data.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class SurroundingEntity {
	
	@PrimaryKey(autoGenerate = true)
	public int id;
	
	public long time;
}
