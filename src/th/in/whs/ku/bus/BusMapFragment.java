package th.in.whs.ku.bus;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import th.in.whs.ku.bus.BusStopListFragment.Sort;
import th.in.whs.ku.bus.api.BusPosition;
import th.in.whs.ku.bus.api.BusStopList;
import th.in.whs.ku.bus.util.ListenerList;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;

public class BusMapFragment extends Fragment implements OnItemSelectedListener {
	
	private MapView mapView;
	private GoogleMap map;
	private BusMapController mapController;
	private int listenerId = -1;
	private BusStopAdapter adapter;
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
		return inflater.inflate(R.layout.map_fragment, container, false);
    }
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState){
		super.onActivityCreated(savedInstanceState);
		
		setHasOptionsMenu(true);
		
		Spinner linePicker = (Spinner) getView().findViewById(R.id.linePicker);
		adapter = new BusStopAdapter(getActivity());
		linePicker.setOnItemSelectedListener(this);
		linePicker.setAdapter(adapter);
		
		mapView = (MapView) getView().findViewById(R.id.map);
		mapView.onCreate(savedInstanceState);
		map = mapView.getMap();
		if(map == null){
			DialogFragment exit = new ExitMessageFragment();
			Bundle args = new Bundle();
			args.putString("message", getString(R.string.map_api_error));
			exit.setArguments(args);
			exit.show(getFragmentManager(), "dialog");
			return;
		}
		map.setMyLocationEnabled(true);
		mapController = new BusMapController(getActivity(), map);
		
		mapController.registerListener();
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.refresh, menu);
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
	
	@Override
	public void onDestroy(){
		super.onDestroy();
		if(mapView != null){
			mapView.onDestroy();
		}
		unloadEvents();
	}
	
	@Override
	public void onLowMemory(){
		super.onLowMemory();
		if(mapView != null){
			mapView.onLowMemory();
		}
		mapController.onLowMemory();
	}
	
	@Override
	public void onPause(){
		super.onPause();
		if(mapView != null){
			mapView.onPause();
			mapController.onPause();
		}
		BusPosition.removeUpdateListener(listenerId);
	}
	
	@Override
	public void onResume(){
		super.onResume();
		if(mapView != null){
			mapView.onResume();
			mapController.onResume();
		}
		listenerId = BusPosition.registerUpdateListener(new BusPositionUpdatedListener(), true);
	}

	@Override
	public void onSaveInstanceState(Bundle outState){
		super.onSaveInstanceState(outState);
		if(mapView != null){
			mapView.onSaveInstanceState(outState);
		}
	}
	
	public void refresh() {
		getView().findViewById(R.id.progress).setVisibility(View.VISIBLE);
		BusPosition.refresh();
	}

	private void unloadEvents(){
		if(listenerId != -1){
			BusPosition.removeUpdateListener(listenerId);
		}
		if(mapController != null){
			mapController.unregisterListener();
		}
	}

	private final class BusPositionUpdatedListener extends ListenerList.Listener {
		@Override
		public void onFired() {
			try{
				getView().findViewById(R.id.progress).setVisibility(View.GONE);
			}catch(NullPointerException e){}
			adapter.update();
		}
	}
	
	private class BusStopAdapter extends ArrayAdapter<Line>{

		public BusStopAdapter(Context context) {
			super(context, android.R.layout.simple_spinner_item);
			setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		}
		
		public void update(){
			this.clear();
			JSONObject data = BusStopList.data();
			if(data == null){
				return;
			}
			try {
				JSONArray line = data.getJSONArray("Line");
				for(int i=0; i<line.length(); i++){
					String item = line.getString(i);
					this.add(new Line(i, item));
				}
			} catch (JSONException e) {
				Toast.makeText(getContext(), getString(R.string.json_error), Toast.LENGTH_LONG).show();
			}
			this.notifyDataSetChanged();
		}

		@Override
		public View getDropDownView(int position, View convertView,
				ViewGroup parent) {
			View view = super.getDropDownView(position, convertView, parent);
			setColor(view, position, true);
			return view;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = super.getView(position, convertView, parent);
			setColor(view, position, false);
			return view;
		}
		
		private void setColor(View view, int position, boolean colorBg){
			TextView text = (TextView) view.findViewById(android.R.id.text1);
			
			Line line = this.getItem(position);
			int colorRes = Color.TRANSPARENT;
			if(position != 0){
				colorRes = getResources().getColor(BusMapController.getColor(line.index));
			}
			if(colorBg){
				int bgColor = colorRes, txtColor = Color.BLACK;
				view.setBackgroundColor(bgColor);
				
				if(position != 0){
					txtColor = Color.WHITE;
				}
				text.setTextColor(txtColor);
			}else{
				if(position == 0){
					colorRes = Color.BLACK;
				}
				text.setTextColor(colorRes);
				view.setBackgroundColor(Color.TRANSPARENT);
			}
		}
		
	}
	
	private class Line {
		public int index;
		public String name;
		
		public Line(int index, String name) {
			this.index = index;
			this.name = name;
		}

		public boolean isAll(){
			return index == 0;
		}
		
		public String toString(){
			if(isAll()){
				return getString(R.string.all_line);
			}
			return name;
		}
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int pos,
			long id) {
		if(!isAdded()){
			return;
		}
		Line line = adapter.getItem(pos);
		if(line.isAll()){
			mapController.clearFilter();
			mapController.clearPolyline();
			mapController.setFilterActive(false);
			return;
		}
		mapController.drawPolyline(line.index);
		mapController.clearFilter();
		mapController.addFilter(new BusMapController.Filter(BusMapController.Filter.FilterType.LINE, line.index));
		mapController.setFilterActive(true);
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {
		mapController.clearFilter();
		mapController.clearPolyline();
	}
}
