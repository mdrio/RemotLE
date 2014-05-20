package com.example.musiccontroller;
import java.util.Locale;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

public class Speaker implements TextToSpeech.OnInitListener{
	private static TextToSpeech tts;
	
	public Speaker(Context context) {
		super();
		tts = new TextToSpeech(context, this);
	}
	
	@Override
	public void onInit(int status) {
		// TODO Auto-generated method stub
		if (status == TextToSpeech.SUCCESS) {
			 
            int result = tts.setLanguage(Locale.ITALY);
 
            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "This Language is not supported");
            }
 
        } else {
            Log.e("TTS", "Initilization Failed!");
        }
		
	}
	
	private void speakOut(String text) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }

}
