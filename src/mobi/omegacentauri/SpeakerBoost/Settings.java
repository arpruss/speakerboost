package mobi.omegacentauri.SpeakerBoost;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.audiofx.Equalizer;
import android.media.audiofx.LoudnessEnhancer;
import android.os.Build;
import android.util.Log;

public class Settings {
	public int boostValue;
	private short bands;
	private short rangeLow;
	private short rangeHigh;
	public static final int NOMINAL_RANGE_HIGH = 1500;
//	public boolean override;
	public boolean shape = true;
	private boolean released = true;;
//	private ArrayList<SessionEqualizer> eqs;
	private static final int PRIORITY = 87654325; // Integer.MAX_VALUE;
	                                   
	private Equalizer eq = null;
	private LoudnessEnhancer le = null;
	private TimerTask timerTask;
	private Timer timer;
	private MediaPlayer mp;

	@SuppressLint("NewApi")
	public Settings(Context context, boolean activeEqualizer) {
		eq = null;
		
		if (!activeEqualizer)
			return;
		
//		eqs = new ArrayList<SessionEqualizer>();
		
		if (19 <= Build.VERSION.SDK_INT) {
			try {
				le = new LoudnessEnhancer(0);
				if (!activeEqualizer) {
					le.release();
					released = true;
				}
				else {
					released = false;
				}
			}
			catch (Exception e) {
				SpeakerBoost.log("Exception "+e);
				le = null;
			}
		}
		else if (9 <= Build.VERSION.SDK_INT) {
			try {
		        eq = new Equalizer(PRIORITY, 0);
				bands = eq.getNumberOfBands();
				
				SpeakerBoost.log("Set up equalizer, have "+bands+" bands");
				
				rangeLow = eq.getBandLevelRange()[0];
				rangeHigh = eq.getBandLevelRange()[1];

				SpeakerBoost.log("range "+rangeLow+ " "+rangeHigh);
				
				if (!activeEqualizer) {
					eq.release();
					released = true;
				}
				else {
					released = false;
				}
			}
			catch (UnsupportedOperationException e) {
				SpeakerBoost.log("Exception "+e);
				eq = null;
			}
			catch (IllegalArgumentException e) {
				SpeakerBoost.log("Exception "+e);
				eq = null;
			}
		}
	}
	
	public void load(SharedPreferences pref) {
    	boostValue = pref.getInt(Options.PREF_BOOST, 0);
    	int maxBoost = Options.getMaximumBoost(pref) * NOMINAL_RANGE_HIGH / 100;
    	if (boostValue > maxBoost)
    		boostValue = maxBoost;
    	shape = pref.getBoolean(Options.PREF_SHAPE, true);
//    	override = false; // pref.getBoolean(Options.PREF_OVERRIDE, false);
	}
	
	public void save(SharedPreferences pref) {
    	SharedPreferences.Editor ed = pref.edit();
    	ed.putInt(Options.PREF_BOOST, boostValue);
//    	ed.putBoolean(Options.PREF_OVERRIDE, override);
    	ed.commit();
	}
	
	@SuppressLint("NewApi")
	public void setEqualizer() {
		if (le != null) {
			Log.v("SpeakerBoost", "setting loudness boost to "+(boostValue * NOMINAL_RANGE_HIGH / 100));
			try {
				le.setEnabled(boostValue > 0);
				le.setTargetGain(boostValue * NOMINAL_RANGE_HIGH / 100);
			}
			catch(Exception e) {
				Log.e("SpeakerBoost", "le "+e);
			}
		}
		else 
			setEqualizer(eq);
	}
	
