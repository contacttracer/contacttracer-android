package com.dawsoftware.contacttracker.util;

import android.view.WindowManager.LayoutParams;

import androidx.fragment.app.FragmentActivity;

public class DisplayUtil {
	private DisplayUtil() { }
	
	public static void setScreenAlwaysOn(final boolean isAlwaysOn, final FragmentActivity activity) {
		
		if (activity == null) {
			return;
		}
		
		if (isAlwaysOn) {
			activity.getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
		} else {
			activity.getWindow().clearFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
		}
	}
}
