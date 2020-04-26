package com.dawsoftware.contacttracker.data.network.models;

import com.google.gson.annotations.SerializedName;

public class RegisterIds {
	
	public RegisterIds(final String deviceId, final String btMac) {
		this.deviceId = deviceId;
		this.btMac = btMac;
	}
	
	@SerializedName("android_id")
	public String deviceId;
	
	@SerializedName("bluetooth_id")
	public String btMac;
}
