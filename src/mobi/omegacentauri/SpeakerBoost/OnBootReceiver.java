package mobi.omegacentauri.SpeakerBoost;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.preference.PreferenceManager;

public class OnBootReceiver extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context context, Intent intent) {
		SharedPreferences options = PreferenceManager.getDefaultSharedPreferences(context); 
		
		try {
			if (options.getInt(Options.PREF_WARNED_LAST_VERSION, 0) != context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode) {
				return;
			}
		} catch (NameNotFoundException e) {
			return;
		}		
		
		Settings settings = new Settings(context, false);
		settings.load(options);
		
		if (settings.needService()) {
			Intent i = new Intent(context, SpeakerBoostService.class);
			context.startService(i);
		}
	}
}
