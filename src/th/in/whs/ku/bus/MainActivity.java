package th.in.whs.ku.bus;

import im.delight.apprater.AppRater;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import th.in.whs.ku.bus.api.API;
import th.in.whs.ku.bus.api.BusPosition;
import th.in.whs.ku.bus.api.BusStopList;
import th.in.whs.ku.bus.api.ListenerList.Listener;
import th.in.whs.ku.bus.api.NFCBuilder;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.MapsInitializer;
import com.joshdholtz.sentry.Sentry;

public class MainActivity extends ActionBarActivity implements Handler.Callback, NFCSettableActivity {
	
	public final static String OPEN_STOP = "th.in.whs.bus.ku.STOP";
	static final int REQUEST_CODE_RECOVER_PLAY_SERVICES = 1001;
	static final private int CLOSE = 9999;
	public final int REFRESH_INTERVAL = 60000;
	public final static boolean USE_TRACE = false;
	public final static boolean USE_NFC_PROGRAM = false;

    private BusPositionRefresher refresher = new BusPositionRefresher();
    private Handler handler = new Handler(this);
    private boolean canRefresh = true;
    private Fragment currentFragment;
    private GCMController gcm;
    private NfcAdapter nfc;
    
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
    protected void onCreate(Bundle savedInstanceState) {
		if(USE_TRACE){
			Debug.startMethodTracing("kusmartbus");
		}
		
    	if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= 9) {
    		if(Build.VERSION.SDK_INT >= 11){
    			StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
		            .detectAll()
		            .penaltyLog()
		            .build());
	    		StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
	    			.detectAll()
	    			.setClassInstanceLimit(BusStopFragment.class, 1)
	    			.setClassInstanceLimit(BusStopInfoFragment.class, 1)
	    			.setClassInstanceLimit(BusStopListFragment.class, 1)
	    			.setClassInstanceLimit(BusMapFragment.class, 1)
	    			.setClassInstanceLimit(ThereFragment.class, 1)
	    			.setClassInstanceLimit(API.class, 1)
	    			.setClassInstanceLimit(BusStopList.class, 1)
	    			.setClassInstanceLimit(BusPosition.class, 1)
	    			.setClassInstanceLimit(BusMapController.class, 1)
	    			.build());
    		}else{
    			StrictMode.enableDefaults();
    		}
        }
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
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_PROGRESS);
        this.gcm = new GCMController(this);
        this.gcm.init();
        
        if(!checkGooglePlay()){
        	return;
        }
        
        MapsInitializer.initialize(this);
        
        this.restoreBusPosition(savedInstanceState);
		this.restoreBusList(savedInstanceState);
        
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.setDisplayShowTitleEnabled(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT);
        
        actionBar.addTab(actionBar.newTab()
        		.setText(R.string.stop_title)
        		.setTabListener(new TabHandler(BusStopFragment.class, "BusStop")));
        
        actionBar.addTab(actionBar.newTab()
        		.setText(R.string.map_title)
        		.setTabListener(new TabHandler(BusMapFragment.class, "BusMap")));
        
        actionBar.addTab(actionBar.newTab()
        		.setText(R.string.route_title)
        		.setTabListener(new TabHandler(ThereFragment.class, "There")));

        Intent intent = getIntent();
        String openStop = intent.getStringExtra(OPEN_STOP);
        if(openStop != null){
        	try{
        		showBusStop(Integer.valueOf(openStop));
        	}catch(NumberFormatException e){
        		Toast.makeText(this, android.R.string.httpErrorBadUrl, Toast.LENGTH_LONG).show();
        	}
        }
        
        if(savedInstanceState != null){
			int tab = savedInstanceState.getInt("tab");
			getSupportActionBar().setSelectedNavigationItem(tab);
		}
        
        AppRater appRater = new AppRater(this);
    	appRater.setDaysBeforePrompt(3);
    	appRater.setLaunchesBeforePrompt(5);
    	appRater.setPhrases(R.string.rate_title, R.string.rate_explanation, R.string.rate_now, R.string.rate_later, R.string.rate_never);
    	appRater.show();
    	
    	// GINGERBREAD_MR1 support NFC, but does not support createUri
    	// if I have access to a gingerbread phone with NFC I might fix this
    	if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH){
    		NfcAdapter nfc = NfcAdapter.getDefaultAdapter(this);
    	}
    }

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.main, menu);
    	menu.findItem(R.id.tagProgram).setVisible(BuildConfig.DEBUG && USE_NFC_PROGRAM);
		return true;
	}
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    	case R.id.facebook:
    		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("fb://page/"+getString(R.string.fb_page_id)));
    		if(getPackageManager().resolveActivity(intent, 0) == null){
    			intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/"+getString(R.string.fb_page_id)));
    		}
    		startActivity(intent);
    		overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    		return true;
    	case R.id.tagProgram:
    		Intent nfcIntent = new Intent(this, NFCProgramActivity.class);
    		startActivity(nfcIntent);
    		return true;
    	default:
    		return super.onOptionsItemSelected(item);
    	}
    }

	private void startBusPositionTimer() {
    	refresher.stop = false;
    	if(!refresher.running){
    		handler.postDelayed(refresher, REFRESH_INTERVAL);
    	}
	}

    @Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelable("BusPosition", BusPosition.instance());
		outState.putParcelable("BusList", BusStopList.instance());
		outState.putInt("tab", getSupportActionBar().getSelectedNavigationIndex());
	}
    
    private void restoreBusPosition(Bundle savedInstanceState){
        if(savedInstanceState != null){
        	BusPosition busPositionParcel = (BusPosition) savedInstanceState.getParcelable("BusPosition");
        	busPositionParcel.setContext(this);
        	if(busPositionParcel != null){
            	Log.d("MainActivity", "BusPosition load state");
            	BusPosition.initialize(busPositionParcel);
            }
        }else{
        	BusPosition.context(this);
        }
        if(checkGooglePlayBool()){
        	BusPosition.refreshIfNoData();
        }
    }
    
    private void restoreBusList(Bundle savedInstanceState){
    	BusStopList busPositionParcel = null;
        if(savedInstanceState != null){
        	busPositionParcel = (BusStopList) savedInstanceState.getParcelable("BusList");
        	if(busPositionParcel != null){
            	Log.d("MainActivity", "BusList load state");
            	BusStopList.initialize(busPositionParcel);
            }
        }
        if(checkGooglePlayBool()){
        	BusStopList.refreshIfNoData();
        }
    }

    @SuppressWarnings("deprecation")
	@TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
	public void onPause(){
    	super.onPause();
    	refresher.stop = true;
    	
    	if(nfc != null && Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH){
    		nfc.disableForegroundNdefPush(this);
    	}
    }
    
    public void onResume(){
    	super.onResume();
    	startBusPositionTimer();
    	
    	if(nfc != null){
    		this.setNFC();
    	}
    }
    
    @Override
	protected void onDestroy() {
		super.onDestroy();
		if(USE_TRACE){
			Debug.stopMethodTracing();
		}
	}

	private boolean checkGooglePlayBool(){
    	return GooglePlayServicesUtil.isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS; 
    }
    
    private boolean checkGooglePlay(){
    	final int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
    	if (status != ConnectionResult.SUCCESS) {
    		if (GooglePlayServicesUtil.isUserRecoverableError(status)) {
    			runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						Dialog gpsError = GooglePlayServicesUtil.getErrorDialog(status, MainActivity.this, REQUEST_CODE_RECOVER_PLAY_SERVICES, new DialogInterface.OnCancelListener(){

							@Override
							public void onCancel(DialogInterface arg0) {
								// directly call finish here does not work
								handler.sendEmptyMessage(CLOSE);
							}
		    				
		    			});
		    			gpsError.show();
					}
				});
    			return false;
    		}else{
    			DialogFragment exit = new ExitMessageFragment();
    			Bundle args = new Bundle();
    			args.putString("message", getString(R.string.map_api_error));
    			exit.setArguments(args);
    			exit.show(getSupportFragmentManager(), "dialog");
    			return false;
    		}
    	}
        return true;
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	switch (requestCode) {
    	case REQUEST_CODE_RECOVER_PLAY_SERVICES:
    		if (resultCode == RESULT_CANCELED) {
    			Toast.makeText(this, getString(R.string.require_play),
    					Toast.LENGTH_SHORT).show();
    			finish();
    		}
    		return;
    	}
    	super.onActivityResult(requestCode, resultCode, data);
    }
    
    @Override
    public boolean handleMessage (Message msg){
    	Log.d("MainActivity", "Got message "+msg.toString());
    	if(msg.what == CLOSE){
    		finish();
    	}
    	return true;
    }
    
    // http://stackoverflow.com/questions/13418436/android-4-2-back-stack-behaviour-with-nested-fragments
    // https://code.google.com/p/android/issues/detail?id=40323
    @Override
    public void onBackPressed() {
    	Fragment content = getSupportFragmentManager().findFragmentById(android.R.id.content);
    	if(content != null){
    		if(content.getChildFragmentManager().getBackStackEntryCount() > 0){
    			content.getChildFragmentManager().popBackStack();
    			return;
    		}
    	}
    	super.onBackPressed();
    }
    
    private JSONObject showBusStopOnTab;

	public void showBusStopList(){
		ActionBar actionBar = getSupportActionBar();
		actionBar.selectTab(actionBar.getTabAt(0));
	}
	public void showBusStop(final int id){
		BusStopList.registerUpdateListener(new Listener(){

			@Override
			public void onFired() {
				BusStopList.removeUpdateListener(this);
				try{
					JSONObject item = BusStopList.data().getJSONObject("Stop").getJSONObject(String.valueOf(id));
					showBusStop(item);
				}catch(JSONException e){
					Toast.makeText(MainActivity.this, R.string.no_stop, Toast.LENGTH_LONG).show();
				}
			}
			
		}, true);
	}
	public void showBusStop(JSONObject item){
		showBusStopOnTab = null;
		if(currentFragment instanceof BusStopFragment){
			BusStopFragment busStop = (BusStopFragment) currentFragment;
			busStop.stopSelected(item);
		}else{
			showBusStopOnTab = item;
			showBusStopList();
		}
	}
	
	private NdefMessage lastNFCMessage;
	
	private void setNFC(){
		if(lastNFCMessage == null){
			return;
		}
		this.setNFC(lastNFCMessage);
	}
	
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	public void setNFC(NdefMessage message){
		lastNFCMessage = message;
		if(nfc == null){
    		return;
    	}else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH){
			// android beam
    		nfc.setNdefPushMessage(message, this);
		}
	}
	
	public void unsetNFC(){
		if(nfc != null){
			this.setNFC(NFCBuilder.createMessage(this, Uri.parse("kubus://home")));
		}
	}

	private class BusPositionRefresher implements Runnable {
    	public boolean stop = false;
    	public boolean running = false;
		public void run() {
			running = true;
    		if(!stop){
    			BusPosition.refresh();
    			handler.postDelayed(this, REFRESH_INTERVAL);
    		}else{
    			running = false;
    		}
    	}
    }
    
    private class TabHandler implements ActionBar.TabListener{
    	
    	private Class<? extends Fragment> page;
    	public Fragment fragment;
    	private String tag;
    	
    	public TabHandler(Class<? extends Fragment> page, String tag){
    		this.page = page;
    		this.tag = tag;
    		if(getSupportFragmentManager().findFragmentByTag(tag) != null){
    			this.fragment = getSupportFragmentManager().findFragmentByTag(tag);
    		}
    	}

		@Override
		public void onTabReselected(Tab arg0, FragmentTransaction arg1) {
		}

		@Override
		public void onTabSelected(Tab tab, FragmentTransaction ft) {
			ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
			if(fragment == null){
				fragment = Fragment.instantiate(MainActivity.this, page.getName());
				ft.add(android.R.id.content, fragment, tag);
			}else{
				ft.attach(fragment);
			}
			if(tag.equals("BusStop") && showBusStopOnTab != null){
				BusStopFragment frg = (BusStopFragment) fragment;
				frg.setShowStop(showBusStopOnTab);
				showBusStopOnTab = null;
			}
			currentFragment = fragment;
		}

		@Override
		public void onTabUnselected(Tab tab, FragmentTransaction ft) {
			if(fragment != null){
				ft.detach(fragment);
			}
		}
    	
    }

}
