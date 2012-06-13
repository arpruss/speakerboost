package mobi.omegacentauri.SpeakerBoost;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mobi.omegacentauri.SpeakerBoost.R;

import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.media.AudioManager;
import android.media.audiofx.Equalizer;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.Gravity;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SpeakerBoostService extends Service implements SensorEventListener {
	
	private final Messenger messenger = new Messenger(new IncomingHandler());
	private static final int PROXIMITY_SCREEN_OFF_WAKE_LOCK = 32;
	private SharedPreferences options;
	private Settings settings;
	private PowerManager pm;
	private KeyguardManager km;
	private WakeLock wakeLock = null;
	private KeyguardLock guardLock = null;
	private static final String PROXIMITY_TAG = "mobi.omegacentauri.SpeakerBoost.EarpieceService.proximity";
	private static final String GUARD_TAG = "mobi.omegacentauri.SpeakerBoost.EarpieceService.guard";
 	
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
		SpeakerBoost.log("Creating service");
		options = PreferenceManager.getDefaultSharedPreferences(this);
	    pm = (PowerManager)getSystemService(POWER_SERVICE);
	    km = (KeyguardManager)getSystemService(KEYGUARD_SERVICE);
		
		settings = new Settings(this);
		settings.load(options);

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
	}
	
	@Override
	public void onDestroy() {
		settings.load(options);
//		if (settings.isEqualizerActive()) {
			SpeakerBoost.log("disabling equalizer");
			settings.disableEqualizer();
//		}
		disableProximity();
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
	
	private void activateProximity() {
		wakeLock = pm.newWakeLock(PROXIMITY_SCREEN_OFF_WAKE_LOCK, 
				PROXIMITY_TAG);
		wakeLock.acquire();
		guardLock = km.newKeyguardLock(GUARD_TAG);
		guardLock.disableKeyguard();
	}
	
	private void disableProximity() {
		if (null != wakeLock) {
			wakeLock.release();
			wakeLock = null;
		}
		if (null != guardLock) {
			guardLock.reenableKeyguard();
			guardLock = null;
		}		
	}
	
	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		
	}

}
