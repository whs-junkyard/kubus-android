package th.in.whs.ku.bus;

import org.json.JSONException;
import org.json.JSONObject;

import th.in.whs.ku.bus.BusStopListFragment.Sort;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;

/**
 * Clone of BusStopListActivity with ActionBar disabled
 * https://code.google.com/p/android/issues/detail?id=61025
 */
public class BusStopListGBActivity extends FragmentActivity implements StopSelectedInterface {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.w("BusStopListGBActivity", "Using Gingerbread support version of activity");
		overridePendingTransition(R.anim.abc_slide_in_bottom, R.anim.abc_slide_out_bottom);
		
		Bundle bundle = new Bundle();
		
		boolean closest = getIntent().getBooleanExtra("returnClosest", false);
		if(closest){
			bundle.putBoolean("returnClosest", closest);
		}
		
		FragmentManager manager = this.getSupportFragmentManager();
		if(manager.findFragmentByTag("BusList") == null){
			Fragment fragment = new BusStopListFragment();
			fragment.setArguments(bundle);
			
			FragmentTransaction ft = this.getSupportFragmentManager().beginTransaction();
			ft.add(closest ? 0 : android.R.id.content, fragment, "BusList");
			ft.commit();
		}
		
		int title = R.string.select_stop;
		int type = getIntent().getIntExtra("type", -1);
		switch(type){
		case 0:
			title = R.string.select_stop_from;
			break;
		case 1:
			title = R.string.select_stop_to;
			break;
		default:
		}
		setTitle(title);
	}

	@Override
	public void stopSelected(JSONObject item) {
		Intent returnIntent = new Intent();
		if(item != null){
			try {
				returnIntent.putExtra("name", item.getString("Name"));
				returnIntent.putExtra("id", item.getString("ID"));
			} catch (JSONException e) {
				finish();
				return;
			}
			setResult(RESULT_OK, returnIntent);
		}else{
			setResult(RESULT_CANCELED, returnIntent);
		}
		finish();
		overridePendingTransition(R.anim.abc_slide_in_bottom, R.anim.abc_slide_out_bottom);
	}
	
}
