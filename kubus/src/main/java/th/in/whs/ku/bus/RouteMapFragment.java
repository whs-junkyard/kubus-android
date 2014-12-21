package th.in.whs.ku.bus;

import java.util.ArrayList;

import th.in.whs.ku.bus.map.BusMapFragment;
import th.in.whs.ku.bus.map.Filter;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

public class RouteMapFragment extends Fragment {
	private static final int VIEW_ID = 415618165;
	private BusMapFragment map;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		FrameLayout out = new FrameLayout(inflater.getContext());
		out.setId(VIEW_ID);
		
		FragmentManager manager = getChildFragmentManager();
		
		if(manager.findFragmentById(VIEW_ID) == null){
			GoogleMapOptions options = new GoogleMapOptions();
			options.camera(
				new CameraPosition.Builder()
					.target(new LatLng(13.848914, 100.572238))
					.tilt(45)
					.zoom(14.5f)
					.build()
			);
			Bundle bundle = new Bundle();
			bundle.putParcelable("option", options);
			bundle.putBoolean("allowBusStopClick", false);
			bundle.putBoolean("filterActive", true);
			
			Bundle arguments = getArguments();
			bundle.putBoolean("filterActive", true);
			bundle.putBoolean("drawPolylineRoute", true);
			bundle.putStringArray("filterActive",
				new String[]{
					String.valueOf(arguments.getInt("line")),
					arguments.getString("from"),
					arguments.getString("to")
				}
			);
			ArrayList<Parcelable> filters = new ArrayList<Parcelable>();
			filters.add(new Filter(
					Filter.FilterType.BUS,
					arguments.getInt("bus")
			));
			bundle.putParcelableArrayList("filter", filters);
			
			map = new BusMapFragment();
			map.setArguments(bundle);
			FragmentTransaction ft = manager.beginTransaction();
			ft.replace(VIEW_ID, map);
			ft.commit();
		}else{
			map = (BusMapFragment) manager.findFragmentById(VIEW_ID);
		}
		
		return out;
	}
}
