package com.dawsoftware.contacttracker.data.db;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface IntersectionDao {
	
	/*
	 * Intersections
	 */
	
	@Insert
	void insertIntersections(List<IntersectionEntity> entities);
	
	@Query("SELECT * FROM IntersectionEntity ORDER BY time DESC")
	List<IntersectionEntity> getAllIntersectionsDESC();
	
	@Delete
	void deleteIntersections(List<IntersectionEntity> entities);
	
	/*
	 * Intersection counters
	 */
	
	@Query("SELECT * FROM IntersectionCounterEntity LIMIT 1")
	IntersectionCounterEntity getIntersectionCounters();
	
	@Query("SELECT * FROM IntersectionCounterEntity")
	List<IntersectionCounterEntity> getAllIntersectionCounters();
	
	@Update
	void updateCounters(IntersectionCounterEntity entity);
	
	@Insert
	void insertCounters(IntersectionCounterEntity entity);
}
