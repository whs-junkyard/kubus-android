package th.in.whs.ku.bus;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import th.in.whs.ku.bus.api.BusPosition;
import th.in.whs.ku.bus.api.BusStopList;
import th.in.whs.ku.bus.map.BusMapFragment;
import th.in.whs.ku.bus.map.Filter;
import th.in.whs.ku.bus.util.BusColor;
import th.in.whs.ku.bus.util.ListenerList;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
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

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

public class MapFragment extends Fragment implements OnItemSelectedListener {
	
	private BusPositionUpdatedListener listener = new BusPositionUpdatedListener();
	private int listenerId = -1;
	private BusStopAdapter adapter;
	private BusMapFragment map;
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
		FragmentManager manager = getChildFragmentManager();
		if(manager.findFragmentById(R.id.map) == null){
			FragmentTransaction tx = manager.beginTransaction();
			
			GoogleMapOptions options = new GoogleMapOptions()
				.camera(new CameraPosition(new LatLng(13.847f, 100.572238f), 14.5f, 0, 0));
			
			Bundle bundle = new Bundle();
			bundle.putParcelable("option", options);
			
			map = new BusMapFragment();
			map.setArguments(bundle);
			tx.replace(R.id.map, map);
			tx.commitAllowingStateLoss();
		}else{
			map = (BusMapFragment) manager.findFragmentById(R.id.map);
		}
		
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
		
		((KuBusApplication) getActivity().getApplication()).report("BusMapFragment");
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
		unloadEvents();
	}
	
	@Override
	public void onPause(){
		super.onPause();
		BusPosition.removeUpdateListener(listenerId);
	}
	
	@Override
	public void onResume(){
		super.onResume();
		listenerId = BusPosition.registerUpdateListener(listener, true);
	}
	
	public void refresh() {
		getView().findViewById(R.id.progress).setVisibility(View.VISIBLE);
		BusPosition.refresh();
	}

	private void unloadEvents(){
		if(listenerId != -1){
			BusPosition.removeUpdateListener(listenerId);
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
				colorRes = getResources().getColor(BusColor.getColor(line.index));
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
		
		Tracker t = ((KuBusApplication) getActivity().getApplication()).getTracker();
        t.send(new HitBuilders.EventBuilder()
            .setCategory("Map")
            .setAction("Filter line")
            .setLabel(line.name)
            .build());
		
		if(line.isAll()){
			map.clearFilter();
			map.clearPolyline();
			map.setFilterActive(false);
			return;
		}
		map.drawPolyline(line.index);
		map.clearFilter();
		map.addFilter(new Filter(Filter.FilterType.LINE, line.index));
		map.setFilterActive(true);
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {
		map.clearFilter();
		map.clearPolyline();
	}
}
