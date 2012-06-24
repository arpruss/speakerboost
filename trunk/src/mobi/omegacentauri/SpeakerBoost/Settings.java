package mobi.omegacentauri.SpeakerBoost;

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
	public boolean shape = true;
	
	private Equalizer eq;

	public Settings(Context context) {
		eq = null;
		
		if (9 <= Build.VERSION.SDK_INT) {
			try {
		        eq = new Equalizer(0, 0);
				bands = eq.getNumberOfBands();
				
				rangeLow = eq.getBandLevelRange()[0];
				rangeHigh = eq.getBandLevelRange()[1];
			}
			catch (UnsupportedOperationException e) {
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
    	shape = pref.getBoolean(Options.PREF_SHAPE, true);
	}
	
	public void save(SharedPreferences pref) {
    	SharedPreferences.Editor ed = pref.edit();
    	ed.putInt(Options.PREF_BOOST, boostValue);
    	ed.putBoolean(Options.PREF_SHAPE, shape);
    	ed.commit();
	}
	
	public void setEqualizer() {
		SpeakerBoost.log("setEqualizer "+boostValue);
		
		if (eq == null) 
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

        	eq.setBandLevel(i, adj);
    	}
    	
    	eq.setEnabled(v > 0);
	}
	
	public void setAll() {
		setEqualizer();
	}

	public boolean haveEqualizer() {
		return eq != null;
	}

	public void disableEqualizer() {
		if (eq != null) {
			SpeakerBoost.log("Closing equalizer");
			eq.setEnabled(false);
		}
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
}
