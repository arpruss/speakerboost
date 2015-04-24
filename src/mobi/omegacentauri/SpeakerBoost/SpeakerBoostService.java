package mobi.omegacentauri.SpeakerBoost;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class SpeakerBoostService extends Service {
	
	private final Messenger messenger = new Messenger(new IncomingHandler());
	private SharedPreferences options;
	private Settings settings;
	protected boolean interruptReader;
//	private Thread logThread;
//	private Process logProcess;
	private long t0;
 	
	public class IncomingHandler extends Handler {
		public static final int MSG_OFF = 0;
		public static final int MSG_ON = 1;
		public static final int MSG_RELOAD_SETTINGS = 2;
		
		@Override 
		public void handleMessage(Message m) {
			SpeakerBoost.log("Message: "+m.what);
			switch(m.what) {
			case MSG_RELOAD_SETTINGS:
				settings.load(options);
				settings.setEqualizer();
				break;
			default:
				super.handleMessage(m);
			}
		}
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		return messenger.getBinder();
	}
	
	@Override
	public void onCreate() {
		t0 = System.currentTimeMillis();
		SpeakerBoost.log("Creating service at "+t0);
		options = PreferenceManager.getDefaultSharedPreferences(this);
		
		settings = new Settings(this, true);
		settings.load(options);
		if (!settings.haveEqualizer()) {
			Toast.makeText(this, "Error: Try later or reboot", Toast.LENGTH_LONG).show();
			SpeakerBoost.log("Error setting up equalizer");
			settings.boostValue = 0;
			settings.save(options);
		}
		else {
//			Toast.makeText(this, "Equalizer activated", 5000).show();
			SpeakerBoost.log("Success setting up equalizer");
		}

		if (Options.getNotify(options) != Options.NOTIFY_NEVER) {
	        Notification n = new Notification(
					R.drawable.equalizer,
					"SpeakerBoost", 
					System.currentTimeMillis());
			Intent i = new Intent(this, SpeakerBoost.class);
			i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			n.flags = Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT; 
			n.setLatestEventInfo(this, "SpeakerBoost", 
					settings.describe(), 
					PendingIntent.getActivity(this, 0, i, 0));
			SpeakerBoost.log("notify from service "+n.toString());
	
			startForeground(SpeakerBoost.NOTIFICATION_ID, n);
		}
		else {			
		}
		
		if (settings.isEqualizerActive())
			settings.setEqualizer();
		else
			settings.disableEqualizer();
		
//		if (settings.override) {
//	        Runnable logRunnable = new Runnable(){
//	        	@Override
//	        	public void run() {
//	                interruptReader = false;
//					monitorLog();
//				}};  
//			logThread = new Thread(logRunnable);
//			
//			logThread.start();
//		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		settings.load(options);
		SpeakerBoost.log("disabling equalizer");
		settings.destroyEqualizer();

		SpeakerBoost.log("Destroying service");
		if(Options.getNotify(options) != Options.NOTIFY_NEVER)
			stopForeground(true);
	}
	
	@Override
	public void onStart(Intent intent, int flags) {
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		onStart(intent, flags);
		return START_STICKY;
	}
}
