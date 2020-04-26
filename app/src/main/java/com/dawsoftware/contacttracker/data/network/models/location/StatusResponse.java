package com.dawsoftware.contacttracker.data.network.models.location;

import com.google.gson.annotations.SerializedName;

public class StatusResponse {
	
	@SerializedName("time_to_recover")
	public long recoverTime;
}
