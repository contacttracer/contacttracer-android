package com.dawsoftware.contacttracker.data.network.models;

import com.google.gson.annotations.SerializedName;

public class UserDataResponse {
	@SerializedName("status")
	public Integer status;
	
	@SerializedName("result")
	public UserData userData;
}
