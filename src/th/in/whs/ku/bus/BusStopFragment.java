package th.in.whs.ku.bus;

import org.json.JSONException;
import org.json.JSONObject;

import com.joshdholtz.sentry.Sentry;

import th.in.whs.ku.bus.api.NFCBuilder;
import android.content.Context;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class BusStopFragment extends Fragment implements StopSelectedInterface, UserRefreshInterface {
	
	/**
	 * Used when asking stopSelected
	 * but view is missing
	 */
	private JSONObject showStop;

	public void setShowStop(JSONObject showStop) {
		this.showStop = showStop;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.twopane, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		if(getChildFragmentManager().findFragmentById(R.id.left) == null){
			FragmentTransaction ft = getChildFragmentManager().beginTransaction();
			ft.add(R.id.left, new BusStopListFragment(), "BusStopList");
			ft.commit();
		}
		setHasOptionsMenu(true);
	}

	@Override
	public void stopSelected(JSONObject item) {
		if(getView() == null){
			showStop = item;
			return;
		}
		boolean hasRight = getView().findViewById(R.id.right) != null;
		Fragment stopInfo = new BusStopInfoFragment();
		Bundle params = new Bundle();
		params.putString("data", item.toString());
		stopInfo.setArguments(params);
		
		FragmentTransaction ft = getChildFragmentManager().beginTransaction();
		ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
		ft.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, android.R.anim.slide_in_left, android.R.anim.slide_out_right);

		if(hasRight){
			Log.d("BusStopFragment", "Attaching BusStopInfo to right");
			ft.replace(R.id.right, stopInfo, "BusStopInfo");
		}else{
			Log.d("BusStopFragment", "Attaching BusStopInfo to left");
			ft.replace(R.id.left, stopInfo, "BusStopInfo");
			try {
				ft.addToBackStack(item.getString("Name"));
			} catch (JSONException e) {
				ft.addToBackStack(null);
			}
		}
		ft.commitAllowingStateLoss();
		
		try {
			if(getActivity() instanceof NFCSettableActivity && Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH){
				((NFCSettableActivity) getActivity()).setNFC(
						NFCBuilder.createMessage(getActivity(), Uri.parse("kubus://stop/" + item.get("ID")))
				);
			}
		} catch (JSONException e) {
		} catch (NoSuchMethodError e){
			// I'm not sure why this happen...
			// http://sentry.whs.in.th/kusmartbus/android/group/131/
			Sentry.captureException(e, Sentry.SentryEventBuilder.SentryEventLevel.WARNING);
		}
	}

	@Override
	public void onRefresh() {
		UserRefreshInterface leftFragment = (UserRefreshInterface) getChildFragmentManager().findFragmentById(R.id.left);
		if(leftFragment != null){
			leftFragment.onRefresh();
		}
		UserRefreshInterface rightFragment = (UserRefreshInterface) getChildFragmentManager().findFragmentById(R.id.right);
		if(rightFragment != null){
			rightFragment.onRefresh();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if(showStop != null){
			stopSelected(showStop);
			showStop = null;
		}
	}
}
