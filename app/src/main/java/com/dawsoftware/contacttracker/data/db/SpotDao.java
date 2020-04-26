package com.dawsoftware.contacttracker.data.db;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface SpotDao {
	
	@Insert
	void insert(SpotEntity... entity);
	
	@Delete
	void deleteAll(List<SpotEntity> entity);
	
	@Query("SELECT * FROM SpotEntity WHERE time == :time")
	List<SpotEntity> getByTime(long time);
}
