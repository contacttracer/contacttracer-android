package com.dawsoftware.contacttracker.util;

import android.content.Context;
import android.os.Build;
import android.os.Build.VERSION_CODES;

import java.util.ArrayList;

import androidx.core.content.PermissionChecker;

import static android.Manifest.permission.ACCESS_BACKGROUND_LOCATION;
import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static androidx.core.content.PermissionChecker.PERMISSION_GRANTED;

public class PermissionUtil {
	private PermissionUtil() { }
	
	public static ArrayList<String> unaskedLocationPermissions(final Context context) {
		final ArrayList<String> permissions = new ArrayList<>();
		
		permissions.clear();
		
		permissions.add(ACCESS_FINE_LOCATION);
		permissions.add(ACCESS_COARSE_LOCATION);
		
		if (Build.VERSION.SDK_INT >= VERSION_CODES.Q) {
			permissions.add(ACCESS_BACKGROUND_LOCATION);
		}
		
		return findUnAskedPermissions(permissions, context);
	}
	
	private static ArrayList<String> findUnAskedPermissions(ArrayList<String> wanted, Context context) {
		ArrayList<String> result = new ArrayList<>();
		
		for (String perm : wanted) {
			if (!hasPermission(perm, context)) {
				result.add(perm);
			}
		}
		
		return result;
	}
	
	public static boolean hasPermission(String permission, Context context) {
		if (canMakeSmores()) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				return (PermissionChecker.checkSelfPermission(context, permission) == PERMISSION_GRANTED);
			}
		}
		return true;
	}
	
	private static boolean canMakeSmores() {
		return (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1);
	}
}
