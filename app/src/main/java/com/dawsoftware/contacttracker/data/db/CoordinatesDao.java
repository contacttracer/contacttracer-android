package com.dawsoftware.contacttracker.data.db;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface CoordinatesDao {
	
	@Query("SELECT * FROM CoordinateEntity")
	List<CoordinateEntity> getAll();
	
	@Query("SELECT * FROM CoordinateEntity LIMIT :limit")
	List<CoordinateEntity> getLimited(int limit);
	
	@Query("SELECT * FROM CoordinateEntity WHERE time == :time")
	List<CoordinateEntity> getByTime(long time);
	
	@Insert
	void insert(CoordinateEntity coordinate);
	
	@Delete
	void deleteAll(List<CoordinateEntity> coordinate);
}
