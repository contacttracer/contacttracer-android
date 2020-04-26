package com.dawsoftware.contacttracker.services;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothProfile.ServiceListener;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.dawsoftware.contacttracker.services.BluetoothHelper.BluetoothDeviceHolder;

import static com.dawsoftware.contacttracker.services.LocationService.MESSAGE_BLUETOOTH_AVAILABLE;
import static com.dawsoftware.contacttracker.services.LocationService.MESSAGE_BLUETOOTH_UNAVAILABLE;

public class BluetoothHelper implements LocationDataSource<Set<BluetoothDeviceHolder>> {
	
	private final short DEFAULT_BT_RSSI = -66;
	
	private final int[] states = new int[] {BluetoothProfile.STATE_CONNECTED};
	
	private Set<BluetoothDeviceHolder> listOfConnectedDevices;
	
	private BluetoothManager bluetoothManager;
	private BluetoothAdapter bluetoothAdapter;
	
	private final Context parentService;
	
	public BluetoothHelper(final Context parentService) {
		this.parentService = parentService;
		
		bluetoothManager = (BluetoothManager) parentService.getSystemService(Context.BLUETOOTH_SERVICE);
		
		if (bluetoothManager != null) {
			bluetoothAdapter = bluetoothManager.getAdapter();
		}
		
		listOfConnectedDevices = new HashSet<>();
	}
	
	private void registerBT() {
		
		if (bluetoothAdapter == null) {
			return;
		}
		
		IntentFilter filterFound = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		IntentFilter filterStart = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
		IntentFilter filterFinish = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		IntentFilter filterState = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
		
		parentService.registerReceiver(btReceiver, filterFound);
		parentService.registerReceiver(btReceiver, filterStart);
		parentService.registerReceiver(btReceiver, filterFinish);
		parentService.registerReceiver(btReceiver, filterState);
	}
	
	private void unregisterBT() {
		if (bluetoothAdapter == null) {
			return;
		}
		
		bluetoothAdapter.cancelDiscovery();
		
		parentService.unregisterReceiver(btReceiver);
	}
	
	private void scanBluetooth() {
		
		if (bluetoothManager == null) {
			Log.i("wutt", "there is no bt manager");
			return;
		}
		
		if (bluetoothAdapter == null) {
			Log.i("wutt", "there is no bt support");
			return;
		}
		
		bluetoothAdapter.startDiscovery();

		getConnected(bluetoothAdapter, parentService);
	}
	
	private void getConnected(final BluetoothAdapter btAdapter, final Context context) {
		btAdapter.getProfileProxy(context, connectedListener, BluetoothProfile.A2DP);
		btAdapter.getProfileProxy(context, connectedListener, BluetoothProfile.HEADSET);
		btAdapter.getProfileProxy(context, connectedListener, BluetoothProfile.HEALTH);
		btAdapter.getProfileProxy(context, connectedListener, BluetoothProfile.GATT);
		btAdapter.getProfileProxy(context, connectedListener, BluetoothProfile.GATT_SERVER);
		
		HashSet<BluetoothDevice> gattDevices = new HashSet<>();
		gattDevices.addAll(bluetoothManager.getConnectedDevices(BluetoothProfile.GATT));
		gattDevices.addAll(bluetoothManager.getConnectedDevices(BluetoothProfile.GATT_SERVER));

		for (BluetoothDevice d: gattDevices) {
			listOfConnectedDevices.add(new BluetoothDeviceHolder(d, DEFAULT_BT_RSSI));
		}
	}
	
	private final BroadcastReceiver btReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				
				if (device == null) {
					return;
				}
				
				short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, DEFAULT_BT_RSSI);
				
				BluetoothDeviceHolder holder = new BluetoothDeviceHolder(device, rssi);
				
				listOfConnectedDevices.add(holder);

			}
			
			if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
				final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
				
				switch (state) {
					case BluetoothAdapter.STATE_OFF: {
						
						bluetoothAdapter.cancelDiscovery();
						listOfConnectedDevices.clear();
						
						if (parentService instanceof LocationService) {
							((LocationService) parentService).serviceHandler.sendEmptyMessage(MESSAGE_BLUETOOTH_UNAVAILABLE);
							Log.i("wutt", "service detected bluetooth turning off");
						}
						
						break;
					}
					case BluetoothAdapter.STATE_ON: {
						if (parentService instanceof LocationService) {
							((LocationService) parentService).serviceHandler.sendEmptyMessage(MESSAGE_BLUETOOTH_AVAILABLE);
							Log.i("wutt", "service detected bluetooth turning on");
						}
						
						break;
					}
				}
			}
		}
	};
	
	private final BluetoothProfile.ServiceListener connectedListener = new ServiceListener() {
		@Override
		public void onServiceConnected(final int profile, final BluetoothProfile proxy) {
			if (proxy != null) {

				List<BluetoothDevice> devices = proxy.getDevicesMatchingConnectionStates(states);
				for(BluetoothDevice d: devices) {
					listOfConnectedDevices.add(new BluetoothDeviceHolder(d, DEFAULT_BT_RSSI));
				}
			}
		}

		@Override
		public void onServiceDisconnected(final int profile) {}
	};
	
	@Override
	public void initSource() {
		registerBT();
	}
	
	@Override
	public void destroySource() {
		unregisterBT();
	}
	
	@Override
	public void startCollectData() {
		listOfConnectedDevices.clear();
		scanBluetooth();
	}
	
	@Override
	public Set<BluetoothDeviceHolder> stopAndGetCollectedData() {
		if (bluetoothAdapter != null) {
			bluetoothAdapter.cancelDiscovery();
		}
		return listOfConnectedDevices;
	}
	
	public class BluetoothDeviceHolder {
		public BluetoothDevice device;
		public int rssi;
		
		public BluetoothDeviceHolder(final BluetoothDevice device, int rssi) {
			this.device = device;
			this.rssi = rssi;
		}
		
		@Override
		public int hashCode() {
			return device == null ?
			       super.hashCode() :
			       device.getAddress().hashCode();
		}
		
		@Override
		public boolean equals(@Nullable final Object obj) {
			if (!(obj instanceof BluetoothDeviceHolder)) {
				return false;
			}
			
			return device == null ?
			       super.equals(obj) :
			       device.getAddress().equals(((BluetoothDeviceHolder) obj).device.getAddress());
		}
		
		@NonNull
		@Override
		public String toString() {
			return device == null ?
			       super.toString() :
			       device.getAddress();
		}
	}
}
