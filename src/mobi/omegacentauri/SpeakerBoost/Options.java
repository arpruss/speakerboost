package mobi.omegacentauri.SpeakerBoost;

import mobi.omegacentauri.SpeakerBoost.R;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;

public class Options extends PreferenceActivity {
	public static final String PREF_EQUALIZER_ACTIVE = "equalizerActive";
	public static final String PREF_NOTIFY = "notification";
	public static final String PREF_FIRST_TIME = "firstTime";
	public static final int NOTIFY_NEVER = 0;
	public static final int NOTIFY_AUTO = 1;
	public static final int NOTIFY_ALWAYS = 2;
	public static final String PREF_AD = "lastAd";
	public static final String PREF_BOOST = "boost2";
	public static final String PREF_WARNED_LAST_VERSION = "warnedLastVersion";
	public static final String PREF_LAST_VERSION = "lastVersion1";
	public static final String PREF_VOLUME = "volumeControl";
	public static final String PREF_SHAPE = "shape";
	public static final String PREF_MAXIMUM_BOOST = "maximumBoost2";
	public static final String PREF_MAXIMUM_BOOST_OLD = "maximumBoost";
	public static final String PREF_OVERRIDE = "override";
	public static final String PREF_FORCE = "force";
	public static final String PREF_DONATE_MESSAGE = "donateDidMessage";
	public static final String PREF_BOOST_ON_BOOT = "boostOnBoot";
	public static final String PREF_NO_WARN = "noWarn";
	
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		addPreferencesFromResource(R.xml.options);
//
//		Preference p = getPreferenceScreen().findPreference(PREF_VOLUME);
//		if (p != null) {
//			SpeakerBoost.log("Setting PREF_VOLUME to "+defaultShowVolume());
//			p.setDefaultValue(defaultShowVolume());
//		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
	}
	
	@Override
	public void onStop() {
		super.onStop();
	}

	public static boolean isKindle() {
		return Build.MODEL.equalsIgnoreCase("Kindle Fire");		
	}
	
	public static int getNotify(SharedPreferences options) {
		int n = Integer.parseInt(options.getString(PREF_NOTIFY, "2"));
//		if (n == NOTIFY_NEVER)
//			return NOTIFY_AUTO;
//		else
//			return n;
		return n;
   	}
	
	public static boolean defaultShowVolume() {
		return isKindle();
	}
	
	public static int getMaximumBoost(SharedPreferences options) {
		try {
			int old = Integer.parseInt(options.getString(PREF_MAXIMUM_BOOST_OLD, "-1"));
			
			if (old < 0)
				return Integer.parseInt(options.getString(PREF_MAXIMUM_BOOST, "60"));
			
			options.edit().putString(PREF_MAXIMUM_BOOST_OLD, "-1").commit();
			
			return old < 60 ? old : 60;
		}
		catch (NumberFormatException e) {
			return 60;
		}
	}
}
