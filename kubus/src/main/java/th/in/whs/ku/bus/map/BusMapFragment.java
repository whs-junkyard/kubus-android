package th.in.whs.ku.bus.map;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.ActivityManagerCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import th.in.whs.ku.bus.MainActivity;
import th.in.whs.ku.bus.R;
import th.in.whs.ku.bus.ReportActivity;
import th.in.whs.ku.bus.api.Bus;
import th.in.whs.ku.bus.api.BusPosition;
import th.in.whs.ku.bus.api.BusStopList;
import th.in.whs.ku.bus.util.BusColor;
import th.in.whs.ku.bus.util.ListenerList;
import th.in.whs.ku.bus.util.NearestLineToPoint;

public class BusMapFragment extends Fragment implements OnInfoWindowClickListener, OnMarkerClickListener, OnMapClickListener {
	
	private static final float OUT_OF_SERVICE_ALPHA = 0.6f;
	private static final int VIEW_ID = 165189189;
	private SupportMapFragment fragment;

	public void getMapAsync(OnMapReadyCallback callback){
		fragment.getMapAsync(callback);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		FrameLayout view = new FrameLayout(inflater.getContext());
		view.setId(VIEW_ID);
		FragmentTransaction tx = getChildFragmentManager().beginTransaction();
		GoogleMapOptions options = (GoogleMapOptions) getArguments().getParcelable("option");
		fragment = SupportMapFragment.newInstance(options);
		tx.replace(VIEW_ID, fragment, "map");
		tx.commitAllowingStateLoss();
		
		loadArguments();
		
		return view;
	}

	@Override
	public void onStart() {
		super.onStart();
        fragment.getMapAsync(new OnMapReadyCallback() {
			@Override
			public void onMapReady(GoogleMap map) {
				Bundle args = getArguments();
				if (!args.getBoolean("noMyLocation")) {
					map.setMyLocationEnabled(true);
				}

				map.getUiSettings().setMapToolbarEnabled(false);
				map.setOnInfoWindowClickListener(BusMapFragment.this);
				map.setOnMarkerClickListener(BusMapFragment.this);
				map.setOnMapClickListener(BusMapFragment.this);
				registerListener();
			}
		});
	}

	public void onResume(){
		super.onResume();
		getMapAsync(new OnMapReadyCallback() {
			@Override
			public void onMapReady(GoogleMap map) {
				map.clear();
			}
		});
		if(listenerId != -1){
			BusPosition.wsConnect();
		}
	}

	public void onLowMemory(){
		super.onLowMemory();
		BusIconCache.instance.evictAll();
		DirectionIconCache.instance.evictAll();
		StopIconCache.instance.evictAll();
	}

	/**
	 * Disconnect from busposition service
	 */
	public void onPause(){
		super.onPause();
        markers.clear();
		BusPosition.wsDisconnect();
	}

	@Override
	public void onStop() {
		super.onStop();
		unregisterListener();
	}
	
	private void loadArguments(){
		Bundle args = getArguments();
		if(args.containsKey("filter")){
			ArrayList<Parcelable> filters = args.getParcelableArrayList("filter");
			clearFilter();
			for(Parcelable filter : filters){
				addFilter((Filter) filter);
			}
		}
		if(args.containsKey("filterActive")){
			setFilterActive(args.getBoolean("filterActive"));
		}
		if(args.containsKey("allowBusStopClick")){
			setAllowBusStopClick(args.getBoolean("allowBusStopClick"));
		}
		if(args.containsKey("drawPolyline")){
			drawPolyline(args.getInt("drawPolyline"));
		}
		if(args.containsKey("drawPolylineRoute")){
			String[] route = args.getStringArray("drawPolylineRoute");
			drawPolyline(route[0], route[1], route[2]);
		}
	}
	
	//
	// network code
	//

	private int listenerId = -1;
	/**
	 * Connect to the bus position service and show bus in map
	 */
	private void registerListener(){
        if(listenerId != -1){
            return;
        }
		BusPosition.wsConnect();
		listenerId = BusPosition.registerUpdateListener(new ListenerList.Listener(){
	
			@Override
			public void onFired() {
				update(BusPosition.gets());
			}
			
		}, true);
	}

	/**
	 * Disconnect from the bus position service
	 */
	private void unregisterListener(){
		BusPosition.removeUpdateListener(listenerId);
		listenerId = -1;
		BusPosition.wsDisconnect();
	}

