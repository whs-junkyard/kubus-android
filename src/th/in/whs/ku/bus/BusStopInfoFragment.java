package th.in.whs.ku.bus;

import java.util.Collections;
import java.util.List;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import th.in.whs.ku.bus.api.API;
import th.in.whs.ku.bus.api.Bus;
import th.in.whs.ku.bus.api.BusStatus;
import th.in.whs.ku.bus.api.BusStopList;
import th.in.whs.ku.bus.api.TimeAgo;
import th.in.whs.ku.bus.widget.RoutePassingFormatter;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.NinePatchDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

public class BusStopInfoFragment extends Fragment {
	
	/**
	 * Duration which autorefresh() will be fired
	 */
	private static final int AUTOREFRESH_MS = 10000;
	/**
	 * Duration which startTimeupdate() will be fired
	 */
	private static final int TIMER_UPDATE_MS = 500;
	
	private JSONObject stopData;
	private JSONArray stopBus;
	private MapView mapView;
	private BusMapController mapController;
	private ListView list;
	private BusStopAdapter adapter;
	private LayoutInflater inflater=null;
	private Handler handler = new Handler();
	private Autorefresher autorefresh = new Autorefresher();
	private View headerView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// update the page title
		Bundle args = this.getArguments();
		try {
			stopData = new JSONObject(args.getString("data"));
		} catch (JSONException e) {
		}
	}

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
		this.inflater = inflater;
		View view = inflater.inflate(R.layout.stopinfo_fragment, container, false);
		
		list = (ListView) view.findViewById(R.id.busList);
		headerView = inflater.inflate(R.layout.busstop_head_row, null, false);
		list.addHeaderView(headerView);
		
		return view;
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		
		setHasOptionsMenu(true);
		
		try {
			// prepare the list
			adapter = new BusStopAdapter(getActivity());
			list.setAdapter(adapter);
			// on clicking on list item (a bus)
			list.setOnItemClickListener(new AdapterView.OnItemClickListener(){

				@Override
				public void onItemClick (AdapterView<?> parent, View view, int position, long id) {
					BusStatus item = (BusStatus) list.getItemAtPosition(position);
					if(item == null){
						return;
					}
					Marker mark = mapController.get(item.id);
					if(mark != null){
						// show marker's bubble window and move camera to marker position
						mark.showInfoWindow();
						LatLng latlng = mark.getPosition();
						mapView.getMap().animateCamera(CameraUpdateFactory.newLatLngZoom(
								new LatLng(latlng.latitude + 0.0004, latlng.longitude)
						, 17));
						mapController.clearPolyline();
						mapController.drawPolyline(item.lineid);
					}
				}
				
			});
			
			TextView title = (TextView) headerView.findViewById(R.id.title);
			
			// update the page title
			String titleStr = stopData.getString("Name");
			title.setText(titleStr);
			title.setSelected(true);
			
			TextView routePassing = (TextView) headerView.findViewById(R.id.linePassing);
			
			List<String> passingLines;
			try {
				passingLines = BusStopList.getPassingLine(stopData.getString("ID"), stopData.getString("ID"));
				routePassing.setText(RoutePassingFormatter.getRoutePassingFormatted(getActivity(), passingLines));
			} catch (JSONException e) {
				routePassing.setText(getString(R.string.line_passing) + getString(R.string.json_error));
			}
			
			mapView = (MapView) headerView.findViewById(R.id.map);
			mapView.onCreate(savedInstanceState);
			
			mapController = new BusMapController(getActivity(), mapView.getMap());
			mapController.setFilterActive(true);
			mapController.setAllowBusStopClick(false);
			mapController.registerListener();
			
			onMapCreated();
			
			boolean downloadBus = true;
			if(savedInstanceState != null){
				String data = savedInstanceState.getString("data");
				if(data != null){
					infoLoaded(new JSONArray(data));
					downloadBus = false;
				}
			}else if(stopBus != null){
				infoLoaded(stopBus);
				downloadBus = false;
			}
			if(downloadBus){
				// download bus list
				setShowProgress(true);
				loadInfo();
			}
		} catch (JSONException e) {
			Log.e("BusStopInfoFragment", "JSON Error", e);
			Toast.makeText(getActivity(), R.string.json_error, Toast.LENGTH_LONG).show();
			FragmentManager fragmentManager = getFragmentManager();
			fragmentManager.popBackStack();
			return;
		}
		
		startTimeupdate();
	}
	
	private void onMapCreated(){
		try {
			LatLng latlng = new LatLng(
					stopData.getDouble("Latitude"),
					stopData.getDouble("Longitude")
			);
		
			LatLng latlngView = new LatLng(
					latlng.latitude + 0.001,
					latlng.longitude
			);
			
			// move camera to bus stop
			mapView.getMap().moveCamera(CameraUpdateFactory.newLatLng(latlngView));
			mapView.getMap().setPadding(0, 0, 0, 90);
			// draw the bus stop marker
			MarkerOptions marker = new MarkerOptions();
			marker.position(latlng);
			marker.title(stopData.getString("Name"));
			Marker mark = mapView.getMap().addMarker(marker);
			mark.showInfoWindow();
		} catch (JSONException e) {
			return;
		}
	}

	/**
	 * Load bus stop information from POST map/getBusStatusData 
	 * @throws JSONException
	 */
	private void loadInfo() throws JSONException{
		RequestParams params = new RequestParams();
		params.put("busstationid", stopData.get("ID"));
		Log.d("BusStopInfoFragment", "Downloading stop info...");
		API.get(getActivity(), "map/getBusStatusData", params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers,
					JSONArray data) {
            	infoLoaded(data);
            }

			@Override
			public void onProgress(int bytesWritten, int totalSize) {
				if(getActivity() == null){
					return;
				}
				getActivity().setProgress((int) Math.ceil((bytesWritten/totalSize) * 10000));
			}

			@Override
			public void onFailure(int statusCode, Header[] headers,
					String responseString, Throwable throwable) {
				if(getActivity() == null){
					// http://sentry.whs.in.th/kusmartbus/android/group/112/
					// http://sentry.whs.in.th/kusmartbus/android/group/119/
					return;
				}
				Toast.makeText(getActivity(), R.string.internet_error, Toast.LENGTH_LONG).show();
				Log.d("BusStopInfoFragment", "Error", throwable);
			}
		});
	}
	
	private void infoLoaded(JSONArray data){
		Log.d("BusStopInfoFragment", "Data loaded");
		setShowProgress(false);
		this.stopBus = data;
    	adapter.clear();
    	
    	if(getView() != null){
	    	if(data.length() == 0){
	    		getView().findViewById(R.id.nobus).setVisibility(View.VISIBLE);
	    	}else{
	    		getView().findViewById(R.id.nobus).setVisibility(View.GONE);
	    	}
	    	autorefresh();
    	}else{
    		Log.w("BusStopInfo", "Data comes too late! View is gone!");
    		return;
    	}
    	
    	// add each bus to the list
    	for(int i=0; i<data.length(); i++){
    		BusStatus item;
			try {
				item = new BusStatus(data.getJSONObject(i));
			} catch (JSONException e) {
				Log.e("BusStopInfo", "Skipping JSON Error", e);
				continue;
			}
			if(item.getBus() != null && item.getBus().isinpark){
				continue;
			}
			mapController.addFilter(new BusMapController.Filter(BusMapController.Filter.FilterType.BUS, item.id));
    		adapter.add(item);
    	}
    	adapter.notifyDataSetChanged();
	}
	
	@Override
	public void onDestroy(){
		super.onDestroy();
		if(mapView != null){
			mapView.onDestroy();
		}
		if(mapController != null){
			mapController.unregisterListener();
		}
		API.cancel(getActivity());
	}
	
	@Override
	public void onLowMemory(){
		super.onLowMemory();
		if(mapView != null){
			mapView.onLowMemory();
		}
		if(mapController != null){
			mapController.onLowMemory();
		}
	}
	
	@Override
	public void onPause(){
		super.onPause();
		if(mapView != null){
			mapView.onPause();
		}
		if(mapController != null){
			mapController.onPause();
		}
		if(autorefresh != null){
			handler.removeCallbacks(autorefresh);
		}
	}
	
	@Override
	public void onResume(){
		super.onResume();
		if(mapView != null){
			mapView.onResume();
		}
		if(mapController != null){
			mapController.onResume();
		}
		if(this.stopBus != null){
			autorefresh();
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState){
		super.onSaveInstanceState(outState);
		if(mapView != null){
			mapView.onSaveInstanceState(outState);
		}
		if(stopBus != null){
			outState.putString("data", stopBus.toString());
		}
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		MenuInflater menuInflater = getActivity().getMenuInflater();
		menuInflater.inflate(R.menu.refresh, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()){
		case R.id.refresh:
			refresh();
			return true;
		}
		return false;
	}
	
	public void refresh() {
		adapter.clear();
		setShowProgress(true);
		try {
			loadInfo();
		} catch (JSONException e) {
		}
	}

	/**
	 * Timer to update duration until bus arrive in the view
	 */
	private void startTimeupdate(){
		handler.postDelayed(new Runnable(){

			@Override
			public void run() {
				if(!isVisible()){
					return;
				}
				adapter.notifyDataSetChanged();
				startTimeupdate();
			}
			
		}, TIMER_UPDATE_MS);
	}
	
	/**
	 * Reload data after a timeout
	 */
	private void autorefresh(){
		// prevent duplicated call
		handler.removeCallbacks(autorefresh);
		handler.postDelayed(autorefresh, AUTOREFRESH_MS);
	}
	
	/**
	 * Show or hide the progress circle
	 * @param show Show the progress circle or not
	 */
	private void setShowProgress(boolean show){
		if(getView() == null){
			return;
		}
		View progress = getView().findViewById(R.id.progress);
		if(progress == null){
			return;
		}
		if(show){
			progress.setVisibility(View.VISIBLE);
		}else{
			progress.setVisibility(View.GONE);
		}
	}
	
	private final class Autorefresher implements Runnable {
		@Override
		public void run() {
			if(isVisible()){
				return;
			}
			try {
				loadInfo();
			} catch (JSONException e) {
			}
		}
	}

	private class BusStopAdapter extends ArrayAdapter<BusStatus>{
		public BusStopAdapter(Context context) {
			super(context, R.layout.busline_row);
		}

		@SuppressWarnings("deprecation")
		@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
		public View getView(int position, View convertView, ViewGroup parent){
			View vi = convertView;
			if(convertView == null){
				vi = inflater.inflate(R.layout.busline_row, null);
			}
			BusStatus data = this.getItem(position);

			TextView busName = (TextView) vi.findViewById(R.id.busName);
			TextView busNo = (TextView) vi.findViewById(R.id.busNo);
			TextView lineNo = (TextView) vi.findViewById(R.id.lineNo);
			
			String timeString;
			try{
				TimeAgo time = data.estimated_time;
				int min = time.getMinuteLeft();
				if(min == 0){
					timeString = getString(R.string.lt_min);
				}else{
					timeString = String.valueOf(min);
				}
			}catch(Exception e){
				timeString = getString(R.string.unknown_time);
			}
			busNo.setText(timeString);
			
			Bus bus = data.getBus();
			if(bus != null){
				busName.setText(bus.name);
				
				NinePatchDrawable background = (NinePatchDrawable) getActivity().getResources().getDrawable(R.drawable.cards);
				int color = getResources().getColor(bus.getColor());
				background.setColorFilter(color, android.graphics.PorterDuff.Mode.MULTIPLY);
				View bgView = vi.findViewById(R.id.busLineRowBg);
				if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN){
					bgView.setBackground(background);
				}else{
					bgView.setBackgroundDrawable(background);
				}
			}else{
				busName.setText(data.name);
			}
			lineNo.setText(data.linename);
			
			return vi;
		}
	}
	
	
}
