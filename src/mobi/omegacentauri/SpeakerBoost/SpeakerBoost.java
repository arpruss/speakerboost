package mobi.omegacentauri.SpeakerBoost;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Rect;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class SpeakerBoost extends Activity implements ServiceConnection {
	private static boolean DEBUG = true;
	private SharedPreferences options;
	private Messenger messenger;
	private int SLIDER_MAX = 10000;
	private SeekBar boostBar;
	private SeekBar volumeBar;
	private Settings settings;
//	private TextView ad;
	private AudioManager am;
	private LinearLayout main;
	private boolean showVolume = true;
	private boolean disable = false;
	private int versionCode;
//	private OnSharedPreferenceChangeListener preferenceChangeListener = null;
	
	static final int NOTIFICATION_ID = 1;

	public static void log(String s) {
		if (DEBUG )
			Log.v("SpeakerBoost", s);
	}
	
    /** Called when the activity is first created. */
    @SuppressLint("NewApi")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
		options = PreferenceManager.getDefaultSharedPreferences(this);
		
		try {
			versionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
		} catch (NameNotFoundException e) {
			versionCode = 0;
		} 
		
        am = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		main = (LinearLayout)getLayoutInflater().inflate(R.layout.main, null);
		
		if (Build.VERSION.SDK_INT>=11) setFinishOnTouchOutside(true);

		setContentView(main);

		settings = new Settings(this, false);
		
//		if (! settings.haveEqualizer()) {
//			fatalError("Error", "Your device is not supported by SpeakerBoost.");
//		}
		
		boolean o = options.getBoolean(Options.PREF_VOLUME, Options.defaultShowVolume());
		/* This ensures that the default shows up correctly in the settings screen.
		 * This should be doable via setDefaultValue() in Options, but somehow that
		 * wasn't working.
		 */
		options.edit().putBoolean(Options.PREF_VOLUME, o).commit();

    	boostBar = (SeekBar)findViewById(R.id.boost);
    	volumeBar = (SeekBar)findViewById(R.id.vol);    	
//        ad = (TextView)findViewById(R.id.ad);
//        
//        ad.setOnClickListener(new OnClickListener(){
//
//			@Override
//			public void onClick(View arg0) {
//				market();
//			}});
    }
    
    void market() {
    	MarketDetector.launch(this);
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
    		am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, showVolume?0:AudioManager.FLAG_SHOW_UI);
    		updateVolumeDisplay();
    		return true;
    	}
    	else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
    		am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, showVolume?0:AudioManager.FLAG_SHOW_UI);
    		updateVolumeDisplay();
    		return true;
    	}
    		
    	return super.onKeyDown(keyCode, event);
    }
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);

		if (MarketDetector.NO_MARKET)
				menu.findItem(R.id.please_buy).setVisible(false);
		return true;
	}
	
	public void menuButton(View v) {
		openOptionsMenu();
	}
	
	private void reloadSettings() {
		sendMessage(SpeakerBoostService.IncomingHandler.MSG_RELOAD_SETTINGS, 0, 0);
	}

	private void warning() {
		AlertDialog alertDialog = new AlertDialog.Builder(this).create();

		settings.boostValue = 0;
		settings.save(options);
		boostBar.setProgress(0);
		reloadSettings();

		alertDialog.setTitle("Warning");
		alertDialog.setMessage(Html.fromHtml(getAssetFile("warning.html")));
		alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, 
				"Yes", 
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				options.edit().putInt(Options.PREF_WARNED_LAST_VERSION, versionCode).commit();
			} });
		alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, 
				"No", 
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				SpeakerBoost.this.finish();
			} });
		alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
			public void onCancel(DialogInterface dialog) {
			SpeakerBoost.this.finish();
			} });
		alertDialog.show();

	}
	
