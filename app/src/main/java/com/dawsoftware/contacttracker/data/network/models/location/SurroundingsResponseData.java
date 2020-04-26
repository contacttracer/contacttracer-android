package com.dawsoftware.contacttracker.data.network.models.location;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import com.dawsoftware.contacttracker.data.network.models.Intersection;

public class SurroundingsResponseData {
	
	@SerializedName("infected_intersections")
	public List<Intersection> intersections;
	
	@SerializedName("infected_near")
	public Integer infectedNear;
	
	@SerializedName("infected_total")
	public Integer infectedTotal;
	
	@SerializedName("infected_24h")
	public Integer infected24h;
	
	@SerializedName("status")
	public String status;
}
