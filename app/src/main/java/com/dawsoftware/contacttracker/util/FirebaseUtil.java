package com.dawsoftware.contacttracker.util;

import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

public class FirebaseUtil {
	private FirebaseUtil() {}
	
	public static void getFirebaseToken(final GetFirebaseTokenCallback callback) {
		FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(task -> {
			if (!task.isSuccessful()) {
				Log.w("wutt", "getInstanceId failed", task.getException());
				
				if (callback != null) {
					callback.onFail();
				}
				
				return;
			}
			
			final InstanceIdResult result = task.getResult();
			if (result != null) {
				Log.i("wutt", "FCM token: " + result.getToken());
				
				if (callback != null) {
					callback.onSuccess(result.getToken());
				}
			}
		});
	}
	
	public interface GetFirebaseTokenCallback {
		void onSuccess(final String token);
		void onFail();
	}
}
