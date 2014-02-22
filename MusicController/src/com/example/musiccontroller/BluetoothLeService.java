package com.example.musiccontroller;

import java.util.List;
import java.util.UUID;

import android.app.Service;
import android.bluetooth.*;
import android.content.Intent;
import android.media.AudioManager;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

enum SimpleKeysStatus {
    // Warning: The order in which these are defined matters.
    OFF_OFF, OFF_ON, ON_OFF, ON_ON;
}

public class BluetoothLeService extends Service {
	private final static String TAG = BluetoothLeService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";
    
    static final String CMDTOGGLEPAUSE = "togglepause";
	static final String CMDPAUSE = "pause";
	static final String CMDPREVIOUS = "previous";
	static final String CMDNEXT = "next";
	static final String SERVICECMD = "com.android.music.musicservicecommand";
	static final String CMDNAME = "command";
	static final String CMDSTOP = "stop";
    
    private static final UUID SIMPLE_KEYS_SERVICE_UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    private static final UUID SIMPLE_KEYS_DATA_UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");
    private BluetoothGattCharacteristic keyCharacteristic;
    private static SimpleKeysStatus previousStatus;

    private BluetoothDevice sensorTag;
    
    @Override
    public void onStart(Intent intent, int startId) {
    	sensorTag = intent.getExtras().getParcelable("SensorTag");
    	Log.i(TAG, "Blconnected "+ sensorTag.getName());
    	mBluetoothGatt = sensorTag.connectGatt(this, false, mGattCallback);
    }

    // Various callback methods defined by the BLE API.
    private final BluetoothGattCallback mGattCallback =
            new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());
                
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }
        
        @Override
        public void  onCharacteristicChanged (BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            /*
             * The key state is encoded into 1 unsigned byte.
             * bit 0 designates the right key.
             * bit 1 designates the left key.
             * bit 2 designates the side key.
             * 
             * Weird, in the userguide left and right are opposite.
             */
        	Log.i(TAG, "onCharacteristicChanged");
        	if (characteristic == keyCharacteristic){
	            Integer encodedInteger = characteristic.getIntValue(0x11, 0);
	        	    
	            SimpleKeysStatus newValue = SimpleKeysStatus.values()[encodedInteger % 4];
	            Log.i(TAG, "newValue " +  newValue.toString());
	            AudioManager mAudioManager = (AudioManager) getSystemService(BluetoothLeService.this.AUDIO_SERVICE);
	            Intent i = new Intent(SERVICECMD);
				if(mAudioManager.isMusicActive()) {
				    if (newValue == SimpleKeysStatus.OFF_ON){
				    	i.putExtra(CMDNAME , CMDNEXT );	
		        	}
		            else if (newValue == SimpleKeysStatus.ON_OFF){
		       		    i.putExtra(CMDNAME , CMDPREVIOUS);
	    			}
				BluetoothLeService.this.sendBroadcast(i);
				previousStatus = newValue;
        		}
				
        	}
		}
	        
        @Override
        // New services discovered
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
            	Log.i(TAG, "onServicesDiscovered received");
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                List<BluetoothGattService> services = gatt.getServices();
                for (int i = 0; i < services.size(); i++){
                	Log.i(TAG, "service " + services.get(i).getUuid());
                }
                keyCharacteristic = gatt.getService(SIMPLE_KEYS_SERVICE_UUID).getCharacteristic(SIMPLE_KEYS_DATA_UUID);
                boolean notification_enabled = gatt.setCharacteristicNotification(keyCharacteristic, true);
                Log.i(TAG, "notification_enabled: " + notification_enabled);
                
                BluetoothGattDescriptor descriptor = keyCharacteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                mBluetoothGatt.writeDescriptor(descriptor);
//                Toast.makeText(BluetoothLeService.this, "", 5000).show();
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }
        
        @Override
        // Result of a characteristic read operation
        public void onCharacteristicRead(BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic,
                int status) {
        	
        	Log.i(TAG, characteristic.getUuid().toString());
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }
    };
    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }
    
    private void broadcastUpdate(final String action,
        final BluetoothGattCharacteristic characteristic) {
		final Intent intent = new Intent(action);
//		sendBroadcast(intent);
		}


	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
}
	