	/**
	 * Array of bus markers
	 * Key is bus id
	 */
	private SparseArray<BusMarker> markers = new SparseArray<BusMarker>();
	/**
	 * Redraw
	 * @param data Bus position data from BusPosition.gets()
	 */
	public void update(final SparseArray<Bus> data){
        fragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap map) {
                ArrayList<Integer> hasBus = new ArrayList<Integer>();
                for(int i=0; i < data.size(); i++){
                    Bus bus = data.valueAt(i);
                    if(useFilter && !isFiltered(bus)){
                        continue;
                    }
                    hasBus.add(bus.id);

                    BusMarker busMarker = markers.get(bus.id);
                    Marker marker;
                    boolean newlyCreated = false;

                    int lineid = bus.lineid;
                    if(lineid == 6){
                        lineid = 4;
                    }

                    if(busMarker == null || busMarker.marker == null){
                        LatLng latlng = new LatLng(bus.latitude, bus.longitude);
                        marker = map.addMarker(new MarkerOptions().position(latlng));
                        busMarker = new BusMarker(bus, marker);
                        markers.put(bus.id, busMarker);
                        newlyCreated = true;
                    }else{
                        marker = busMarker.marker;

                        if(!bus.isLocationEqual(marker.getPosition())){
                            LatLng latlng = new LatLng(bus.latitude, bus.longitude);
                            marker.setPosition(latlng);
                        }

                        if(busMarker.bus.equals(bus)){
                            continue;
                        }
                    }

                    if(lineid == 0 || bus.isinpark){
                        marker.setTitle(bus.name);
                        if(bus.isinpark){
                            marker.setSnippet(getString(R.string.bus_parking));
                        }else{
                            marker.setSnippet(getString(R.string.bus_no_line));
                        }
                    }else{
                        marker.setTitle(String.format(getString(R.string.bus_line), lineid));
                        marker.setSnippet(bus.name);
                    }

                    if(newlyCreated || busMarker.bus.lineid != lineid){
                        Log.d("BusMapController", "Requesting icon "+bus.lineid+" id "+bus.id);
                        BitmapDescriptor icon = BusIconCache.instance.getCache(bus.lineid);
                        marker.setIcon(icon);
                    }

                    if(!bus.isinline || bus.isinpark){
                        marker.setAlpha(OUT_OF_SERVICE_ALPHA);
                    }else{
                        marker.setAlpha(1f);
                    }

                    busMarker.bus = bus;
                }
                ArrayList<Integer> remove = new ArrayList<Integer>();
                int size = markers.size();
                for(int i=0; i<size; i++){
                    int key = markers.keyAt(i);
                    if(!hasBus.contains(key)){
                        markers.valueAt(i).marker.remove();
                        remove.add(markers.keyAt(i));
                    }
                }
                // remove after otherwise .keyAt() will shift
                for(int removeKey : remove){
                    markers.remove(removeKey);
                }
            }
        });
	}

	public Marker get(int id){
		BusMarker out = markers.get(id);
		if(out == null){
			return null;
		}
		return out.marker;
	}

	private boolean useFilter = false;
	private ArrayList<Filter> filter = new ArrayList<Filter>();
	/**
	 * Is the filter enabled?
	 * @param set true to enable filter, false otherwise
	 * @see .addFilter
	 */
	public void setFilterActive(boolean set){
		this.useFilter = set;
		update(BusPosition.gets());
	}

	public boolean getFilterActive(){
		return this.useFilter;
	}

	/**
	 * Filter is used to show only information of interest 
	 * @param item
	 */
	public void addFilter(Filter item){
		if(filter.contains(item)){
			return;
		}
		filter.add(item);
		if(useFilter){
			update(BusPosition.gets());
		}
	}

	public boolean removeFilter(Filter item){
		boolean out = filter.remove(item);
		if(out && useFilter){
			update(BusPosition.gets());
		}
		return out;
	}

	public void clearFilter(){
		boolean update = filter.size() > 0;
		filter.clear();
		if(update && useFilter){
			update(BusPosition.gets());
		}
	}

	/**
	 * Array of bus stop markers
	 */
	private ArrayList<StopMarker> stopMarker = new ArrayList<StopMarker>();
	private ArrayList<Marker> directionMarker = new ArrayList<Marker>();
	private Polyline polyline;
	private String currentPolyline = "0";
	private enum PolylineRequest {
		NO, // no polyline is visible
		USER, // the polyline is draw by drawPolyline
		BUS // the polyline is drawn by tapping on bus marker
	};
	/**
	 * How the polyline become visible
	 * 0: no (no call yet)
	 * 1: yes (direct call to drawPolyline)
	 * 2: no, but drawn by marker tap (onMarkerClick)
	 */
	private PolylineRequest polylineRequestedByUser = PolylineRequest.NO;
	/**
	 * Checker for filter
	 */
	private boolean isFiltered(Bus bus) {
		for(Filter f : filter){
			if(f.type == Filter.FilterType.BUS && bus.id == f.value){
				return true;
			}else if(f.type == Filter.FilterType.LINE && bus.lineid == f.value){
				return true;
			}else if(f.type == Filter.FilterType.LINE && f.value == 4 && bus.lineid == 6){
				return true;
			}
		}
		return false;
	}

	public void drawPolyline(int lineId){
		drawPolyline(String.valueOf(lineId));
	}
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public void drawPolyline(final String lineId, final String from, final String to){
		polylineRequestedByUser = PolylineRequest.USER;
		
		JSONObject data = BusStopList.data();
		if(data == null){
			return;
		}
		try {
			JSONObject stopList = data.getJSONObject("Stop");
			JSONObject fromStop = stopList.getJSONObject(from);
			JSONObject toStop = stopList.getJSONObject(to);
			JSONArray line = data.getJSONObject("Polyline").getJSONArray(lineId);
			
			Double[][] inputFrom = new Double[line.length() + 1][2];
			Double[][] inputTo = new Double[line.length() + 1][2];
			inputFrom[0] = new Double[]{
					fromStop.getDouble("Latitude"),
					fromStop.getDouble("Longitude")
			};
			inputTo[0] = new Double[]{
					toStop.getDouble("Latitude"),
					toStop.getDouble("Longitude")
			};
			for(int i=0; i<line.length(); i++){
				String[] splitted = line.getString(i).split(",");
				for(int j=0; j<splitted.length; j++){
					inputFrom[i+1][j] = Double.valueOf(splitted[j]);
					inputTo[i+1][j] = Double.valueOf(splitted[j]);
				}
			}
			
			final Double[][][] results = new Double[][][]{null, null};
			
			class LocalNearestLineToPoint extends NearestLineToPoint {
				
				private int index;
				
				public LocalNearestLineToPoint(int index){
					super();
					this.index = index;
				}

				@Override
				protected void onPostExecute(Double[][] result) {
					results[index] = result;
					if(results[0] != null && results[1] != null){
						_drawPolyline(lineId, from, to, results[0], results[1]);
					}
				}
				
			}
			
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
				new LocalNearestLineToPoint(0)
					.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, inputFrom);
				new LocalNearestLineToPoint(1)
					.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, inputTo);
			}else{
				new LocalNearestLineToPoint(0)
					.execute(inputFrom);
				new LocalNearestLineToPoint(1)
					.execute(inputTo);
			}
		} catch (JSONException e) {
			return;
		}
	}
	
	public void drawPolyline(String lineId){
		polylineRequestedByUser = PolylineRequest.USER;
		_drawPolyline(lineId);
	}
	
	private void _drawPolyline(String lineId){
		_drawPolyline(lineId, null, null, null, null);
	}
	
	/**
	 * Draw a polyline and remove other polylines
	 * @param lineId
	 * @param fromStop Stop ID
	 * @param toStop Stop ID
	 * @param polylineSpliceFrom Polyline close to "from" as returned from `NearestLineToPoint`. The last point must be an existing polyline point.
	 * @param polylineSpliceTo Polyline close to "to" as returned from `NearestLineToPoint`
	 */
	private void _drawPolyline(final String lineId, final String fromStop, final String toStop, final Double[][] polylineSpliceFrom, final Double[][] polylineSpliceTo){
        fragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap map) {
                if(lineId.equals(currentPolyline)){
                    return;
                }
                currentPolyline = lineId;
                _clearPolyline();

                // somehow inlining this check still make race condition
                // http://sentry.whs.in.th/kusmartbus/android/group/107/
                JSONObject listRoot = BusStopList.data();
                if(listRoot == null){
                    return;
                }

                int color = getResources().getColor(BusColor.getColor(Integer.valueOf(lineId)));

                boolean foundStart = fromStop == null;
                boolean foundStop = toStop == null;

                // Draw stop marker
                try{
                    JSONObject data = listRoot.getJSONObject("StopOrder");
                    // or maybe /group/107/ could be from this line
                    if(data == null){
                        return;
                    }
                    JSONArray line = data.getJSONArray(lineId);
                    JSONObject stopData = listRoot.getJSONObject("Stop");
                    BitmapDescriptor icon = StopIconCache.instance.getCache(lineId);

                    stopMarker.ensureCapacity(line.length());

                    for(int loop=0; loop<2; loop++){
                        for(int i=0; i<line.length(); i++){
                            JSONObject stop = stopData.getJSONObject(line.getString(i));

                            if(!foundStart){
                                if(stop.getString("ID").equals(fromStop)){
                                    foundStart = true;
                                }else{
                                    continue;
                                }
                            }

                            MarkerOptions marker = new MarkerOptions();
                            marker.position(
                                    new LatLng(
                                            stop.getDouble("Latitude"),
                                            stop.getDouble("Longitude")
                                    )
                            ).title(stop.getString("Name"))
                                    .anchor(0.5f, 0.5f)
                                    .icon(icon);

                            stopMarker.add(new StopMarker(map.addMarker(marker), stop));

                            if(toStop != null && stop.getString("ID").equals(toStop)){
                                foundStop = true;
                                break;
                            }
                        }
                        // we traced the entire line and
                        // still doesn't find the start stop
                        if(!foundStart){
                            return;
                        }
                        // if we want to draw single loop
                        // then don't wrap around
                        if(foundStop){
                            break;
                        }
                    }
                }catch(JSONException e){
                }
                // Draw lines
                foundStart = false;
                foundStop = false;
                try {
                    JSONObject data = listRoot.getJSONObject("Polyline");
                    if(data == null){
                        return;
                    }
                    JSONArray line = data.getJSONArray(lineId);
                    if(line == null){
                        return;
                    }

                    double[] initialItem = toDouble(line.getString(0).split(","));
                    float lastBearing = Float.MIN_VALUE;
                    LatLng lastItem = new LatLng(initialItem[0], initialItem[1]);
                    PolylineOptions pol = new PolylineOptions().width(5).color(color);

                    BitmapDescriptor directionIcon = null;
                    // seems that drawing direction arrow is too much for low ram devices
                    if(!ActivityManagerCompat.isLowRamDevice((ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE))){
                        directionIcon = DirectionIconCache.instance.getCache(lineId);
                    }

                    for(int loop=0; loop<2; loop++){
                        // XXX: i don't know why 1 is here
                        // if i put 0 routing from ngam1 to sci fac will break
                        // if i put 1 mapfragment will break
                        // i could use an if, but i think i should get better understanding
                        // of the situation first
                        for(int i=1; i<line.length(); i++){
                            String[] rawItem = line.getString(i).split(",");
                            double[] item = toDouble(rawItem);
                            LatLng latlng = new LatLng(item[0], item[1]);

                            if(polylineSpliceFrom != null){
                                if(polylineSpliceFrom[1][0].equals(item[0]) && polylineSpliceFrom[1][1].equals(item[1])){
                                    foundStart = true;
                                    lastItem = new LatLng(
                                            polylineSpliceFrom[0][0],
                                            polylineSpliceFrom[0][1]
                                    );
                                    pol.add(lastItem);
                                }
                                if(!foundStart){
                                    continue;
                                }
                            }

                            if(foundStart && polylineSpliceTo != null){
                                if(polylineSpliceTo[1][0].equals(item[0]) && polylineSpliceTo[1][1].equals(item[1])){
                                    pol.add(new LatLng(
                                            polylineSpliceTo[0][0],
                                            polylineSpliceTo[0][1]
                                    ));
                                    foundStop = true;
                                    break;
                                }
                            }

                            if(item[0] == 13.85004 && item[1] == 100.572433){
                                // https://code.google.com/p/gmaps-api-issues/issues/detail?id=5313
                                latlng = new LatLng(latlng.latitude + 0.000001, latlng.longitude);
                            }

                            pol.add(latlng);

                            if(directionIcon != null){
                                // we don't use distance, only bearing
                                float[] distance = new float[2];
                                Location.distanceBetween(lastItem.latitude, lastItem.longitude, latlng.latitude, latlng.longitude, distance);
                                float bearing = distance[1] - 90;

                                if(Math.abs(bearing - lastBearing) > 20){
                                    MarkerOptions marker = new MarkerOptions();
                                    marker.position(lastItem).anchor(0.5f, 0.5f)
                                            .flat(true)
                                            .rotation(bearing)
                                            .icon(directionIcon);
                                    directionMarker.add(map.addMarker(marker));
                                }

                                lastBearing = bearing;
                            }

                            lastItem = latlng;
                        }
                        if(polylineSpliceFrom == null || foundStop){
                            break;
                        }else if(polylineSpliceFrom != null && !foundStart){
                            Log.d("BusMapController", "Can't find from stop in polyline.");
                            break;
                        }
                    }

                    polyline = map.addPolyline(pol);
                } catch (JSONException e) {
                }
            }
        });
	}
	
	/**
	 * Hide all polylines
	 */
	public void clearPolyline(){
		polylineRequestedByUser = PolylineRequest.NO;
		currentPolyline = "0";
		_clearPolyline();
	}

	private void _clearPolyline(){
		if(polyline == null){
			return;
		}
		polyline.remove();
		for(StopMarker mark : stopMarker){
			mark.marker.remove();
		}
		stopMarker.clear();
		for(Marker mark : directionMarker){
			mark.remove();
		}
		directionMarker.clear();
	}

	/**
	 * Convert array of string of float number to array of float
	 * @param str String of float numbers
	 * @return Float array
	 */
	private double[] toDouble(String[] str){
		double[] out = new double[str.length];
		for (int i = 0; i < str.length; i++) {
			out[i] = Double.valueOf(str[i]);
		}
		return out;
	}

	private boolean allowBusStopClick = true;

	public boolean isAllowBusStopClick() {
		return allowBusStopClick;
	}

	/**
	 * Whether tapping on bus stop infobox will show that bus stop's page
	 * Default is true
	 * @param allowBusStopClick
	 */
	public void setAllowBusStopClick(boolean allowBusStopClick) {
		this.allowBusStopClick = allowBusStopClick;
	}

	@Override
	public void onInfoWindowClick(Marker marker) {
		Activity activity = getActivity();
		if(!allowBusStopClick || activity == null){
			return;
		}
		for(StopMarker mark : stopMarker){
			if(mark.marker.equals(marker)){
				try {
					// try opening by fragment first
					if(activity instanceof MainActivity){
						MainActivity main = (MainActivity) activity;
						Log.d("BusMapController", mark.stop.toString());
						main.showBusStop(mark.stop);
					}else{
						Intent intent = new Intent(activity, MainActivity.class);
						intent.putExtra(MainActivity.OPEN_STOP, mark.stop.getInt("ID"));
						activity.startActivity(intent);
					}
				} catch (JSONException e) {
				}
				return;
			}
		}
		for(int i = 0; i < markers.size(); i++){
			BusMarker mark = markers.valueAt(i);
			if(mark.marker.equals(marker)){
				Intent reportIntent = new Intent(activity, ReportActivity.class);
				reportIntent.putExtra(ReportActivity.REPORT_BUS, mark.bus.name);
				activity.startActivity(reportIntent);
				return;
			}
		}
	}

	@Override
	public boolean onMarkerClick(Marker marker) {
		if(polylineRequestedByUser == PolylineRequest.USER){
			return false;
		}
		for(int i=0; i<markers.size(); i++){
			if(markers.valueAt(i).marker.equals(marker)){
				int busid = markers.keyAt(i);
				Bus bus = BusPosition.get(busid);
				polylineRequestedByUser = PolylineRequest.BUS;
				String lineid = String.valueOf(bus.lineid);
				if(lineid.equals("6")){
					lineid = "4";
				}
				_drawPolyline(lineid);
				break;
			}
		}
		// show info box
		return false;
	}

	@Override
	public void onMapClick(LatLng point) {
		if(polylineRequestedByUser == PolylineRequest.BUS){
			clearPolyline();
		}
	}

	private class BusMarker{
		public Bus bus;
		public Marker marker;
		
		public BusMarker(Bus bus, Marker marker) {
			this.bus = bus;
			this.marker = marker;
		}
	}
	
	private class StopMarker{
		public Marker marker;
		public JSONObject stop;
		public StopMarker(Marker marker, JSONObject stop) {
			if(marker == null){
				throw new IllegalArgumentException("marker is null");
			}
			this.marker = marker;
			this.stop = stop;
		}
	}
	
}
