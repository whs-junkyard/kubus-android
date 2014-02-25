package th.in.whs.ku.bus;

import java.util.HashMap;

import th.in.whs.ku.bus.api.API;

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
	
}
