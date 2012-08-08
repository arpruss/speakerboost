package mobi.omegacentauri.SpeakerBoost;

import java.util.ArrayList;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.audiofx.Equalizer;
import android.os.Build;

public class Settings {
	public int boostValue;
	public short bands;
	public short rangeLow;
	public short rangeHigh;
//	public boolean override;
	public boolean shape = true;
	private boolean released = true;;
	private ArrayList<SessionEqualizer> eqs;
	private static final int PRIORITY = 87654325; // Integer.MAX_VALUE;
	                                   
	private Equalizer eq;

	public Settings(Context context, boolean activeEqualizer) {
		eq = null;
		eqs = new ArrayList<SessionEqualizer>();
		
		if (9 <= Build.VERSION.SDK_INT) {
			try {
		        eq = new Equalizer(activeEqualizer ? PRIORITY : -15, 0);
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
    	int maxBoost = Options.getMaximumBoost(pref) * rangeHigh / 100;
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
	
	public void setEqualizer() {
//		if (override) {
//			for (SessionEqualizer e: eqs) {
//				SpeakerBoost.log("Setting equalizer for session "+e.session);
//				setEqualizer(e);
//			}
//		}
//		else {
			setEqualizer(eq);
//		}
	}
	
	public void setEqualizer(Equalizer e) {
		SpeakerBoost.log("setEqualizer "+boostValue);
		
		if (e == null) 
			return;
		
		short v;
		
		v = (short)boostValue;

		if (v < 0)
    		v = 0;
    	
    	if (v > rangeHigh)
    		v = rangeHigh;
    	
    	for (short i=0; i<bands; i++) {
        	
        	short adj = v;
        	
        	if (shape) {
	    		int hz = eq.getCenterFreq(i)/1000;
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
    	
    	e.setEnabled(v != 0);
	}
	
	public void setAll() {
		setEqualizer();
	}

	public boolean haveEqualizer() {
		return eq != null;
	}

	
	public void destroyEqualizer() {
		disableEqualizer();
		if (eq != null) {
			SpeakerBoost.log("Destroying equalizer");
			eq.release();
			released = true;
			eq = null;
		}
		
//		if (override) {
//			for (SessionEqualizer e: eqs) 
//				e.release();
//			eqs = null;
//		}
	}

	public void disableEqualizer() {
		if (eq != null && ! released) {
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
		return eq != null && boostValue>0;
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

	public void addSession(int stream, int session) {
		SpeakerBoost.log("Adding session "+session+" (stream "+stream+")");
		deleteSession(session);
		
		SessionEqualizer e = new SessionEqualizer(stream, session, PRIORITY); 
		eqs.add(e);
		setEqualizer(e);
	}
	
	public void deleteSession(int session) {
		SpeakerBoost.log("Deleting session "+session);
		ArrayList<SessionEqualizer> newEqs = new ArrayList<SessionEqualizer>();
		
		for (SessionEqualizer e: eqs) {
			if (e.session == session) {
				e.setEnabled(false);
				eqs.remove(e);
			}
			else {
				newEqs.add(e);
			}
		}
		
		eqs = newEqs;
	}
	
	public class SessionEqualizer extends Equalizer {
		public int stream;
		public int session;
		public int priority;
		
		public SessionEqualizer(int stream, int session, int priority) {
			super(priority, session);

			this.stream = stream;
			this.session = session;
			this.priority = priority;
			
			SpeakerBoost.log("Creating equalizer for session "+session);
		}
		
		public boolean equals(SessionEqualizer e) {
			return e.session == session && e.stream == stream && e.priority == priority;
		}
	}
}
