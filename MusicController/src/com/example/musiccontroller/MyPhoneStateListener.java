package com.example.musiccontroller;

import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

//see http://www.codeproject.com/Tips/578817/Reject-and-Accept-an-Incoming-Call for responding
public class MyPhoneStateListener extends PhoneStateListener  {
	private static final String TAG = "PHONELISTENER";
	private boolean isRinging = false;

	public void onCallStateChanged(int state, String incomingNumber) {
		   super.onCallStateChanged(state, incomingNumber);
		   if (state == TelephonyManager.CALL_STATE_RINGING){
			   Log.i(TAG, "CALL_STATE_RINGING" + incomingNumber);
			   isRinging(true);			   	
		   }
		   else{
			   isRinging(false);
		   }
		   
		  }

	public boolean isRinging() {
		return isRinging;
	}

	private void isRinging(boolean isRinging) {
		this.isRinging = isRinging;
	}

 }
