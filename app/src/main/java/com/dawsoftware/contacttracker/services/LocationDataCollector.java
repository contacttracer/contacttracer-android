package com.dawsoftware.contacttracker.services;

public interface LocationDataCollector {
	void startCollectData(boolean isRequestedForUI);
	void stopCollectData();
	LocationCollectedData getCollectedData();
	void destroyCollector();
	boolean isRequestedForUI();
}
