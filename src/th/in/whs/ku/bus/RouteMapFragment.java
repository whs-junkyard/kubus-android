package th.in.whs.ku.bus;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

public class RouteMapFragment extends Fragment {
	private BusMapController controller;
	private SupportMapFragment map;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if(map == null){
			GoogleMapOptions options = new GoogleMapOptions();
			options.camera(
				new CameraPosition.Builder()
					.target(new LatLng(13.848914, 100.572238))
					.tilt(45)
					.zoom(14.5f)
					.build()
			);
			map = SupportMapFragment.newInstance(options);
			FragmentManager manager = this.getChildFragmentManager();
			FragmentTransaction ft = manager.beginTransaction();
			ft.add(android.R.id.content, map, "Map");
			ft.commit();
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		ViewGroup out = new FrameLayout(inflater.getContext());
		out.setId(android.R.id.content);
		return out;
	}

	@Override
	public void onResume() {
		super.onResume();
		
		if(controller == null && map.getMap() != null){
			map.getMap().setMyLocationEnabled(true);
			controller = new BusMapController(getActivity().getApplicationContext(), map.getMap());
			controller.setAllowBusStopClick(false);
			controller.registerListener();
			
			Bundle arguments = getArguments();
			controller.addFilter(new BusMapController.Filter(BusMapController.Filter.FilterType.BUS, arguments.getInt("bus")));
			controller.setFilterActive(true);
			controller.drawPolyline(String.valueOf(arguments.getInt("line")), arguments.getString("from"), arguments.getString("to"));
		}
		if(controller != null){
			controller.onResume();
		}
	}
	
	@Override
	public void onPause(){
		super.onPause();
		if(controller != null){
			controller.onPause();
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if(controller != null){
			controller.unregisterListener();
		}
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		controller.onLowMemory();
	}
}
