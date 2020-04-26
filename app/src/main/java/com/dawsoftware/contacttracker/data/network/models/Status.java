package com.dawsoftware.contacttracker.data.network.models;

import java.util.ArrayList;

public class Status {
	
	public String status;
	public ArrayList<String> symptoms;
	
	public Status(final String status, final ArrayList<String> symptoms) {
		this.status = status;
		this.symptoms = symptoms;
	}
}
