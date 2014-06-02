package com.example.musiccontroller;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import android.app.Service;
import android.bluetooth.*;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.IBinder;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;

enum SimpleKeysStatus {
    // Warning: The order in which these are defined matters.
    OFF_OFF, OFF_ON, ON_OFF, ON_ON;
}

public class BluetoothLeService extends Service{
	private final static String TAG = BluetoothLeService.class.getSimpleName();
	
	private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    private BluetoothGatt mBluetoothGatt;
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
    private BluetoothDevice sensorTag;
    
    private PhoneStateListener phoneListener;
    private static AudioManager mAudioManager;
    private static Speaker speaker;
    private boolean isRinging = false;
    private boolean isOffHook = false;
    private String incomingNumber;
        
    @Override
    public void onStart(Intent intent, int startId) {
    	sensorTag = intent.getExtras().getParcelable("SensorTag");
    	Log.i(TAG, "Blconnected "+ sensorTag.getName());
    	mBluetoothGatt = sensorTag.connectGatt(this, false, mGattCallback);
    	speaker = new Speaker(this);
    	
    	phoneListener = new PhoneStateListener(){
    		private static final String TAG = "PHONELISTENER";
    		public void onCallStateChanged(int state, String incomingNumber) {
			   super.onCallStateChanged(state, incomingNumber);
			   
			   switch (state) {
		        case TelephonyManager.CALL_STATE_IDLE:
		            Log.d("DEBUG", "IDLE");
		            BluetoothLeService.this.isRinging = false;
		            BluetoothLeService.this.isOffHook = false;
					BluetoothLeService.this.incomingNumber = null;
					mAudioManager.setMode(AudioManager.MODE_NORMAL);
					mAudioManager.setSpeakerphoneOn(false);
					
        			        			
		            break;
		        case TelephonyManager.CALL_STATE_OFFHOOK:
		            Log.d("DEBUG", "OFFHOOK");
		            BluetoothLeService.this.isRinging = false;
		            BluetoothLeService.this.isOffHook = true;
					BluetoothLeService.this.incomingNumber = null;
					if (mConnectionState == STATE_CONNECTED){
						mAudioManager.setMode(AudioManager.MODE_IN_CALL);
	        			mAudioManager.setSpeakerphoneOn(true);
						
					}
					
		            break;
		        case TelephonyManager.CALL_STATE_RINGING:
		            Log.d("DEBUG", "RINGING");
		            BluetoothLeService.this.isRinging = true;	
		            BluetoothLeService.this.isOffHook = false;
		            BluetoothLeService.this.incomingNumber = incomingNumber;
					mAudioManager.setSpeakerphoneOn(true);
					String contactDisplayName = getContactDisplayNameByNumber(incomingNumber);
					Log.i(TAG, "contact" +  contactDisplayName);
					speaker.speakOut(contactDisplayName);
					
		            break;
		        }
			    
    		}
	
		};
    	
    	
    	TelephonyManager TelephonyMgr = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        TelephonyMgr.listen(phoneListener, PhoneStateListener.LISTEN_CALL_STATE);
        mAudioManager = (AudioManager) getSystemService(BluetoothLeService.this.AUDIO_SERVICE);
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
                broadcastUpdate(intentAction);
                mConnectionState = STATE_CONNECTED;
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
        		
        		if (BluetoothLeService.this.isRinging || BluetoothLeService.this.isOffHook){
        			
        			Intent i = new Intent(Intent.ACTION_MEDIA_BUTTON);
        			i.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_UP,
        			            KeyEvent.KEYCODE_HEADSETHOOK));
        			BluetoothLeService.this.sendOrderedBroadcast(i, null);
        			
        		}
        		else{
        			Integer encodedInteger = characteristic.getIntValue(0x11, 0);
    	            SimpleKeysStatus newValue = SimpleKeysStatus.values()[encodedInteger % 4];
    	            Log.i(TAG, "newValue " +  newValue.toString());
    	            
    	            Intent i = new Intent(SERVICECMD);
    				if(mAudioManager.isMusicActive()) {
    				    if (newValue == SimpleKeysStatus.OFF_ON){
    				    	i.putExtra(CMDNAME , CMDNEXT );	
    		        	}
    		            else if (newValue == SimpleKeysStatus.ON_OFF){
    		       		    i.putExtra(CMDNAME , CMDPREVIOUS);
    	    			}
    				BluetoothLeService.this.sendBroadcast(i);
    				
            		}
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
	
	public String getContactDisplayNameByNumber(String number) {
	    Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
	    String name = "";

	    ContentResolver contentResolver = getContentResolver();
	    Cursor contactLookup = contentResolver.query(uri, new String[] {BaseColumns._ID,
	            ContactsContract.PhoneLookup.DISPLAY_NAME }, null, null, null);

	    try {
	        if (contactLookup != null && contactLookup.getCount() > 0) {
	            contactLookup.moveToNext();
	            name = contactLookup.getString(contactLookup.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));
	            //String contactId = contactLookup.getString(contactLookup.getColumnIndex(BaseColumns._ID));
	        }
	    } finally {
	        if (contactLookup != null) {
	            contactLookup.close();
	        }
	    }

	    return name;
	}
	
}
	
