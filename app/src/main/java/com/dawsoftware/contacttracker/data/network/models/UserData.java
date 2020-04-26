package com.dawsoftware.contacttracker.data.network.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class UserData {
	
	public Boolean active;
	
	public String status;
	
	@SerializedName("checkup_time")
	public String[] checkupTime;
	
	@SerializedName("time_to_recover")
	public Integer timeToRecover;
	
	@SerializedName("infected_intersections")
	public List<Intersection> intersections;
	
	@SerializedName("infected_total")
	public Integer infectedTotal;
	
	@SerializedName("infected_24h")
	public Integer infected24h;
}
