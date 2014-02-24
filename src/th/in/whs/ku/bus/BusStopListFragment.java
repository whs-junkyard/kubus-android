package th.in.whs.ku.bus;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import th.in.whs.ku.bus.api.BusStopList;
import th.in.whs.ku.bus.api.ListenerList;
import th.in.whs.ku.bus.widget.FullTextSearchListAdapter;
import th.in.whs.ku.bus.widget.FullTextSearchListAdapter.SearchableItem;
import android.annotation.TargetApi;
import android.content.Context;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.ListFragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.OnCloseListener;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

public class BusStopListFragment extends ListFragment implements OnQueryTextListener, OnCloseListener, 
	LocationListener, GooglePlayServicesClient.ConnectionCallbacks
{
	
	public static enum Sort {
		NAME,
		DISTANCE
	};
	
	protected List<BusStopJSONObject> list = Collections.synchronizedList(new ArrayList<BusStopJSONObject>());
	protected ArrayAdapter<BusStopJSONObject> adapter;
	private int listenerId = -1;
	private int errorListenerId = -1;
	private int progressListenerId = -1;
	private LocationClient locClient;
	private boolean returnClosest = false;
	private Sort sort = Sort.DISTANCE;
	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if(savedInstanceState != null){
			this.sort = Sort.valueOf(savedInstanceState.getString("sort"));
		}
		locClient = new LocationClient(getActivity(), this, null);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		setHasOptionsMenu(true);
		
		adapter = new BusStopAdapter(getActivity(), list);
		this.setListAdapter(adapter);
		this.setEmptyText(getString(R.string.loading));
		setListShown(false);
		
		Bundle args = this.getArguments();
		if(args != null){
			returnClosest = args.getBoolean("returnClosest");
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		if(getActivity() == null){
			return;
		}
		
		MenuInflater menuInflater = getActivity().getMenuInflater();
		menuInflater.inflate(R.menu.search, menu);
		menuInflater.inflate(R.menu.refresh, menu);
		menuInflater.inflate(R.menu.busstoplist, menu);
		
		setSortItemName(menu.findItem(R.id.sort), this.sort);
		
		SearchView search = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.search));
		if(search != null){
			search.setOnQueryTextListener(this);
			search.setOnCloseListener(this);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()){
		case R.id.sort:
    		int index = (sort.ordinal() + 1) % Sort.values().length;
    		sort = Sort.values()[index];
    		setSortItemName(item, sort);
    		sort();
	    	
			return true;
		case R.id.refresh:
			refresh();
			return true;
		}
		return false;
	}

	@Override
	public void onStart(){
		super.onStart();
		locClient.connect();
	}
	
	public void onResume(){
		super.onResume();
		if(listenerId == -1){
			listenerId = BusStopList.registerUpdateListener(new BusListListener(), false);
		}
		if(errorListenerId == -1){
			errorListenerId = BusStopList.registerErrorListener(new BusErrorListener(), true);
		}
		if(progressListenerId == -1){
			progressListenerId = BusStopList.registerProgressListener(new BusProgressListener());
		}
	}
	
	public void onPause(){
		super.onPause();
		if(listenerId != -1){
			BusStopList.removeUpdateListener(listenerId);
			listenerId = -1;
		}
		if(errorListenerId != -1){
			BusStopList.removeErrorListener(errorListenerId);
			errorListenerId = -1;
		}
		if(progressListenerId != -1){
			BusStopList.removeProgressListener(progressListenerId);
			progressListenerId = -1;
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString("sort", this.sort.toString());
	}

	public void onListItemClick(ListView l, View v, int position, long id){
		synchronized (list) {
			stopSelected(position);
		}
	}
	
	private void stopSelected(int index){
		if(list.size() <= index){
			stopSelected(null);
		}else{
			stopSelected(adapter.getItem(index));
		}
	}
	
	private void stopSelected(BusStopJSONObject item){
		StopSelectedInterface parent = (StopSelectedInterface) getParentFragment();
		if(parent == null){
			parent = (StopSelectedInterface) getActivity();
		}
		parent.stopSelected(item);
	}
	
	public Sort getSort() {
		return sort;
	}

	public void setSort(Sort sort) {
		// XXX: This does not change the menu item.
		// Where to find it?
		this.sort = sort;
		sort();
	}
	
	private void setSortItemName(MenuItem item, Sort sort){
		int title;
		
		switch(sort){
		case DISTANCE:
			title = R.string.sort_distance;
			break;
		case NAME:
			title = R.string.sort_name;
			break;
		default:
			title = R.string.sort_distance;
		}
		
		item.setTitle(title);
	}

	private void sort(){
		new AsyncTask<Void, Void, Void>(){
	
			@Override
			protected Void doInBackground(Void... arg0) {
				sortSync();
				return null;
			}
	
			@Override
			protected void onPostExecute(Void result) {
				adapter.notifyDataSetChanged();
			}
			
			
		}.execute();
	}

	private void sortSync(){
		synchronized (list) {
			Collections.sort(list, new Comparator<BusStopJSONObject>(){
	
				@Override
				public int compare(BusStopJSONObject arg0, BusStopJSONObject arg1) {
					switch(sort){
					case DISTANCE:
						int out = (int) (arg0.distance - arg1.distance);
						if(out == 0){
							out = arg0.toString().compareTo(arg1.toString());
						}
						return out;
					case NAME:
						try {
							return arg0.getString("Name").compareTo(arg1.getString("Name"));
						} catch (JSONException e) {
							return 0;
						}
					default:
						return 0;
					}
				}
	        	
	        });
		}
	}

	public void refresh(){
		BusStopList.refresh();
		this.setEmptyText(getString(R.string.loading));
		setListShown(false);
	}
	
	private void processData(){
		JSONObject data = BusStopList.data();
    	boolean displayedError = false;
    	setListShown(true);
    	setEmptyText("");
    	
    	if(data == null){
    		setEmptyText(getString(R.string.internet_error));
    		toastError(R.string.internet_error);
    		return;
    	}
    	
    	JSONObject stopData;
		try {
			stopData = data.getJSONObject("Stop");
		} catch (JSONException e1) {
			toastError(R.string.json_error);
			displayedError = true;
			return;
		}
		
		Log.d("BusStopList", String.format("Found %d data", stopData.length()));
		
		list.clear();
    	
    	@SuppressWarnings("unchecked")
		Iterator<String> keys = stopData.keys();
    	Location location = locClient.getLastLocation();
    	while(keys.hasNext()){
        	try{
        		BusStopJSONObject tag = new BusStopJSONObject(stopData.getJSONObject(keys.next()));
        		updateLocationObject(location, tag);
        		list.add(tag);
        	}catch(JSONException e){
        		if(!displayedError){
        			toastError(R.string.json_error);
        			displayedError = true;
        		}
        		Log.e("BusStopList", "Exception", e);
        	}
        }
        
		if(!returnClosest){
			sort();
		}else{
			// if we don't do this synchronously
			// it will make the activity flash into view
			sortSync();
			stopSelected(list.get(0));
		}
	}

	private Toast toastError(int messageId){
		if(getActivity() == null){
			return null;
		}
		Toast toast = Toast.makeText(getActivity(), messageId, Toast.LENGTH_LONG);
		toast.show();
		return toast;
	}

	/**
	 * Google play location service on connected
	 */
	@Override
	public void onConnected(Bundle connectionHint) {
		LocationRequest req = LocationRequest.create();
		req.setPriority(LocationRequest.PRIORITY_LOW_POWER);
		req.setInterval(5000l);
		req.setFastestInterval(1000l);
		locClient.requestLocationUpdates(req, this);
		
		if(BusStopList.data() != null){
			processData();
		}
	}

	@Override
	public void onLocationChanged(Location location) {
		synchronized(list){
			for(BusStopJSONObject stop : list){
				updateLocationObject(location, stop);
			}
		}
		sort();
	}
	
	private void updateLocationObject(Location location, BusStopJSONObject stop){
		if(location == null){
			return;
		}
		
		float[] result = new float[2];
		try {
			Location.distanceBetween(
					location.getLatitude(),
					location.getLongitude(),
					stop.getDouble("Latitude"),
					stop.getDouble("Longitude"),
					result
			);
		} catch (JSONException e) {
			result[0] = -1f;
		}
		stop.distance = result[0];
	}

	private static class BusStopJSONObject extends JSONObject implements Parcelable, SearchableItem{
		public float distance = -1f;
		public BusStopJSONObject(JSONObject data) throws JSONException{
			super(data.toString());
		}
		public BusStopJSONObject(String data) throws JSONException{
			super(data);
		}
		public String formatDistance(Context context){
			if(distance == -1f){
				return "";
			}
			NumberFormat formatter = NumberFormat.getInstance();
			formatter.setMaximumFractionDigits(2);
			formatter.setMinimumFractionDigits(2);
			if(distance > 1000){
				return formatter.format(distance/1000) + context.getString(R.string.kilometer);
			}else{
				return formatter.format(distance) + context.getString(R.string.meter);
			}
		}
		@Override
		public int describeContents() {
			return 0;
		}
		@Override
		public void writeToParcel(Parcel out, int flags) {
			out.writeString(toString());
		}
		@SuppressWarnings("unused")
		public static final Parcelable.Creator<BusStopJSONObject> CREATOR
		= new Parcelable.Creator<BusStopJSONObject>() {
			public BusStopJSONObject createFromParcel(Parcel in) {
				try {
					return new BusStopJSONObject(in.readString());
				} catch (JSONException e) {
					return null;
				}
			}

			public BusStopJSONObject[] newArray(int size) {
				return new BusStopJSONObject[size];
			}
		};
		@Override
		public String getSearchString() {
			try {
				return this.getString("Name");
			} catch (JSONException e) {
				return "";
			}
		}
	}
	
	private class BusStopAdapter extends FullTextSearchListAdapter<BusStopJSONObject>{

		public BusStopAdapter(Context context,
				List<BusStopJSONObject> objects) {
			super(context, R.layout.busstop_row, objects);
		}

		@TargetApi(11)
		public View getView(int position, View convertView, ViewGroup parent){
			View vi=convertView;
			if(convertView==null){
				vi = getActivity().getLayoutInflater().inflate(R.layout.busstop_row, null);
			}
			BusStopJSONObject data = this.getItem(position);
			TextView title = (TextView) vi.findViewById(R.id.title);
			TextView distance = (TextView) vi.findViewById(R.id.distance);
			try {
				title.setText(data.getString("Name"));
				distance.setText(data.formatDistance(getActivity()));
			} catch (JSONException e) {
				title.setText(R.string.json_error);
				distance.setText("");
			}
			return vi;
		}
	
	}
	
	private class BusListListener extends ListenerList.Listener {
		@Override
		public void onFired() {
			if(!locClient.isConnected()){
				return;
			}
			processData();
		}
	}
	private class BusProgressListener extends ListenerList.Listener {

		@Override
		public void onFired(Bundle data) {
			getActivity().setProgress((int) Math.ceil(data.getFloat("progress") * 10000));
		}

		@Override
		public void onFired() {	
		}
	
	}
	
	private class BusErrorListener extends ListenerList.Listener {

		@Override
		public void onFired() {	
			setListShown(true);
			setEmptyText(getString(R.string.internet_error));
		}
	
	}

	@Override
	public boolean onQueryTextChange(String query) {
		adapter.getFilter().filter(query);
		return true;
	}

	@Override
	public boolean onQueryTextSubmit(String query) {
		return false;
	}

	/**
	 * On search menu close
	 */
	@Override
	public boolean onClose() {
		adapter.getFilter().filter("");
		return true;
	}

	// unused from Google Play Location provider
	@Override
	public void onDisconnected() {
	}
}
