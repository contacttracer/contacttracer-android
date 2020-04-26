package com.dawsoftware.contacttracker.data.network.models;

import com.google.gson.annotations.SerializedName;

public class RegisterResponse {
	@SerializedName("status")
	public Integer status;
	
	@SerializedName("result")
	public UserId userId;
}
