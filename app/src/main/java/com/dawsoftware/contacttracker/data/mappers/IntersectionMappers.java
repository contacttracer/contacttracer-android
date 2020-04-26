package com.dawsoftware.contacttracker.data.mappers;

import java.util.ArrayList;
import java.util.List;

import com.dawsoftware.contacttracker.data.db.IntersectionEntity;
import com.dawsoftware.contacttracker.data.network.models.Intersection;

public class IntersectionMappers {
	private IntersectionMappers() {}
	
	public static IntersectionEntity intersectionToDb(final Intersection intersection) {
		
		if (intersection == null) {
			return null;
		}
		
		return new IntersectionEntity(intersection.status, intersection.time, intersection.userId, intersection.lat,
		                              intersection.lng);
	}
	
	public static List<IntersectionEntity> intersectionsToDb(final List<Intersection> intersections) {
		final List<IntersectionEntity> result = new ArrayList<>();
		
		if (intersections == null || intersections.size() <= 0) {
			return result;
		}
		
		for (int i = 0; i < intersections.size(); i++) {
			IntersectionEntity entity = intersectionToDb(intersections.get(i));
			if (entity != null) {
				result.add(entity);
			}
		}
		
		return result;
	}
	
	public static Intersection intersectionFromDb(final IntersectionEntity intersection) {
		
		if (intersection == null) {
			return null;
		}
		
		return new Intersection(intersection.status, intersection.time, intersection.userId, intersection.lat, intersection.lng);
	}
	
	public static List<Intersection> intersectionsFromDb(final List<IntersectionEntity> intersections) {
		List<Intersection> result = new ArrayList<>();
		
		if (intersections == null || intersections.size() <= 0) {
			return result;
		}
		
		for (int i = 0; i < intersections.size(); i++) {
			Intersection item = intersectionFromDb(intersections.get(i));
			if (item != null) {
				result.add(item);
			}
		}
		
		return result;
	}
}
