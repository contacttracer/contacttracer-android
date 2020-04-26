package com.dawsoftware.contacttracker.data.network.models;

import java.util.ArrayList;

public class StatusDebug extends Status {
	
	public long past;
	
	public StatusDebug(final String status, final ArrayList<String> symptoms, final long past) {
		super(status, symptoms);
		
		this.past = past;
	}
}
