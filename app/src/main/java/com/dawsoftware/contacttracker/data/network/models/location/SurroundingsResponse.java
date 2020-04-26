package com.dawsoftware.contacttracker.data.network.models.location;

import com.google.gson.annotations.SerializedName;

public class SurroundingsResponse {
	
	@SerializedName("status")
	public Integer status;
	
	@SerializedName("result")
	public SurroundingsResponseData result;
}
