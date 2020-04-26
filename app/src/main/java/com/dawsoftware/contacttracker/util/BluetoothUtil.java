package com.dawsoftware.contacttracker.util;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.util.Log;

import static android.content.Context.BLUETOOTH_SERVICE;

public class BluetoothUtil {
	
	private BluetoothUtil() {}
	
	public static boolean checkBluetoothEnabled(final Context context) {
		final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(BLUETOOTH_SERVICE);
		
		if (bluetoothManager == null) {
			Log.i("wutt", "there is no bt manager");
			return false;
		}
		
		final BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
		if (bluetoothAdapter == null) {
			Log.i("wutt", "there is no bt support");
			return false;
		}
		
		return bluetoothAdapter.isEnabled();
	}
	
	public static boolean checkBluetoothSupported(final Context context) {
		final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(BLUETOOTH_SERVICE);
		
		if (bluetoothManager == null) {
			Log.i("wutt", "there is no bt manager");
			return false;
		}
		
		final BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
		if (bluetoothAdapter == null) {
			Log.i("wutt", "there is no bt support");
			return false;
		}
		
		return true;
	}
}
