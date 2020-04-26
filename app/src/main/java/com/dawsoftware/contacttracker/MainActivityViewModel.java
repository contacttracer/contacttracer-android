package com.dawsoftware.contacttracker;

import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class MainActivityViewModel extends ViewModel {
	
	private MutableLiveData<Boolean> allPermissionsGranted;
	private MutableLiveData<Boolean> mustShowBottomBadge;
	
	public boolean isScanningInProgress;
	
	public MainActivityViewModel() {
		initLiveData();
	}
	
	private void initLiveData() {
		allPermissionsGranted = new MediatorLiveData<>();
		allPermissionsGranted.setValue(false);
		
		mustShowBottomBadge = new MutableLiveData<>();
		mustShowBottomBadge.setValue(false);
	}
	
	public MutableLiveData<Boolean> getAllPermissionsGranted() {
		return allPermissionsGranted;
	}
	
	public MutableLiveData<Boolean> getMustShowBottomBadge() {
		return mustShowBottomBadge;
	}
}
