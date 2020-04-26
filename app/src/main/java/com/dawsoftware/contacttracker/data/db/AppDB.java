package com.dawsoftware.contacttracker.data.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {CoordinateEntity.class, BeaconEntity.class, SpotEntity.class, SurroundingEntity.class,
                      IntersectionEntity.class, IntersectionCounterEntity.class}, version = 3)
public abstract class AppDB extends RoomDatabase {
	
	private static final String DB_NAME = "app.db";
	
	protected AppDB() {}
	
	private static AppDB instance = null;
	
	public static AppDB getInstance(@NonNull final Context applicationContext) {
		if (instance == null) {
			synchronized (AppDB.class) {
				if (instance == null) {
					instance = create(applicationContext);
				}
			}
		}
		
		return instance;
	}
	
	private static AppDB create(@NonNull final Context applicationContext) {
		return Room.databaseBuilder(applicationContext, AppDB.class, DB_NAME)
		           
		           //TODO: убери при написании нормальной миграции
		           .fallbackToDestructiveMigration()
		           
		           .build();
	}
	
	public abstract CoordinatesDao coordinatesDao();
	public abstract BeaconDao beaconsDao();
	public abstract SpotDao spotsDao();
	public abstract SurroundingDao surroundingDao();
	public abstract IntersectionDao intersectionDao();
}
