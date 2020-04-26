package com.dawsoftware.contacttracker.data.db;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface SurroundingDao {
	
	@Insert
	void insert(SurroundingEntity entity);
	
	@Delete
	void delete(SurroundingEntity entity);
	
	@Query("SELECT * FROM SurroundingEntity WHERE time == :time")
	List<SurroundingEntity> getByTime(long time);
	
	@Query("SELECT * FROM SurroundingEntity LIMIT 1")
	SurroundingEntity getLast();
	
	@Query("SELECT * FROM SurroundingEntity")
	List<SurroundingEntity> getAll();
}
