package com.example.musiccontroller;

import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
import android.util.Log;

public class MainActivity extends Activity {
	Button nextSong;
	static private final String TAG = "MusicController";
	static final String CMDTOGGLEPAUSE = "togglepause";
	static final String CMDPAUSE = "pause";
	static final String CMDPREVIOUS = "previous";
	static final String CMDNEXT = "next";
	static final String SERVICECMD = "com.android.music.musicservicecommand";
	static final String CMDNAME = "command";
	static final String CMDSTOP = "stop";
	private static final int REQUEST_ENABLE_BT = 0;
	private BluetoothAdapter mBluetoothAdapter;
	private static final long SCAN_PERIOD = 10000;
	private boolean mScanning;
    private Handler mHandler;
    private Button scanBLE;
    
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi,
                byte[] scanRecord) {
            runOnUiThread(new Runnable() {
               @Override
               public void run() {
            	   Log.i(TAG,"Found device " + device.getName());	
            	   if (device.getName().contains("SensorTag") ){
            		   Toast.makeText(MainActivity.this, "SensorTag found! ", Toast.LENGTH_SHORT).show();
            		   Log.i(TAG,"Starting service...");
            		   	mBluetoothAdapter.stopLeScan(mLeScanCallback);
            		   	Intent intent = new Intent(MainActivity.this, BluetoothLeService.class);
	   					intent.putExtra("SensorTag",device);
	   					startService(intent);   
            	   }
               }
           });
       }
    };
	

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        scanBLE = (Button) findViewById(R.id.scan);
        scanBLE.setOnClickListener(new OnClickListener() {
        	@Override
    		public void onClick(View v) {
    			scanLeDevice(true);
    		}
        });
        
        nextSong = (Button) findViewById(R.id.nextsong);
        nextSong.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
//				Intent i = new Intent(Intent.ACTION_MEDIA_BUTTON);
//				i.putExtra(Intent.EXTRA_KEY_EVENT,new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_NEXT));
//				//i.putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent.KEYCODE_MEDIA_NEXT);
//				sendOrderedBroadcast(i, null);
//				Log.i(TAG, "Send intent in broadcast");
			
//			AudioManager mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
//
//			if(mAudioManager.isMusicActive()) {
//			    Intent i = new Intent(SERVICECMD);
//			    i.putExtra(CMDNAME , CMDNEXT );
//			    MainActivity.this.sendBroadcast(i);
//			}
			
			
			}
		});
        
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
       
        scanLeDevice(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    private void scanLeDevice(final boolean enable) {
    	Toast.makeText(MainActivity.this, "Start scanning for BLE... ", Toast.LENGTH_SHORT).show();
    	Log.i(TAG, "starting scanLeDevice");
    	mScanning = true;
        mBluetoothAdapter.startLeScan(mLeScanCallback);
    	if (enable) {
            // Stops scanning after a pre-defined scan period.
    		mHandler = new Handler();
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, SCAN_PERIOD);

        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    
    }
}
