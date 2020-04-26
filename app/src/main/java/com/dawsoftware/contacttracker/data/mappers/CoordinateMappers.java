package com.dawsoftware.contacttracker.data.mappers;

import android.location.Location;

import java.util.ArrayList;
import java.util.List;

import com.dawsoftware.contacttracker.data.db.CoordinateEntity;
import com.dawsoftware.contacttracker.data.network.models.location.Coordinates;

public class CoordinateMappers {
	private CoordinateMappers() {}
	
	public static Coordinates coordinatesFromDb(final CoordinateEntity entity) {
		if (entity == null) {
			return null;
		}
		
		return new Coordinates(entity.lat, entity.lng, entity.time);
	}
	
	public static ArrayList<Coordinates> coordinatesListFromDb(final List<CoordinateEntity> entities) {
		final ArrayList<Coordinates> result = new ArrayList<>();
		
		if (entities == null || entities.isEmpty()) {
			return result;
		}
		
		for (int i = 0; i < entities.size(); i++) {
			final Coordinates coordinate = coordinatesFromDb(entities.get(i));
			
			if (coordinate != null) {
				result.add(coordinate);
			}
		}
		
		return result;
	}
	
	public static CoordinateEntity coordinatesToDbWithTime(final Location location, long timeSec) {
		if (location == null || timeSec <= 0) {
			return null;
		}
		
		return new CoordinateEntity(location.getLatitude(), location.getLongitude(), timeSec);
	}
}