//	private void fatalError(String title, String msg) {
//		message(title, msg, true);
//	}
	
	private void message(String title, String msg) {
		message(title, msg, false);
	}
	
	private void message(String title, String msg, final Boolean exit) {
		AlertDialog alertDialog = new AlertDialog.Builder(this).create();

		alertDialog.setTitle(title);
		alertDialog.setMessage(Html.fromHtml(msg));
		alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, 
				"OK", 
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				if (exit) SpeakerBoost.this.finish();} });
		alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
			public void onCancel(DialogInterface dialog) {
				if (exit) SpeakerBoost.this.finish();
				
			} });
		alertDialog.show();
	}
	
	private void show(String title, String filename) {
		message(title, getAssetFile(filename));
	}
	
	private void versionUpdate() {
		log("version "+versionCode);
		
		if (options.getInt(Options.PREF_LAST_VERSION, 0) != versionCode) {
			options.edit().putInt(Options.PREF_LAST_VERSION, versionCode).commit();
			show("Change log", "changelog.html");
		}
		
		if (options.getInt(Options.PREF_WARNED_LAST_VERSION, 0) != versionCode) {
			warning();
		}
	}
	

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.options:
			Intent i = new Intent(this, Options.class);
			startActivity(i);
			return true;
		case R.id.change_log:
			show("Change log", "changelog.html");
			return true;
		case R.id.help:
			show("Questions and answers", "help.html");
			return true;
		case R.id.please_buy:
			market();
			return true;
		default:
			return false;
		}
	}

	void updateService(boolean value) {
		if (value) {
			log("restartService");
			restartService(true);
    	}
		else {
			log("stopService");
			stopService();
	    	updateNotification();
		}
    }
    
    void updateService() {
    	log("needService = "+settings.needService());
    	updateService(settings.needService());
    }
    
    private void updateBoostText(int progress) {
		String t = "Boost: "+((progress*100+SLIDER_MAX/2)/SLIDER_MAX)+"%"; 
		((TextView)findViewById(R.id.boost_value)).setText(t);
    }
    
    private void updateVolText(int progress) {
		String t = "Vol.: "+((progress*100+SLIDER_MAX/2)/SLIDER_MAX)+"%"; 
		((TextView)findViewById(R.id.vol_value)).setText(t);
    }
    
    void setupEqualizer() {
    	log("setupEqualizer");

		boostBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				log("progress changed");
				if (fromUser) {
					int oldBoost = settings.boostValue;
					settings.boostValue = fromSlider(progress,0,Settings.NOMINAL_RANGE_HIGH);
					log("setting "+settings.boostValue);
					settings.save(options);
					
					if ((settings.boostValue==0) != (oldBoost==0)) {
						updateService();
					}
					else {
						sendMessage(SpeakerBoostService.IncomingHandler.MSG_RELOAD_SETTINGS, 0, 0);
					}
				}
				updateBoostText(progress);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
		});

		int progress = toSlider(options.getInt(Options.PREF_BOOST, 0), 0,
				settings.NOMINAL_RANGE_HIGH);
		boostBar.setProgress(progress);
		updateBoostText(progress);
    }
    
    private int fromSlider(int value, int min, int max) {
    	return (min * (SLIDER_MAX - value) + max * value + SLIDER_MAX/2) / SLIDER_MAX;
    }

    private int toSlider(int value, int min, int max) {
    	return ((value-min)*SLIDER_MAX + (max-min)/2) / (max-min);
    }

    private void updateVolume() {
    	if (options.getBoolean(Options.PREF_VOLUME, Options.defaultShowVolume())) {
    		findViewById(R.id.vol_layout).setVisibility(View.VISIBLE);
    		showVolume = true;
    	}
    	else {
    		findViewById(R.id.vol_layout).setVisibility(View.GONE);
    		showVolume = false;
    		return;
    	}
    	
		findViewById(R.id.vol_layout).setVisibility(View.VISIBLE);
        final int maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		volumeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				am.setStreamVolume(AudioManager.STREAM_MUSIC, fromSlider(progress, 0, maxVolume), 0);
				updateVolText(progress);
			}
		});
		updateVolumeDisplay();
    }
    
    private void updateVolumeDisplay() {
        int p = toSlider(am.getStreamVolume(AudioManager.STREAM_MUSIC), 0, am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)); 
		volumeBar.setProgress(p);
		updateVolText(p);
    }
    
    void resize() {
    	log("resize1");
    	LinearLayout ll = main;
    	log("resize2");
    	FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams)ll.getLayoutParams();
    	log("resize3");

    	Rect r = new Rect();
    	ll.getWindowVisibleDisplayFrame(r);
    	int h = r.height();
    	int w = r.width();
    	
    	
    	if (w>h) {
    		lp.width = h * 89 / 100;
//    		lp.setMargins((w-h)/2,0,(w-h)/2,0);
    	}
    	else {
    		lp.width = w * 89 / 100;
//    		lp.setMargins(0,0,0,0);    		
    	}
    	
    	
		ll.setLayoutParams(lp);
    }
    
    @Override
    public void onResume() {
    	super.onResume();
        
//		preferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
//			
//			@Override
//			public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
//					String key) {
//				if (0 == sharedPreferences.getInt(Options.PREF_BOOST, 0)) {
//					if (boostBar != null)
//						boostBar.setProgress(0);						
//				}
//			}
//		};
//		
//		options.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
        
        versionUpdate();

    	resize();

    	settings.load(options);
    	log("loaded boost = "+settings.boostValue);
    	
    	int maxBoost = Options.getMaximumBoost(options);
    	boostBar.setMax(SLIDER_MAX * maxBoost / 100);
    	if (settings.boostValue > settings.NOMINAL_RANGE_HIGH * maxBoost / 100) {
    		settings.boostValue = settings.NOMINAL_RANGE_HIGH * maxBoost / 100;
    		settings.save(options);
    	}

    	setupEqualizer();		
		updateService();
		updateNotification();
		updateVolume();

//		ad.setVisibility(havePaidApp() ? View.GONE : View.VISIBLE);		
    }
    
    private boolean have(String p) {
    	try {
			return getPackageManager().getPackageInfo(p, 0) != null;
		} catch (NameNotFoundException e) {
			return false;
		}    	
    }
    
    private boolean havePaidApp() {
    	return have("mobi.omegacentauri.ScreenDim.Full") ||
    		have("mobi.pruss.force2sd") ||
    		have("mobi.omegacentauri.LunarMap.HD");
	}

	@Override
    public void onPause() {
    	super.onPause();
    	
    	if (messenger != null) {
			log("unbind");
			unbindService(this);
			messenger = null;
		}
    	
//    	if (preferenceChangeListener != null)
//    		options.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
    }
    
	public static void setNotification(Context c, NotificationManager nm, Settings s) {
		Notification n = new Notification(
				s.somethingOn()?R.drawable.equalizer:R.drawable.equalizeroff,
				"SpeakerBoost", 
				System.currentTimeMillis());
		Intent i = new Intent(c, SpeakerBoost.class);		
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		n.flags = Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT; 
		n.setLatestEventInfo(c, "SpeakerBoost", s.describe(), 
				PendingIntent.getActivity(c, 0, i, 0));
		nm.notify(NOTIFICATION_ID, n);
		log("notify "+n.toString());
	}
	
	private void updateNotification() {
		updateNotification(this, options, 
				(NotificationManager)getSystemService(NOTIFICATION_SERVICE), 
				settings);
	}
	
	public static void updateNotification(Context c, 
			SharedPreferences options, NotificationManager nm, Settings s) {
		log("notify "+Options.getNotify(options));
		switch(Options.getNotify(options)) {
		case Options.NOTIFY_NEVER:
			nm.cancelAll();
			break;
		case Options.NOTIFY_AUTO:
			if (s.needService())
				setNotification(c, nm, s);
			else {
				log("trying to cancel notification");
				nm.cancelAll();
			}
			break;
		case Options.NOTIFY_ALWAYS:
			setNotification(c, nm, s);
			break;
		}
	}

	void stopService() {
		log("stop service");
		if (messenger != null) {
			unbindService(this);
			messenger = null;
		}
		
		stopService(new Intent(this, SpeakerBoostService.class));
	}
	
	void saveSettings() {
	}
	
	void bind() {
		log("bind");
		Intent i = new Intent(this, SpeakerBoostService.class);
		bindService(i, this, 0);
	}
	
	void restartService(boolean bind) {
		stopService();
		saveSettings();		
		log("starting service");
		Intent i = new Intent(this, SpeakerBoostService.class);
		startService(i);
		if (bind) {
			bind();
		}
	}
	
