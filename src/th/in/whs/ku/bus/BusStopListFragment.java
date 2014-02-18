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
import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class BusStopListFragment extends ListFragment {
	
	public static enum Sort {
		NAME,
		DISTANCE
	};
	
	protected ArrayList<BusStopJSONObject> list = new ArrayList<BusStopJSONObject>();
	protected BaseAdapter adapter;
	private int listenerId = -1;
	private int errorListenerId = -1;
	private int progressListenerId = -1;
	private LocationManager locman;
	private LayoutInflater inflater;
	private SensorManager sensors;
	private float compass = 0;
	private CompassHandler compassHandler = new CompassHandler();
	private final LocationHandler locationHandler = new LocationHandler();
	private boolean returnClosest = false;
	private Sort sort = Sort.DISTANCE;
	

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
		if(returnClosest){
			if(BusStopList.data() == null){
				stopSelected(0);
			}
		}
		
		locman = (LocationManager) this.getActivity().getSystemService(Context.LOCATION_SERVICE);
		inflater = (LayoutInflater) this.getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		sensors = (SensorManager) this.getActivity().getSystemService(Context.SENSOR_SERVICE);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		MenuInflater menuInflater = getActivity().getMenuInflater();
		menuInflater.inflate(R.menu.refresh, menu);
		menuInflater.inflate(R.menu.busstoplist, menu);
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

	private void sort(){
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
		if(returnClosest){
			stopSelected(0);
		}
		adapter.notifyDataSetChanged();
	}
	
	public void refresh(){
		BusStopList.refresh();
		setListShown(false);
	}
	
	public void onResume(){
		super.onResume();
		/*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && sensors.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null && sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null){
			sensors.registerListener(
					compasshandler,
					sensors.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
					SensorManager.SENSOR_DELAY_NORMAL
			);
			sensors.registerListener(
					compasshandler,
					sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
					SensorManager.SENSOR_DELAY_NORMAL
			);
		}*/
		locman.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 500L, 0.01f, locationHandler);
		if(listenerId == -1){
			listenerId = BusStopList.registerUpdateListener(new BusListListener(), true);
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
		//sensors.unregisterListener(compasshandler);
		locman.removeUpdates(locationHandler);
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
	
	public void onListItemClick(ListView l, View v, int position, long id){		
		stopSelected(position);
	}
	
	private void stopSelected(int index){
		StopSelectedInterface parent = (StopSelectedInterface) getParentFragment();
		if(parent == null){
			parent = (StopSelectedInterface) getActivity();
		}
		if(list.size() <= index){
			parent.stopSelected(null);
		}else{
			parent.stopSelected(list.get(index));
		}
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

	private class LocationHandler implements LocationListener {
		@Override
		public void onProviderDisabled(String arg0) {}

		@Override
		public void onProviderEnabled(String arg0) {}

		@Override
		public void onStatusChanged(String arg0, int arg1, Bundle arg2) {}

		@Override
		public void onLocationChanged(Location location) {
			for(BusStopJSONObject stop : list){
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
				stop.bearing = (result[1]+360)%360;
			}
			sort();
		}
	}

	private static class BusStopJSONObject extends JSONObject implements Parcelable{
		public float distance = -1f;
		/**
		 * Bearing from GPS
		 */
		public float bearing = -1f;
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
	}
	
	private class BusStopAdapter extends ArrayAdapter<BusStopJSONObject>{

		public BusStopAdapter(Context context,
				List<BusStopJSONObject> objects) {
			super(context, R.layout.busstop_row, objects);
		}
		
		@TargetApi(11)
		public View getView(int position, View convertView, ViewGroup parent){
			View vi=convertView;
			if(convertView==null){
				vi = inflater.inflate(R.layout.busstop_row, null);
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
//			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
//				if(data.bearing == -1f){
//					vi.findViewById(R.id.compass).setVisibility(View.GONE);
//				}else{
//					ImageView compassView = (ImageView) vi.findViewById(R.id.compass);
//					compassView.setVisibility(View.VISIBLE);
//					compassView.setRotation(data.bearing - compass);
//				}
//			}else{
				vi.findViewById(R.id.compass).setVisibility(View.GONE);
//			}
			return vi;
		}
	
	}
	
	private class CompassHandler implements SensorEventListener {

		private float[] accel;
		private float[] magnetic;
		
		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
		}

		@Override
		public void onSensorChanged(SensorEvent event) {
			if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
				accel = event.values;
			}else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD){
		        magnetic = event.values;
			}
		    if (accel != null && magnetic != null) {
		        float R[] = new float[9];
		        float I[] = new float[9];
		        if(SensorManager.getRotationMatrix(R, I, accel, magnetic)){
		            float orientation[] = new float[3];
		            SensorManager.getOrientation(R, orientation);
		            float azimuth = orientation[0];
		            float azimuthDeg = ((float)Math.toDegrees(azimuth)+360)%360;
		            compass = azimuthDeg;
		            adapter.notifyDataSetChanged();
		        }
		    }
		}
		
	}
	
	private class BusListListener extends ListenerList.Listener {
		@Override
		public void onFired() {
			JSONObject data = BusStopList.data();
        	boolean displayedError = false;
        	setListShown(true);
        	
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
        	Location location = locman.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
        	while(keys.hasNext()){
            	try{
            		BusStopJSONObject tag = new BusStopJSONObject(stopData.getJSONObject(keys.next()));
            		if(location != null){
	            		float[] result = new float[2];
						Location.distanceBetween(
								location.getLatitude(),
								location.getLongitude(),
								tag.getDouble("Latitude"),
								tag.getDouble("Longitude"),
								result
						);
						tag.distance = result[0];
						tag.bearing = (result[1]+360)%360;
            		}
            		list.add(tag);
            	}catch(JSONException e){
            		if(!displayedError){
            			toastError(R.string.json_error);
            			displayedError = true;
            		}
            		Log.e("BusStopList", "Exception", e);
            	}
            }
            
            sort();
		}
		private Toast toastError(int messageId){
			if(getActivity() == null){
				return null;
			}
			Toast toast = Toast.makeText(getActivity(), messageId, Toast.LENGTH_LONG);
			toast.show();
			return toast;
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
}
