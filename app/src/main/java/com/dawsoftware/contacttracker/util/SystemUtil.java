package com.dawsoftware.contacttracker.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.PowerManager;

public class SystemUtil {
	private SystemUtil() { }
	
	@TargetApi(23)
	public static boolean isDozing(final Context context) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			final PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
			
			return powerManager != null &&
					powerManager.isDeviceIdleMode() &&
					!powerManager.isIgnoringBatteryOptimizations(context.getPackageName());
		} else {
			return false;
		}
	}
}