//	void setActive(boolean value, boolean bind) {
//		SharedPreferences.Editor ed = options.edit();
//		ed.putBoolean(Options.PREF_EQUALIZER_ACTIVE, value);
//		ed.commit();
//		if (value) {
//			restartService(bind);
//		}
//		else {
//			stopService();
//		}
//		equalizerActive = value;
//		updateNotification();
//	}
	
	public void sendMessage(int n, int arg1, int arg2) {
		if (messenger == null) 
			return;
		
		try {
			log("message "+n+" "+arg1+" "+arg2);
			messenger.send(Message.obtain(null, n, arg1, arg2));
		} catch (RemoteException e) {
		}
	}
	
	@Override
	public void onServiceConnected(ComponentName classname, IBinder service) {
		log("connected");
		messenger = new Messenger(service);
//		try {
//			messenger.send(Message.obtain(null, IncomingHandler.MSG_ON, 0, 0));
//		} catch (RemoteException e) {
//		}
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
		log("disconnected"); 
//		stopService(new Intent(this, SpeakerBoostService.class));
		messenger = null;		
	}

	static private String getStreamFile(InputStream stream) {
		BufferedReader reader;
		try {
			reader = new BufferedReader(new InputStreamReader(stream));

			String text = "";
			String line;
			while (null != (line=reader.readLine()))
				text = text + line;
			return text;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			return "";
		}
	}
	
	public String getAssetFile(String assetName) {
		try {
			return getStreamFile(getAssets().open(assetName));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			return "";
		}
	}
	
	public void optionsClick(View v) {
		openOptionsMenu();
	}
}
