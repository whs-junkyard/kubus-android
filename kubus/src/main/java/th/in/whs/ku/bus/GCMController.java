package th.in.whs.ku.bus;

import java.io.IOException;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;

import com.google.android.gms.gcm.GoogleCloudMessaging;

public class GCMController {
	private static GCMController instance;
	public static GCMController getInstance(){
		return instance;
	}
	
	private Context context;
	private String regid;
	private GoogleCloudMessaging gcm;
	
	private static final String SENDER_ID = "209951392219";
	
	public GCMController(Context context){
		this.context = context.getApplicationContext();
		instance = this;
	}
	
	public void init(){
		new Thread(new Runnable(){

			@Override
			public void run() {
				gcm = GoogleCloudMessaging.getInstance(context);
		        regid = getRegistrationId(context);

		        if (regid.length() == 0) {
		            register();
		        }
			}
			
		}).start();
	}
	
	public String getRegID(){
		return this.regid;
	}

	private void register() {
		if (gcm == null) {
            gcm = GoogleCloudMessaging.getInstance(context);
        }
        try {
			regid = gcm.register(SENDER_ID);
		} catch (IOException e) {
			return;
		}
        storeRegistrationId(context, regid);
	}
	
	private void storeRegistrationId(Context context, String regid) {
		final SharedPreferences prefs = getGCMPreferences(context);
	    int appVersion = getAppVersion(context);
	    SharedPreferences.Editor editor = prefs.edit();
	    editor.putString("gcmID", regid);
	    editor.putInt("gcmVersion", appVersion);
	    editor.commit();
	}

	private String getRegistrationId(Context context) {
	    final SharedPreferences prefs = getGCMPreferences(context);
	    String registrationId = prefs.getString("gcmID", "");
	    if (registrationId.length() == 0) {
	        return "";
	    }
	    // Check if app was updated; if so, it must clear the registration ID
	    // since the existing regID is not guaranteed to work with the new
	    // app version.
	    int registeredVersion = prefs.getInt("gcmVersion", Integer.MIN_VALUE);
	    int currentVersion = getAppVersion(context);
	    if (registeredVersion != currentVersion) {
	        return "";
	    }
	    return registrationId;
	}
	
	private int getAppVersion(Context context) {
		try {
	        PackageInfo packageInfo = context.getPackageManager()
	                .getPackageInfo(context.getPackageName(), 0);
	        return packageInfo.versionCode;
	    } catch (NameNotFoundException e) {
	        // should never happen
	        throw new RuntimeException("Could not get package name: " + e);
	    }
	}

	/**
	 * @return Application's {@code SharedPreferences}.
	 */
	private SharedPreferences getGCMPreferences(Context context) {
	    // This sample app persists the registration ID in shared preferences, but
	    // how you store the regID in your app is up to you.
	    return context.getSharedPreferences(MainActivity.class.getSimpleName(),
	            Context.MODE_PRIVATE);
	}
}
