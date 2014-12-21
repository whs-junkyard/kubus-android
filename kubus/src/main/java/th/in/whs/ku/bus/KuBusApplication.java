package th.in.whs.ku.bus;

import java.util.HashMap;

import th.in.whs.ku.bus.api.API;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Logger.LogLevel;
import com.google.android.gms.analytics.Tracker;
import com.joshdholtz.sentry.Sentry;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.util.Log;

public class KuBusApplication extends Application {

	@Override
	public void onCreate() {
		super.onCreate();

		try {
			ApplicationInfo ai = getPackageManager().getApplicationInfo(this.getPackageName(), PackageManager.GET_META_DATA);
			Bundle bundle = ai.metaData;
			PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			String dsn = bundle.getString("sentry_server");

			HashMap<String, String> tags = new HashMap<String, String>();
			tags.put("version", pInfo.versionName);
			tags.put("debug", BuildConfig.DEBUG ? "true" : "false");
			Sentry.init(this, "http://sentry.whs.in.th", dsn, tags);
			API.init(pInfo.versionName);
		} catch (NameNotFoundException e) {
			Log.e("MainActivity", "Failed to load meta-data, NameNotFound: " + e.getMessage());
			API.init("unknown");
		} catch (NullPointerException e) {
			Log.e("MainActivity", "Failed to load meta-data, NullPointer: " + e.getMessage());
			API.init("unknown");
		}
	}
	
	private Tracker tracker; 

	public Tracker getTracker() {
		if(tracker == null){
			GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
			if(BuildConfig.DEBUG){
				analytics.getLogger().setLogLevel(LogLevel.VERBOSE);
			}
			tracker = analytics.newTracker(R.xml.global_tracker);
			tracker.enableAdvertisingIdCollection(true);
		}
		return tracker;
	}
	
	public void report(String view){
		Tracker t = getTracker();
        t.setScreenName("BusStopList");
        t.send(new HitBuilders.AppViewBuilder().build());
	}

}