	private void setEqualizer(Equalizer e) {
		SpeakerBoost.log("setEqualizer "+boostValue);
		
		if (e == null) 
			return;
		
		short v = (short)( (boostValue * rangeHigh + NOMINAL_RANGE_HIGH/2) / NOMINAL_RANGE_HIGH );

		if (v < 0)
    		v = 0;
    	
    	if (v > rangeHigh)
    		v = rangeHigh;
    	
    	
    	if (v != 0) {
	    	e.setEnabled(true);	
	    	
	    	for (short i=0; i<bands; i++) {        	
	        	short adj = v;
	        	
	        	if (shape) {
		    		int hz = e.getCenterFreq(i)/1000;
		        	if (hz < 150)
		        		adj = 0;
		        	else if (hz < 250)
		        		adj = (short)(v/2);
		        	else if (hz > 8000)
		        		adj = (short)(3*(int)v/4);
	        	}
	
	        	SpeakerBoost.log("boost "+i+" ("+(eq.getCenterFreq(i)/1000)+"hz) to "+adj);        	
	        	SpeakerBoost.log("previous value "+eq.getBandLevel(i));
	
	        	try {
	        		e.setBandLevel(i, (short) adj); 
	        	}
	        	catch (Exception exc) {
	        		SpeakerBoost.log("Error "+exc);
	        	}
	    	}
    	}
    	else {
    		e.setEnabled(false);
    	}    	
	}
	
	public void setAll() {
		setEqualizer();
	}

	public boolean haveEqualizer() {
		return le != null || eq != null;
	}

	
	@SuppressLint("NewApi")
	public void destroyEqualizer() {
		disableEqualizer();
		if (le != null) {
			SpeakerBoost.log("Destroying le");
			le.release();
			released = true;
			le = null;
		}
		if (eq != null) {
			SpeakerBoost.log("Destroying equalizer");
			eq.release();
			released = true;
			eq = null;
		}
//		if (timer != null) {
//			timer.cancel();
//			timer = null;
//		}
//		if (timerTask != null) {
//			timerTask.cancel();
//			timerTask= null;
//		}
		
//		if (override) {
//			for (SessionEqualizer e: eqs) 
//				e.release();
//			eqs = null;
//		}
	}

	@SuppressLint("NewApi")
	public void disableEqualizer() {
		if (le != null && ! released) {
			SpeakerBoost.log("Closing loudnessenhancer");
			le.setEnabled(false);			
		}
		else if (eq != null && ! released) {
			SpeakerBoost.log("Closing equalizer");
			eq.setEnabled(false);
		}
		
//		if (override) 
//			for (SessionEqualizer e: eqs)
//				e.setEnabled(false);
	}
	
	public boolean haveProximity() {
		return true;//sm.getSensorList(SensorManager.SENSOR_PROXIMITY).isEmpty();
	}
	
	public boolean isEqualizerActive() {
		return boostValue>0;
	}
	
	public boolean needService() {
		return isEqualizerActive();
	}
	
//    private static String onoff(boolean v) {
//    	return v ? "on" : "off";
//    }
    
    public boolean somethingOn() {
    	return isEqualizerActive();
    }
    
	public String describe() {
		if (! somethingOn())
			return "SpeakerBoost is off";
		
		String[] list = new String[1];
		int count;
		
		count = 0;
		if (isEqualizerActive())
			list[count++] = "Boost is on";
		
		String out = "";
		for (int i=0; i<count; i++) {
			out = out + list[i];
			if (i+1<count)
				out += ", ";
		}
		return out;
	}

//	public void addSession(int stream, int session) {
//		SpeakerBoost.log("Adding session "+session+" (stream "+stream+")");
//		deleteSession(session);
//		
//		SessionEqualizer e = new SessionEqualizer(stream, session, PRIORITY); 
//		eqs.add(e);
//		setEqualizer(e);
//	}
//	
//	public void deleteSession(int session) {
//		SpeakerBoost.log("Deleting session "+session);
//		ArrayList<SessionEqualizer> newEqs = new ArrayList<SessionEqualizer>();
//		
//		for (SessionEqualizer e: eqs) {
//			if (e.session == session) {
//				e.setEnabled(false);
//				eqs.remove(e);
//			}
//			else {
//				newEqs.add(e);
//			}
//		}
//		
//		eqs = newEqs;
//	}
//	
//	public class SessionEqualizer extends Equalizer {
//		public int stream;
//		public int session;
//		public int priority;
//		
//		public SessionEqualizer(int stream, int session, int priority) {
//			super(priority, session);
//
//			this.stream = stream;
//			this.session = session;
//			this.priority = priority;
//			
//			SpeakerBoost.log("Creating equalizer for session "+session);
//		}
//		
//		public boolean equals(SessionEqualizer e) {
//			return e.session == session && e.stream == stream && e.priority == priority;
//		}
//	}
}
