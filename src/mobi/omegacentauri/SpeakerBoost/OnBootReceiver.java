package mobi.omegacentauri.SpeakerBoost;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class OnBootReceiver extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context context, Intent intent) {
		Settings settings = new Settings(context, false);
		settings.load(PreferenceManager.getDefaultSharedPreferences(context));
		
		if (settings.needService()) {
			Intent i = new Intent(context, SpeakerBoostService.class);
			context.startService(i);
		}
	}
}
