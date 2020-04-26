package com.dawsoftware.contacttracker.services;

public interface LocationDataSource<T> {
	void initSource();
	void destroySource();
	
	void startCollectData();
	T stopAndGetCollectedData();
}
