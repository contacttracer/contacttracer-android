package com.dawsoftware.contacttracker.data.network.models;

import com.google.gson.annotations.SerializedName;

public class StatusResponse {
	public Integer status;
	
	@SerializedName("result")
	public StatusResponse result;
}
