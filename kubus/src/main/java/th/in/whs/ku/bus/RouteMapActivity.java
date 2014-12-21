package th.in.whs.ku.bus;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;

import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

public class RouteMapActivity extends ActionBarActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		overridePendingTransition(R.anim.abc_slide_in_bottom, R.anim.abc_slide_out_bottom);
		
		if(this.getSupportFragmentManager().findFragmentById(android.R.id.content) != null){
			return;
		}
		
		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		
		Fragment fragment = new RouteMapFragment();
		fragment.setArguments(extras);
		
		FragmentTransaction ft = this.getSupportFragmentManager().beginTransaction();
		ft.add(android.R.id.content, fragment);
		ft.commit();
		
		((KuBusApplication) getApplication()).report("RouteMapActivity");
	}
	
}
