package com.dawsoftware.contacttracker.data.db;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface BeaconDao {
	
	@Insert
	void insert(BeaconEntity... beaconEntity);
	
	@Delete
	void deleteAll(List<BeaconEntity> beaconEntity);
	
	@Query("SELECT * FROM BeaconEntity WHERE time == :time")
	List<BeaconEntity> selectByTime(long time);
	
}
