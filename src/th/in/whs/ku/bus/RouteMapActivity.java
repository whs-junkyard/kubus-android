package th.in.whs.ku.bus;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;

import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

public class RouteMapActivity extends ActionBarActivity {
	private BusMapController controller;
	private SupportMapFragment map;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		this.overridePendingTransition(R.anim.abc_slide_in_bottom, R.anim.abc_slide_out_bottom);
		
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
			FragmentManager manager = this.getSupportFragmentManager();
			FragmentTransaction ft = manager.beginTransaction();
			ft.add(android.R.id.content, map, "Map");
			ft.commit();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		if(controller == null && map.getMap() != null){
			map.getMap().setMyLocationEnabled(true);
			controller = new BusMapController(this, map.getMap());
			controller.setAllowBusStopClick(false);
			controller.registerListener();
			
			Intent intent = getIntent();
			Bundle extras = intent.getExtras();
			controller.addFilter(new BusMapController.Filter(BusMapController.Filter.FilterType.BUS, extras.getInt("bus")));
			controller.setFilterActive(true);
			controller.drawPolyline(String.valueOf(extras.getInt("line")), extras.getString("from"), extras.getString("to"));
		}
		if(controller != null){
			controller.onResume();
		}
	}
	
	@Override
	protected void onPause(){
		super.onPause();
		if(controller != null){
			controller.onPause();
		}
	}

	@Override
	protected void onDestroy() {
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
